package domain.model.mlhfm

import domain.math.CurveFitter
import domain.math.map.Map3d

object MlhfmFitter {
    fun fitMlhfm(mlhfmMap: Map3d, degree: Int): Map3d {
        val y = mlhfmMap.yAxis.toList()
        val z = mlhfmMap.zAxis.map { it[0] }

        val coeff = CurveFitter.fitCurve(y, z, degree)
        val fit = CurveFitter.buildCurve(coeff, y)

        val zOut = Array(z.size) { i -> arrayOf(maxOf(0.0, fit[i])) }

        return Map3d(emptyArray(), y.toTypedArray(), zOut)
    }
}
