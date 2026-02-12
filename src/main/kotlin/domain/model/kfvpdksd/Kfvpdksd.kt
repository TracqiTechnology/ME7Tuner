package domain.model.kfvpdksd

import data.contract.Me7LogFileContract
import domain.math.Index
import java.util.Collections

data class Kfvpdksd(val kfvpdksd: Array<Array<Double>>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Kfvpdksd) return false
        return kfvpdksd.contentDeepEquals(other.kfvpdksd)
    }

    override fun hashCode(): Int = kfvpdksd.contentDeepHashCode()

    companion object {
        fun parsePressure(log: Map<Me7LogFileContract.Header, List<Double>>, rpmAxis: Array<Double>): Array<Double> {
            val boostValues = List(rpmAxis.size) { mutableListOf<Double>() }

            val timestamps = log[Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER]!!
            val throttleAngle = log[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!
            val rpm = log[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!
            val barometricPressure = log[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER]!!
            val absolutePressure = log[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!

            for (i in timestamps.indices) {
                if (throttleAngle[i] > 80) {
                    val index = Index.getInsertIndex(rpmAxis.toList(), rpm[i])
                    boostValues[index].add(absolutePressure[i] - barometricPressure[i])
                }
            }

            return Array(rpmAxis.size) { i ->
                Collections.sort(boostValues[i])
                boostValues[i].reverse()

                val numElements = if (boostValues[i].isNotEmpty()) maxOf(1, (boostValues[i].size * 0.05).toInt()) else 0

                if (numElements > 0) {
                    var sum = 0.0
                    for (j in 0 until numElements) {
                        sum += boostValues[i][j]
                    }
                    sum / numElements
                } else {
                    0.0
                }
            }
        }

        fun generate(maxPressure: Array<Double>, rpmAxis: Array<Double>, pressureRatioAxis: Array<Double>): Kfvpdksd {
            val pressureRatio = Array(maxPressure.size) { i ->
                val ratio = (maxPressure[i] + 1000) / 1000
                if (ratio.isNaN()) 0.0 else ratio
            }

            val kfvpdksd = Array(rpmAxis.size) { rpmIndex ->
                Array(pressureRatioAxis.size) { prIndex ->
                    when {
                        prIndex == 0 -> 0.965
                        pressureRatioAxis[prIndex] > pressureRatio[rpmIndex] -> 1.016
                        else -> 0.965
                    }
                }
            }

            return Kfvpdksd(kfvpdksd)
        }
    }
}
