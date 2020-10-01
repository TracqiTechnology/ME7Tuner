package model.closedloopfueling.mlfhm;

import contract.Me7LogFileContract;
import derivative.Derivative;
import math.Index;
import math.map.Map2d;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import util.Util;

import java.util.*;

public class ClosedLoopMlhfmCorrectionManager {

    private static int MIN_SAMPLES_THRESHOLD = 5;

    private final int lambdaControlEnabled = 1;
    private final double minThrottleAngle;
    private final double minRpm;
    private final double maxDerivative;

    private Map2d correctedMlhfm = new Map2d();
    private Map<Double, List<Double>> correctionsAfrMap = new HashMap<>();
    private Map<Double, List<Double>> filteredVoltageDt = new HashMap<>();
    private Map<Double, Double> meanAfrMap = new HashMap<>();
    private Map<Double, double[]> modeAfrMap = new HashMap<>();
    private Map<Double, Double> correctedAfrMap = new HashMap<>();

    private ClosedLoopMlhfmCorrection closedLoopMlhfmCorrection;

    public ClosedLoopMlhfmCorrectionManager(double minThrottleAngle, double minRpm, double maxDerivative) {
        this.minThrottleAngle = minThrottleAngle;
        this.minRpm = minRpm;
        this.maxDerivative = maxDerivative;
    }

    public ClosedLoopMlhfmCorrection getClosedLoopMlhfmCorrection() {
        return closedLoopMlhfmCorrection;
    }

    public void correct(Map<String, List<Double>> me7LogMap, Map2d mlhfm) {
        Map<Double, List<Double>> correctionErrorMap = new HashMap<>();

        for (Double voltage : mlhfm.axis) {
            correctionErrorMap.put(voltage, new ArrayList<>());
            filteredVoltageDt.put(voltage, new ArrayList<>());
            correctionsAfrMap.put(voltage, new ArrayList<>());
            meanAfrMap.put(voltage, 0d);
            modeAfrMap.put(voltage, new double[0]);
            correctedAfrMap.put(voltage, 0d);
        }

        calculateCorrections(correctionErrorMap, me7LogMap, mlhfm);

        List<Double> correctionErrorList = new ArrayList<>();

        int maxCorrectionIndex = processCorrections(correctionErrorList, correctionErrorMap, mlhfm);

        postProcessCorrections(correctionErrorList, maxCorrectionIndex);

        smooth(correctionErrorList);

        applyCorrections(correctionErrorList, mlhfm);

        closedLoopMlhfmCorrection = new ClosedLoopMlhfmCorrection(mlhfm, correctedMlhfm, filteredVoltageDt, correctionsAfrMap, meanAfrMap, modeAfrMap, correctedAfrMap);
    }

    private void calculateCorrections(Map<Double, List<Double>> correctionError, Map<String, List<Double>> me7LogMap, Map2d mlhfm) {
        List<Double> me7Voltages = me7LogMap.get(Me7LogFileContract.MAF_VOLTAGE_HEADER);
        List<Double> me7Timestamps = me7LogMap.get(Me7LogFileContract.TIME_COLUMN_HEADER);
        List<Double> me7voltageDt = Derivative.getDt(me7Voltages, me7Timestamps);
        List<Double> stft = me7LogMap.get(Me7LogFileContract.STFT_COLUMN_HEADER);
        List<Double> ltft = me7LogMap.get(Me7LogFileContract.LTFT_COLUMN_HEADER);
        List<Double> lambdaControl = me7LogMap.get(Me7LogFileContract.LAMBDA_CONTROL_ACTIVE_HEADER);
        List<Double> throttleAngle = me7LogMap.get(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER);
        List<Double> rpm = me7LogMap.get(Me7LogFileContract.RPM_COLUMN_HEADER);

        for (int i = 0; i < me7voltageDt.size(); i++) {
            // Closed loop only and not idle
            if (lambdaControl.get(i + 1) == lambdaControlEnabled && throttleAngle.get(i + 1) > minThrottleAngle && rpm.get(i + 1) > minRpm && me7voltageDt.get(i) < maxDerivative) {
                // Get every logged voltage
                double me7Voltage = me7Voltages.get(i + 1);
                // Look up the corresponding voltage from MLHFM
                int mlhfmVoltageIndex = Index.getInsertIndex(Arrays.asList(mlhfm.axis), me7Voltage);

                double mlhfmVoltageKey = mlhfm.axis[mlhfmVoltageIndex];

                double voltageScaler = me7Voltage/mlhfmVoltageKey;

                // Calculate the error based on LTFT and STFT
                double stftValue = (stft.get(i + 1) - 1)*voltageScaler;
                double ltftValue = (ltft.get(i + 1) - 1)*voltageScaler;
                double afrCorrectionError = stftValue + ltftValue;

                // Record the correction.
                correctionError.get(mlhfmVoltageKey).add(afrCorrectionError);

                // Keep track of the dt of the logged voltages relative to the MLHFM voltages
                filteredVoltageDt.get(mlhfmVoltageKey).add(me7voltageDt.get(i));
                correctionsAfrMap.get(mlhfmVoltageKey).add(afrCorrectionError);
            }
        }
    }

