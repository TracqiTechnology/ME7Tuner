package domain.model.wdkugdn

import domain.math.LinearInterpolation
import domain.math.map.Map3d
import domain.model.load.EngineLoad
import java.util.Arrays
import kotlin.math.abs

object Wdkugdn {
    fun calculateWdkugdn(wdkugdn: Map3d, kfwdkmsn: Map3d, displacement: Double): Map3d {
        val xAxis = wdkugdn.xAxis
        val correctedWdkugdn = Map3d(wdkugdn)

        for (i in xAxis.indices) {
            val rpm = xAxis[i]
            val chokedAirflow = EngineLoad.getAirflow(1.0, rpm, displacement) * 3.6 / 0.528

            var rpmIndex = Arrays.binarySearch(kfwdkmsn.xAxis, rpm)
            if (rpmIndex < 0) rpmIndex = abs(rpmIndex + 1)
            rpmIndex = minOf(rpmIndex, kfwdkmsn.xAxis.size - 1)

            val throttleAngle = DoubleArray(kfwdkmsn.yAxis.size) { j -> kfwdkmsn.zAxis[j][rpmIndex] }

            var airflowIndex = Arrays.binarySearch(kfwdkmsn.yAxis, chokedAirflow)
            if (airflowIndex < 0) airflowIndex = abs(airflowIndex + 1)
            airflowIndex = minOf(airflowIndex, kfwdkmsn.yAxis.size - 1)
            airflowIndex = maxOf(airflowIndex, 1)

            val x = arrayOf(kfwdkmsn.yAxis[airflowIndex - 1], kfwdkmsn.yAxis[airflowIndex])
            val y = arrayOf(throttleAngle[airflowIndex - 1], throttleAngle[airflowIndex])
            val xi = arrayOf(chokedAirflow)

            val chokedThrottleAngle = LinearInterpolation.interpolate(x, y, xi)
            correctedWdkugdn.zAxis[0][i] = chokedThrottleAngle[0]
        }

        return correctedWdkugdn
    }
}
