package domain.math

import domain.math.map.Map3d

/**
 * Rescales a 2D map's xAxis to a new maximum value and interpolates
 * all zAxis rows onto the new breakpoints.
 *
 * Used on MED17/DS1 where KFMIOP is a scalar and cannot provide a
 * reference xAxis — screens fall back to rescaling maps using their
 * own xAxis stretched to a user-specified max load.
 */
object RescaleMap {

    /**
     * Rescale [input]'s xAxis so the last breakpoint becomes [newMax],
     * then linearly interpolate every zAxis row onto the new breakpoints.
     *
     * @return A new Map3d with the rescaled xAxis and interpolated zAxis.
     *         yAxis is copied unchanged.
     */
    fun rescaleMapXAxis(input: Map3d, newMax: Double): Map3d? {
        val oldXAxis = input.xAxis
        if (oldXAxis.size < 2 || input.zAxis.isEmpty() || input.zAxis[0].isEmpty()) {
            return null
        }

        val newXAxis = RescaleAxis.rescaleAxis(oldXAxis, newMax)

        val newZAxis = Array(input.zAxis.size) { row ->
            interpolateRow(oldXAxis, input.zAxis[row], newXAxis)
        }

        return Map3d(newXAxis, input.yAxis.copyOf(), newZAxis)
    }

    /**
     * Linearly interpolate a single row of values from [oldXAxis] breakpoints
     * onto [newXAxis] breakpoints, clamping to endpoint values for extrapolation.
     */
    private fun interpolateRow(
        oldXAxis: Array<Double>,
        oldValues: Array<Double>,
        newXAxis: Array<Double>
    ): Array<Double> {
        return Array(newXAxis.size) { j ->
            val x = newXAxis[j]

            // Clamp to endpoints
            if (x <= oldXAxis.first()) return@Array oldValues.first()
            if (x >= oldXAxis.last()) return@Array oldValues.last()

            // Find bracketing interval
            var lo = 0
            for (k in 0 until oldXAxis.size - 1) {
                if (oldXAxis[k + 1] >= x) {
                    lo = k
                    break
                }
            }

            val x0 = oldXAxis[lo]
            val x1 = oldXAxis[lo + 1]
            val y0 = oldValues[lo]
            val y1 = oldValues[lo + 1]

            if (x1 == x0) y0
            else y0 + (x - x0) / (x1 - x0) * (y1 - y0)
        }
    }
}
