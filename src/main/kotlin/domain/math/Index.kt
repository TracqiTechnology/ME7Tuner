package domain.math

import java.util.Collections
import kotlin.math.abs

object Index {

    fun proportion(a: Double, b: Double, x: Double): Double {
        if (a == b) return a
        require(a <= b) { "b must be greater than a! -> b: $b a: $a" }
        return (x - a) / (b - a)
    }

    fun getInsertIndex(values: List<Double>, value: Double): Int {
        if (!isSorted(values)) {
            return linearScanNearest(values, value)
        }

        var index = Collections.binarySearch(values, value)
        if (index < 0) {
            index = abs(index + 1)
        }
        index = minOf(index, values.size - 1)

        if (index > 0) {
            val a = values[index - 1]
            val b = values[index]
            val p = proportion(a, b, value)
            if (p < 0.50) {
                index--
            }
        }

        return index
    }

    private fun isSorted(values: List<Double>): Boolean {
        for (i in 1 until values.size) {
            if (values[i] < values[i - 1]) return false
        }
        return true
    }

    private fun linearScanNearest(values: List<Double>, value: Double): Int {
        var nearestIndex = 0
        var nearestDistance = abs(values[0] - value)
        for (i in 1 until values.size) {
            val distance = abs(values[i] - value)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestIndex = i
            }
        }
        return nearestIndex
    }
}
