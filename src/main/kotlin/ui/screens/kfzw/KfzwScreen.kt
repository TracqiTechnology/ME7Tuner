package ui.screens.kfzw

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
import data.preferences.kfzw.KfzwPreferences
import data.writer.BinWriter
import domain.math.RescaleAxis
import domain.math.map.Map3d
import domain.model.kfzw.Kfzw
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
fun KfzwScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val tableDefinitions by XdfParser.tableDefinitions.collectAsState()

    var showKfzwPicker by remember { mutableStateOf(false) }
    var showKfmiopPicker by remember { mutableStateOf(false) }

    // Reactive map selection — recompose when user picks a new map
    var mapVersion by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { KfzwPreferences.mapChanged.collect { mapVersion++ } }
    LaunchedEffect(Unit) { KfmiopPreferences.mapChanged.collect { mapVersion++ } }

    val kfzwPair = remember(mapList, mapVersion) { findMap(mapList, KfzwPreferences) }
    val kfmiopPair = remember(mapList, mapVersion) { findMap(mapList, KfmiopPreferences) }

    val inputKfzw = kfzwPair?.second
    val kfmiopXAxis = kfmiopPair?.second?.xAxis

    // Editable X-axis (from KFMIOP)
    var editedXAxis by remember(kfmiopXAxis) {
        mutableStateOf(
            if (kfmiopXAxis != null) arrayOf(kfmiopXAxis.copyOf())
            else arrayOf(emptyArray<Double>())
        )
    }

    // Editable input map
    var editedInputMap by remember(inputKfzw) {
        mutableStateOf(inputKfzw?.let { Map3d(it) })
    }

    // Calculate the rescaled KFZW output
    val outputKfzw = remember(editedInputMap, editedXAxis) {
        val input = editedInputMap
        if (input != null && editedXAxis.isNotEmpty() && editedXAxis[0].isNotEmpty()) {
            val newXAxis = editedXAxis[0]
            val maxValue = newXAxis.last()
            val rescaledXAxis = RescaleAxis.rescaleAxis(input.xAxis, maxValue)
            val newZAxis = Kfzw.generateKfzw(input.xAxis, input.zAxis, rescaledXAxis)
            Map3d(rescaledXAxis, input.yAxis, newZAxis)
        } else null
    }

    // Timing comparison chart data — peak timing per RPM
    val timingChartData = remember(outputKfzw, kfzwPair) {
        val originalKfzw = kfzwPair?.second
        val calculatedKfzw = outputKfzw
        if (originalKfzw == null || calculatedKfzw == null) {
            return@remember Pair(emptyList<Pair<Double, Double>>(), emptyList<Pair<Double, Double>>())
        }

        val originalPeaks = originalKfzw.yAxis.mapIndexed { i, rpm ->
            val peakTiming = originalKfzw.zAxis[i].maxOrNull() ?: 0.0
            Pair(rpm, peakTiming)
        }
        val calculatedPeaks = calculatedKfzw.yAxis.mapIndexed { i, rpm ->
            val peakTiming = calculatedKfzw.zAxis[i].maxOrNull() ?: 0.0
            Pair(rpm, peakTiming)
        }
        Pair(originalPeaks, calculatedPeaks)
    }

    // Write prerequisites
    val binFile by BinFilePreferences.file.collectAsState()
    val binLoaded = binFile.exists() && binFile.isFile
    val kfzwMapConfigured = kfzwPair != null
    val kfmiopMapConfigured = kfmiopPair != null
    val canWrite = binLoaded && kfzwMapConfigured && kfmiopMapConfigured && outputKfzw != null

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
    if (showKfzwPicker) {
        MapPickerDialog(
            title = "Select KFZW Map",
            tableDefinitions = tableDefinitions,
            initialValue = kfzwPair?.first,
            onSelected = { KfzwPreferences.setSelectedMap(it) },
            onDismiss = { showKfzwPicker = false }
        )
    }

    if (showKfmiopPicker) {
        MapPickerDialog(
            title = "Select KFMIOP Map",
            tableDefinitions = tableDefinitions,
            initialValue = kfmiopPair?.first,
            onSelected = { KfmiopPreferences.setSelectedMap(it) },
            onDismiss = { showKfmiopPicker = false }
        )
    }

    if (showWriteConfirmation) {
        AlertDialog(
            onDismissRequest = { showWriteConfirmation = false },
            title = { Text("Write KFZW") },
            text = { Text("Are you sure you want to write KFZW to the binary?") },
            confirmButton = {
                TextButton(onClick = {
                    showWriteConfirmation = false
                    val tableDef = kfzwPair?.first
                    if (outputKfzw != null && tableDef != null) {
                        try {
                            BinWriter.write(BinFilePreferences.file.value, tableDef, outputKfzw)
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
            kfzwMapName = kfzwPair?.first?.tableName,
            kfmiopMapName = kfmiopPair?.first?.tableName,
            onSelectKfzw = { showKfzwPicker = true },
            onSelectKfmiop = { showKfmiopPicker = true },
            editedXAxis = editedXAxis,
            onXAxisChanged = { newData -> editedXAxis = newData }
        )

        ComparisonArea(
            modifier = Modifier.weight(1f),
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            editedInputMap = editedInputMap,
            onInputMapChanged = { editedInputMap = it },
            originalKfzw = kfzwPair?.second,
            outputKfzw = outputKfzw,
            originalPeakTiming = timingChartData.first,
            calculatedPeakTiming = timingChartData.second
        )

        WriteToBinarySection(
            binLoaded = binLoaded,
            binFileName = if (binLoaded) binFile.name else null,
            kfzwMapConfigured = kfzwMapConfigured,
            kfzwMapName = kfzwPair?.first?.tableName,
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
    kfzwMapName: String?,
    kfmiopMapName: String?,
    onSelectKfzw: () -> Unit,
    onSelectKfmiop: () -> Unit,
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
                text = "KFZW Calculator",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Rescale ignition timing for new load axis from KFMIOP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            MapSelectorRow(
                label = "KFZW:",
                mapName = kfzwMapName,
                onSelectMap = onSelectKfzw
            )

            MapSelectorRow(
                label = "KFMIOP (X-Axis):",
                mapName = kfmiopMapName,
                onSelectMap = onSelectKfmiop
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
                        text = "(provides the target load breakpoints for rescaling)",
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
    originalKfzw: Map3d?,
    outputKfzw: Map3d?,
    originalPeakTiming: List<Pair<Double, Double>>,
    calculatedPeakTiming: List<Pair<Double, Double>>
) {
    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = { Text("KFZW (Input)") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = { Text("KFZW (Comparison)") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                text = { Text("Timing Comparison") }
            )
        }

        when (selectedTab) {
            0 -> EditableInputSection(
                editedInputMap = editedInputMap,
                onInputMapChanged = onInputMapChanged,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            1 -> SideBySideTables(
                originalKfzw = originalKfzw,
                calculatedKfzw = outputKfzw,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            2 -> TimingComparisonChart(
                originalPeakTiming = originalPeakTiming,
                calculatedPeakTiming = calculatedPeakTiming,
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
                text = "KFZW Values",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Edit values to adjust the interpolation.",
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
    originalKfzw: Map3d?,
    calculatedKfzw: Map3d?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = "KFZW (Original)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (originalKfzw != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapTable(map = originalKfzw, editable = false)
                }
            } else {
                Text("No map loaded", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = "KFZW (Calculated)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (calculatedKfzw != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapTable(map = calculatedKfzw, editable = false)
                }
            } else {
                Text("No data", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TimingComparisonChart(
    originalPeakTiming: List<Pair<Double, Double>>,
    calculatedPeakTiming: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LineChart(
            series = listOf(
                ChartSeries(
                    name = "Original Peak Timing",
                    points = originalPeakTiming,
                    color = Primary
                ),
                ChartSeries(
                    name = "Calculated Peak Timing",
                    points = calculatedPeakTiming,
                    color = ChartRed
                )
            ),
            title = "Peak Timing Comparison",
            xAxisLabel = "RPM",
            yAxisLabel = "Timing (grad KW)"
        )
    }
}

@Composable
private fun WriteToBinarySection(
    binLoaded: Boolean,
    binFileName: String?,
    kfzwMapConfigured: Boolean,
    kfzwMapName: String?,
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
                label = "KFZW map",
                detail = if (kfzwMapConfigured) kfzwMapName!! else "Not configured",
                met = kfzwMapConfigured
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
                    Text("Write KFZW")
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
                    !binLoaded && !kfzwMapConfigured && !kfmiopMapConfigured ->
                        "Load a BIN file and select both KFZW and KFMIOP map definitions."
                    !binLoaded -> "Load a BIN file to write."
                    !kfzwMapConfigured && !kfmiopMapConfigured ->
                        "Select both KFZW and KFMIOP map definitions above."
                    !kfzwMapConfigured -> "Select the KFZW map definition above."
                    !kfmiopMapConfigured -> "Select the KFMIOP map definition above."
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
