package model.closedloopfueling.kfkhfm;

import contract.Me7LogFileContract;
import derivative.Derivative;
import math.map.Map3d;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import util.Util;

import java.util.*;

public class ClosedLoopKfkhfmCorrectionManager {

    private static int MIN_SAMPLES_THRESHOLD = 5;

    private final int lambdaControlEnabled = 1;
    private final double minThrottleAngle;
    private final double minRpm;
    private final double maxDerivative;

    private Map<Double, List<Double>> filteredLoadDt = new HashMap<>();
    private Map<Double, List<Double>> correctionsAfrMap = new HashMap<>();
    private Map<Double, Double> meanAfrMap = new HashMap<>();
    private Map<Double, double[]> modeAfrMap = new HashMap<>();

    public ClosedLoopKfkhfmCorrectionManager(double minThrottleAngle, double minRpm, double maxDerivative) {
        this.minThrottleAngle = minThrottleAngle;
        this.minRpm = minRpm;
        this.maxDerivative = maxDerivative;
    }

    public ClosedLoopKfkhfmCorrection correct(Map<String, List<Double>> me7LogMap, Map3d kfkhfm) {

        for(int i = 0; i < kfkhfm.xAxis.length; i++) {
            filteredLoadDt.put(kfkhfm.xAxis[i], new ArrayList<>());
            correctionsAfrMap.put(kfkhfm.xAxis[i], new ArrayList<>());
            meanAfrMap.put(kfkhfm.xAxis[i], 0d);
            modeAfrMap.put(kfkhfm.xAxis[i], new double[0]);
        }

        Map3d kfkhfmCorrected = new Map3d(kfkhfm);

        List<List<List<Double>>> corrections = new ArrayList<>();

        for (int i = 0; i < kfkhfmCorrected.data.length; i++) {
            corrections.add(new ArrayList<>());
            for (int j = 0; j < kfkhfmCorrected.data[i].length; j++) {
                corrections.get(i).add(new ArrayList<>());
            }
        }
        calculateCorrections(me7LogMap, kfkhfmCorrected, corrections);
        processCorrections(corrections, kfkhfmCorrected);

        return new ClosedLoopKfkhfmCorrection(kfkhfm, kfkhfmCorrected, filteredLoadDt, correctionsAfrMap, meanAfrMap, modeAfrMap);
    }

    private void calculateCorrections(Map<String, List<Double>> me7LogMap, Map3d kfkhfm, List<List<List<Double>>> corrections) {
        List<Double> me7Voltages = me7LogMap.get(Me7LogFileContract.MAF_VOLTAGE_HEADER);
        List<Double> me7Timestamps = me7LogMap.get(Me7LogFileContract.TIME_COLUMN_HEADER);
        List<Double> me7voltageDt = Derivative.getDt(me7Voltages, me7Timestamps);
        List<Double> stft = me7LogMap.get(Me7LogFileContract.STFT_COLUMN_HEADER);
        List<Double> ltft = me7LogMap.get(Me7LogFileContract.LTFT_COLUMN_HEADER);
        List<Double> lambdaControl = me7LogMap.get(Me7LogFileContract.LAMBDA_CONTROL_ACTIVE_HEADER);
        List<Double> throttleAngle = me7LogMap.get(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER);
        List<Double> rpm = me7LogMap.get(Me7LogFileContract.RPM_COLUMN_HEADER);
        List<Double> engineLoad = me7LogMap.get(Me7LogFileContract.ENGINE_LOAD_HEADER);

        double maxRpm = 0;
        double maxLoad = 0;

        int maxRpmIndex = 0;
        int maxLoadIndex = 0;

        for (int i = 0; i < me7voltageDt.size(); i++) {
            // Closed loop only and not idle
            if (lambdaControl.get(i + 1) == lambdaControlEnabled && throttleAngle.get(i +1) > minThrottleAngle && rpm.get(i + 1) > minRpm && me7voltageDt.get(i) < maxDerivative) {
                // Get every logged voltage
                double engineLoadValue = engineLoad.get(i + 1);
                double rpmValue = rpm.get(i + 1);

                if(rpmValue > maxRpm) {
                    maxRpm = rpmValue;
                }

                if(engineLoadValue > maxLoad) {
                    maxLoad = engineLoadValue;
                }

                // Look up the corresponding load from KFKHFM
                int kfkhfmLoadIndex = Arrays.binarySearch(kfkhfm.xAxis, engineLoadValue);
                if(kfkhfmLoadIndex < 0) {
                    kfkhfmLoadIndex = Math.abs(kfkhfmLoadIndex + 1);
                }

                int kfkhfmRpmIndex = Arrays.binarySearch(kfkhfm.yAxis, rpmValue);
                if(kfkhfmRpmIndex < 0) {
                    kfkhfmRpmIndex = Math.abs(kfkhfmRpmIndex + 1);
                }

                if(kfkhfmLoadIndex > maxLoadIndex) {
                    maxLoadIndex = kfkhfmLoadIndex;
                }

                if(kfkhfmRpmIndex > maxRpmIndex) {
                    maxRpmIndex = kfkhfmRpmIndex;
                }

                double loadScaler = engineLoadValue/kfkhfm.xAxis[kfkhfmLoadIndex];
                double rpmScaler = rpmValue/kfkhfm.yAxis[kfkhfmRpmIndex];

                // Calculate the error based on LTFT and STFT
                double stftValue = (stft.get(i + 1) - 1)*loadScaler*rpmScaler;
                double ltftValue = (ltft.get(i + 1) - 1)*loadScaler*rpmScaler;
                double afrCorrectionError = stftValue + ltftValue;

                // Record the correction.
                corrections.get(kfkhfmRpmIndex).get(kfkhfmLoadIndex).add(afrCorrectionError);
                filteredLoadDt.get(kfkhfm.xAxis[kfkhfmLoadIndex]).add(me7voltageDt.get(i));
                correctionsAfrMap.get(kfkhfm.xAxis[kfkhfmLoadIndex]).add(afrCorrectionError);
            }
        }
    }

    private void processCorrections(List<List<List<Double>>> corrections, Map3d kfkhfm) {
        Mean mean = new Mean();
        for (int i = 0; i < corrections.size(); i++) {
            for (int j = 0; j < corrections.get(i).size(); j++) {

                if (corrections.get(i).get(j).size() > MIN_SAMPLES_THRESHOLD) {
                    // Get the mean of the correction set
                    double meanValue = mean.evaluate(Util.toDoubleArray(corrections.get(i).get(j).toArray(new Double[0])), 0, corrections.get(i).get(j).size());
                    // Get the mode of the correction set
                    double[] mode = StatUtils.mode(Util.toDoubleArray(corrections.get(i).get(j).toArray(new Double[0])));

                    meanAfrMap.put(kfkhfm.xAxis[j], meanValue);
                    modeAfrMap.put(kfkhfm.xAxis[j], mode);

                    System.out.println("Load: " + kfkhfm.xAxis[j] + " Mean: " + meanValue);

                    double correction = meanValue;

                    for (double v : mode) {
                        correction += v;
                    }

                    // Get the average of the mean and the mode
                    correction /= 1 + mode.length;

                    System.out.println("Correction: " + correction);

                    // Keep track of the largest index a correction was made at
                    if (!Double.isNaN(correction)) {
                        kfkhfm.data[i][j] *= 1 + correction;
                    }
                }
            }
        }
    }
}
