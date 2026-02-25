package domain.model.optimizer

import domain.math.map.Map3d

/**
 * Per-cell delta and confidence information for a corrected map.
 *
 * Shows exactly what changed, by how much, and how confident we are
 * in each cell based on the number of WOT samples that contributed.
 */
data class MapDelta(
    val mapName: String,
    val current: Map3d,
    val suggested: Map3d,
    val delta: Map3d,              // suggested.z - current.z (absolute)
    val deltaPercent: Map3d,       // (suggested.z - current.z) / current.z * 100
    val sampleCounts: Array<IntArray>,
    val cellsWithData: Int,
    val totalCells: Int,
    val avgSamplesPerModifiedCell: Double
) {
    /** Fraction of cells that have log data backing them. */
    val coverage: Double get() = if (totalCells > 0) cellsWithData.toDouble() / totalCells else 0.0

    /** Number of cells that were actually changed (delta != 0). */
    val cellsModified: Int get() {
        var count = 0
        for (row in delta.zAxis) {
            for (v in row) {
                if (kotlin.math.abs(v) > 0.001) count++
            }
        }
        return count
    }

    /** Average absolute change across all modified cells. */
    val avgAbsoluteDelta: Double get() {
        val changes = mutableListOf<Double>()
        for (row in delta.zAxis) {
            for (v in row) {
                if (kotlin.math.abs(v) > 0.001) changes.add(kotlin.math.abs(v))
            }
        }
        return if (changes.isNotEmpty()) changes.average() else 0.0
    }

    /** Confidence level per cell: LOW (<5 samples), MEDIUM (5-20), HIGH (>20). */
    enum class Confidence { NONE, LOW, MEDIUM, HIGH }

    fun cellConfidence(rowIdx: Int, colIdx: Int): Confidence {
        if (rowIdx >= sampleCounts.size || colIdx >= sampleCounts[0].size) return Confidence.NONE
        val count = sampleCounts[rowIdx][colIdx]
        return when {
            count == 0 -> Confidence.NONE
            count < 5 -> Confidence.LOW
            count <= 20 -> Confidence.MEDIUM
            else -> Confidence.HIGH
        }
    }

    companion object {
        /**
         * Build a MapDelta from current and suggested maps plus sample counts.
         */
        fun build(
            mapName: String,
            current: Map3d,
            suggested: Map3d,
            sampleCounts: Array<IntArray>
        ): MapDelta {
            val rows = current.yAxis.size
            val cols = current.xAxis.size

            val deltaZ = Array(rows) { r ->
                Array(cols) { c ->
                    suggested.zAxis[r][c] - current.zAxis[r][c]
                }
            }

            val deltaPctZ = Array(rows) { r ->
                Array(cols) { c ->
                    val orig = current.zAxis[r][c]
                    if (kotlin.math.abs(orig) > 0.0001) {
                        (suggested.zAxis[r][c] - orig) / orig * 100.0
                    } else 0.0
                }
            }

            var cellsWithData = 0
            var totalSamples = 0
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (sampleCounts[r][c] > 0) {
                        cellsWithData++
                        totalSamples += sampleCounts[r][c]
                    }
                }
            }

            val totalCells = rows * cols
            val avgSamples = if (cellsWithData > 0) totalSamples.toDouble() / cellsWithData else 0.0

            return MapDelta(
                mapName = mapName,
                current = current,
                suggested = suggested,
                delta = Map3d(current.xAxis, current.yAxis, deltaZ),
                deltaPercent = Map3d(current.xAxis, current.yAxis, deltaPctZ),
                sampleCounts = sampleCounts,
                cellsWithData = cellsWithData,
                totalCells = totalCells,
                avgSamplesPerModifiedCell = avgSamples
            )
        }
    }
}

/**
 * Complete set of suggested map corrections with deltas and confidence.
 */
data class SuggestedMaps(
    val kfldrl: MapDelta? = null,
    val kfldimx: MapDelta? = null,
    val kfpbrk: MapDelta? = null,
    val kfmiop: MapDelta? = null,
    val kfmirl: MapDelta? = null
)

/**
 * Per-RPM analysis for a specific chain link.
 */
data class RpmBreakpointAnalysis(
    val rpm: Double,
    val sampleCount: Int,
    val avgError: Double,
    val maxError: Double,
    val correction: Double,
    val confidence: MapDelta.Confidence
)

/**
 * Prediction of what logs will look like after applying corrections.
 */
data class PredictionResult(
    val predictedPressureSeries: List<Pair<Double, Double>>,   // RPM → predicted pvdks
    val predictedLoadSeries: List<Pair<Double, Double>>,       // RPM → predicted rl_w
    val currentAvgLoadDeficit: Double,
    val predictedAvgLoadDeficit: Double,
    val currentAvgPressureError: Double,
    val predictedAvgPressureError: Double,
    val predictedChainHealth: OptimizerCalculator.ChainDiagnosis,
    val convergenceImprovement: Double  // % reduction in total error
)

/**
 * Summary of a single log file's contribution to the analysis.
 */
data class LogSummary(
    val fileName: String,
    val wotSampleCount: Int,
    val rpmRange: String,
    val avgPressureError: Double
)

