package domain.math

import kotlin.math.abs

object RescaleAxis {
    fun rescaleAxis(axis: Array<Double>, max: Double): Array<Double> {
        val scalar = DoubleArray(axis.size)
        val range = abs(max - axis[0])

        for (i in axis.indices) {
            scalar[i] = (axis[i] - axis[0]) / (axis[axis.size - 1] - axis[0])
        }

        return Array(axis.size) { i ->
            if (i == 0) axis[0]
            else axis[0] + scalar[i] * range
        }
    }
}
