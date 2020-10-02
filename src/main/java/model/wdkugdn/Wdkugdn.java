package model.wdkugdn;

public class Wdkugdn {

    public static Double[][] getMap() {
        Double[][] map = new Double[][]{
                {5.0480, 12.8367, 18.1960, 22.4017, 28.3302, 31.8934, 35.5894, 38.4033, 41.3622, 43.8038, 48.1041, 50.2039}
        };

        return map;
    }

    public static Double[] getXAxis() {
        return new Double[]{240d, 520d, 760d, 1000d, 1520d, 2000d, 2520d, 3000d, 3520d, 4000d, 5000d, 6000d};
    }

    public static Double[] getYAxis() {
        return new Double[]{0d};
    }


}