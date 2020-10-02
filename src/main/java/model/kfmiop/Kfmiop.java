package model.kfmiop;

public class Kfmiop {

    public static Double[][] getMap() {
        return new Double[16][11];
    }

    public static Double[] getXAxis() {
        return new Double[] {9.75, 18.0, 27.0, 50.25, 62.25, 74.25, 97.5, 120.75, 144.75, 168.0, 191.25};
    }

    public static Double[] getYAxis() {
        return new Double[] {440d, 720d, 1000d, 1240d, 1520d, 1760d, 2000d, 2520d, 3000d, 3520d, 4000d, 4520d, 5000d, 5520d, 6000d, 6520d };
    }
}
