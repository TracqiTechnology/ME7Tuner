package model.closedloopfueling.kfkhfm;

import contract.Me7LogFileContract;
import derivative.Derivative;
import math.map.Map3d;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import util.Util;

import java.util.*;

public class ClosedLoopKfkhfmCorrectionManager {

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

        for (int i = 0; i < stft.size(); i++) {
            // Closed loop only and not idle
            if (lambdaControl.get(i) == lambdaControlEnabled && throttleAngle.get(i) > minThrottleAngle && rpm.get(i) > minRpm && me7voltageDt.get(i) < maxDerivative) {
                // Get every logged voltage
                double engineLoadValue = engineLoad.get(i);
                double rpmValue = rpm.get(i);

                // Look up the corresponding load from KFKHFM
                int kvkhfmLoadIndex = Math.abs(Arrays.binarySearch(kfkhfm.xAxis, engineLoadValue));
                int kvkhfmRpmIndex = Math.abs(Arrays.binarySearch(kfkhfm.yAxis, rpmValue));

                // Calculate the error based on LTFT and STFT
                double stftValue = stft.get(i) - 1;
                double ltftValue = ltft.get(i) - 1;
                double afrCorrectionError = stftValue + ltftValue;

                // Record the correction.
                corrections.get(kvkhfmLoadIndex).get(kvkhfmRpmIndex).add(afrCorrectionError);
                filteredLoadDt.get(kfkhfm.xAxis[kvkhfmLoadIndex]).add(me7voltageDt.get(i));
                correctionsAfrMap.get(kfkhfm.xAxis[kvkhfmLoadIndex]).add(afrCorrectionError);
            }
        }
    }

    private void processCorrections(List<List<List<Double>>> corrections, Map3d kfkhfm) {
        Mean mean = new Mean();
        for (int i = 0; i < corrections.size(); i++) {
            for (int j = 0; j < corrections.get(i).size(); j++) {
                // Get the mean of the correction set
                double meanValue = mean.evaluate(Util.toDoubleArray(corrections.get(i).get(j).toArray(new Double[0])), 0, corrections.get(i).get(j).size());
                // Get the mode of the correction set
                double[] mode = StatUtils.mode(Util.toDoubleArray(corrections.get(i).get(j).toArray(new Double[0])));

                meanAfrMap.put(kfkhfm.xAxis[i], meanValue);
                modeAfrMap.put(kfkhfm.xAxis[i], mode);

                double correction = meanValue;

                for (double v : mode) {
                    correction += v;
                }

                // Get the average of the mean and the mode
                correction /= 1 + mode.length;

                // Keep track of the largest index a correction was made at
                if (!Double.isNaN(correction)) {
                    kfkhfm.data[i][j] *= 1 + correction;
                }
            }
        }
    }
}
