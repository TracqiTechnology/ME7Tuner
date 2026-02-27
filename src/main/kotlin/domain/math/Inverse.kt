package domain.math

import domain.math.map.Map3d

object Inverse {
    fun calculateInverse(input: Map3d, output: Map3d): Map3d {
        val inverse = Map3d(output)

        for (i in input.yAxis.indices) {
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
