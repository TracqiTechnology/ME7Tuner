package domain.math

import domain.math.map.Map3d

object Inverse {
    fun calculateInverse(input: Map3d, output: Map3d): Map3d {
        val inverse = Map3d(output)

        // Guard: iterate only over rows that exist in BOTH maps' y-axes and z-axes
        val rowCount = minOf(input.yAxis.size, input.zAxis.size, inverse.yAxis.size, inverse.zAxis.size)
        for (i in 0 until rowCount) {
            for (j in output.xAxis.indices) {
                val x = input.zAxis[i]
                val y = input.xAxis
                val xi = arrayOf(output.xAxis[j])
                inverse.zAxis[i][j] = LinearInterpolation.interpolate(x, y, xi)[0]
            }
        }

        return inverse
    }
}
