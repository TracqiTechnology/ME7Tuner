package domain.math

object FindMax {
    fun findMax(values: Array<Array<Double>>): Double {
        var max = 0.0
        for (array in values) {
            for (value in array) {
                if (value > max) max = value
            }
        }
        return max
    }
}
