package domain.math

object LinearExtrapolation {
    fun extrapolate(x: Array<Double>, y: Array<Double>, v: Double): Double {
        return y[0] + (v - x[0]) / (x[1] - x[0]) * (y[1] - y[0])
    }
}
