package domain.model.optimizer

import data.parser.xdf.TableDefinition
import domain.math.map.Map3d
import domain.model.rlsol.Rlsol
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * KFPRG Solver: Finds optimal per-RPM residual gas partial pressure values from logs.
 *
 * KFPRG is the internal exhaust partial pressure offset (hPa) in the BGSRM VE model.
 * It represents the manifold pressure at which cylinder filling (rl) = 0.
 * The BGSRM formula (me7-raw.txt line 54297–54327):
 *
 *   rl = fupsrl_w × (ps - pirg_w) × FPBRKDS
 *   where pirg_w = KFPRG(nmot, wnw) × fho_w
 *
 * Calibration method (me7-raw.txt line 54393):
 *   "ps-Offset KFPRG (bei rl = 0) und Geradensteigung KFURL ermitteln."
 *   (At rl = 0, determine ps-offset KFPRG, then determine slope KFURL.)
 *
 * The solver minimizes the RMSE between actual load (rl_w from MAF) and
 * VE-model-predicted load (from measured pressure via Rlsol) by searching
 * for the KFPRG value that best fits the data.
 *
 * XDF: "Internal exhaust partial pressure dependent on cam adjustment when sumode=0"
 * Typical range: 30–150 hPa, varies with RPM and cam angle.
 *
 * @see documentation/me7-alpha-n-calibration.md §Priority 2
 * @see documentation/me7-boost-control.md §3
 */
object KfprgSolver {

    /** Default KFPRG when no data is available (stock 1.8T typical value) */
    const val DEFAULT_KFPRG = 70.0

    /**
     * Auto-read KFPRG from the BIN/XDF map list.
     * KFPRG is a 2D map (RPM × cam angle → hPa) or may be stored as a 1D Kennlinie.
     *
     * @return The KFPRG value as a scalar (first value), or null if not found.
     */
    fun findKfprgFromBin(mapList: List<Pair<TableDefinition, Map3d>>): Double? {
        val kfprgMap = findKfprgMapFromBin(mapList)
        if (kfprgMap != null) {
            val z = kfprgMap.zAxis
            if (z.isNotEmpty() && z[0].isNotEmpty()) {
                // Return the average of the first row (across RPM at first cam position)
                return z[0].average()
            }
        }
        return null
    }

    /**
     * Auto-read KFPRG as a full Map3d from BIN/XDF.
     *
     * @return The KFPRG Map3d, or null if not found.
     */
    fun findKfprgMapFromBin(mapList: List<Pair<TableDefinition, Map3d>>): Map3d? {
        val kfprgPair = mapList.find {
            it.first.tableName.equals("KFPRG", ignoreCase = true)
        }
        return kfprgPair?.second
    }

    /**
     * Look up KFPRG at a given RPM from a KFPRG map.
     * KFPRG is indexed by RPM on x-axis and cam angle on y-axis.
     * For engines without variable cam timing, uses the first y-axis value.
     *
     * @param kfprgMap The KFPRG Map3d from the BIN
     * @param rpm Engine RPM
     * @param camAngle Cam angle in °KW (default 0.0 for non-variable cam)
     * @return KFPRG value in hPa
     */
    fun lookupKfprg(kfprgMap: Map3d?, rpm: Double, camAngle: Double = 0.0): Double {
        if (kfprgMap == null) return DEFAULT_KFPRG
        if (kfprgMap.xAxis.isEmpty() || kfprgMap.yAxis.isEmpty()) return DEFAULT_KFPRG

        // KFPRG XDF: x=RPM, y=cam°KW → hPa
        return kfprgMap.lookup(rpm, camAngle)
    }

