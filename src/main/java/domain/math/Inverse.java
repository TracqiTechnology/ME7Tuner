package domain.math;

import domain.math.map.Map3d;

public class Inverse {

    public static Map3d calculateInverse(Map3d input, Map3d output) {

        Map3d inverse = new Map3d(output);

        for(int i = 0; i < input.yAxis.length; i++) {
            for(int j = 0; j < output.xAxis.length; j++) {
                Double[] x = input.zAxis[i];
                Double[] y = input.xAxis;
                Double[] xi = new Double[]{output.xAxis[j]};

                inverse.zAxis[i][j] = LinearInterpolation.interpolate(x, y , xi)[0];
            }
        }

        return inverse;
    }
}
