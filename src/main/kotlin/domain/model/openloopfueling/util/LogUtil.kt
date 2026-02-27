package domain.model.openloopfueling.util

object LogUtil {
    fun isValidLogLength(start: Int, minPoints: Int, minThrottleAngle: Double, throttleAngle: List<Double>): Boolean {
        val minValidIndex = start + minPoints
        if (minValidIndex < throttleAngle.size) {
            for (i in start until minValidIndex) {
                if (throttleAngle[i] <= minThrottleAngle) return false
            }
            return true
        }
        return false
    }

    fun findEndOfLog(start: Int, minThrottleAngle: Double, throttleAngle: List<Double>): Int {
        for (i in start until throttleAngle.size) {
            if (throttleAngle[i] < minThrottleAngle) return i
        }
        return throttleAngle.size - 1
    }
}
