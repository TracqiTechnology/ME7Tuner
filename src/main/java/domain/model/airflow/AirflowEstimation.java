package domain.model.airflow;

import java.util.List;

public record AirflowEstimation(List<List<Double>> estimatedAirflowGramsPerSecondLogs,
                                List<List<Double>> measuredAirflowGramsPerSecondLogs,
                                List<List<Double>> measuredRpmLogs) {

    @Override
    public List<List<Double>> estimatedAirflowGramsPerSecondLogs() {
        return estimatedAirflowGramsPerSecondLogs;
    }

    @Override
    public List<List<Double>> measuredAirflowGramsPerSecondLogs() {
        return measuredAirflowGramsPerSecondLogs;
    }

    @Override
    public List<List<Double>> measuredRpmLogs() {
        return measuredRpmLogs;
    }
}
