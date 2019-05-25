package math;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.ArrayList;
import java.util.List;

public class CurveFitter {
    public static double[] fitCurve(List<Double> x, List<Double> y, int degree) {
        if(x.size() != y.size()) {
            throw new IllegalStateException("x and y must have equal length!");
        }

        final WeightedObservedPoints obs = new WeightedObservedPoints();

        for(int i = 0; i < x.size(); i++) {
            obs.add(x.get(i), y.get(i));
        }

        final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);

        // Retrieve fitted parameters (coefficients of the polynomial function).
        return fitter.fit(obs.toList());
    }

    public static List<Double> buildCurve(double[] coeff, List<Double> x) {
        PolynomialFunction polynomialFunction = new PolynomialFunction(coeff);
        List<Double> y = new ArrayList<>(x.size());

        for(int i = 0; i < x.size(); i++) {
            y.add(polynomialFunction.value(x.get(i)));
        }

        return y;
    }
}
