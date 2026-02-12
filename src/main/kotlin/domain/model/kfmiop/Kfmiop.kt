package domain.model.kfmiop

import domain.math.RescaleAxis
import domain.math.map.Map3d
import domain.model.plsol.Plsol

data class Kfmiop(
    val outputKfmiop: Map3d,
    val inputBoost: Map3d,
    val outputBoost: Map3d,
    val maxMapSensorPressure: Double,
    val maxBoostPressure: Double
) {
    companion object {
        fun calculateKfmiop(baseKfmiop: Map3d, maxMapSensorLoad: Double, maxBoostPressureLoad: Double): Kfmiop {
            val xAxis = baseKfmiop.xAxis
            val yAxis = baseKfmiop.yAxis
            val zAxis = baseKfmiop.zAxis

            val optimalLoad = Array(yAxis.size) { Array(xAxis.size) { 0.0 } }

            var maxBoost = 1.0
            var maxTorque = 0.0
            for (i in optimalLoad.indices) {
                for (j in optimalLoad[i].indices) {
                    maxTorque = maxOf(zAxis[i][j], maxTorque)
                }
            }

            val currentMaxLoad = (xAxis[xAxis.size - 1] / maxTorque) * 100
            maxBoost = maxOf(Plsol.plsol(1013.0, maxBoost, 0.0, 96.0, 0.106, currentMaxLoad), maxBoost)

            val rescaledXAxis = RescaleAxis.rescaleAxis(xAxis, maxBoostPressureLoad)

            val kfmiop = Array(yAxis.size) { Array(xAxis.size) { 0.0 } }
            val inputBoost = Array(yAxis.size) { Array(xAxis.size) { 0.0 } }
            val outputBoost = Array(yAxis.size) { Array(xAxis.size) { 0.0 } }

            for (i in optimalLoad.indices) {
                for (j in optimalLoad[i].indices) {
                    kfmiop[i][j] = ((zAxis[i][j] / 100 * currentMaxLoad) / (zAxis[i][j] / 100 * maxMapSensorLoad) * zAxis[i][j]) * (rescaledXAxis[j] / xAxis[j])
                    inputBoost[i][j] = (Plsol.plsol(1013.0, 1013.0, 0.0, 96.0, 0.106, (zAxis[i][j] / 100 * currentMaxLoad)) - 1013) * 0.0145038
                    outputBoost[i][j] = (Plsol.plsol(1013.0, 1013.0, 0.0, 96.0, 0.106, (kfmiop[i][j] / 100 * maxMapSensorLoad)) - 1013) * 0.0145038
                }
            }

            val outputKfmiopMap = Map3d(rescaledXAxis, yAxis, kfmiop)
            val inputBoostMap = Map3d(xAxis, yAxis, inputBoost)
            val outputBoostMap = Map3d(rescaledXAxis, yAxis, outputBoost)
            val maxMap = Plsol.plsol(1013.0, maxBoost, 0.0, 96.0, 0.106, currentMaxLoad)

            return Kfmiop(outputKfmiopMap, inputBoostMap, outputBoostMap, maxMap, maxBoost)
        }
    }
}
