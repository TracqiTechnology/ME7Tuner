package domain.model.pfi

import data.contract.Med17LogFileContract
import data.contract.Med17LogFileContract.Header

/**
 * Result of an RPM-dependent PFI share calculation.
 *
 * @property rpmAxis          RPM breakpoints (sorted ascending)
 * @property pfiSharePercent  PFI share at each RPM breakpoint (0–100 %)
 * @property loggedRpmAxis    RPM breakpoints extracted from log data (null when no log was used)
 * @property loggedPfiPercent PFI share from log at each [loggedRpmAxis] point (null when no log)
 */
data class PfiShareResult(
    val rpmAxis: DoubleArray,
    val pfiSharePercent: DoubleArray,
    val loggedRpmAxis: DoubleArray? = null,
    val loggedPfiPercent: DoubleArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PfiShareResult) return false
        return rpmAxis.contentEquals(other.rpmAxis) &&
            pfiSharePercent.contentEquals(other.pfiSharePercent) &&
            loggedRpmAxis.contentNullEquals(other.loggedRpmAxis) &&
            loggedPfiPercent.contentNullEquals(other.loggedPfiPercent)
    }

    override fun hashCode(): Int {
        var h = rpmAxis.contentHashCode()
        h = 31 * h + pfiSharePercent.contentHashCode()
        h = 31 * h + (loggedRpmAxis?.contentHashCode() ?: 0)
        h = 31 * h + (loggedPfiPercent?.contentHashCode() ?: 0)
        return h
    }

    private fun DoubleArray?.contentNullEquals(other: DoubleArray?): Boolean = when {
        this == null && other == null -> true
        this != null && other != null -> contentEquals(other)
        else -> false
    }
}

/**
 * Computes RPM-dependent PFI share curves for MED17 dual-injection systems.
 *
 * MED17 2.5T (RS3 / TTRS) uses `InjSys_facPrtnPfi` (0 = pure GDI, 1 = pure PFI)
 * to control the port-vs-direct fuel split.  Real-world behaviour ramps PFI share
 * up towards torque peak, holds steady through the mid-range, then sharply declines
 * towards redline where GDI alone can support the required fuel mass.
 *
 * ## Typical default shape (RS3 2.5T)
 * ```
 *   RPM   1000  2000  3000  4000  4500  5000  5500  6000  6500  7000
 *   PFI%    30    40    50    58    60    60    60    55    35    20
 * ```
 */
object PfiShareCalculator {

    /** Default RPM axis covering idle → redline for the 2.5T 5-cylinder. */
    val DEFAULT_RPM_AXIS = doubleArrayOf(
        1000.0, 2000.0, 3000.0, 4000.0, 4500.0,
        5000.0, 5500.0, 6000.0, 6500.0, 7000.0
    )

    /** Default PFI share (%) matching [DEFAULT_RPM_AXIS]. */
    val DEFAULT_PFI_SHARE = doubleArrayOf(
        30.0, 40.0, 50.0, 58.0, 60.0,
        60.0, 60.0, 55.0, 35.0, 20.0
    )

    /**
     * Build an RPM-dependent PFI share curve.
     *
     * If [targetPfiShare] is `null` the [DEFAULT_PFI_SHARE] is linearly interpolated
     * onto [rpmAxis].  Otherwise [rpmAxis] and [targetPfiShare] must be the same length
     * and are returned directly after validation.
     *
     * @param rpmAxis         RPM breakpoints (must be sorted ascending, at least 1 element)
     * @param targetPfiShare  Desired PFI share (0–100 %) at each RPM point, or null for defaults
     * @return [PfiShareResult] with the computed curve
     */
    fun calculateRpmDependentShare(
        rpmAxis: DoubleArray = DEFAULT_RPM_AXIS,
        targetPfiShare: DoubleArray? = null
    ): PfiShareResult {
        require(rpmAxis.isNotEmpty()) { "RPM axis must not be empty" }
        require(isSortedAscending(rpmAxis)) { "RPM axis must be sorted ascending" }

        val share = if (targetPfiShare != null) {
            require(targetPfiShare.size == rpmAxis.size) {
                "targetPfiShare (${targetPfiShare.size}) must match rpmAxis (${rpmAxis.size})"
            }
            DoubleArray(rpmAxis.size) { i -> targetPfiShare[i].coerceIn(0.0, 100.0) }
        } else {
            // Interpolate default curve onto the requested RPM axis
            DoubleArray(rpmAxis.size) { i ->
                interpolateClamped(rpmAxis[i], DEFAULT_RPM_AXIS, DEFAULT_PFI_SHARE)
            }
        }

        return PfiShareResult(rpmAxis.copyOf(), share)
    }

