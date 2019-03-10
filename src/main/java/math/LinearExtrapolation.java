package math;

public class LinearExtrapolation {
    private static double[] x;
    private static double[] y;

    public static Double extrapolate(Double[] x, Double[] y, Double v) {
       return y[0] + (v - x[0])/(x[1] - x[0])*(y[1]-y[0]);
    }

}
