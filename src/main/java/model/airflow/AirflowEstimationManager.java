package model.airflow;

import contract.AfrLogFileContract;
import contract.Me7LogFileContract;
import model.openloopfueling.util.AfrLogUtil;
import model.openloopfueling.util.Me7LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AirflowEstimationManager {

    private final int minPointsMe7;
    private final int minPointsAfr;
    private final double minThrottleAngle;
    private final int lambdaControlEnabled = 0;
    private final double minRpm;
    private final double maxAfr;

    private final double totalFuelFlowGramsPerMinute;
    private final double totalMethanolFlowGramsPerMinute;

    private List<List<Double>> estimatedAirflowGramsPerSecondLogs;
    private List<List<Double>> measuredAirflowGramsPerSecondLogs;
    private List<List<Double>> measuredRpmLogs;

    private AirflowEstimation airflowEstimation;

    public AirflowEstimationManager(double minThrottleAngle, double minRpm, int minPointsMe7, int minPointsAfr, double maxAfr, double fuelInjectorCubicCentimetersPerMinute, double numFuelInjectors, double methanolInjectorCubicCentimetersPerMinute, double numMethanolInjectors, double gasolineGramsPerCubicCentimeter, double methanolGramsPerCubicCentimeter) {
        this.minThrottleAngle = minThrottleAngle;
        this.minRpm = minRpm;
        this.minPointsMe7 = minPointsMe7;
        this.minPointsAfr = minPointsAfr;
        this.maxAfr = maxAfr;

        this.totalFuelFlowGramsPerMinute = fuelInjectorCubicCentimetersPerMinute *numFuelInjectors* gasolineGramsPerCubicCentimeter;
        this.totalMethanolFlowGramsPerMinute = methanolInjectorCubicCentimetersPerMinute *numMethanolInjectors* methanolGramsPerCubicCentimeter;

        this.estimatedAirflowGramsPerSecondLogs = new ArrayList<>();
        this.measuredAirflowGramsPerSecondLogs = new ArrayList<>();
        this.measuredRpmLogs = new ArrayList<>();
    }

    public AirflowEstimation getAirflowEstimation() {
        return airflowEstimation;
    }

    public void estimate(Map<String, List<Double>> me7LogMap, Map<String, List<Double>> afrLogMap) {
        List<Map<String, List<Double>>> me7LogList = Me7LogUtil.findMe7Logs(me7LogMap, minThrottleAngle, lambdaControlEnabled, minRpm, minPointsMe7);
        List<Map<String, List<Double>>> afrLogList = AfrLogUtil.findAfrLogs(afrLogMap, minThrottleAngle, minRpm, maxAfr, minPointsAfr);

        List<List<Double>> dutyCycleLogs = new ArrayList<>();

        for(Map<String, List<Double>> me7log:me7LogList) {
            List<Double> rpm = me7log.get(Me7LogFileContract.RPM_COLUMN_HEADER);
            List<Double> fuelInjectorOnTime = me7log.get(Me7LogFileContract.FUEL_INJECTOR_ON_TIME_HEADER);
            List<Double> gramsPerSecond = me7log.get(Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER);

            dutyCycleLogs.add(getInjectorDutyCycle(rpm, fuelInjectorOnTime));

            measuredAirflowGramsPerSecondLogs.add(gramsPerSecond);
            measuredRpmLogs.add(rpm);
        }

        for(int i = 0; i < dutyCycleLogs.size(); i++) {
            estimatedAirflowGramsPerSecondLogs.add(new ArrayList<>());

            List<Double> dutyCycleLog = dutyCycleLogs.get(i);
            List<Double> me7RpmLog = me7LogList.get(i).get(Me7LogFileContract.RPM_COLUMN_HEADER);
            List<Double> afrRpmLog = afrLogList.get(i).get(AfrLogFileContract.RPM_HEADER);
            List<Double> afrLog = afrLogList.get(i).get(AfrLogFileContract.AFR_HEADER);
            for(int j = 0; j < dutyCycleLogs.get(i).size(); j++) {
                double totalFuelGramsPerSecond = ((dutyCycleLog.get(j) * totalFuelFlowGramsPerMinute) + totalMethanolFlowGramsPerMinute)/60;
                int afrIndex = Collections.binarySearch(afrRpmLog, me7RpmLog.get(j));
                if(afrIndex < 0) {
                    afrIndex = Math.abs(afrIndex + 1);
                }
                double afr = afrLog.get(Math.min(afrIndex, afrLog.size() - 1));
                double airflowGramsPerSecond = totalFuelGramsPerSecond*afr;
                estimatedAirflowGramsPerSecondLogs.get(i).add(airflowGramsPerSecond);
            }
        }

        airflowEstimation = new AirflowEstimation(estimatedAirflowGramsPerSecondLogs, measuredAirflowGramsPerSecondLogs, measuredRpmLogs);
    }

    private List<Double> getInjectorDutyCycle(List<Double> rpm, List<Double> fuelInjectorOnTime) {
        List<Double> dutyCycle = new ArrayList<>();

        for(int i = 0; i < rpm.size(); i++) {
            double engineCycleMs = (1/((rpm.get(i)/2.0)/60.0))*1000;
            double injectorOnTime = fuelInjectorOnTime.get(i);
            dutyCycle.add(injectorOnTime/engineCycleMs);
        }

        return dutyCycle;
    }
}
