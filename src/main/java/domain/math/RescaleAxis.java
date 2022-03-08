package domain.math;

public class RescaleAxis {
    public static Double[] rescaleAxis(Double[] axis, Double max) {

        double[] scalar = new double[axis.length];

        double range = Math.abs(max - axis[0]);

        for(int i = 0; i < axis.length; i++) {
            scalar[i] = (axis[i] - axis[0])/ (axis[axis.length - 1] - axis[0]);
        }

        Double[] newAxis = new Double[axis.length];

        for(int i = 0; i < axis.length; i++) {
            if(i == 0) {
                newAxis[0] = axis[0];
            } else {
                newAxis[i] = axis[0] + (scalar[i] * range);
            }
        }

        return newAxis;
    }
}
