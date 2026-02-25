package domain.model.optimizer

import domain.model.plsol.Plsol
import domain.math.map.Map3d
import data.parser.xdf.TableDefinition

/**
 * Phase 22: Auto-read KFURL from BIN/XDF and solve for best-fit value.
 *
 * KFURL is the slope of the rl(ps) characteristic — the single most important
 * VE constant for simulation accuracy. This solver finds the value that
 * minimizes pssol error across all WOT samples.
 *
 * @see documentation/me7-boost-control.md §3
 * @see documentation/me7-maps-reference.md — KFURL entry
 */
object KfurlSolver {

    /**
     * Auto-read KFURL from the BIN/XDF map list.
     * KFURL may be stored as a 1×1 map (scalar), a 1×N Kennlinie, or named table.
     *
     * @return The KFURL value, or null if not found in the map list.
     */
    fun findKfurlFromBin(mapList: List<Pair<TableDefinition, Map3d>>): Double? {
        // Try exact name match first
        val kfurlMap = mapList.find {
            it.first.tableName.equals("KFURL", ignoreCase = true) ||
                it.first.tableName.contains("KFURL", ignoreCase = true)
        }

        if (kfurlMap != null) {
            val z = kfurlMap.second.zAxis
            if (z.isNotEmpty() && z[0].isNotEmpty()) {
                // For a scalar or small map, return the first value
                // (KFURL is typically a single scalar or a 1D curve vs RPM)
                return z[0][0]
            }
        }
        return null
    }

    /**
     * Solve for the KFURL that minimizes pssol error across WOT samples.
     *
     * For each WOT entry: error = |computePssol(rlsol, kfurl) − logged_pssol|
     * Grid search over kfurl range to find the minimum total squared error.
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
            return SolverResult(0.106, Double.MAX_VALUE, 0.0, emptyList())
        }

        var bestKfurl = 0.106
        var bestRmse = Double.MAX_VALUE
        val sensitivityData = mutableListOf<Pair<Double, Double>>() // kfurl → RMSE

        for (i in 0..steps) {
            val kfurl = searchRange.start + (searchRange.endInclusive - searchRange.start) * i / steps
            var totalSquaredError = 0.0

            for (entry in wotEntries) {
                val simPssol = Plsol.plsol(
                    entry.barometricPressure,
                    entry.barometricPressure,
                    20.0, 96.0, // standard conditions
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

        // Compute error reduction vs default 0.106
        val defaultRmse = sensitivityData.minByOrNull {
            kotlin.math.abs(it.first - 0.106)
        }?.second ?: bestRmse

        val errorReduction = if (defaultRmse > 0) {
            ((defaultRmse - bestRmse) / defaultRmse) * 100.0
        } else 0.0

        return SolverResult(
            optimalKfurl = bestKfurl,
            rmse = bestRmse,
            errorReductionPercent = errorReduction,
            sensitivityCurve = sensitivityData
        )
    }

    data class SolverResult(
        val optimalKfurl: Double,
        val rmse: Double,
        val errorReductionPercent: Double,
        /** kfurl value → RMSE — for charting KFURL sensitivity. */
        val sensitivityCurve: List<Pair<Double, Double>>
    )
}

