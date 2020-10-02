package model.wdkugdn;

import contract.Me7LogFileContract;
import math.Index;
import math.map.Map3d;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WdkugdnCalculator {

    public static WdkugdnCorrection calculateWdkugdn(Map3d wdkugdn, Map<String, List<Double>> me7LogMap) {
        List<Double> throttlePlateAngle = me7LogMap.get(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER);
        List<Double> maf = me7LogMap.get(Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER);
        List<Double> mafAtThrottlePlate = me7LogMap.get(Me7LogFileContract.MAF_AT_THROTTLE_PLATE);
        List<Double> rpm = me7LogMap.get(Me7LogFileContract.RPM_COLUMN_HEADER);

        double[][] sums = new double[Wdkugdn.getYAxis().length][Wdkugdn.getXAxis().length];
        double[][] counts = new double[Wdkugdn.getYAxis().length][Wdkugdn.getXAxis().length];

        for (int i = 0; i < throttlePlateAngle.size(); i++) {

            if (throttlePlateAngle.get(i) >= 0) {
                double mafValue = maf.get(i);
                double mafAtThrottlePlateValue = mafAtThrottlePlate.get(i);

                double correction = mafValue / mafAtThrottlePlateValue;
                double me7Rpm = rpm.get(i);

                int wdkugdnRpmIndex = Index.getInsertIndex(Arrays.asList(Wdkugdn.getXAxis()), me7Rpm);

                sums[0][wdkugdnRpmIndex] += correction;
                counts[0][wdkugdnRpmIndex] += 1;
            }
        }

        Double[][] correction = new Double[Wdkugdn.getYAxis().length][Wdkugdn.getXAxis().length];
        for (int i = 0; i < correction.length; i++) {
            for (int j = 0; j < correction[i].length; j++) {
                correction[i][j] = sums[i][j] / counts[i][j];

                if (Double.isNaN(correction[i][j]) || counts[i][j]< 50) {
                    correction[i][j] = 1d;
                }
            }
        }

//        System.out.println("Sums");
//        for(double[] array: sums) {
//            System.out.println(Arrays.toString(array));
//        }
//
//        System.out.println("Counts");
//        for(double[] array: counts) {
//            System.out.println(Arrays.toString(array));
//        }
//
//        System.out.println("Corrections");
//        for(Double[] array: correction) {
//            System.out.println(Arrays.toString(array));
//        }

        Map3d result = new Map3d(wdkugdn);

        for (int i = 0; i < wdkugdn.data.length; i++) {
            for (int j = 0; j < wdkugdn.data[i].length; j++) {
                result.data[i][j] = wdkugdn.data[i][j] * correction[i][j];
            }
        }

        return new WdkugdnCorrection(result, correction);
    }
}
