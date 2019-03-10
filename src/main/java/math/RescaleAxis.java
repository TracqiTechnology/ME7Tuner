package math;

import java.util.Arrays;

public class RescaleAxis {
    public static Double[] rescaleAxis(Double[] axis, Double max) {
        Double min = axis[axis.length - 1];

        Double scalar = max / min;

        Double[] output = Arrays.copyOf(axis, axis.length);

        for (int i = 1; i < output.length; i++) {
            output[i] = Precision.round(output[i] * scalar, 2);
        }

        output[output.length-1] += 0.25;

        return output;
    }
}
