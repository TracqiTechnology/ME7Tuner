package domain.model.airflow

data class AirflowEstimation(
    val estimatedAirflowGramsPerSecondLogs: List<List<Double>>,
    val measuredAirflowGramsPerSecondLogs: List<List<Double>>,
    val measuredRpmLogs: List<List<Double>>
)
