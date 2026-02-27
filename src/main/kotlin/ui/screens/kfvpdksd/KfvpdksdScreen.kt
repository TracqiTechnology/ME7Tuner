package ui.screens.kfvpdksd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.parser.bin.BinParser
import data.parser.me7log.KfvpdksdLogParser
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.components.ChartSeries
import ui.components.LineChart
import ui.components.MapPickerDialog
import ui.components.MapTable
import ui.theme.Primary
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

private enum class WriteStatus { Idle, Success, Error }

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
    var logFilePath by remember { mutableStateOf("No Directory Selected") }
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

    // Build chart series for boost pressure
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
                    showPoints = true
                )
            )
        } else emptyList()
    }

    // Write state
    val binFile by BinFilePreferences.file.collectAsState()
    val binLoaded = binFile.exists() && binFile.isFile
    val kfvpdksdConfigured = kfvpdksdPair != null
    val kfvpdksdMapName = kfvpdksdPair?.first?.tableName

    var showWriteConfirmation by remember { mutableStateOf(false) }
    var writeStatus by remember { mutableStateOf(WriteStatus.Idle) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(writeStatus) {
        if (writeStatus != WriteStatus.Idle) {
            delay(3000)
            writeStatus = WriteStatus.Idle
        }
    }

    // Map picker dialog
    if (showMapPicker) {
        MapPickerDialog(
            title = "Select KFVPDKSD Map",
            tableDefinitions = tableDefinitions,
            initialValue = kfvpdksdPair?.first,
            onSelected = { KfvpdksdPreferences.setSelectedMap(it) },
            onDismiss = { showMapPicker = false }
        )
    }

    // Write confirmation dialog
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
                            writeStatus = WriteStatus.Success
                        } catch (e: Exception) {
                            e.printStackTrace()
                            writeStatus = WriteStatus.Error
                        }
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showWriteConfirmation = false }) { Text("No") }
            }
        )
    }

    // ── Main layout ───────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Tab row ───────────────────────────────────────────────────
        val tabTitles = listOf("Boost Chart", "KFVPDKSD Table")
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                val enabled = when (index) {
                    1 -> outputMap != null
                    else -> true
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { if (enabled) selectedTab = index },
                    enabled = enabled,
                    text = {
                        Text(
                            title,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                )
            }
        }

        // ── Tab content ───────────────────────────────────────────────
        when (selectedTab) {
            0 -> BoostChartTab(
                maxPressure = maxPressure,
                inputKfvpdksd = inputKfvpdksd,
                chartSeries = chartSeries,
                modifier = Modifier.weight(1f)
            )
            1 -> KfvpdksdTableTab(
                inputKfvpdksd = inputKfvpdksd,
                outputMap = outputMap,
                hasResult = kfvpdksdResult != null,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Action bar ────────────────────────────────────────────────
        KfvpdksdActionBar(
            kfvpdksdConfigured = kfvpdksdConfigured,
            kfvpdksdMapName = kfvpdksdMapName,
            binLoaded = binLoaded,
            binFileName = if (binLoaded) binFile.name else null,
            isLoading = isLoading,
            loadProgress = loadProgress,
            logFilePath = logFilePath,
            canWrite = binLoaded && kfvpdksdConfigured && outputMap != null && kfvpdksdResult != null,
            writeStatus = writeStatus,
            onSelectMap = { showMapPicker = true },
            onLoadLogs = {
                val dialog = FileDialog(Frame(), "Select Log Directory", FileDialog.LOAD)
                System.setProperty("apple.awt.fileDialogForDirectories", "true")
                val lastDir = KfvpdksdPreferences.lastDirectory
                if (lastDir.isNotEmpty()) dialog.directory = lastDir
                dialog.isVisible = true
                System.setProperty("apple.awt.fileDialogForDirectories", "false")
                val dir = dialog.directory
                val file = dialog.file
                if (dir != null && file != null) {
                    val selectedDir = File(dir, file)
                    logFilePath = selectedDir.path
                    KfvpdksdPreferences.lastDirectory = selectedDir.parentFile?.absolutePath ?: ""
                    isLoading = true
                    scope.launch(Dispatchers.IO) {
                        KfvpdksdLogParser.loadDirectory(selectedDir) { value, max ->
                            loadProgress = if (max > 0) value.toFloat() / max.toFloat() else 0f
                            if (value >= max - 1) isLoading = false
                        }
                    }
                }
            },
            onWrite = { showWriteConfirmation = true }
        )
    }
}

