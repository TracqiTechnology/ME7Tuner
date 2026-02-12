package domain.model.openloopfueling.util

import data.contract.Me7LogFileContract

object Me7LogUtil {
    fun findMe7Logs(me7Log: Map<Me7LogFileContract.Header, List<Double>>, minThrottleAngle: Double, lambdaControlEnabled: Double, minRpm: Double, minPointsMe7: Int): List<Map<Me7LogFileContract.Header, List<Double>>> {
        val logList = mutableListOf<Map<Me7LogFileContract.Header, List<Double>>>()
        val lambdaControl = me7Log[Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER]!!
        val throttleAngle = me7Log[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!
        val rpm = me7Log[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!

        var i = 0
        while (i < throttleAngle.size) {
            if (throttleAngle[i] >= minThrottleAngle && lambdaControl[i] == lambdaControlEnabled && rpm[i] >= minRpm) {
                if (LogUtil.isValidLogLength(i, minPointsMe7, minThrottleAngle, throttleAngle)) {
                    val endOfLog = LogUtil.findEndOfLog(i, minThrottleAngle, throttleAngle)
                    logList.add(getMe7Log(i, endOfLog, me7Log))
                    i = endOfLog + 1
                    continue
                }
            }
            i++
        }

        return logList
    }

    private fun getMe7Log(start: Int, end: Int, me7Log: Map<Me7LogFileContract.Header, List<Double>>): Map<Me7LogFileContract.Header, List<Double>> {
        val log = mutableMapOf<Me7LogFileContract.Header, List<Double>>()
        log[Me7LogFileContract.Header.MAF_VOLTAGE_HEADER] = me7Log[Me7LogFileContract.Header.MAF_VOLTAGE_HEADER]!!.subList(start, end)
        log[Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER] = me7Log[Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER]!!.subList(start, end)
        log[Me7LogFileContract.Header.STFT_COLUMN_HEADER] = me7Log[Me7LogFileContract.Header.STFT_COLUMN_HEADER]!!.subList(start, end)
        log[Me7LogFileContract.Header.LTFT_COLUMN_HEADER] = me7Log[Me7LogFileContract.Header.LTFT_COLUMN_HEADER]!!.subList(start, end)
        log[Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER] = me7Log[Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER]!!.subList(start, end)
        log[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER] = me7Log[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!.subList(start, end)
        log[Me7LogFileContract.Header.RPM_COLUMN_HEADER] = me7Log[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!.subList(start, end)
        log[Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER] = me7Log[Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER]!!.subList(start, end)
        log[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER] = me7Log[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER]!!.subList(start, end)
        val afr = me7Log[Me7LogFileContract.Header.WIDE_BAND_O2_HEADER]!!
        if (afr.isNotEmpty()) {
            log[Me7LogFileContract.Header.WIDE_BAND_O2_HEADER] = afr.subList(start, end)
        }
        return log
    }
}
