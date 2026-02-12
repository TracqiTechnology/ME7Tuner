package ui.screens.plsol

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.preferences.plsol.PlsolPreferences
import domain.model.plsol.Airflow
import domain.model.plsol.Horsepower
import domain.model.plsol.Plsol
import ui.components.ChartSeries
import ui.components.LineChart
import ui.theme.ChartRed
import ui.theme.Primary

@Composable
fun PlsolScreen() {
    var barometricPressure by remember { mutableStateOf(PlsolPreferences.barometricPressure.toString()) }
    var intakeAirTemp by remember { mutableStateOf(PlsolPreferences.intakeAirTemperature.toString()) }
    var kfurl by remember { mutableStateOf(PlsolPreferences.kfurl.toString()) }
    var displacement by remember { mutableStateOf(PlsolPreferences.displacement.toString()) }
    var maxRpm by remember { mutableStateOf(PlsolPreferences.rpm.toString()) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Load", "Airflow", "Power")

    // Calculate all chart data
    val chartData by remember(barometricPressure, intakeAirTemp, kfurl, displacement, maxRpm) {
        derivedStateOf {
            val pu = barometricPressure.toDoubleOrNull() ?: 1013.0
            val tans = intakeAirTemp.toDoubleOrNull() ?: 20.0
            val kfurlVal = kfurl.toDoubleOrNull() ?: 0.1037
            val dispVal = displacement.toDoubleOrNull() ?: 2.7
            val rpmVal = maxRpm.toIntOrNull() ?: 6000

            val plsol = Plsol(pu, tans, kfurlVal)

            // Pressure/Load chart: absolute and relative (boost) PSI
            val absolutePoints = plsol.points.map { p -> Pair(p.x, p.y * 0.0145038) }
            val relativePoints = plsol.points.map { p -> Pair(p.x, (p.y - pu) * 0.0145038) }

            // Airflow chart
            val airflow = Airflow(plsol.points, dispVal, rpmVal)
            val airflowPoints = airflow.points.map { p -> Pair(p.x, p.y) }

            // Horsepower chart
            val horsepower = Horsepower(airflow.points)
            val horsepowerPoints = horsepower.points.map { p -> Pair(p.x, p.y) }

            PlsolChartData(absolutePoints, relativePoints, airflowPoints, horsepowerPoints)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Charts area with tabs
        Column(modifier = Modifier.weight(1f)) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                when (selectedTab) {
                    0 -> {
                        LineChart(
                            series = listOf(
                                ChartSeries(
                                    name = "Requested Absolute",
                                    points = chartData.absolutePoints,
                                    color = Primary
                                ),
                                ChartSeries(
                                    name = "Requested Relative (Boost)",
                                    points = chartData.relativePoints,
                                    color = ChartRed
                                )
                            ),
                            title = "PLSOL",
                            xAxisLabel = "Requested Load",
                            yAxisLabel = "PSI"
                        )
                    }
                    1 -> {
                        LineChart(
                            series = listOf(
                                ChartSeries(
                                    name = "Airflow",
                                    points = chartData.airflowPoints,
                                    color = Primary
                                )
                            ),
                            title = "Airflow",
                            xAxisLabel = "Requested Load",
                            yAxisLabel = "g/sec"
                        )
                    }
                    2 -> {
                        LineChart(
                            series = listOf(
                                ChartSeries(
                                    name = "Horsepower",
                                    points = chartData.horsepowerPoints,
                                    color = Primary
                                )
                            ),
                            title = "Horsepower",
                            xAxisLabel = "Requested Load",
                            yAxisLabel = "bhp"
                        )
                    }
                }
            }
        }

        // Constants panel at bottom
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlsolConstantField(
                    label = "Barometric Pressure",
                    value = barometricPressure,
                    unit = "mbar",
                    onValueChange = {
                        barometricPressure = it
                        it.toDoubleOrNull()?.let { v -> PlsolPreferences.barometricPressure = v }
                    },
                    modifier = Modifier.weight(1f)
                )
                PlsolConstantField(
                    label = "Intake Air Temperature",
                    value = intakeAirTemp,
                    unit = "C",
                    onValueChange = {
                        intakeAirTemp = it
                        it.toDoubleOrNull()?.let { v -> PlsolPreferences.intakeAirTemperature = v }
                    },
                    modifier = Modifier.weight(1f)
                )
                PlsolConstantField(
                    label = "KFURL",
                    value = kfurl,
                    unit = "%",
                    onValueChange = {
                        kfurl = it
                        it.toDoubleOrNull()?.let { v -> PlsolPreferences.kfurl = v }
                    },
                    modifier = Modifier.weight(1f)
                )
                PlsolConstantField(
                    label = "Displacement",
                    value = displacement,
                    unit = "L",
                    onValueChange = {
                        displacement = it
                        it.toDoubleOrNull()?.let { v -> PlsolPreferences.displacement = v }
                    },
                    modifier = Modifier.weight(1f)
                )
                PlsolConstantField(
                    label = "RPM",
                    value = maxRpm,
                    unit = "",
                    onValueChange = {
                        maxRpm = it
                        it.toIntOrNull()?.let { v -> PlsolPreferences.rpm = v }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlsolConstantField(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).height(48.dp)
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

private data class PlsolChartData(
    val absolutePoints: List<Pair<Double, Double>>,
    val relativePoints: List<Pair<Double, Double>>,
    val airflowPoints: List<Pair<Double, Double>>,
    val horsepowerPoints: List<Pair<Double, Double>>
)
