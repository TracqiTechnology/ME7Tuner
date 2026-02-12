package ui.screens.kfvpdksd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.parser.bin.BinParser
import data.parser.me7log.KfvpdksdLogParser
import data.parser.me7log.Me7LogParser
import data.parser.xdf.TableDefinition
import data.parser.xdf.XdfParser
import data.preferences.MapPreference
import data.preferences.bin.BinFilePreferences
import data.preferences.kfvpdksd.KfvpdksdPreferences
import data.writer.BinWriter
import domain.math.RescaleAxis
import domain.math.map.Map3d
import domain.model.kfvpdksd.Kfvpdksd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.components.ChartSeries
import ui.components.LineChart
import ui.components.MapPickerDialog
import ui.components.MapTable
import ui.theme.Primary
import java.io.File
import javax.swing.JFileChooser

private fun findMap(
    mapList: List<Pair<TableDefinition, Map3d>>,
    pref: MapPreference
): Pair<TableDefinition, Map3d>? {
    val selected = pref.getSelectedMap()
    return if (selected != null) {
        mapList.find { it.first.tableName == selected.first.tableName }
    } else null
}

@Composable
fun KfvpdksdScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val tableDefinitions by XdfParser.tableDefinitions.collectAsState()
    val scope = rememberCoroutineScope()

    var showMapPicker by remember { mutableStateOf(false) }

    // Find the KFVPDKSD map based on preference
    val kfvpdksdPair = remember(mapList) { findMap(mapList, KfvpdksdPreferences) }
    val inputKfvpdksd = kfvpdksdPair?.second

    // Log-derived maximum pressure values
    var maxPressure by remember { mutableStateOf<Array<Double>?>(null) }
    var logFilePath by remember { mutableStateOf("No File Selected") }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableStateOf(0f) }

    // Collect log parser results
    LaunchedEffect(Unit) {
        KfvpdksdLogParser.logs.collect { log ->
            if (inputKfvpdksd != null) {
                maxPressure = Kfvpdksd.parsePressure(log, inputKfvpdksd.yAxis)
            }
        }
    }

    // Compute the KFVPDKSD output from max pressure data
    val kfvpdksdResult = remember(maxPressure, inputKfvpdksd) {
        val pressure = maxPressure
        val kfvpdksdMap = inputKfvpdksd
        if (pressure != null && kfvpdksdMap != null) {
            val maxVal = pressure.maxOrNull() ?: 0.0
            val rescaledPressureRatio = RescaleAxis.rescaleAxis(kfvpdksdMap.xAxis, (1000 + maxVal) / 1000.0)
            Kfvpdksd.generate(pressure, kfvpdksdMap.yAxis, rescaledPressureRatio)
        } else null
    }

    // Build the output Map3d for display
    val outputMap = remember(kfvpdksdResult, maxPressure, inputKfvpdksd) {
        val result = kfvpdksdResult
        val pressure = maxPressure
        val kfvpdksdMap = inputKfvpdksd
        if (result != null && pressure != null && kfvpdksdMap != null) {
            val maxVal = pressure.maxOrNull() ?: 0.0
            val rescaledPressureRatio = RescaleAxis.rescaleAxis(kfvpdksdMap.xAxis, (1000 + maxVal) / 1000.0)
            Map3d(rescaledPressureRatio, kfvpdksdMap.yAxis, result.kfvpdksd)
        } else inputKfvpdksd
    }

    // Build chart series for boost pressure scatter/line
    val chartSeries = remember(maxPressure, inputKfvpdksd) {
        val pressure = maxPressure
        val kfvpdksdMap = inputKfvpdksd
        if (pressure != null && kfvpdksdMap != null) {
            val rpmAxis = kfvpdksdMap.yAxis
            val points = rpmAxis.indices.map { i ->
                Pair(rpmAxis[i], pressure[i] * 0.0145038)
            }
            listOf(
                ChartSeries(
                    name = "Boost (PSI)",
                    points = points,
                    color = Primary,
                    showLine = true,
                    showPoints = false
                )
            )
        } else emptyList()
    }

    // Write confirmation dialog
    var showWriteConfirmation by remember { mutableStateOf(false) }

    if (showMapPicker) {
        MapPickerDialog(
            title = "Select KFVPDKSD Map",
            tableDefinitions = tableDefinitions,
            initialValue = kfvpdksdPair?.first,
            onSelected = { KfvpdksdPreferences.setSelectedMap(it) },
            onDismiss = { showMapPicker = false }
        )
    }

    if (showWriteConfirmation) {
        AlertDialog(
            onDismissRequest = { showWriteConfirmation = false },
            title = { Text("Write KFVPDKSD") },
            text = { Text("Are you sure you want to write KFVPDKSD to the binary?") },
            confirmButton = {
                TextButton(onClick = {
                    showWriteConfirmation = false
                    val tableDef = kfvpdksdPair?.first
                    if (outputMap != null && tableDef != null) {
                        try {
                            BinWriter.write(BinFilePreferences.getStoredFile(), tableDef, outputMap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showWriteConfirmation = false }) { Text("No") }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left side: Boost input, chart, log loader
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Maximum Boost (Input)", style = MaterialTheme.typography.titleMedium)

            // Boost pressure display table (1-row table)
            if (maxPressure != null && inputKfvpdksd != null) {
                val boostData = remember(maxPressure) {
                    val psi = maxPressure!!.map { it * 0.0145038 }.toTypedArray()
                    Map3d(
                        inputKfvpdksd.yAxis,
                        arrayOf(0.0),
                        arrayOf(psi)
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    MapTable(map = boostData, editable = false)
                }
            }

            // Scatter/Line chart for boost
            Text("Maximum Boost", style = MaterialTheme.typography.titleMedium)
            Box(modifier = Modifier.fillMaxWidth().height(275.dp)) {
                LineChart(
                    series = chartSeries,
                    title = "Maximum Boost",
                    xAxisLabel = "RPM",
                    yAxisLabel = "Boost (PSI)"
                )
            }

            // Log file loading
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val chooser = JFileChooser().apply {
                            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            val lastDir = KfvpdksdPreferences.lastDirectory
                            if (lastDir.isNotEmpty()) {
                                currentDirectory = File(lastDir)
                            }
                        }
                        val result = chooser.showOpenDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            val dir = chooser.selectedFile
                            logFilePath = dir.path
                            KfvpdksdPreferences.lastDirectory = dir.parentFile?.absolutePath ?: ""
                            isLoading = true
                            KfvpdksdLogParser.loadDirectory(dir, Me7LogParser.ProgressCallback { value, max ->
                                loadProgress = if (max > 0) value.toFloat() / max.toFloat() else 0f
                                if (value >= max - 1) isLoading = false
                            })
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text("Load Logs")
            }

            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = logFilePath,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Right side: KFVPDKSD Output
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("KFVPDKSD (Output)", style = MaterialTheme.typography.titleMedium)
            if (outputMap != null) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)) {
                    MapTable(map = outputMap, editable = false)
                }
            } else {
                Text("No data", style = MaterialTheme.typography.bodyMedium)
            }

            Text(
                text = kfvpdksdPair?.first?.tableName ?: "No Map Selected",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { showMapPicker = true }) {
                Text("Select KFVPDKSD Map")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showWriteConfirmation = true },
                enabled = outputMap != null && kfvpdksdPair != null
            ) {
                Text("Write KFVPDKSD")
            }
        }
    }
}
