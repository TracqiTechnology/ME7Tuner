package domain.math

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints

object CurveFitter {
    fun fitCurve(x: List<Double>, y: List<Double>, degree: Int): DoubleArray {
        check(x.size == y.size) { "x and y must have equal length!" }
        val obs = WeightedObservedPoints()
        for (i in x.indices) {
            obs.add(x[i], y[i])
        }
        val fitter = PolynomialCurveFitter.create(degree)
        return fitter.fit(obs.toList())
    }

    fun buildCurve(coeff: DoubleArray, x: List<Double>): List<Double> {
        val polynomialFunction = PolynomialFunction(coeff)
        return x.map { polynomialFunction.value(it) }
    }
}
