package domain.math;

public class LinearExtrapolation {
    public static Double extrapolate(Double[] x, Double[] y, Double v) {
       return y[0] + (v - x[0])/(x[1] - x[0])*(y[1]-y[0]);
    }

}
