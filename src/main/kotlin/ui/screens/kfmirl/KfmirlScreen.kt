package ui.screens.kfmirl

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
import data.parser.xdf.TableDefinition
import data.parser.xdf.XdfParser
import data.preferences.MapPreference
import data.preferences.bin.BinFilePreferences
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfmirl.KfmirlPreferences
import data.writer.BinWriter
import domain.math.Inverse
import domain.math.RescaleMap
import domain.math.map.Map3d
import data.model.EcuPlatform
import data.preferences.platform.EcuPlatformPreference
import kotlinx.coroutines.delay
import ui.components.ChartSeries
import ui.components.LineChart
import ui.components.MapAxis
import ui.components.MapPickerDialog
import ui.components.MapTable
import ui.components.ParameterField
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
fun KfmirlScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val tableDefinitions by XdfParser.tableDefinitions.collectAsState()

    var showKfmiopPicker by remember { mutableStateOf(false) }
    var showKfmirlPicker by remember { mutableStateOf(false) }

    // Reactive map selection — recompose when user picks a new map
    var mapVersion by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { KfmiopPreferences.mapChanged.collect { mapVersion++ } }
    LaunchedEffect(Unit) { KfmirlPreferences.mapChanged.collect { mapVersion++ } }

    val kfmiopPair = remember(mapList, mapVersion) { findMap(mapList, KfmiopPreferences) }
    val kfmirlPair = remember(mapList, mapVersion) { findMap(mapList, KfmirlPreferences) }

    val inputKfmiop = kfmiopPair?.second

    // Detect scalar KFMIOP (DS1: 1×1 map with empty axes)
    val kfmiopIsScalar = inputKfmiop != null && inputKfmiop.xAxis.isEmpty() && inputKfmiop.yAxis.isEmpty()

    // --- DS1 scalar mode: target max load input ---
    val kfmiopScalarValue = if (kfmiopIsScalar) {
        inputKfmiop!!.zAxis.firstOrNull()?.firstOrNull() ?: 400.0
    } else 0.0

    var targetMaxLoad by remember(kfmiopScalarValue) {
        mutableStateOf(kfmiopScalarValue.toString())
    }

    // --- ME7 / non-DS1 mode: editable KFMIOP xAxis ---
    var editedXAxis by remember(inputKfmiop, kfmiopIsScalar) {
        mutableStateOf(
            if (!kfmiopIsScalar && inputKfmiop != null) arrayOf(inputKfmiop.xAxis.copyOf())
            else arrayOf(emptyArray<Double>())
        )
    }

    var editedInputMap by remember(inputKfmiop, kfmiopIsScalar) {
        mutableStateOf(if (!kfmiopIsScalar) inputKfmiop?.let { Map3d(it) } else null)
    }

    // Calculate KFMIRL output — different path for scalar vs 2D KFMIOP
    val outputKfmirl = remember(editedInputMap, editedXAxis, kfmirlPair, kfmiopIsScalar, targetMaxLoad) {
        val kfmirlBase = kfmirlPair?.second

        if (kfmiopIsScalar && kfmirlBase != null) {
            // DS1 path: rescale KFMIRL's own xAxis to the target max load
            val newMax = targetMaxLoad.toDoubleOrNull() ?: kfmiopScalarValue
            RescaleMap.rescaleMapXAxis(kfmirlBase, newMax)
        } else {
            // ME7 path: inverse of KFMIOP
            val kfmiop = editedInputMap
            if (kfmiop != null && kfmirlBase != null && editedXAxis.isNotEmpty() && editedXAxis[0].isNotEmpty()) {
                val kfmiopWithNewXAxis = Map3d(editedXAxis[0], kfmiop.yAxis, kfmiop.zAxis)
                val inverse = Inverse.calculateInverse(kfmiopWithNewXAxis, kfmirlBase)
                // Preserve the first column from the original KFMIRL
                for (i in inverse.zAxis.indices) {
                    if (inverse.zAxis[i].isNotEmpty() && kfmirlBase.zAxis[i].isNotEmpty()) {
                        inverse.zAxis[i][0] = kfmirlBase.zAxis[i][0]
                    }
                }
                inverse
            } else null
        }
    }

    // Load comparison chart data — peak load per RPM
    val loadChartData = remember(outputKfmirl, kfmirlPair) {
        val originalKfmirl = kfmirlPair?.second
        val calculatedKfmirl = outputKfmirl
        if (originalKfmirl == null || calculatedKfmirl == null) {
            return@remember Pair(emptyList<Pair<Double, Double>>(), emptyList<Pair<Double, Double>>())
        }

        val originalPeaks = originalKfmirl.yAxis.mapIndexedNotNull { i, rpm ->
            if (i < originalKfmirl.zAxis.size) {
                val peakLoad = originalKfmirl.zAxis[i].maxOrNull() ?: 0.0
                Pair(rpm, peakLoad)
            } else null
        }
        val calculatedPeaks = calculatedKfmirl.yAxis.mapIndexedNotNull { i, rpm ->
            if (i < calculatedKfmirl.zAxis.size) {
                val peakLoad = calculatedKfmirl.zAxis[i].maxOrNull() ?: 0.0
                Pair(rpm, peakLoad)
            } else null
        }
        Pair(originalPeaks, calculatedPeaks)
    }

    // Write prerequisites — scalar mode only needs KFMIRL configured
    val binFile by BinFilePreferences.file.collectAsState()
    val binLoaded = binFile.exists() && binFile.isFile
    val kfmiopMapConfigured = kfmiopPair != null
    val kfmirlMapConfigured = kfmirlPair != null
    val canWrite = binLoaded && kfmirlMapConfigured && outputKfmirl != null &&
        (kfmiopIsScalar || kfmiopMapConfigured)

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
    if (showKfmiopPicker) {
        MapPickerDialog(
            title = "Select KFMIOP Map",
            tableDefinitions = tableDefinitions,
            initialValue = kfmiopPair?.first,
            onSelected = { KfmiopPreferences.setSelectedMap(it) },
            onDismiss = { showKfmiopPicker = false }
        )
    }

    if (showKfmirlPicker) {
        MapPickerDialog(
            title = "Select KFMIRL Map",
            tableDefinitions = tableDefinitions,
            initialValue = kfmirlPair?.first,
            onSelected = { KfmirlPreferences.setSelectedMap(it) },
            onDismiss = { showKfmirlPicker = false }
        )
    }

    if (showWriteConfirmation) {
        AlertDialog(
            onDismissRequest = { showWriteConfirmation = false },
            title = { Text("Write KFMIRL") },
            text = { Text("Are you sure you want to write KFMIRL to the binary?") },
            confirmButton = {
                TextButton(onClick = {
                    showWriteConfirmation = false
                    val tableDef = kfmirlPair?.first
                    if (outputKfmirl != null && tableDef != null) {
                        try {
                            BinWriter.write(BinFilePreferences.file.value, tableDef, outputKfmirl)
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
        // DS1 note for MED17 users
        if (EcuPlatformPreference.platform == EcuPlatform.MED17) {
            Surface(
                shape = MaterialTheme.shapes.small,
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (kfmiopIsScalar)
                        "ℹ DS1 Note: KFMIOP is a scalar on this ECU. " +
                            "KFMIRL will be rescaled to the target max load using its own axis."
                    else
                        "ℹ DS1 Note: DS1 reduces KFMIOP/KFMIRL to scalar values and bypasses " +
                            "the native torque model. KFMIRL here works as a simple inverse calculator " +
                            "to convert between load and torque values.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        if (kfmiopIsScalar) {
            // DS1 scalar mode: show target max load input
            ScalarRescaleConfigCard(
                kfmirlMapName = kfmirlPair?.first?.tableName,
                onSelectKfmirl = { showKfmirlPicker = true },
                currentMaxLoad = kfmiopScalarValue,
                targetMaxLoad = targetMaxLoad,
                onTargetMaxLoadChange = { targetMaxLoad = it }
            )
        } else {
            // ME7 mode: full KFMIOP axis editor
            ConfigurationCard(
                kfmiopMapName = kfmiopPair?.first?.tableName,
                kfmirlMapName = kfmirlPair?.first?.tableName,
                onSelectKfmiop = { showKfmiopPicker = true },
                onSelectKfmirl = { showKfmirlPicker = true },
                editedXAxis = editedXAxis,
                onXAxisChanged = { newData ->
                    editedXAxis = newData
                    editedInputMap?.let { currentMap ->
                        editedInputMap = Map3d(newData[0], currentMap.yAxis, currentMap.zAxis)
                    }
                }
            )
        }

        ComparisonArea(
            modifier = Modifier.weight(1f),
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            editedInputMap = editedInputMap,
            onInputMapChanged = { editedInputMap = it },
            originalKfmirl = kfmirlPair?.second,
            outputKfmirl = outputKfmirl,
            originalPeakLoad = loadChartData.first,
            calculatedPeakLoad = loadChartData.second
        )

        WriteToBinarySection(
            binLoaded = binLoaded,
            binFileName = if (binLoaded) binFile.name else null,
            kfmiopMapConfigured = kfmiopMapConfigured,
            kfmiopMapName = kfmiopPair?.first?.tableName,
            kfmirlMapConfigured = kfmirlMapConfigured,
            kfmirlMapName = kfmirlPair?.first?.tableName,
            canWrite = canWrite,
            writeStatus = writeStatus,
            onWriteClick = { showWriteConfirmation = true }
        )
    }
}

@Composable
private fun ScalarRescaleConfigCard(
    kfmirlMapName: String?,
    onSelectKfmirl: () -> Unit,
    currentMaxLoad: Double,
    targetMaxLoad: String,
    onTargetMaxLoadChange: (String) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "KFMIRL — Rescale Load Axis",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Rescale KFMIRL's load axis to match a new max load ceiling.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            MapSelectorRow(
                label = "KFMIRL:",
                mapName = kfmirlMapName,
                onSelectMap = onSelectKfmirl
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("KFMIOP Value (current):", style = MaterialTheme.typography.bodySmall)
                Text(
                    "%.2f%%".format(currentMaxLoad),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                Text("Target Max Load:", style = MaterialTheme.typography.bodySmall)
                ParameterField(
                    value = targetMaxLoad,
                    onValueChange = onTargetMaxLoadChange,
                    label = "%",
                    tooltip = "Target max load for KFMIRL axis rescale. " +
                        "Defaults to the KFMIOP scalar. Increase for higher-power builds.",
                    readOnly = false,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(130.dp).height(56.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfigurationCard(
    kfmiopMapName: String?,
    kfmirlMapName: String?,
    onSelectKfmiop: () -> Unit,
    onSelectKfmirl: () -> Unit,
    editedXAxis: Array<Array<Double>>,
    onXAxisChanged: (Array<Array<Double>>) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "KFMIRL Calculator",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Inverse torque-to-load mapping from KFMIOP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            MapSelectorRow(
                label = "KFMIOP (Input):",
                mapName = kfmiopMapName,
                onSelectMap = onSelectKfmiop
            )

            MapSelectorRow(
                label = "KFMIRL (Target):",
                mapName = kfmirlMapName,
                onSelectMap = onSelectKfmirl
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (editedXAxis.isNotEmpty() && editedXAxis[0].isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "KFMIOP Load Axis (Editable)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "(defines the output load breakpoints)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MapAxis(
                    data = editedXAxis,
                    editable = true,
                    onDataChanged = onXAxisChanged
                )
            }
        }
    }
}

@Composable
private fun MapSelectorRow(
    label: String,
    mapName: String?,
    onSelectMap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = mapName ?: "No Definition Selected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = onSelectMap) {
            Text("Select Map")
        }
    }
}

@Composable
private fun ComparisonArea(
    modifier: Modifier = Modifier,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    editedInputMap: Map3d?,
    onInputMapChanged: (Map3d) -> Unit,
    originalKfmirl: Map3d?,
    outputKfmirl: Map3d?,
    originalPeakLoad: List<Pair<Double, Double>>,
    calculatedPeakLoad: List<Pair<Double, Double>>
) {
    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = { Text("KFMIOP (Input)") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = { Text("KFMIRL (Comparison)") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                text = { Text("Load Comparison") }
            )
        }

        when (selectedTab) {
            0 -> EditableInputSection(
                editedInputMap = editedInputMap,
                onInputMapChanged = onInputMapChanged,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            1 -> SideBySideTables(
                originalKfmirl = originalKfmirl,
                calculatedKfmirl = outputKfmirl,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            2 -> LoadComparisonChart(
                originalPeakLoad = originalPeakLoad,
                calculatedPeakLoad = calculatedPeakLoad,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)
            )
        }
    }
}

@Composable
private fun EditableInputSection(
    editedInputMap: Map3d?,
    onInputMapChanged: (Map3d) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "KFMIOP Values",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Edit values to adjust the inverse calculation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (editedInputMap != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                MapTable(
                    map = editedInputMap,
                    editable = true,
                    onMapChanged = onInputMapChanged
                )
            }
        } else {
            Text("No map loaded", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SideBySideTables(
    originalKfmirl: Map3d?,
    calculatedKfmirl: Map3d?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = "KFMIRL (Original)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (originalKfmirl != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapTable(map = originalKfmirl, editable = false)
                }
            } else {
                Text("No map loaded", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = "KFMIRL (Calculated)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (calculatedKfmirl != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapTable(map = calculatedKfmirl, editable = false)
                }
            } else {
                Text("No data", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LoadComparisonChart(
    originalPeakLoad: List<Pair<Double, Double>>,
    calculatedPeakLoad: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LineChart(
            series = listOf(
                ChartSeries(
                    name = "Original Peak Load",
                    points = originalPeakLoad,
                    color = Primary
                ),
                ChartSeries(
                    name = "Calculated Peak Load",
                    points = calculatedPeakLoad,
                    color = ChartRed
                )
            ),
            title = "Peak Load Comparison",
            xAxisLabel = "RPM",
            yAxisLabel = "Load (%)"
        )
    }
}

@Composable
private fun WriteToBinarySection(
    binLoaded: Boolean,
    binFileName: String?,
    kfmiopMapConfigured: Boolean,
    kfmiopMapName: String?,
    kfmirlMapConfigured: Boolean,
    kfmirlMapName: String?,
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

            PrerequisiteRow(
                label = "KFMIRL map",
                detail = if (kfmirlMapConfigured) kfmirlMapName!! else "Not configured",
                met = kfmirlMapConfigured
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onWriteClick,
                    enabled = canWrite
                ) {
                    Text("Write KFMIRL")
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
                    !binLoaded && !kfmiopMapConfigured && !kfmirlMapConfigured ->
                        "Load a BIN file and select both KFMIOP and KFMIRL map definitions."
                    !binLoaded -> "Load a BIN file to write."
                    !kfmiopMapConfigured && !kfmirlMapConfigured ->
                        "Select both KFMIOP and KFMIRL map definitions above."
                    !kfmiopMapConfigured -> "Select the KFMIOP map definition above."
                    !kfmirlMapConfigured -> "Select the KFMIRL map definition above."
                    else -> "Configure all prerequisites above."
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
