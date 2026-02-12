package domain.derivative

import data.contract.Me7LogFileContract
import domain.math.Index
import domain.math.map.Map3d
import kotlin.math.abs

object Derivative {

    fun getMlfhm(me7Logs: Map<Me7LogFileContract.Header, List<Double>>, mlhfm: Map3d): Map<Double, List<Double>> {
        val rawVoltageDt = mutableMapOf<Double, MutableList<Double>>()

        for (voltage in mlhfm.yAxis) {
            rawVoltageDt[voltage] = mutableListOf()
        }

        val me7Voltages = me7Logs[Me7LogFileContract.Header.MAF_VOLTAGE_HEADER]!!
        val me7Timestamps = me7Logs[Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER]!!
        val me7voltageDt = getDt(me7Voltages, me7Timestamps)

        for (i in me7voltageDt.indices) {
            val me7Voltage = me7Voltages[i + 1]
            val mlhfmVoltageIndex = Index.getInsertIndex(mlhfm.yAxis.toList(), me7Voltage)
            val mlhfmVoltageKey = mlhfm.yAxis[mlhfmVoltageIndex]
            rawVoltageDt[mlhfmVoltageKey]!!.add(me7voltageDt[i])
        }

        return rawVoltageDt
    }

    fun getDt(voltages: List<Double>, timestamps: List<Double>): List<Double> {
        return (0 until voltages.size - 1).map { i ->
            val v1 = voltages[i]
            val v2 = voltages[i + 1]
            val t1 = timestamps[i]
            val t2 = timestamps[i + 1]
            abs((v2 - v1) / (t2 - t1))
        }
    }
}
