package domain.model.kfmirl;

public class Kfmirl {
    public static Double[][] getStockKfmirlMap() {
        Double[][] map = new Double[][]{
                {7.34, 21.66, 33.61, 47.56, 75.47, 111.96, 159.54, 187.00, 188.00, 189.00, 190.00, 191.00},
                {6.87, 21.28, 31.59, 41.95, 61.48, 91.08, 129.89, 175.32, 188.00, 189.00, 190.00, 191.00},
                {6.77, 20.70, 30.80, 40.55, 59.63, 83.37, 109.85, 148.53, 184.88, 189.00, 190.00, 191.00},
                {6.52, 19.73, 30.26, 40.24, 59.56, 80.39, 101.93, 132.50, 184.55, 189.00, 190.00, 191.00},
                {5.60, 19.06, 29.53, 39.56, 58.88, 78.19, 97.85, 119.93, 171.05, 189.00, 190.00, 191.00},
                {4.50, 18.21, 28.85, 38.74, 57.99, 76.88, 96.38, 116.89, 162.45, 189.00, 190.00, 191.00},
                {4.52, 17.65, 28.50, 38.37, 57.99, 76.97, 95.63, 114.94, 153.47, 186.40, 190.00, 191.00},
                {4.22, 17.18, 27.70, 37.27, 56.60, 76.24, 94.55, 112.64, 141.40, 176.96, 190.00, 191.00},
                {4.08, 17.02, 25.85, 36.00, 56.74, 76.20, 94.08, 111.87, 136.46, 172.48, 190.00, 191.00},
                {4.01, 17.30, 25.81, 35.79, 56.02, 75.89, 93.59, 111.24, 134.84, 167.00, 190.00, 191.00},
                {4.10, 17.30, 25.66, 35.84, 56.34, 75.31, 93.03, 110.46, 133.03, 164.68, 188.91, 191.00},
                {4.05, 17.60, 25.57, 35.65, 55.62, 74.04, 91.90, 109.69, 131.86, 162.17, 185.37, 191.00},
                {4.41, 17.79, 25.48, 35.72, 55.85, 73.31, 90.78, 109.01, 131.60, 162.59, 184.11, 191.00},
                {3.98, 18.00, 25.38, 35.09, 55.06, 72.49, 90.63, 109.08, 132.24, 163.69, 184.97, 191.00},
                {4.34, 18.00, 25.27, 35.06, 55.64, 72.49, 91.27, 109.81, 133.85, 165.71, 187.25, 191.00},
                {4.34, 17.95, 22.43, 30.38, 49.45, 67.90, 86.53, 105.28, 127.46, 156.05, 183.80, 191.00}
        };

        return map;
    }

    public static Double[][] getStockKfmirlScalerMap() {
        Double[][] kfmirl = getStockKfmirlMap();
        Double[][] scaler = new Double[kfmirl.length][];

        for (int i = 0; i < kfmirl.length; i++) {
            scaler[i] = new Double[kfmirl[i].length];

            for (int j = 0; j < kfmirl[i].length; j++) {
                scaler[i][j] = kfmirl[i][j] / 191.0;
            }
        }

        return scaler;
    }

    public static Double[][] getScaledKfmirlMap(double maxSpecifiedLoad, int minimumLoadIndex) {
        Double[][] kfmirl = getStockKfmirlMap();
        Double[][] scaler = getStockKfmirlScalerMap();
        Double[][] scaledKfmirl = new Double[scaler.length][];

        for (int i = 0; i < scaledKfmirl.length; i++) {
            scaledKfmirl[i] = new Double[scaler[i].length];

            for (int j = 0; j < scaler[i].length; j++) {
                if (j < minimumLoadIndex) {
                    scaledKfmirl[i][j] = kfmirl[i][j];
                } else if (j == minimumLoadIndex) {
                    scaledKfmirl[i][j] = scaler[i][j] * (maxSpecifiedLoad/1.5);
                } else if (j == minimumLoadIndex + 1) {
                    scaledKfmirl[i][j] = scaler[i][j] * (maxSpecifiedLoad/1.25);
                } else
                    scaledKfmirl[i][j] = scaler[i][j] * maxSpecifiedLoad;
            }
        }

        // Enforce monotonicity: each value must be >= the previous column value
        for (int i = 0; i < scaledKfmirl.length; i++) {
            for (int j = 1; j < scaledKfmirl[i].length; j++) {
                if (scaledKfmirl[i][j] < scaledKfmirl[i][j - 1]) {
                    scaledKfmirl[i][j] = scaledKfmirl[i][j - 1];
                }
            }
        }

        return scaledKfmirl;
    }

    public static Double[] getStockKfmirlXAxis() {
        return new Double[]{0d, 10d, 15d, 20d, 30d, 40d, 50d, 60d, 70d, 80d, 90d, 99d};
    }

    public static Double[] getStockKfmirlYAxis() {
        return new Double[]{440d, 720d, 1000d, 1240d, 1520d, 1760d, 2000d, 2520d, 3000d, 3520d, 4000d, 4520d, 5000d, 5520d, 6000d, 7000d};
    }
}
