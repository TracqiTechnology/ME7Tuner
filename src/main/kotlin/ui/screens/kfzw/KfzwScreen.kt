package ui.screens.kfzw

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun KfzwScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val tableDefinitions by XdfParser.tableDefinitions.collectAsState()

    var showMapPicker by remember { mutableStateOf(false) }

    // Find the KFZW and KFMIOP maps based on preferences
    val kfzwPair = remember(mapList) { findMap(mapList, KfzwPreferences) }
    val kfmiopPair = remember(mapList) { findMap(mapList, KfmiopPreferences) }

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
    // KFZW has potentially more resolution than KFMIRL, so we rescale the axis
    val outputKfzw = remember(editedInputMap, editedXAxis) {
        val input = editedInputMap
        if (input != null && editedXAxis.isNotEmpty() && editedXAxis[0].isNotEmpty()) {
            val newXAxis = editedXAxis[0]
            val maxValue = newXAxis.last()
            // Rescale the KFZW x-axis up to the max load value
            val rescaledXAxis = RescaleAxis.rescaleAxis(input.xAxis, maxValue)
            val newZAxis = Kfzw.generateKfzw(input.xAxis, input.zAxis, rescaledXAxis)
            Map3d(rescaledXAxis, input.yAxis, newZAxis)
        } else null
    }

    // Write confirmation dialog
    var showWriteConfirmation by remember { mutableStateOf(false) }

    if (showMapPicker) {
        MapPickerDialog(
            title = "Select KFZW Map",
            tableDefinitions = tableDefinitions,
            initialValue = kfzwPair?.first,
            onSelected = { KfzwPreferences.setSelectedMap(it) },
            onDismiss = { showMapPicker = false }
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
                            BinWriter.write(BinFilePreferences.getStoredFile(), tableDef, outputKfzw)
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
        // Left side: KFZW Input
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("KFMIOP X-Axis (Input)", style = MaterialTheme.typography.titleMedium)
            if (editedXAxis.isNotEmpty() && editedXAxis[0].isNotEmpty()) {
                MapAxis(
                    data = editedXAxis,
                    editable = true,
                    onDataChanged = { newData ->
                        editedXAxis = newData
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Text("KFZW (Input)", style = MaterialTheme.typography.titleMedium)
            if (editedInputMap != null) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)) {
                    MapTable(
                        map = editedInputMap!!,
                        editable = true,
                        onMapChanged = { newMap ->
                            editedInputMap = newMap
                        }
                    )
                }
            } else {
                Text("No map loaded", style = MaterialTheme.typography.bodyMedium)
            }

            Text(
                text = kfzwPair?.first?.tableName ?: "No Map Selected",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { showMapPicker = true }) {
                Text("Select KFZW Map")
            }
        }

        // Right side: KFZW Output
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("KFZW (Output)", style = MaterialTheme.typography.titleMedium)
            if (outputKfzw != null) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)) {
                    MapTable(map = outputKfzw, editable = false)
                }
            } else {
                Text("No data", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showWriteConfirmation = true },
                enabled = outputKfzw != null && kfzwPair != null
            ) {
                Text("Write KFZW")
            }
        }
    }
}
