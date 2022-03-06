package domain.model.mlhfm;

import domain.math.CurveFitter;
import domain.math.map.Map3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MlhfmFitter {
    public static Map3d fitMlhfm(Map3d mlhfmMap, int degree) {
        List<Double> y = Arrays.asList(mlhfmMap.yAxis);
        List<Double> z = new ArrayList<>();

        for(int i = 0; i < mlhfmMap.zAxis.length; i++) {
            z.add(mlhfmMap.zAxis[i][0]);
        }

        double[] coeff = CurveFitter.fitCurve(y, z, degree);

        Double[][] zOut = new Double[z.size()][1];

        List<Double> fit = CurveFitter.buildCurve(coeff, y);

        for(int i = 0; i < fit.size(); i++) {
            zOut[i][0] = Math.max(0, fit.get(i));
        }

        return new Map3d(new Double[0], y.toArray(new Double[0]), zOut);
    }
}
