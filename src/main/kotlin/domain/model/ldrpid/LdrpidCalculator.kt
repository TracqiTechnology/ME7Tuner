package domain.model.ldrpid

import data.contract.Me7LogFileContract
import domain.math.Index
import domain.math.LinearInterpolation
import domain.math.map.Map3d
import java.util.Collections
import kotlin.math.ceil

object LdrpidCalculator {

    data class LdrpidResult(
        val nonLinearOutput: Map3d,
        val linearOutput: Map3d,
        val kfldrl: Map3d,
        val kfldimx: Map3d
    )

    fun calculateNonLinearTable(values: Map<Me7LogFileContract.Header, List<Double>>, kfldrlMap: Map3d): Map3d {
        val throttlePlateAngles = values[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!
        val rpms = values[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!
        val dutyCycles = values[Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER]!!
        val barometricPressures = values[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER]!!
        val absoluteBoostPressures = values[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!

        val nonLinearTable = Array(kfldrlMap.yAxis.size) { Array(kfldrlMap.xAxis.size) { 0.0 } }
        val pressure = Array(kfldrlMap.yAxis.size) { DoubleArray(kfldrlMap.xAxis.size) }
        val count = Array(kfldrlMap.yAxis.size) { DoubleArray(kfldrlMap.xAxis.size) }

        for (i in throttlePlateAngles.indices) {
            if (throttlePlateAngles[i] >= 80) {
                val rpm = rpms[i]
                val dutyCycle = dutyCycles[i]
                val barometricPressure = barometricPressures[i]
                val absoluteBoostPressure = absoluteBoostPressures[i]
                val relativeBoostPressure = absoluteBoostPressure - barometricPressure

                val rpmIndex = Index.getInsertIndex(kfldrlMap.yAxis.toList(), rpm)
                val dutyCycleIndex = Index.getInsertIndex(kfldrlMap.xAxis.toList(), dutyCycle)

                if (relativeBoostPressure > 0) {
                    pressure[rpmIndex][dutyCycleIndex] += relativeBoostPressure
                    count[rpmIndex][dutyCycleIndex] += 1
                }
            }
        }

        for (j in nonLinearTable.indices) {
            for (k in nonLinearTable[j].indices) {
                nonLinearTable[j][k] = if (count[j][k] != 0.0) {
                    (pressure[j][k] / count[j][k]) * 0.0145038
                } else {
                    pressure[j][k] * 0.0145038
                }
            }
        }

        for (array in nonLinearTable) {
            array.sort()
            for (i in 0 until array.size - 1) {
                if (array[i] == 0.0) array[i] = 0.1
                if (array[i] >= array[i + 1]) {
                    array[i + 1] = if (i > 0) {
                        val theta = array[i] / array[i - 1]
                        array[i] * (1 + (theta - 1) / 2)
                    } else {
                        array[i] * 1.1
                    }
                    if (array[i + 1].isNaN() || array[i + 1] == 0.0) {
                        array[i + 1] = array[i] + 0.1
                    }
                }
            }
        }

        return Map3d(kfldrlMap.xAxis, kfldrlMap.yAxis, nonLinearTable)
    }

    fun calculateLinearTable(nonLinearTable: Array<Array<Double>>, kfldrlMap: Map3d): Map3d {
        val linearTable = Array(nonLinearTable.size) { Array(nonLinearTable[0].size) { 0.0 } }

        for (i in nonLinearTable[0].indices) {
            val min = nonLinearTable[0][i]
            val max = nonLinearTable[nonLinearTable.size - 1][i]
            val step = (max - min) / (nonLinearTable.size - 1)

            for (j in linearTable.indices) {
                linearTable[j][i] = min + step * j
            }
        }

        return Map3d(kfldrlMap.xAxis, kfldrlMap.yAxis, linearTable)
    }

    fun calculateKfldrl(nonLinearTable: Array<Array<Double>>, linearTable: Array<Array<Double>>, kfldrlMap: Map3d): Map3d {
        val kfldrl = Array(nonLinearTable.size) { i ->
            Array(nonLinearTable[i].size) { j ->
                val x = nonLinearTable[i]
                val y = kfldrlMap.xAxis
                val xi = arrayOf(linearTable[i][j])
                val result = LinearInterpolation.interpolate(x, y, xi)[0]
                if (result.isNaN()) 0.0 else result
            }
        }
        return Map3d(kfldrlMap.xAxis, kfldrlMap.yAxis, kfldrl)
    }

    fun calculateKfldimx(nonLinearTable: Array<Array<Double>>, linearTable: Array<Array<Double>>, kfldrlMap: Map3d, kfldimxMap: Map3d): Map3d {
        val linearBoostMax = Array(linearTable[0].size) { i ->
            val linearBoost = linearTable.map { it[i] * 68.9476 }
            Collections.max(linearBoost)
        }

        val kfldimxXAxis = Array(kfldimxMap.xAxis.size) { 0.0 }
        val min = ceil(linearBoostMax[0] / 100.0) * 100
        val max = ceil(linearBoostMax[linearBoostMax.size - 1] / 100.0) * 100
        val interval = (max - min) / (kfldimxXAxis.size - 1)

        for (i in kfldimxXAxis.indices) {
            kfldimxXAxis[i] = min + interval * i
        }

        val kfldimx = Array(nonLinearTable.size) { i ->
            Array(kfldimxXAxis.size) { j ->
                val x = linearBoostMax
                val y = kfldrlMap.xAxis
                val xi = arrayOf(kfldimxXAxis[j])
                LinearInterpolation.interpolate(x, y, xi)[0]
            }
        }

        return Map3d(kfldimxXAxis, kfldimxMap.yAxis, kfldimx)
    }

    fun calculateLdrpid(values: Map<Me7LogFileContract.Header, List<Double>>, kfldrlMap: Map3d, kfldimxMap: Map3d): LdrpidResult {
        val nonLinearTable = calculateNonLinearTable(values, kfldrlMap)
        val linearTable = calculateLinearTable(nonLinearTable.zAxis, kfldrlMap)
        val kfldrl = calculateKfldrl(nonLinearTable.zAxis, linearTable.zAxis, kfldrlMap)
        val kfldimxMap3d = calculateKfldimx(nonLinearTable.zAxis, linearTable.zAxis, kfldrlMap, kfldimxMap)

        return LdrpidResult(nonLinearTable, linearTable, kfldrl, kfldimxMap3d)
    }
}
