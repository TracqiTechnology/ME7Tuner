package domain.model.airflow

import data.contract.AfrLogFileContract
import data.contract.Me7LogFileContract
import domain.math.Index
import domain.model.openloopfueling.util.AfrLogUtil
import domain.model.openloopfueling.util.Me7LogUtil

class AirflowEstimationManager(
    private val minThrottleAngle: Double,
    private val minRpm: Double,
    private val minPointsMe7: Int,
    private val minPointsAfr: Int,
    private val maxAfr: Double,
    fuelInjectorCubicCentimetersPerMinute: Double,
    numFuelInjectors: Double,
    gasolineGramsPerCubicCentimeter: Double
) {
    private val totalFuelFlowGramsPerMinute = fuelInjectorCubicCentimetersPerMinute * numFuelInjectors * gasolineGramsPerCubicCentimeter
    private val estimatedAirflowGramsPerSecondLogs = mutableListOf<MutableList<Double>>()
    private val measuredAirflowGramsPerSecondLogs = mutableListOf<List<Double>>()
    private val measuredRpmLogs = mutableListOf<List<Double>>()

    var airflowEstimation: AirflowEstimation? = null
        private set

    fun estimate(me7LogMap: Map<Me7LogFileContract.Header, List<Double>>, afrLogMap: Map<String, List<Double>>) {
        val me7LogList = Me7LogUtil.findMe7Logs(me7LogMap, minThrottleAngle, LAMBDA_CONTROL_ENABLED, minRpm, minPointsMe7)
        val afrLogList = AfrLogUtil.findAfrLogs(afrLogMap, minThrottleAngle, minRpm, maxAfr, minPointsAfr)

        val dutyCycleLogs = mutableListOf<List<Double>>()

        for (me7log in me7LogList) {
            val rpm = me7log[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!
            val fuelInjectorOnTime = me7log[Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER]!!
            val gramsPerSecond = me7log[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER]!!

            dutyCycleLogs.add(getInjectorDutyCycle(rpm, fuelInjectorOnTime))
            measuredAirflowGramsPerSecondLogs.add(gramsPerSecond)
            measuredRpmLogs.add(rpm)
        }

        val size = minOf(dutyCycleLogs.size, afrLogList.size)
        if (me7LogList.size != afrLogList.size) {
            System.err.println("[AirflowEstimationManager] Warning: ME7 pull count (${me7LogList.size}) does not match AFR pull count (${afrLogList.size}). Only the first $size pulls will be used.")
        }

        for (i in 0 until size) {
            estimatedAirflowGramsPerSecondLogs.add(mutableListOf())
            val dutyCycleLog = dutyCycleLogs[i]
            val me7RpmLog = me7LogList[i][Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!
            val afrRpmLog = afrLogList[i][AfrLogFileContract.RPM_HEADER]!!
            val afrLog = afrLogList[i][AfrLogFileContract.AFR_HEADER]!!

            for (j in dutyCycleLog.indices) {
                val totalFuelGramsPerSecond = (dutyCycleLog[j] * totalFuelFlowGramsPerMinute) / 60
                val afrIndex = Index.getInsertIndex(afrRpmLog, me7RpmLog[j])
                val afr = afrLog[minOf(afrIndex, afrLog.size - 1)]
                val airflowGramsPerSecond = totalFuelGramsPerSecond * afr
                estimatedAirflowGramsPerSecondLogs[i].add(airflowGramsPerSecond)
            }
        }

        airflowEstimation = AirflowEstimation(estimatedAirflowGramsPerSecondLogs, measuredAirflowGramsPerSecondLogs, measuredRpmLogs)
    }

    private fun getInjectorDutyCycle(rpm: List<Double>, fuelInjectorOnTime: List<Double>): List<Double> {
        return rpm.indices.map { i ->
            if (rpm[i] <= 0) {
                0.0
            } else {
                val engineCycleMs = (1 / ((rpm[i] / 2.0) / 60.0)) * 1000
                fuelInjectorOnTime[i] / engineCycleMs
            }
        }
    }

    companion object {
        private const val LAMBDA_CONTROL_ENABLED = 0.0
    }
}
