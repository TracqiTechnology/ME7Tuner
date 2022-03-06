package domain.model.kfurl;

import data.contract.AfrLogFileContract;
import data.contract.Me7LogFileContract;
import domain.math.Index;
import domain.math.map.Map3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KfurlCalculator {


    public static KfurlCorrection calculateKfurl(Map3d kfurl, Map<Me7LogFileContract.Header, List<Double>> me7LogMap, Map<String, List<Double>> zeitLogMap) {
        if(me7LogMap == null || zeitLogMap == null) {
            return null;
        }

        List<Double> zeitTimestamps = zeitLogMap.get(AfrLogFileContract.TIMESTAMP);
        List<Double> zeitRelativeBoosts = zeitLogMap.get(AfrLogFileContract.BOOST_HEADER);

        List<Double> me7ModeledBoosts = me7LogMap.get(Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_MODELED_HEADER);
        List<Double> me7Timestamps = me7LogMap.get(Me7LogFileContract.Header.TIME_COLUMN_HEADER);
        List<Double> me7Rpms = me7LogMap.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER);

        List<List<Double>> corrections = new ArrayList<>();

        for(int i = 0; i < Kfurl.getXAxis().length; i++) {
            corrections.add(new ArrayList<>());
        }

        double[][] sums = new double[Kfurl.getYAxis().length][Kfurl.getXAxis().length];
        double[][] counts = new double[Kfurl.getYAxis().length][Kfurl.getXAxis().length];

        for(int i = 0; i < zeitTimestamps.size(); i++) {

            if (zeitLogMap.get(AfrLogFileContract.TPS_HEADER).get(i) > 80) {

                double zeitBoost = zeitRelativeBoosts.get(i);

                double zeitTimestamp = zeitTimestamps.get(i);
                int me7TimestampIndex = Index.getInsertIndex(me7Timestamps, zeitTimestamp);

                double me7Boost = me7ModeledBoosts.get(me7TimestampIndex);

                double correction = me7Boost / zeitBoost;

                double me7Rpm = me7Rpms.get(me7TimestampIndex);

                int kfurlRpmIndex = Index.getInsertIndex(Arrays.asList(Kfurl.getXAxis()), me7Rpm);

                corrections.get(kfurlRpmIndex).add(correction);

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

        for(int i = 0; i < kfurl.zAxis.length; i++) {
            for(int j = 0; j < kfurl.zAxis[i].length; j++) {
                result.zAxis[i][j] = kfurl.zAxis[i][j] * correction[i][j];
            }
        }

        return new KfurlCorrection(result, correction, corrections);
    }
}