    /**
     * Solve for a single best-fit KFPRG scalar across all WOT samples.
     *
     * Grid-searches KFPRG over the given range, computing the VE model load
     * prediction at each candidate value and measuring RMSE against actual load.
     *
     * @param wotEntries WOT log entries with actualMap (pvdks_w), actualLoad (rl_w)
     * @param kfurl KFURL value to use (scalar or per-RPM average)
     * @param kfpbrkMap Optional KFPBRK map for per-cell VE correction
     * @param searchRange KFPRG search range in hPa
     * @param steps Number of grid points to evaluate
     * @return SolverResult with optimal KFPRG, RMSE, and sensitivity data
     */
    fun solve(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        kfurl: Double = 0.106,
        kfpbrkMap: Map3d? = null,
        searchRange: ClosedFloatingPointRange<Double> = 20.0..200.0,
        steps: Int = 180
    ): SolverResult {
        if (wotEntries.isEmpty()) {
            return SolverResult(
                optimalKfprg = DEFAULT_KFPRG,
                rmse = Double.MAX_VALUE,
                errorReductionPercent = 0.0,
                sensitivityCurve = emptyList(),
                perRpmKfprg = null
            )
        }

        val scalar = solveScalar(wotEntries, kfurl, kfpbrkMap, searchRange, steps)

        // Also solve per-RPM if enough data
        val perRpmResult = solvePerRpm(wotEntries, kfurl, kfpbrkMap, searchRange, steps)

        return SolverResult(
            optimalKfprg = scalar.optimalKfprg,
            rmse = scalar.rmse,
            errorReductionPercent = scalar.errorReductionPercent,
            sensitivityCurve = scalar.sensitivityCurve,
            perRpmKfprg = perRpmResult
        )
    }

    private data class ScalarSolveResult(
        val optimalKfprg: Double,
        val rmse: Double,
        val errorReductionPercent: Double,
        val sensitivityCurve: List<Pair<Double, Double>>
    )

    private fun solveScalar(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        kfurl: Double,
        kfpbrkMap: Map3d?,
        searchRange: ClosedFloatingPointRange<Double>,
        steps: Int
    ): ScalarSolveResult {
        var bestKfprg = DEFAULT_KFPRG
        var bestRmse = Double.MAX_VALUE
        val sensitivityData = mutableListOf<Pair<Double, Double>>()

        for (i in 0..steps) {
            val kfprg = searchRange.start + (searchRange.endInclusive - searchRange.start) * i / steps
            val rmse = computeRmse(wotEntries, kfurl, kfprg, kfpbrkMap)
            sensitivityData.add(kfprg to rmse)

            if (rmse < bestRmse) {
                bestRmse = rmse
                bestKfprg = kfprg
            }
        }

        // RMSE at default KFPRG for comparison
        val defaultRmse = computeRmse(wotEntries, kfurl, DEFAULT_KFPRG, kfpbrkMap)
        val errorReduction = if (defaultRmse > 0) {
            ((defaultRmse - bestRmse) / defaultRmse) * 100.0
        } else 0.0

        return ScalarSolveResult(bestKfprg, bestRmse, errorReduction, sensitivityData)
    }

