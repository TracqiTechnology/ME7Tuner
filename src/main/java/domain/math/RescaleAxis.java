package domain.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RescaleAxis {
    public static Double[] rescaleAxis(Double[] axis, Double max) {

        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();

        for(int i = 0; i < axis.length; i++) {
            x.add((double) i);
            y.add(axis[i]);
        }

        double[] coefficients = CurveFitter.fitCurve(x, y, 3);

        x.clear();

        double count = 0;
        while(count < 32) {
            x.add(count);
            count += 0.1;
        }

        List<Double> curve = CurveFitter.buildCurve(coefficients, x);

        int index = Collections.binarySearch(curve, max);

        if (index < 0) {
            index = Math.abs(index + 1);
        }

        double[] scalar = new double[axis.length];

        for(int i = 0; i < axis.length; i++) {
            scalar[i] = (axis[i] - axis[0])/ (axis[axis.length - 1] - axis[0]);
        }

        Double[] newAxis = new Double[axis.length];

        for(int i = 0; i < axis.length; i++) {
            if(i == 0) {
                newAxis[0] = axis[0];
            }

            newAxis[i] = curve.get((int) (scalar[i] * index));
        }

        return newAxis;
    }
}
