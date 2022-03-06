package domain.math;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Index {
    /**
     * Percentage(x,a,b)=(x−a)/(b−a)
     * @param a The smaller number
     * @param b The larger number
     * @param x The number to evaluate between a and b
     * @return The proportion of x between a and b
     */
    public static double proportion(double a, double b, double x) {
        if(a >= b) {
            throw new IllegalArgumentException("b must be greater than a! ->" + "b: " + b + " a: " + a);
        }

        return (x-a)/(b-a);
    }

    public static int getInsertIndex(List<Double> values, double value) {
        int index = Collections.binarySearch(values, value);


        if (index < 0) {
            index = Math.abs(index + 1);
        }

        index = Math.min(index, values.size() - 1);

        // binarySearch() always returns the index with the greater value, even if the input is closer to the lesser value
        if(index > 0) {
            double a = values.get(index - 1);
            double b = values.get(index);
            double proportion = proportion(a, b, value);

            // Is the input closer to the lesser value?
            if(proportion < 0.50) {
                index--;
            }
        }

        return index;
    }
}
