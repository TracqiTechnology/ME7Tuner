package math;

import java.util.Arrays;

public class LinearInterpolation {

    public static Double[] interpolate(Double[] x, Double[] y, Double[] xi) throws IllegalArgumentException {

        if (x.length != y.length) {
            throw new IllegalArgumentException("X and Y must be the same length");
        }
        if (x.length == 1) {
            throw new IllegalArgumentException("X must contain more than one value");
        }
        double[] dx = new double[x.length - 1];
        double[] dy = new double[x.length - 1];
        double[] slope = new double[x.length - 1];
        double[] intercept = new double[x.length - 1];

        // Calculate the line equation (i.e. slope and intercept) between each point
        for (int i = 0; i < x.length - 1; i++) {
            dx[i] = x[i + 1] - x[i];
            if (dx[i] < 0) {
                throw new IllegalArgumentException("X must be sorted " + x[i + 1] + " " + x[i]);
            }

            if (dx[i] == 0) {
                x[i + 1] += x[i + 1] * 0.01;
            }

            dy[i] = y[i + 1] - y[i];
            slope[i] = dy[i] / dx[i];
            intercept[i] = y[i] - x[i] * slope[i];
        }

        // Perform the interpolation here
        Double[] yi = new Double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            if ((xi[i] < x[0])) {
                yi[i] = 0d;
            } else {
                int loc = Arrays.binarySearch(x, xi[i]);

                if (loc < -1) {
                    loc = -loc - 2;
                    yi[i] = Math.min(slope[Math.min(loc, slope.length -1)] * xi[i] + intercept[Math.min(loc, intercept.length -1)], y[y.length -1]);
                }
                else {
                    yi[i] = y[loc];
                }
            }
        }

        return yi;
    }
}