    private int processCorrections(List<Double> correctionErrorList, Map<Double, List<Double>> correctionErrorMap, Map2d mlhfm) {
        int maxCorrectionIndex = 0;
        int index = 0;
        Mean mean = new Mean();
        for (Double voltage : mlhfm.axis) {
            List<Double> corrections = correctionErrorMap.get(voltage);

            if (corrections.size() > MIN_SAMPLES_THRESHOLD) {
                // Get the mean of the correction set
                double meanValue = mean.evaluate(Util.toDoubleArray(corrections.toArray(new Double[0])), 0, corrections.size());
                // Get the mode of the correction set
                double[] mode = StatUtils.mode(Util.toDoubleArray(corrections.toArray(new Double[0])));

                meanAfrMap.put(voltage, meanValue);
                modeAfrMap.put(voltage, mode);

                double correction = meanValue;

                for (double v : mode) {
                    correction += v;
                }

                // Get the average of the mean and the mode
                correction /= 1 + mode.length;

                // Keep track of the largest index a correction was made at
                if (!Double.isNaN(correction)) {
                    maxCorrectionIndex = index;
                }

                correctionErrorList.add(correction);

                index++;
            } else {
                correctionErrorList.add(0d);
                index++;
            }
        }

        return maxCorrectionIndex;
    }

    private void postProcessCorrections(List<Double> correctionErrorList, int maxCorrectionIndex) {
        boolean foundStart = false;
        int lastValidCorrectionIndex = -1;
        // Fill in any missing corrections with 0 if it occurs before the first correction or last correction. Fill in
        // many missing corrections between the first and last correction with the last known correction.
        for (int i = 0; i < correctionErrorList.size(); i++) {
            Double value = correctionErrorList.get(i);
            if (value.isNaN() && !foundStart) {
                correctionErrorList.set(i, 0d);
            } else if (!value.isNaN() && !foundStart) {
                foundStart = true;
                lastValidCorrectionIndex = i;
            } else if (value.isNaN()) {
                if (i < maxCorrectionIndex) {
                    correctionErrorList.set(i, correctionErrorList.get(lastValidCorrectionIndex));
                } else {
                    correctionErrorList.set(i, 0d);
                }
            } else {
                lastValidCorrectionIndex = i;
            }
        }
    }

    private void smooth(List<Double> correctionErrorList) {
        // Smooth
        Mean mean = new Mean();
        double[] meanTempCorrections = Util.toDoubleArray(correctionErrorList.toArray(new Double[0]));
        for(int i = 0; i < meanTempCorrections.length; i++) {
            if(i > 2 && i < meanTempCorrections.length - 2) {
                correctionErrorList.set(i, mean.evaluate(meanTempCorrections, i - 2, 5));
            }
        }
    }

    private void applyCorrections(List<Double> correctionErrorList, Map2d mlhfm) {
        Map<Double, Double> totalCorrectionError = new HashMap<>();
        List<Double> voltage = Arrays.asList(mlhfm.axis);

        for (int i = 0; i < voltage.size(); i++) {
            totalCorrectionError.put(voltage.get(i), correctionErrorList.get(i));
            correctedAfrMap.put(voltage.get(i), correctionErrorList.get(i));
        }

        correctedMlhfm.axis = voltage.toArray(new Double[0]);

        List<Double> oldKgPerHour = Arrays.asList(mlhfm.data);
        List<Double> newKgPerHour = new ArrayList<>();

        for (int i = 0; i < voltage.size(); i++) {
            double oldKgPerHourValue = oldKgPerHour.get(i);
            double totalCorrectionErrorValue = totalCorrectionError.get(voltage.get(i));

            newKgPerHour.add(i, oldKgPerHourValue * ((totalCorrectionErrorValue) + 1));
        }

        correctedMlhfm.data = newKgPerHour.toArray(new Double[0]);
    }
}
