package domain.model.optimizer

import domain.model.plsol.Plsol
import domain.math.map.Map3d
import data.parser.xdf.TableDefinition

/**
 * Phase 22: Auto-read KFURL from BIN/XDF and solve for best-fit value.
 *
 * KFURL is the slope of the rl(ps) characteristic — the single most important
 * VE constant for simulation accuracy.
 *
 * **Finding 2 (me7-raw.txt line 53923, 54368):** KFURL is a Kennlinie (1D curve
 * indexed by RPM), not a scalar. Values typically 0.105–0.142 %/hPa, decreasing
 * with RPM. This solver now supports both scalar and per-RPM solving.
 *
 * @see documentation/me7-boost-control.md §3
 * @see documentation/optimizer-algorithms.md Finding 2
 */
object KfurlSolver {

    /**
     * Auto-read KFURL from the BIN/XDF map list.
     * KFURL may be stored as a 1×1 map (scalar), a 1×N Kennlinie, or named table.
     *
     * @return The KFURL value as a scalar (first value), or null if not found.
     */
    fun findKfurlFromBin(mapList: List<Pair<TableDefinition, Map3d>>): Double? {
        val kfurlMap = findKfurlMapFromBin(mapList)
        if (kfurlMap != null) {
            val z = kfurlMap.zAxis
            if (z.isNotEmpty() && z[0].isNotEmpty()) {
                return z[0][0]
            }
        }
        return null
    }

    /**
     * Auto-read KFURL as a full Map3d (Kennlinie) from BIN/XDF.
     * Returns the full map which may be 1×1 (scalar), 1×N, or N×1 (RPM curve).
     *
     * @return The KFURL Map3d, or null if not found.
     */
    fun findKfurlMapFromBin(mapList: List<Pair<TableDefinition, Map3d>>): Map3d? {
        val kfurlPair = mapList.find {
            it.first.tableName.equals("KFURL", ignoreCase = true) ||
                it.first.tableName.contains("KFURL", ignoreCase = true)
        }
        return kfurlPair?.second
    }

    /**
     * Internal scalar-only solve (no per-RPM, no recursion).
     * Used by both [solve] and [solvePerRpm] to avoid infinite recursion.
     */
    private data class ScalarSolveResult(
        val optimalKfurl: Double,
        val rmse: Double,
        val errorReductionPercent: Double,
        val sensitivityCurve: List<Pair<Double, Double>>
    )

    private fun solveScalar(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        searchRange: ClosedFloatingPointRange<Double>,
        steps: Int
    ): ScalarSolveResult {
        var bestKfurl = 0.106
        var bestRmse = Double.MAX_VALUE
        val sensitivityData = mutableListOf<Pair<Double, Double>>()

        for (i in 0..steps) {
            val kfurl = searchRange.start + (searchRange.endInclusive - searchRange.start) * i / steps
            var totalSquaredError = 0.0

            for (entry in wotEntries) {
                // Use requestedMap as ps for rfagr calculation — at WOT under boost,
                // manifold pressure ≈ pvdks >> baro. Using baro makes rfagr ~2x too large,
                // which shifts the optimal KFURL. requestedMap (pssol_w) is the ECU's
                // own pressure estimate, close to actual at WOT.
                val simPssol = Plsol.plsol(
                    entry.barometricPressure,
                    entry.requestedMap,
                    20.0, 96.0,
                    kfurl,
                    entry.requestedLoad
                )
                val error = simPssol - entry.requestedMap
                totalSquaredError += error * error
            }

            val rmse = kotlin.math.sqrt(totalSquaredError / wotEntries.size)
            sensitivityData.add(kfurl to rmse)

            if (rmse < bestRmse) {
                bestRmse = rmse
                bestKfurl = kfurl
            }
        }

        val defaultRmse = sensitivityData.minByOrNull {
            kotlin.math.abs(it.first - 0.106)
        }?.second ?: bestRmse

        val errorReduction = if (defaultRmse > 0) {
            ((defaultRmse - bestRmse) / defaultRmse) * 100.0
        } else 0.0

        return ScalarSolveResult(bestKfurl, bestRmse, errorReduction, sensitivityData)
    }

    /**
     * Solve for a single best-fit KFURL scalar across all WOT samples,
     * plus per-RPM results if sufficient data exists.
     *
     * @param wotEntries WOT log entries with requestedLoad and requestedMap
     * @param searchRange Range to search for KFURL
     * @param steps Number of grid points to evaluate
     * @return SolverResult with optimal kfurl, error metrics, and sensitivity data
     */
    fun solve(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        searchRange: ClosedFloatingPointRange<Double> = 0.050..0.200,
        steps: Int = 150
    ): SolverResult {
        if (wotEntries.isEmpty()) {
            return SolverResult(0.106, Double.MAX_VALUE, 0.0, emptyList(), null)
        }

        val scalar = solveScalar(wotEntries, searchRange, steps)

        // Also solve per-RPM if we have enough data
        val perRpmResult = solvePerRpm(wotEntries, searchRange, steps)

        return SolverResult(
            optimalKfurl = scalar.optimalKfurl,
            rmse = scalar.rmse,
            errorReductionPercent = scalar.errorReductionPercent,
            sensitivityCurve = scalar.sensitivityCurve,
            perRpmKfurl = perRpmResult
        )
    }

