package domain.math;

public class FindMax {
    public static Double findMax(Double[][] values) {
        Double max = 0d;

        for(Double[] array: values) {
            for(Double value: array) {
                if(value > max) {
                    max = value;
                }
            }
        }

        return max;
    }
}
