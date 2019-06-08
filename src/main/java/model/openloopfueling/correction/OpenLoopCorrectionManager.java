package model.openloopfueling.correction;

import contract.AfrLogFileContract;
import contract.Me7LogFileContract;
import math.map.Map2d;
import model.openloopfueling.util.AfrLogUtil;
import model.openloopfueling.util.Me7LogUtil;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.util.*;

public class OpenLoopCorrectionManager {
    private final int minPointsMe7;
    private final int minPointsAfr;
    private final double minThrottleAngle;
    private final int lambdaControlEnabled = 0;
    private final double minRpm;
    private final double maxAfr;

    private Map2d correctedMlhfm = new Map2d();
    private Map<Double, List<Double>> correctionsAfrMap = new HashMap<>();

    public final Map<Double, Double> meanAfrMap = new HashMap<>();
    public final Map<Double, double[]> modeAfrMap = new HashMap<>();
    public final Map<Double, Double> correctedAfrMap = new HashMap<>();

    private OpenLoopCorrection openLoopCorrection;

    public OpenLoopCorrectionManager(double minThrottleAngle, double minRpm, int minPointsMe7, int minPointsAfr, double maxAfr) {
        this.minThrottleAngle = minThrottleAngle;
        this.minRpm = minRpm;
        this.minPointsMe7 = minPointsMe7;
        this.minPointsAfr = minPointsAfr;
        this.maxAfr = maxAfr;
    }

    public void correct(Map<String, List<Double>> me7Log, Map<String, List<Double>> afrLog, Map2d mlhfm) {

        List<Map<String, List<Double>>> me7LogList = Me7LogUtil.findMe7Logs(me7Log, minThrottleAngle, lambdaControlEnabled, minRpm, minPointsMe7);
        List<Map<String, List<Double>>> afrLogList = AfrLogUtil.findAfrLogs(afrLog, minThrottleAngle, minRpm, maxAfr, minPointsAfr);

        generateMlhfm(mlhfm, me7LogList, afrLogList);

        openLoopCorrection = new OpenLoopCorrection(mlhfm, correctedMlhfm, correctionsAfrMap, meanAfrMap, modeAfrMap, correctedAfrMap);
    }

    public OpenLoopCorrection getOpenLoopCorrection() {
        return openLoopCorrection;
    }

    private void generateMlhfm(Map2d mlhfm, List<Map<String, List<Double>>> me7LogList, List<Map<String, List<Double>>> afrLogList) {
        if (me7LogList.size() != afrLogList.size()) {
            throw new IllegalArgumentException("ME7 Log size does not match AFR Log size! " + me7LogList.size() + " AFR: " + afrLogList.size());
        } else if (me7LogList.size() == 0) {
            throw new IllegalArgumentException("Not enough log files! " + "ME7: " + me7LogList.size() + " AFR: " + afrLogList.size());
        }

        List<Double> mlhfmVoltage = Arrays.asList(mlhfm.axis);

        // Calculate the initial corrections sets
        calculateCorrections(me7LogList, afrLogList, mlhfmVoltage);
        // Process the corrections sets into a single correction
        ArrayList<Double> correctedAfrList = processCorrections(mlhfmVoltage);
        // Clean up the corrections
        postProcessCorrections(correctedAfrList);
        // Smooth the corrections
        smooth(correctedAfrList, 5);
        applyCorrections(mlhfm, correctedAfrList);
    }

