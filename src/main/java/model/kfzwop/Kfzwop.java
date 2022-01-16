package model.kfzwop;

import math.Index;
import math.LinearExtrapolation;

import java.util.Arrays;

public class Kfzwop {
    public static Double[][] generateKfzwop(Double[] xAxisOld, Double[][] kfzwopOld, Double[] xAxisNew) {

        // TODO Build a polynomial function instead of linear extrapolation

        Double[][] kwzwNew = new Double[kfzwopOld.length][kfzwopOld[0].length];

        for (int i = 0; i < kfzwopOld.length; i++) {
            for (int j = 0; j < kfzwopOld[i].length; j++) {

                int indexKey = Index.getInsertIndex(Arrays.asList(xAxisOld), xAxisNew[j]);

                double x0;
                double x1;
                double y0;
                double y1;

                if (indexKey < kfzwopOld[i].length - 1) {
                    x0 = xAxisOld[indexKey];
                    x1 = xAxisOld[indexKey + 1];
                    y0 = kfzwopOld[i][indexKey];
                    y1 = kfzwopOld[i][indexKey + 1];
                } else {
                    x0 = xAxisOld[indexKey - 1];
                    x1 = xAxisOld[indexKey];
                    y0 = kfzwopOld[i][indexKey - 1];
                    y1 = kfzwopOld[i][indexKey];
                }

                double value = Math.max(LinearExtrapolation.extrapolate(new Double[]{x0, x1}, new Double[]{y0, y1}, xAxisNew[j]), -13.5);

                kwzwNew[i][j] = value;
            }
        }

        return kwzwNew;
    }
}
