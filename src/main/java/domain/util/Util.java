package domain.util;

import java.awt.*;

public class Util {
    public static double[] toDoubleArray(Double[] array) {
        double[] result = new double[array.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = array[i];
        }

        return result;
    }

    public static Color newColorWithAlpha(Color original, int alpha) {
        return new Color(original.getRed(), original.getGreen(), original.getBlue(), alpha);
    }
}
