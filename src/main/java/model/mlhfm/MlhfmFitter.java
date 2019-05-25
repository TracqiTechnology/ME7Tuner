package model.mlhfm;

import contract.MlhfmFileContract;
import math.CurveFitter;

import java.util.List;
import java.util.Map;

public class MlhfmFitter {
    public static Map<String, List<Double>> fitMlhfm(Map<String, List<Double>> mlhfmMap, int degree) {
        List<Double> x = mlhfmMap.get(MlhfmFileContract.MAF_VOLTAGE_HEADER);
        List<Double> y = mlhfmMap.get(MlhfmFileContract.KILOGRAM_PER_HOUR_HEADER);

        double[] coeff = CurveFitter.fitCurve(x, y, degree);
        mlhfmMap.put(MlhfmFileContract.KILOGRAM_PER_HOUR_HEADER, CurveFitter.buildCurve(coeff, x));

        return mlhfmMap;
    }
}
