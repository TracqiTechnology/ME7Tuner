package model.mlhfm;

import math.CurveFitter;
import math.map.Map2d;

import java.util.Arrays;
import java.util.List;

public class MlhfmFitter {
    public static Map2d fitMlhfm(Map2d mlhfmMap, int degree) {
        List<Double> x = Arrays.asList(mlhfmMap.axis);
        List<Double> y = Arrays.asList(mlhfmMap.data);

        double[] coeff = CurveFitter.fitCurve(x, y, degree);

        return new Map2d(x.toArray(new Double[0]), CurveFitter.buildCurve(coeff, x).toArray(new Double[0]));
    }
}
