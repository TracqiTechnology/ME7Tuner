package ui.screens.kfmiop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.parser.bin.BinParser
import data.parser.xdf.TableDefinition
import data.parser.xdf.XdfParser
import data.preferences.MapPreference
import data.preferences.bin.BinFilePreferences
import data.preferences.kfmiop.KfmiopPreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import domain.model.kfmiop.Kfmiop
import domain.model.rlsol.Rlsol
import kotlinx.coroutines.delay
import ui.components.ChartSeries
import ui.components.LineChart
import ui.components.MapAxis
import ui.components.MapPickerDialog
import ui.components.MapTable
import ui.theme.ChartRed
import ui.theme.Primary

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
fun KfmiopScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val tableDefinitions by XdfParser.tableDefinitions.collectAsState()

    var showMapPicker by remember { mutableStateOf(false) }

    // Reactive map selection — recompose when user picks a new map
    var mapVersion by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { KfmiopPreferences.mapChanged.collect { mapVersion++ } }
    val kfmiopPair = remember(mapList, mapVersion) { findMap(mapList, KfmiopPreferences) }
    val inputKfmiop = kfmiopPair?.second

    // Desired pressure inputs (editable)
    var desiredMaxMapPressure by remember {
        mutableStateOf(KfmiopPreferences.maxMapPressure.toString())
    }
    var desiredMaxBoostPressure by remember {
        mutableStateOf(KfmiopPreferences.maxBoostPressure.toString())
    }

    // Computed outputs
    val kfmiopResult = remember(inputKfmiop, desiredMaxMapPressure, desiredMaxBoostPressure) {
        if (inputKfmiop != null) {
            val maxMapPressureVal = desiredMaxMapPressure.toDoubleOrNull() ?: KfmiopPreferences.maxMapPressure
            val maxBoostPressureVal = desiredMaxBoostPressure.toDoubleOrNull() ?: KfmiopPreferences.maxBoostPressure

            val maxMapSensorLoad = Rlsol.rlsol(1030.0, maxMapPressureVal, 0.0, 96.0, 0.106, maxMapPressureVal)
            val maxBoostPressureLoad = Rlsol.rlsol(1030.0, maxBoostPressureVal, 0.0, 96.0, 0.106, maxBoostPressureVal)
            Kfmiop.calculateKfmiop(inputKfmiop, maxMapSensorLoad, maxBoostPressureLoad)
        } else null
    }

    // Boost comparison chart data — peak boost per RPM
    val boostChartData = remember(kfmiopResult) {
        val result = kfmiopResult ?: return@remember Pair(emptyList<Pair<Double, Double>>(), emptyList<Pair<Double, Double>>())
        val inputBoost = result.inputBoost
        val outputBoost = result.outputBoost

        val currentPeaks = inputBoost.yAxis.mapIndexed { i, rpm ->
            val peakBoost = inputBoost.zAxis[i].maxOrNull() ?: 0.0
            Pair(rpm, peakBoost)
        }
        val targetPeaks = outputBoost.yAxis.mapIndexed { i, rpm ->
            val peakBoost = outputBoost.zAxis[i].maxOrNull() ?: 0.0
            Pair(rpm, peakBoost)
        }
        Pair(currentPeaks, targetPeaks)
    }

    // Write prerequisites
    val binFile by BinFilePreferences.file.collectAsState()
    val binLoaded = binFile.exists() && binFile.isFile
    val kfmiopMapConfigured = kfmiopPair != null
    val canWrite = binLoaded && kfmiopMapConfigured && kfmiopResult?.outputKfmiop != null

    var showWriteConfirmation by remember { mutableStateOf(false) }
    var writeStatus by remember { mutableStateOf(WriteStatus.Idle) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(writeStatus) {
        if (writeStatus != WriteStatus.Idle) {
            delay(3000)
            writeStatus = WriteStatus.Idle
        }
    }

    // Dialogs
    if (showMapPicker) {
        MapPickerDialog(
            title = "Select KFMIOP Map",
            tableDefinitions = tableDefinitions,
            initialValue = kfmiopPair?.first,
            onSelected = { KfmiopPreferences.setSelectedMap(it) },
            onDismiss = { showMapPicker = false }
        )
    }

    if (showWriteConfirmation) {
        AlertDialog(
            onDismissRequest = { showWriteConfirmation = false },
            title = { Text("Write KFMIOP") },
            text = { Text("Are you sure you want to write KFMIOP to the binary?") },
            confirmButton = {
                TextButton(onClick = {
                    showWriteConfirmation = false
                    val outputMap = kfmiopResult?.outputKfmiop
                    val tableDef = kfmiopPair?.first
                    if (outputMap != null && tableDef != null) {
                        try {
                            BinWriter.write(BinFilePreferences.file.value, tableDef, outputMap)
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

    // Main layout: Configure -> Compare -> Write
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ConfigurationCard(
            analyzedMaxMapPressure = kfmiopResult?.maxMapSensorPressure,
            analyzedMaxBoostPressure = kfmiopResult?.maxBoostPressure,
            desiredMaxMapPressure = desiredMaxMapPressure,
            desiredMaxBoostPressure = desiredMaxBoostPressure,
            onDesiredMaxMapPressureChange = {
                desiredMaxMapPressure = it
                it.toDoubleOrNull()?.let { v -> KfmiopPreferences.maxMapPressure = v }
            },
            onDesiredMaxBoostPressureChange = {
                desiredMaxBoostPressure = it
                it.toDoubleOrNull()?.let { v -> KfmiopPreferences.maxBoostPressure = v }
            },
            mapDefinitionName = kfmiopPair?.first?.tableName,
            onSelectMap = { showMapPicker = true }
        )

        ComparisonArea(
            modifier = Modifier.weight(1f),
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            inputKfmiop = inputKfmiop,
            kfmiopResult = kfmiopResult,
            currentPeakBoost = boostChartData.first,
            targetPeakBoost = boostChartData.second
        )

        WriteToBinarySection(
            binLoaded = binLoaded,
            binFileName = if (binLoaded) binFile.name else null,
            kfmiopMapConfigured = kfmiopMapConfigured,
            kfmiopMapName = kfmiopPair?.first?.tableName,
            canWrite = canWrite,
            writeStatus = writeStatus,
            onWriteClick = { showWriteConfirmation = true }
        )
    }
}

@Composable
private fun ConfigurationCard(
    analyzedMaxMapPressure: Double?,
    analyzedMaxBoostPressure: Double?,
    desiredMaxMapPressure: String,
    desiredMaxBoostPressure: String,
    onDesiredMaxMapPressureChange: (String) -> Unit,
    onDesiredMaxBoostPressureChange: (String) -> Unit,
    mapDefinitionName: String?,
    onSelectMap: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "KFMIOP Calculator",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Rescale optimal torque table for upgraded MAP sensor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PressureColumn(
                    title = "Current (Analyzed)",
                    titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    mapPressure = analyzedMaxMapPressure?.toInt()?.toString() ?: "",
                    boostPressure = analyzedMaxBoostPressure?.toInt()?.toString() ?: "",
                    editable = false,
                    onMapPressureChange = {},
                    onBoostPressureChange = {},
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "transforms to",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                PressureColumn(
                    title = "Target (Desired)",
                    titleColor = MaterialTheme.colorScheme.primary,
                    mapPressure = desiredMaxMapPressure,
                    boostPressure = desiredMaxBoostPressure,
                    editable = true,
                    onMapPressureChange = onDesiredMaxMapPressureChange,
                    onBoostPressureChange = onDesiredMaxBoostPressureChange,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Map Definition:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = mapDefinitionName ?: "No Definition Selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onSelectMap) {
                    Text("Select Map")
                }
            }
        }
    }
}

@Composable
private fun PressureColumn(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color,
    mapPressure: String,
    boostPressure: String,
    editable: Boolean,
    onMapPressureChange: (String) -> Unit,
    onBoostPressureChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = titleColor
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("MAP Sensor Max:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = mapPressure,
                onValueChange = onMapPressureChange,
                readOnly = !editable,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(100.dp).height(48.dp)
            )
            Text("mbar", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Boost Pressure Max:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = boostPressure,
                onValueChange = onBoostPressureChange,
                readOnly = !editable,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(100.dp).height(48.dp)
            )
            Text("mbar", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ComparisonArea(
    modifier: Modifier = Modifier,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    inputKfmiop: Map3d?,
    kfmiopResult: Kfmiop?,
    currentPeakBoost: List<Pair<Double, Double>>,
    targetPeakBoost: List<Pair<Double, Double>>
) {
    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = { Text("Torque") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Boost") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                text = { Text("Boost Comparison") }
            )
        }

        when (selectedTab) {
            0 -> {
                val outputKfmiop = kfmiopResult?.outputKfmiop
                if (outputKfmiop != null) {
                    OutputAxisBanner(outputKfmiop = outputKfmiop)
                }
                SideBySideTables(
                    inputLabel = "KFMIOP (Current)",
                    inputMap = inputKfmiop,
                    outputLabel = "KFMIOP (Rescaled)",
                    outputMap = kfmiopResult?.outputKfmiop,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
            1 -> {
                SideBySideTables(
                    inputLabel = "Boost (Current)",
                    inputMap = kfmiopResult?.inputBoost,
                    outputLabel = "Boost (Rescaled)",
                    outputMap = kfmiopResult?.outputBoost,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
            2 -> {
                BoostComparisonChart(
                    currentPeakBoost = currentPeakBoost,
                    targetPeakBoost = targetPeakBoost,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun OutputAxisBanner(outputKfmiop: Map3d) {
    Surface(
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Rescaled Load Axis",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "(shared with KFZWOP, KFZW)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            val xAxisData = remember(outputKfmiop) {
                arrayOf(outputKfmiop.xAxis.copyOf())
            }
            MapAxis(data = xAxisData, editable = true)
        }
    }
}

@Composable
private fun SideBySideTables(
    inputLabel: String,
    inputMap: Map3d?,
    outputLabel: String,
    outputMap: Map3d?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Input (current)
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = inputLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (inputMap != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapTable(map = inputMap, editable = false)
                }
            } else {
                Text("No map loaded", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Output (rescaled)
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = outputLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (outputMap != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapTable(map = outputMap, editable = false)
                }
            } else {
                Text("No data", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BoostComparisonChart(
    currentPeakBoost: List<Pair<Double, Double>>,
    targetPeakBoost: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LineChart(
            series = listOf(
                ChartSeries(
                    name = "Current Peak Boost",
                    points = currentPeakBoost,
                    color = Primary
                ),
                ChartSeries(
                    name = "Target Peak Boost",
                    points = targetPeakBoost,
                    color = ChartRed
                )
            ),
            title = "Peak Boost Comparison",
            xAxisLabel = "RPM",
            yAxisLabel = "PSI"
        )
    }
}

@Composable
private fun WriteToBinarySection(
    binLoaded: Boolean,
    binFileName: String?,
    kfmiopMapConfigured: Boolean,
    kfmiopMapName: String?,
    canWrite: Boolean,
    writeStatus: WriteStatus,
    onWriteClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Write to Binary",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            PrerequisiteRow(
                label = "BIN file",
                detail = if (binLoaded) binFileName!! else "Not loaded",
                met = binLoaded
            )

            PrerequisiteRow(
                label = "KFMIOP map",
                detail = if (kfmiopMapConfigured) kfmiopMapName!! else "Not configured",
                met = kfmiopMapConfigured
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onWriteClick,
                    enabled = canWrite
                ) {
                    Text("Write KFMIOP")
                }

                Spacer(modifier = Modifier.width(12.dp))

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

            if (!canWrite) {
                val message = when {
                    !binLoaded && !kfmiopMapConfigured -> "Load a BIN file and select the KFMIOP map definition."
                    !binLoaded -> "Load a BIN file to write."
                    else -> "Select the KFMIOP map definition above."
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

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
