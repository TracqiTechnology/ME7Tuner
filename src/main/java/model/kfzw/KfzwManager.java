package model.kfzw;

import math.LinearExtrapolation;

public class KfzwManager {

    public static Double[][] generateKfzw(Double[] xAxisOld, Double[][] kwzwOld, Double[] xAxisNew) {

        Double[][] kwzwNew = new Double[kwzwOld.length][kwzwOld[0].length];

        for (int i = 0; i < kwzwOld.length; i++) {
            for (int j = 0; j < kwzwOld[i].length; j++) {

                double x0;
                double x1;
                double y0;
                double y1;


                if (j < kwzwOld[i].length - 1) {
                    x0 = xAxisOld[j];
                    x1 = xAxisOld[j + 1];
                    y0 = kwzwOld[i][j];
                    y1 = kwzwOld[i][j + 1];
                } else {
                    x0 = xAxisOld[j - 1];
                    x1 = xAxisOld[j];
                    y0 = kwzwOld[i][j - 1];
                    y1 = kwzwOld[i][j];
                }

                Double value = Math.max(LinearExtrapolation.extrapolate(new Double[]{x0, x1}, new Double[]{y0, y1}, xAxisNew[j]), -13.5);

                kwzwNew[i][j] = value;
            }
        }

        return kwzwNew;
    }
}