// ── Tab: Boost Chart ──────────────────────────────────────────────────

@Composable
private fun BoostChartTab(
    maxPressure: Array<Double>?,
    inputKfvpdksd: Map3d?,
    chartSeries: List<ChartSeries>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chart
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LineChart(
                series = chartSeries,
                title = "Maximum Boost vs RPM",
                xAxisLabel = "RPM",
                yAxisLabel = "Boost (PSI)"
            )
        }

        // Boost data table (1-row)
        if (maxPressure != null && inputKfvpdksd != null) {
            Text(
                "Maximum Boost by RPM (PSI)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            val boostData = remember(maxPressure) {
                val psi = maxPressure.map { it * 0.0145038 }.toTypedArray()
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
    }
}

// ── Tab: KFVPDKSD Table ───────────────────────────────────────────────

@Composable
private fun KfvpdksdTableTab(
    inputKfvpdksd: Map3d?,
    outputMap: Map3d?,
    hasResult: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Current KFVPDKSD
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Current KFVPDKSD",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (inputKfvpdksd != null) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        MapTable(map = inputKfvpdksd, editable = false)
                    }
                } else {
                    Text(
                        "KFVPDKSD not configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Suggested KFVPDKSD
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Suggested KFVPDKSD",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (hasResult && outputMap != null) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        MapTable(map = outputMap, editable = false)
                    }
                } else {
                    Text(
                        "Load log data to generate suggestions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Action Bar ────────────────────────────────────────────────────────

@Composable
private fun KfvpdksdActionBar(
    kfvpdksdConfigured: Boolean,
    kfvpdksdMapName: String?,
    binLoaded: Boolean,
    binFileName: String?,
    isLoading: Boolean,
    loadProgress: Float,
    logFilePath: String,
    canWrite: Boolean,
    writeStatus: WriteStatus,
    onSelectMap: () -> Unit,
    onLoadLogs: () -> Unit,
    onWrite: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Prerequisites
            PrerequisiteRow(
                label = "KFVPDKSD map",
                detail = if (kfvpdksdConfigured) kfvpdksdMapName!! else "Not configured",
                met = kfvpdksdConfigured
            )

            PrerequisiteRow(
                label = "BIN file",
                detail = if (binLoaded) binFileName!! else "Not loaded",
                met = binLoaded
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onSelectMap) {
                    Text("Select Map")
                }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = onLoadLogs,
                    enabled = !isLoading && kfvpdksdConfigured
                ) {
                    Text("Load Logs")
                }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = onWrite,
                    enabled = canWrite
                ) {
                    Text("Write KFVPDKSD")
                }

                Spacer(Modifier.width(12.dp))

                AnimatedVisibility(visible = writeStatus != WriteStatus.Idle) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (writeStatus == WriteStatus.Success) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (writeStatus == WriteStatus.Success) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (writeStatus == WriteStatus.Success) "Written successfully" else "Write failed",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (writeStatus == WriteStatus.Success) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Loading progress
            if (isLoading) {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Loading logs...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LinearProgressIndicator(
                        progress = { loadProgress },
                        modifier = Modifier.width(300.dp)
                    )
                }
            }

            // Log path
            if (logFilePath != "No Directory Selected") {
                Text(
                    text = logFilePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (!kfvpdksdConfigured) {
                Text(
                    text = "Configure the KFVPDKSD map definition in the Configuration screen to begin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ── Shared Composables ────────────────────────────────────────────────

@Composable
private fun PrerequisiteRow(label: String, detail: String, met: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (met) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = if (met) "Ready" else "Not ready",
            tint = if (met) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(100.dp)
        )

        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
