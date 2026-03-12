package domain.model.fueltrim

import domain.math.map.Map3d

/**
 * Result of analyzing MED17 fuel trim logs.
 *
 * @param rpmBins RPM axis values for the correction grid
 * @param loadBins Engine load (%) axis values for the correction grid
 * @param avgTrims Average combined fuel trim (%) per bin — `[rpmIdx][loadIdx]`
 * @param corrections Suggested relative fuel mass (rk_w) adjustment per bin — `[rpmIdx][loadIdx]`.
 *        Positive means add fuel, negative means remove fuel.
 * @param warnings Diagnostic messages (threshold violations, missing data, etc.)
 */
data class FuelTrimResult(
    val rpmBins: DoubleArray,
    val loadBins: DoubleArray,
    val avgTrims: Array<DoubleArray>,
    val corrections: Array<DoubleArray>,
    val warnings: List<String>
) {
    /** True when every correction bin is zero (nothing to adjust). */
    val isEmpty: Boolean
        get() = corrections.all { row -> row.all { it == 0.0 } }

    /** Average combined fuel trim as a Map3d (xAxis=load, yAxis=RPM). */
    fun toAvgTrimsMap3d(): Map3d = Map3d(
        loadBins.toTypedArray(),
        rpmBins.toTypedArray(),
        avgTrims.map { it.toTypedArray() }.toTypedArray()
    )

    /** Suggested rk_w corrections as a Map3d (xAxis=load, yAxis=RPM). */
    fun toCorrectionsMap3d(): Map3d = Map3d(
        loadBins.toTypedArray(),
        rpmBins.toTypedArray(),
        corrections.map { it.toTypedArray() }.toTypedArray()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FuelTrimResult) return false
        return rpmBins.contentEquals(other.rpmBins) &&
                loadBins.contentEquals(other.loadBins) &&
                avgTrims.zip(other.avgTrims).all { (a, b) -> a.contentEquals(b) } &&
                corrections.zip(other.corrections).all { (a, b) -> a.contentEquals(b) } &&
                warnings == other.warnings
    }

    override fun hashCode(): Int {
        var result = rpmBins.contentHashCode()
        result = 31 * result + loadBins.contentHashCode()
        result = 31 * result + avgTrims.contentDeepHashCode()
        result = 31 * result + corrections.contentDeepHashCode()
        result = 31 * result + warnings.hashCode()
        return result
    }
}