    private void applyCorrections(Map2d mlhfm, ArrayList<Double> correctedAfrList) {
        Map<Double, Double> totalCorrectionError = new HashMap<>();
        List<Double> voltage = Arrays.asList(mlhfm.axis);

        for (int i = 0; i < voltage.size(); i++) {
            totalCorrectionError.put(voltage.get(i), correctedAfrList.get(i));
            correctedAfrMap.put(voltage.get(i), correctedAfrList.get(i));
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

    private void smooth(ArrayList<Double> correctedAfrList, int window) {
        int halfWindow = window / 2;

        // Smooth
        double[] meanTempCorrections = toDoubleArray(correctedAfrList.toArray(new Double[0]));
        Mean mean = new Mean();
        for (int i = 0; i < meanTempCorrections.length; i++) {
            if (i > halfWindow && i < meanTempCorrections.length - halfWindow) {
                correctedAfrList.set(i, mean.evaluate(meanTempCorrections, i - halfWindow, window));
            }
        }
    }

    private void postProcessCorrections(ArrayList<Double> correctedAfrList) {

        // From the last known correction, fills out the list with the last known correction or 0 if a correction has not yet been found
        boolean foundStart = false;
        int lastValidCorrectionIndex = -1;
        for (int i = 0; i < correctedAfrList.size(); i++) {
            Double value = correctedAfrList.get(i);
            if (value.isNaN() && !foundStart) {
                correctedAfrList.set(i, 0d);
            } else if (!value.isNaN() && !foundStart) {
                foundStart = true;
                lastValidCorrectionIndex = i;
            } else if (value.isNaN()) {
                correctedAfrList.set(i, correctedAfrList.get(lastValidCorrectionIndex));
            } else {
                lastValidCorrectionIndex = i;
            }
        }
    }

    private ArrayList<Double> processCorrections(List<Double> mlhfmVoltage) {
        ArrayList<Double> correctedAfrList = new ArrayList<>();

        Mean mean = new Mean();
        for (Double voltage : mlhfmVoltage) {
            List<Double> corrections = correctionsAfrMap.get(voltage);
            // Get the mean of the correction set
            double meanValue = mean.evaluate(toDoubleArray(corrections.toArray(new Double[0])), 0, corrections.size());
            meanAfrMap.put(voltage, meanValue);
            // Get the mode of the correction set
            double[] mode = StatUtils.mode(toDoubleArray(corrections.toArray(new Double[0])));
            modeAfrMap.put(voltage, mode);

            // Get the average of the mean and the mode
            double correction = meanValue;
            for (double v : mode) {
                correction += v;
            }
            correction /= 1 + mode.length;

            correctedAfrList.add(correction);
        }

        return correctedAfrList;
    }

    private void calculateCorrections(List<Map<String, List<Double>>> me7LogList, List<Map<String, List<Double>>> afrLogList, List<Double> mlhfmVoltageList) {
        // Loop over each log
        for (int i = 0; i < me7LogList.size(); i++) {
            Map<String, List<Double>> me7Log = me7LogList.get(i);
            Map<String, List<Double>> afrLog = afrLogList.get(i);

            // For each log, loop over the voltages in MLHFM and attempt to calculate a correction
            for (int j = 0; j < mlhfmVoltageList.size(); j++) {

                double mlhfmVoltage = mlhfmVoltageList.get(j);

                correctionsAfrMap.put(mlhfmVoltage, new ArrayList<>());

                // Get the measured MAF voltages in the log
                List<Double> me7VoltageList = me7Log.get(Me7LogFileContract.MAF_VOLTAGE_HEADER);

                // Attempt to find the mlhfm voltages in the log.
                List<Integer> me7VoltageIndices = getVoltageToMatchIndices(j, mlhfmVoltageList, me7VoltageList);

                // Calculate a corrected AFR for each index that is found
                for (int me7Index : me7VoltageIndices) {
                    if (me7Index != 1 && me7Index != me7VoltageList.size() - 1) {
                        double stft = me7Log.get(Me7LogFileContract.STFT_COLUMN_HEADER).get(me7Index) - 1;
                        double ltft = me7Log.get(Me7LogFileContract.LTFT_COLUMN_HEADER).get(me7Index) - 1;
                        double rpm = me7Log.get(Me7LogFileContract.RPM_COLUMN_HEADER).get(me7Index);
                        double targetAfr = me7Log.get(Me7LogFileContract.REQUESTED_LAMBDA_HEADER).get(me7Index);

                        // Find the RPM from the ME7 log in the AFR log
                        int afrIndex = Collections.binarySearch(afrLog.get(AfrLogFileContract.RPM_HEADER), rpm);
                        if(afrIndex < 0) {
                            afrIndex = Math.abs(afrIndex + 1);
                        }

                        List<Double> afrList = afrLog.get(AfrLogFileContract.AFR_HEADER);

                        // Now find the AFR that corresponds to the RPM in the AFR Log
                        double afr = afrList.get(Math.min(afrIndex, afrList.size() - 1)) / 14.7;

                        // Calculate a correction accounting for STFT and LTFT
                        double rawAfr = afr / (1 - (stft + ltft));
                        double afrCorrection = (rawAfr / targetAfr) - 1;

                        correctionsAfrMap.get(mlhfmVoltage).add(afrCorrection);
                    } else {
                        correctionsAfrMap.get(mlhfmVoltage).add(Double.NaN);
                    }
                }
            }
        }
    }

    private List<Integer> getVoltageToMatchIndices(int mlhfmVoltageToMatchIndex, List<Double> mlhfmVoltageList, List<Double> me7VoltageList) {
        int previousIndex = mlhfmVoltageToMatchIndex - 1;
        int nextIndex = mlhfmVoltageToMatchIndex + 1;

        double lowValue = previousIndex >= 0 ? mlhfmVoltageList.get(previousIndex) : mlhfmVoltageList.get(mlhfmVoltageToMatchIndex) - 0.0001;
        double highValue = nextIndex <= mlhfmVoltageList.size() - 1 ? mlhfmVoltageList.get(nextIndex) : mlhfmVoltageList.get(mlhfmVoltageToMatchIndex) + 0.0001;

        ArrayList<Integer> indices = new ArrayList<>();

        for (int i = 0; i < me7VoltageList.size(); i++) {
            double voltage = me7VoltageList.get(i);

            if (voltage > lowValue && voltage < highValue) {
                indices.add(i);
            }
        }

        return indices;
    }

    private double[] toDoubleArray(Double[] array) {
        double[] result = new double[array.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = array[i];
        }

        return result;
    }
}
