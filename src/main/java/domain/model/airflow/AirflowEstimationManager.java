package domain.model.airflow;

import data.contract.AfrLogFileContract;
import data.contract.Me7LogFileContract;
import domain.model.openloopfueling.util.AfrLogUtil;
import domain.model.openloopfueling.util.Me7LogUtil;
import domain.math.Index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AirflowEstimationManager {

    private static final int LAMBDA_CONTROL_ENABLED = 0;

    private final int minPointsMe7;
    private final int minPointsAfr;
    private final double minThrottleAngle;
    private final double minRpm;
    private final double maxAfr;

    private final double totalFuelFlowGramsPerMinute;

    private final List<List<Double>> estimatedAirflowGramsPerSecondLogs;
    private final List<List<Double>> measuredAirflowGramsPerSecondLogs;
    private final List<List<Double>> measuredRpmLogs;

    private AirflowEstimation airflowEstimation;

    public AirflowEstimationManager(double minThrottleAngle, double minRpm, int minPointsMe7, int minPointsAfr, double maxAfr, double fuelInjectorCubicCentimetersPerMinute, double numFuelInjectors, double gasolineGramsPerCubicCentimeter) {
        this.minThrottleAngle = minThrottleAngle;
        this.minRpm = minRpm;
        this.minPointsMe7 = minPointsMe7;
        this.minPointsAfr = minPointsAfr;
        this.maxAfr = maxAfr;

        this.totalFuelFlowGramsPerMinute = fuelInjectorCubicCentimetersPerMinute * numFuelInjectors * gasolineGramsPerCubicCentimeter;

        this.estimatedAirflowGramsPerSecondLogs = new ArrayList<>();
        this.measuredAirflowGramsPerSecondLogs = new ArrayList<>();
        this.measuredRpmLogs = new ArrayList<>();
    }

    public AirflowEstimation getAirflowEstimation() {
        return airflowEstimation;
    }

    public void estimate(Map<Me7LogFileContract.Header, List<Double>> me7LogMap, Map<String, List<Double>> afrLogMap) {
        List<Map<Me7LogFileContract.Header, List<Double>>> me7LogList = Me7LogUtil.findMe7Logs(me7LogMap, minThrottleAngle, LAMBDA_CONTROL_ENABLED, minRpm, minPointsMe7);
        List<Map<String, List<Double>>> afrLogList = AfrLogUtil.findAfrLogs(afrLogMap, minThrottleAngle, minRpm, maxAfr, minPointsAfr);

        List<List<Double>> dutyCycleLogs = new ArrayList<>();

        for(Map<Me7LogFileContract.Header, List<Double>> me7log:me7LogList) {
            List<Double> rpm = me7log.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER);
            List<Double> fuelInjectorOnTime = me7log.get(Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER);
            List<Double> gramsPerSecond = me7log.get(Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER);

            dutyCycleLogs.add(getInjectorDutyCycle(rpm, fuelInjectorOnTime));

            measuredAirflowGramsPerSecondLogs.add(gramsPerSecond);
            measuredRpmLogs.add(rpm);
        }

        int size = Math.min(dutyCycleLogs.size(), afrLogList.size());
        for(int i = 0; i < size; i++) {
            estimatedAirflowGramsPerSecondLogs.add(new ArrayList<>());

            List<Double> dutyCycleLog = dutyCycleLogs.get(i);
            List<Double> me7RpmLog = me7LogList.get(i).get(Me7LogFileContract.Header.RPM_COLUMN_HEADER);
            List<Double> afrRpmLog = afrLogList.get(i).get(AfrLogFileContract.RPM_HEADER);
            List<Double> afrLog = afrLogList.get(i).get(AfrLogFileContract.AFR_HEADER);
            for(int j = 0; j < dutyCycleLogs.get(i).size(); j++) {
                double totalFuelGramsPerSecond = ((dutyCycleLog.get(j) * totalFuelFlowGramsPerMinute))/60;
                int afrIndex = Index.getInsertIndex(afrRpmLog, me7RpmLog.get(j));
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
            if(rpm.get(i) <= 0) {
                dutyCycle.add(0.0);
                continue;
            }
            double engineCycleMs = (1/((rpm.get(i)/2.0)/60.0))*1000;
            double injectorOnTime = fuelInjectorOnTime.get(i);
            dutyCycle.add(injectorOnTime/engineCycleMs);
        }

        return dutyCycle;
    }
}
