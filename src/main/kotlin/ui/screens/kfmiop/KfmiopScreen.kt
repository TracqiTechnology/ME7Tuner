package ui.screens.kfmiop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import ui.components.MapAxis
import ui.components.MapPickerDialog
import ui.components.MapTable

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

    // Find the KFMIOP map based on preference
    val kfmiopPair = remember(mapList) { findMap(mapList, KfmiopPreferences) }
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

    // Confirmation dialog for writing
    var showWriteConfirmation by remember { mutableStateOf(false) }

    // Tabbed pane state
    var inputTabIndex by remember { mutableStateOf(0) }
    var outputTabIndex by remember { mutableStateOf(0) }

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
        // Left side: Input
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Calculated Maximum MAP Pressure panel (read-only)
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Calculated Maximum Boost",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("MAP Sensor Maximum:", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = kfmiopResult?.maxMapSensorPressure?.toInt()?.toString() ?: "",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                            label = { Text("mbar") }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Boost Pressure Maximum:", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = kfmiopResult?.maxBoostPressure?.toInt()?.toString() ?: "",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                            label = { Text("mbar") }
                        )
                    }
                }
            }

            // Input tabbed pane: Torque / Boost
            PrimaryTabRow(selectedTabIndex = inputTabIndex) {
                Tab(
                    selected = inputTabIndex == 0,
                    onClick = { inputTabIndex = 0 },
                    text = { Text("Torque") }
                )
                Tab(
                    selected = inputTabIndex == 1,
                    onClick = { inputTabIndex = 1 },
                    text = { Text("Boost") }
                )
            }

            when (inputTabIndex) {
                0 -> {
                    Text("KFMIOP (Input)", style = MaterialTheme.typography.titleMedium)
                    if (inputKfmiop != null) {
                        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)) {
                            MapTable(map = inputKfmiop, editable = false)
                        }
                    } else {
                        Text("No map loaded", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                1 -> {
                    Text("Boost (Input)", style = MaterialTheme.typography.titleMedium)
                    val inputBoost = kfmiopResult?.inputBoost
                    if (inputBoost != null) {
                        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)) {
                            MapTable(map = inputBoost, editable = false)
                        }
                    } else {
                        Text("No data", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Definition label
            Text(
                text = kfmiopPair?.first?.tableName ?: "No Definition Selected",
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(onClick = { showMapPicker = true }) {
                Text("Select KFMIOP Map")
            }
        }

        // Right side: Output
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Desired Maximum MAP Pressure panel (editable)
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Desired Maximum Boost",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("MAP Sensor Maximum:", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = desiredMaxMapPressure,
                            onValueChange = {
                                desiredMaxMapPressure = it
                                it.toDoubleOrNull()?.let { v ->
                                    KfmiopPreferences.maxMapPressure = v
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                            label = { Text("mbar") }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Boost Pressure Maximum:", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = desiredMaxBoostPressure,
                            onValueChange = {
                                desiredMaxBoostPressure = it
                                it.toDoubleOrNull()?.let { v ->
                                    KfmiopPreferences.maxBoostPressure = v
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                            label = { Text("mbar") }
                        )
                    }
                }
            }

            // Output X-Axis
            Text("KFMIOP X-Axis (Output)", style = MaterialTheme.typography.titleMedium)
            val outputKfmiop = kfmiopResult?.outputKfmiop
            if (outputKfmiop != null) {
                val xAxisData = remember(outputKfmiop) {
                    arrayOf(outputKfmiop.xAxis.copyOf())
                }
                MapAxis(data = xAxisData, editable = true)
            }

            // Output tabbed pane: Torque / Boost
            PrimaryTabRow(selectedTabIndex = outputTabIndex) {
                Tab(
                    selected = outputTabIndex == 0,
                    onClick = { outputTabIndex = 0 },
                    text = { Text("Torque") }
                )
                Tab(
                    selected = outputTabIndex == 1,
                    onClick = { outputTabIndex = 1 },
                    text = { Text("Boost") }
                )
            }

            when (outputTabIndex) {
                0 -> {
                    Text("KFMIOP (Output)", style = MaterialTheme.typography.titleMedium)
                    if (outputKfmiop != null) {
                        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)) {
                            MapTable(map = outputKfmiop, editable = false)
                        }
                    } else {
                        Text("No data", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                1 -> {
                    Text("Boost (Output)", style = MaterialTheme.typography.titleMedium)
                    val outputBoost = kfmiopResult?.outputBoost
                    if (outputBoost != null) {
                        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)) {
                            MapTable(map = outputBoost, editable = false)
                        }
                    } else {
                        Text("No data", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Write button
            Button(
                onClick = { showWriteConfirmation = true },
                enabled = outputKfmiop != null && kfmiopPair != null
            ) {
                Text("Write KFMIOP")
            }
        }
    }
}
