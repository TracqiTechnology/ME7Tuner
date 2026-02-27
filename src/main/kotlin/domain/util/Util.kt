package domain.util

object Util {
    fun toDoubleArray(array: Array<Double>): DoubleArray {
        return DoubleArray(array.size) { array[it] }
    }
}
