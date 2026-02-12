package domain.model.openloopfueling.util

import data.contract.AfrLogFileContract

object AfrLogUtil {
    fun findAfrLogs(afrLog: Map<String, List<Double>>, minThrottleAngle: Double, minRpm: Double, maxAfr: Double, minPointsAfr: Int): List<Map<String, List<Double>>> {
        val logList = mutableListOf<Map<String, List<Double>>>()
        val throttleAngle = afrLog[AfrLogFileContract.TPS_HEADER]!!
        val rpm = afrLog[AfrLogFileContract.RPM_HEADER]!!
        val afr = afrLog[AfrLogFileContract.AFR_HEADER]!!

        var i = 0
        while (i < throttleAngle.size) {
            if (throttleAngle[i] >= minThrottleAngle && rpm[i] >= minRpm && afr[i] < maxAfr) {
                if (LogUtil.isValidLogLength(i, minPointsAfr, minThrottleAngle, throttleAngle)) {
                    val endOfLog = LogUtil.findEndOfLog(i, minThrottleAngle, throttleAngle)
                    logList.add(getAfrLog(i, endOfLog, afrLog))
                    i = endOfLog + 1
                    continue
                }
            }
            i++
        }

        return logList
    }

    private fun getAfrLog(start: Int, end: Int, afrLog: Map<String, List<Double>>): Map<String, List<Double>> {
        return mapOf(
            AfrLogFileContract.RPM_HEADER to afrLog[AfrLogFileContract.RPM_HEADER]!!.subList(start, end),
            AfrLogFileContract.AFR_HEADER to afrLog[AfrLogFileContract.AFR_HEADER]!!.subList(start, end),
            AfrLogFileContract.TPS_HEADER to afrLog[AfrLogFileContract.TPS_HEADER]!!.subList(start, end),
            AfrLogFileContract.BOOST_HEADER to afrLog[AfrLogFileContract.BOOST_HEADER]!!.subList(start, end)
        )
    }
}