    /**
     * Extract actual PFI share from MED17 log data and return a smoothed RPM→PFI curve.
     *
     * Reads [Header.RPM_COLUMN_HEADER] and [Header.PFI_SPLIT_FACTOR_HEADER] from the
     * log map.  RPM samples are bucketed into 500 RPM bins and averaged.  Only rows
     * where RPM > 0 are considered.
     *
     * The factor from the log is 0–1; it is converted to 0–100 % in the result.
     *
     * @param logData Parsed MED17 log (header → column of doubles)
     * @return [PfiShareResult] where [PfiShareResult.loggedRpmAxis] and
     *         [PfiShareResult.loggedPfiPercent] contain the observed data, and
     *         [PfiShareResult.rpmAxis] / [PfiShareResult.pfiSharePercent] contain the
     *         default curve for overlay comparison.
     */
    fun refineFromLog(
        logData: Map<Header, List<Double>>
    ): PfiShareResult {
        val rpmColumn = logData[Header.RPM_COLUMN_HEADER]
        val pfiColumn = logData[Header.PFI_SPLIT_FACTOR_HEADER]

        if (rpmColumn.isNullOrEmpty() || pfiColumn.isNullOrEmpty()) {
            return calculateRpmDependentShare()
        }

        val rowCount = minOf(rpmColumn.size, pfiColumn.size)

        // Bucket into 500 RPM bins
        data class Bucket(var sum: Double = 0.0, var count: Int = 0)

        val buckets = mutableMapOf<Int, Bucket>()
        for (i in 0 until rowCount) {
            val rpm = rpmColumn[i]
            if (rpm <= 0.0) continue
            val key = (rpm / RPM_BUCKET_SIZE).toInt() * RPM_BUCKET_SIZE
            val b = buckets.getOrPut(key) { Bucket() }
            b.sum += pfiColumn[i]
            b.count++
        }

        if (buckets.isEmpty()) {
            return calculateRpmDependentShare()
        }

        val sortedKeys = buckets.keys.sorted()
        val logRpm = DoubleArray(sortedKeys.size) { sortedKeys[it].toDouble() }
        val logPfi = DoubleArray(sortedKeys.size) { i ->
            val b = buckets[sortedKeys[i]]!!
            (b.sum / b.count * 100.0).coerceIn(0.0, 100.0)   // factor → %
        }

        val defaultResult = calculateRpmDependentShare()

        return PfiShareResult(
            rpmAxis = defaultResult.rpmAxis,
            pfiSharePercent = defaultResult.pfiSharePercent,
            loggedRpmAxis = logRpm,
            loggedPfiPercent = logPfi
        )
    }

    // ── Internals ────────────────────────────────────────────────────────

    private const val RPM_BUCKET_SIZE = 500

    /** Linear interpolation with clamped extrapolation (same approach as [InjectorScalingSolver]). */
    internal fun interpolateClamped(x: Double, xs: DoubleArray, ys: DoubleArray): Double {
        require(xs.size == ys.size && xs.isNotEmpty())
        if (x <= xs.first()) return ys.first()
        if (x >= xs.last()) return ys.last()

        var lo = 0
        var hi = xs.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (xs[mid] <= x) lo = mid else hi = mid
        }
        val t = (x - xs[lo]) / (xs[hi] - xs[lo])
        return ys[lo] + t * (ys[hi] - ys[lo])
    }

    private fun isSortedAscending(a: DoubleArray): Boolean {
        for (i in 1 until a.size) if (a[i] < a[i - 1]) return false
        return true
    }
}
