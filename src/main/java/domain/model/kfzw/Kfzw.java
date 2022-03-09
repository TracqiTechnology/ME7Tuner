package domain.model.kfzw;

import domain.math.Index;
import domain.math.LinearExtrapolation;

import java.util.Arrays;

public class Kfzw {

    public static Double[][] generateKfzw(Double[] xAxisOld, Double[][] kfzwOld, Double[] xAxisNew) {

        Double[][] kwzwNew = new Double[kfzwOld.length][kfzwOld[0].length];

        for (int i = 0; i < kfzwOld.length; i++) {
            for (int j = 0; j < kfzwOld[i].length; j++) {

                int indexKey = Index.getInsertIndex(Arrays.asList(xAxisOld), xAxisNew[j]);

                double x0;
                double x1;
                double y0;
                double y1;

                if (indexKey < kfzwOld[i].length - 1) {
                    x0 = xAxisOld[indexKey];
                    x1 = xAxisOld[indexKey + 1];
                    y0 = kfzwOld[i][indexKey];
                    y1 = kfzwOld[i][indexKey + 1];
                } else {
                    x0 = xAxisOld[indexKey - 1];
                    x1 = xAxisOld[indexKey];
                    y0 = kfzwOld[i][indexKey - 1];
                    y1 = kfzwOld[i][indexKey];
                }

                double value = Math.max(LinearExtrapolation.extrapolate(new Double[]{x0, x1}, new Double[]{y0, y1}, xAxisNew[j]), -13.5);

                kwzwNew[i][j] = value;
            }
        }

        return kwzwNew;
    }
}