    /**
     * Solve for optimal KFURL at each RPM breakpoint independently.
     *
     * ME7 Reference (line 54368): KFURL varies with RPM — typically 0.105–0.142 %/hPa,
     * decreasing with increasing RPM. This method finds the per-RPM optimal values.
     *
     * @return PerRpmKfurlResult with per-breakpoint values and overall RMSE improvement,
     *         or null if insufficient data.
     */
    fun solvePerRpm(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        searchRange: ClosedFloatingPointRange<Double> = 0.050..0.200,
        steps: Int = 150,
        rpmBinWidth: Double = 500.0
    ): PerRpmKfurlResult? {
        if (wotEntries.size < 20) return null

        // Determine RPM breakpoints from data
        val minRpm = wotEntries.minOf { it.rpm }
        val maxRpm = wotEntries.maxOf { it.rpm }
        if (maxRpm - minRpm < rpmBinWidth) return null

        val rpmBreakpoints = mutableListOf<Double>()
        var r = (minRpm / rpmBinWidth).toInt() * rpmBinWidth + rpmBinWidth / 2
        while (r <= maxRpm + rpmBinWidth / 2) {
            rpmBreakpoints.add(r)
            r += rpmBinWidth
        }

        if (rpmBreakpoints.size < 2) return null

        val perRpmValues = mutableListOf<Pair<Double, Double>>() // rpm → optimal kfurl
        var totalPerRpmSqError = 0.0
        var totalScalarSqError = 0.0
        var totalSamples = 0

        // Solve scalar for comparison (no recursion — uses solveScalar directly)
        val scalarResult = solveScalar(wotEntries, searchRange, steps)

        for (rpmCenter in rpmBreakpoints) {
            val binEntries = wotEntries.filter {
                kotlin.math.abs(it.rpm - rpmCenter) < rpmBinWidth / 2
            }
            if (binEntries.size < 3) {
                perRpmValues.add(rpmCenter to scalarResult.optimalKfurl)
                continue
            }

            var bestKfurl = scalarResult.optimalKfurl
            var bestSqError = Double.MAX_VALUE

            for (i in 0..steps) {
                val kfurl = searchRange.start + (searchRange.endInclusive - searchRange.start) * i / steps
                var sqError = 0.0
                for (entry in binEntries) {
                    val simPssol = Plsol.plsol(
                        entry.barometricPressure, entry.requestedMap,
                        20.0, 96.0, kfurl, entry.requestedLoad
                    )
                    val err = simPssol - entry.requestedMap
                    sqError += err * err
                }
                if (sqError < bestSqError) {
                    bestSqError = sqError
                    bestKfurl = kfurl
                }
            }

            perRpmValues.add(rpmCenter to bestKfurl)
            totalPerRpmSqError += bestSqError
            totalSamples += binEntries.size

            // Scalar error for this bin
            for (entry in binEntries) {
                val simPssol = Plsol.plsol(
                    entry.barometricPressure, entry.requestedMap,
                    20.0, 96.0, scalarResult.optimalKfurl, entry.requestedLoad
                )
                val err = simPssol - entry.requestedMap
                totalScalarSqError += err * err
            }
        }

        if (totalSamples == 0) return null

        val perRpmRmse = kotlin.math.sqrt(totalPerRpmSqError / totalSamples)
        val scalarRmse = kotlin.math.sqrt(totalScalarSqError / totalSamples)
        val improvement = if (scalarRmse > 0) {
            ((scalarRmse - perRpmRmse) / scalarRmse) * 100.0
        } else 0.0

        return PerRpmKfurlResult(
            rpmValues = perRpmValues,
            perRpmRmse = perRpmRmse,
            scalarRmse = scalarRmse,
            improvementPercent = improvement
        )
    }

    data class SolverResult(
        val optimalKfurl: Double,
        val rmse: Double,
        val errorReductionPercent: Double,
        /** kfurl value → RMSE — for charting KFURL sensitivity. */
        val sensitivityCurve: List<Pair<Double, Double>>,
        /** Per-RPM optimal KFURL values, or null if insufficient data. */
        val perRpmKfurl: PerRpmKfurlResult?
    )

    /**
     * Result of per-RPM KFURL solving.
     * Demonstrates that KFURL varies with RPM (Finding 2).
     */
    data class PerRpmKfurlResult(
        /** RPM → optimal KFURL at that RPM */
        val rpmValues: List<Pair<Double, Double>>,
        /** RMSE when using per-RPM KFURL values */
        val perRpmRmse: Double,
        /** RMSE when using a single scalar KFURL */
        val scalarRmse: Double,
        /** Improvement of per-RPM over scalar (%) */
        val improvementPercent: Double
    )
}

