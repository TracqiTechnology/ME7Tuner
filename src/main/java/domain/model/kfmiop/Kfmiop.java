package domain.model.kfmiop;

import domain.math.RescaleAxis;
import domain.math.map.Map3d;
import domain.model.plsol.Plsol;

import java.util.Arrays;

public class Kfmiop {

    private final Map3d outputKfmiop;
    private final Map3d inputBoost;
    private final Map3d outputBoost;
    private final double mapMax;
    private final double boostMax;

    public Kfmiop(Map3d outputKfmiop, Map3d inputBoost, Map3d outputBoost, double mapMax, double boostMax) {
        this.outputKfmiop = outputKfmiop;
        this.inputBoost = inputBoost;
        this.outputBoost = outputBoost;
        this.mapMax = mapMax;
        this.boostMax = boostMax;
    }

    public Map3d getOutputKfmiop() {
        return outputKfmiop;
    }

    public Map3d getInputBoost() {
        return inputBoost;
    }

    public Map3d getOutputBoost() {
        return outputBoost;
    }

    public double getMaxMapSensorPressure() {
        return mapMax;
    }

    public double getMaxBoostPressure() {
        return boostMax;
    }

    public static Kfmiop calculateKfmiop(Map3d baseKfmiop, double maxMapSensorLoad, double maxBoostPressureLoad) {
        Double[] xAxis = baseKfmiop.xAxis;
        Double[] yAxis = baseKfmiop.yAxis;
        Double[][] zAxis = baseKfmiop.zAxis;

        double[] ratios = new double[xAxis.length];

        for (int i = 0; i < xAxis.length - 1; i++) {
            ratios[i] = xAxis[i] / xAxis[i + 1];
        }

        Double[][] optimalLoad = new Double[yAxis.length][xAxis.length];

        double currentMaxLoad;
        double maxBoost = 1;

        double maxTorque = 0;
        for (int i = 0; i < optimalLoad.length; i++) {
            for (int j = 0; j < optimalLoad[i].length; j++) {
                maxTorque = Math.max(zAxis[i][j], maxTorque);
            }
        }

        currentMaxLoad = (xAxis[xAxis.length - 1] / maxTorque) * 100;
        maxBoost = Math.max(Plsol.plsol(1013, maxBoost, 0, 96, 0.106, currentMaxLoad), maxBoost);

        // New Axis
        Double[] rescaledXAxis = RescaleAxis.rescaleAxis(xAxis, maxBoostPressureLoad);

        Double[][] kfmiop = new Double[yAxis.length][xAxis.length];
        Double[][] inputBoost = new Double[yAxis.length][xAxis.length];
        Double[][] outputBoost = new Double[yAxis.length][xAxis.length];

        for (int i = 0; i < optimalLoad.length; i++) {
            for (int j = 0; j < optimalLoad[i].length; j++) {
//                  rescaled
                kfmiop[i][j] = ((zAxis[i][j] / 100 * currentMaxLoad) / (zAxis[i][j] / 100 * maxMapSensorLoad) * zAxis[i][j]) * (rescaledXAxis[j] / xAxis[j]);
                // scalar
//                   kfmiop[i][j] =  zAxis[i][j]/100*currentMaxLoad;

                // boost
                inputBoost[i][j] = (Plsol.plsol(1013, 1013, 0, 96, 0.106, (zAxis[i][j] / 100 * currentMaxLoad)) - 1013) * 0.0145038;
                outputBoost[i][j] = (Plsol.plsol(1013, 1013, 0, 96, 0.106, (kfmiop[i][j] / 100 * maxMapSensorLoad)) - 1013) * 0.0145038;
            }
        }

//        System.out.println("boost");
//        for (int i = 0; i < inputBoost.length; i++) {
//            System.out.println(Arrays.toString(inputBoost[i]));
//        }
//
//        System.out.println("boost");
//        for (int i = 0; i < kfmiop.length; i++) {
//            System.out.println(Arrays.toString(kfmiop[i]));
//        }

        Map3d outputKfmiopMap = new Map3d(rescaledXAxis, yAxis, kfmiop);
        Map3d inputBoostMap = new Map3d(xAxis, yAxis, inputBoost);
        Map3d outputBoostMap = new Map3d(rescaledXAxis, yAxis, outputBoost);
        double maxMap = Plsol.plsol(1013, maxBoost, 0, 96, 0.106, currentMaxLoad);

        return new Kfmiop(outputKfmiopMap, inputBoostMap, outputBoostMap, maxMap, maxBoost);
    }
}
