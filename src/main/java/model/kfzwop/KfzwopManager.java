package model.kfzwop;

import math.LinearExtrapolation;

public class KfzwopManager {

    public static Double[][] generateKfzwop(Double[] xAxisOld, Double[][] kwzwopOld, Double[] xAxisNew) {

        Double[][] kwzwopNew = new Double[kwzwopOld.length][kwzwopOld[0].length];

        for (int i = 0; i < kwzwopOld.length; i++) {
            for (int j = 0; j < kwzwopOld[i].length; j++) {

                double x0;
                double x1;
                double y0;
                double y1;


                if (j < kwzwopOld[i].length - 1) {
                    x0 = xAxisOld[j];
                    x1 = xAxisOld[j + 1];
                    y0 = kwzwopOld[i][j];
                    y1 = kwzwopOld[i][j + 1];
                } else {
                    x0 = xAxisOld[j - 1];
                    x1 = xAxisOld[j];
                    y0 = kwzwopOld[i][j - 1];
                    y1 = kwzwopOld[i][j];
                }

                Double value = Math.max(LinearExtrapolation.extrapolate(new Double[]{x0, x1}, new Double[]{y0, y1}, xAxisNew[j]), -13.5);

                kwzwopNew[i][j] = value;
            }
        }

        return kwzwopNew;
    }
}
