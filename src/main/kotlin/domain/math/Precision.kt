package domain.math

import kotlin.math.pow
import kotlin.math.roundToLong

object Precision {
    fun round(value: Double, precision: Int): Double {
        val scale = 10.0.pow(precision).toLong()
        return (value * scale).roundToLong().toDouble() / scale
    }
}