    /**
     * Solve for optimal KFPRG at each RPM breakpoint independently.
     *
     * KFPRG varies with RPM: at higher RPM, less time for exhaust gas to clear
     * means potentially more residual gas → higher KFPRG. At low RPM with more
     * valve overlap, residual gas is also high.
     *
     * @return PerRpmKfprgResult with per-breakpoint values, or null if insufficient data.
     */
    fun solvePerRpm(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        kfurl: Double = 0.106,
        kfpbrkMap: Map3d? = null,
        searchRange: ClosedFloatingPointRange<Double> = 20.0..200.0,
        steps: Int = 180,
        rpmBinWidth: Double = 500.0
    ): PerRpmKfprgResult? {
        if (wotEntries.size < 20) return null

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

        // Solve scalar for comparison
        val scalarResult = solveScalar(wotEntries, kfurl, kfpbrkMap, searchRange, steps)

        val perRpmValues = mutableListOf<Pair<Double, Double>>()
        var totalPerRpmSqError = 0.0
        var totalScalarSqError = 0.0
        var totalSamples = 0

        for (rpmCenter in rpmBreakpoints) {
            val binEntries = wotEntries.filter {
                abs(it.rpm - rpmCenter) < rpmBinWidth / 2
            }
            if (binEntries.size < 3) {
                perRpmValues.add(rpmCenter to scalarResult.optimalKfprg)
                continue
            }

            var bestKfprg = scalarResult.optimalKfprg
            var bestSqError = Double.MAX_VALUE

            for (i in 0..steps) {
                val kfprg = searchRange.start + (searchRange.endInclusive - searchRange.start) * i / steps
                var sqError = 0.0
                for (entry in binEntries) {
                    val kfpbrkValue = kfpbrkMap?.lookup(entry.actualLoad, entry.rpm) ?: 1.016
                    val predictedRl = Rlsol.rlsol(
                        pu = entry.barometricPressure,
                        ps = entry.barometricPressure,
                        tans = 20.0,
                        tmot = 96.0,
                        kfurl = kfurl,
                        plsol = entry.actualMap,
                        kfprg = kfprg,
                        fpbrkds = kfpbrkValue
                    )
                    val err = predictedRl - entry.actualLoad
                    sqError += err * err
                }
                if (sqError < bestSqError) {
                    bestSqError = sqError
                    bestKfprg = kfprg
                }
            }

            perRpmValues.add(rpmCenter to bestKfprg)
            totalPerRpmSqError += bestSqError
            totalSamples += binEntries.size

            // Scalar error for this bin
            for (entry in binEntries) {
                val kfpbrkValue = kfpbrkMap?.lookup(entry.actualLoad, entry.rpm) ?: 1.016
                val predictedRl = Rlsol.rlsol(
                    pu = entry.barometricPressure,
                    ps = entry.barometricPressure,
                    tans = 20.0,
                    tmot = 96.0,
                    kfurl = kfurl,
                    plsol = entry.actualMap,
                    kfprg = scalarResult.optimalKfprg,
                    fpbrkds = kfpbrkValue
                )
                val err = predictedRl - entry.actualLoad
                totalScalarSqError += err * err
            }
        }

        if (totalSamples == 0) return null

        val perRpmRmse = sqrt(totalPerRpmSqError / totalSamples)
        val scalarRmse = sqrt(totalScalarSqError / totalSamples)
        val improvement = if (scalarRmse > 0) {
            ((scalarRmse - perRpmRmse) / scalarRmse) * 100.0
        } else 0.0

        return PerRpmKfprgResult(
            rpmValues = perRpmValues,
            perRpmRmse = perRpmRmse,
            scalarRmse = scalarRmse,
            improvementPercent = improvement
        )
    }

    /**
     * Compute RMSE between VE-model-predicted load and actual load for a given KFPRG.
     */
    private fun computeRmse(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        kfurl: Double,
        kfprg: Double,
        kfpbrkMap: Map3d?
    ): Double {
        if (wotEntries.isEmpty()) return Double.MAX_VALUE

        var totalSquaredError = 0.0
        for (entry in wotEntries) {
            val kfpbrkValue = kfpbrkMap?.lookup(entry.actualLoad, entry.rpm) ?: 1.016
            val predictedRl = Rlsol.rlsol(
                pu = entry.barometricPressure,
                ps = entry.barometricPressure,
                tans = 20.0,
                tmot = 96.0,
                kfurl = kfurl,
                plsol = entry.actualMap,
                kfprg = kfprg,
                fpbrkds = kfpbrkValue
            )
            val err = predictedRl - entry.actualLoad
            totalSquaredError += err * err
        }
        return sqrt(totalSquaredError / wotEntries.size)
    }

    // ── Result data classes ─────────────────────────────────────────

    data class SolverResult(
        val optimalKfprg: Double,
        val rmse: Double,
        val errorReductionPercent: Double,
        /** KFPRG value → RMSE — for charting sensitivity. */
        val sensitivityCurve: List<Pair<Double, Double>>,
        /** Per-RPM optimal KFPRG values, or null if insufficient data. */
        val perRpmKfprg: PerRpmKfprgResult?
    )

    /**
     * Result of per-RPM KFPRG solving.
     * Demonstrates that KFPRG varies with RPM (me7-raw.txt line 54365).
     */
    data class PerRpmKfprgResult(
        /** RPM → optimal KFPRG at that RPM (hPa) */
        val rpmValues: List<Pair<Double, Double>>,
        /** RMSE when using per-RPM KFPRG values (% load) */
        val perRpmRmse: Double,
        /** RMSE when using a single scalar KFPRG (% load) */
        val scalarRmse: Double,
        /** Improvement of per-RPM over scalar (%) */
        val improvementPercent: Double
    )
}

