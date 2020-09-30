package model.kfurl;

import contract.AfrLogFileContract;
import contract.Me7LogFileContract;
import math.map.Map3d;

import java.util.*;

public class KfurlCalculator {
    /**
     * Percentage(x,a,b)=(x−a)/(b−a)
     * @param a The smaller number
     * @param b The larger number
     * @param x The number to evaluate between a and b
     * @return The proportion of x between a and b
     */
    private static double proportion(double a, double b, double x) {
        if(a >= b) {
            throw new IllegalArgumentException("b must be greater than a! ->" + "b: " + b + " a: " + a);
        }

        return (x-a)/(b-a);
    }

    private static int getInsertIndex(List<Double> values, double value) {
        int index = Collections.binarySearch(values, value);

        if (index < 0) {
            index = Math.abs(index + 1);
        }

        index = Math.max(index, 0);
        index = Math.min(index, values.size() - 1);

        // binarySearch() always returns the index with the greater value, even if the input is closer to the lesser value
        if(index > 0) {
            double a = values.get(index - 1);
            double b = values.get(index);
            double proportion = proportion(a, b, value);

            // Is the input closer to the lesser value?
            if(proportion < 0.50) {
                index--;
            }
        }

        return index;
    }

    public static KfurlCorrection calculateKfurl(Map3d kfurl, Map<String, List<Double>> me7LogMap, Map<String, List<Double>> zeitLogMap) {
        List<Double> zeitTimestamps = zeitLogMap.get(AfrLogFileContract.TIMESTAMP);
        List<Double> zeitRelativeBoosts = zeitLogMap.get(AfrLogFileContract.BOOST_HEADER);

        List<Double> me7ModeledBoosts = me7LogMap.get(Me7LogFileContract.ABSOLUTE_BOOST_PRESSURE_MODELED_HEADER);
        List<Double> me7Timestamps = me7LogMap.get(Me7LogFileContract.TIME_COLUMN_HEADER);
        List<Double> me7Rpms = me7LogMap.get(Me7LogFileContract.RPM_COLUMN_HEADER);

        double[][] sums = new double[Kfurl.getYAxis().length][Kfurl.getXAxis().length];
        double[][] counts = new double[Kfurl.getYAxis().length][Kfurl.getXAxis().length];

        for(int i = 0; i < zeitTimestamps.size(); i++) {

            if (zeitLogMap.get(AfrLogFileContract.TPS_HEADER).get(i) > 80) {

                double zeitBoost = zeitRelativeBoosts.get(i);

                double zeitTimestamp = zeitTimestamps.get(i);
                int me7TimestampIndex = getInsertIndex(me7Timestamps, zeitTimestamp);

                double me7Boost = me7ModeledBoosts.get(me7TimestampIndex);

                double correction = me7Boost / zeitBoost;

                double me7Rpm = me7Rpms.get(me7TimestampIndex);

                int kfurlRpmIndex = getInsertIndex(Arrays.asList(Kfurl.getXAxis()), me7Rpm);

                for (int j = 0; j < Kfurl.getYAxis().length; j++) {
                    sums[j][kfurlRpmIndex] += correction;
                    counts[j][kfurlRpmIndex] += 1;
                }
            }
        }

        Double[][] correction = new Double[Kfurl.getYAxis().length][Kfurl.getXAxis().length];
        for(int i = 0; i < correction.length; i++) {
            for(int j = 0; j < correction[i].length; j++) {
                correction[i][j] = sums[i][j]/counts[i][j];

                if(Double.isNaN(correction[i][j])) {
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

        Map3d result = new Map3d(kfurl);

        for(int i = 0; i < kfurl.data.length; i++) {
            for(int j = 0; j < kfurl.data[i].length; j++) {
                result.data[i][j] = kfurl.data[i][j] * correction[i][j];
            }
        }

        return new KfurlCorrection(result, correction);
    }
}
