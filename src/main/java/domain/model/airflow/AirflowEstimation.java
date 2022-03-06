package domain.model.airflow;

import java.util.List;

public class AirflowEstimation {
    public final List<List<Double>> estimatedAirflowGramsPerSecondLogs;
    public final List<List<Double>> measuredAirflowGramsPerSecondLogs;
    public final List<List<Double>> measuredRpmLogs;

    public AirflowEstimation(List<List<Double>> estimatedAirflowGramsPerSecondLogs, List<List<Double>> measuredAirflowGramsPerSecondLogs, List<List<Double>> measuredRpmLogs) {
        this.estimatedAirflowGramsPerSecondLogs = estimatedAirflowGramsPerSecondLogs;
        this.measuredAirflowGramsPerSecondLogs = measuredAirflowGramsPerSecondLogs;
        this.measuredRpmLogs = measuredRpmLogs;
    }
}
