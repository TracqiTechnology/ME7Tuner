package ui.screens.kfzwop

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
import data.preferences.kfzwop.KfzwopPreferences
import data.writer.BinWriter
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
fun KfzwopScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val tableDefinitions by XdfParser.tableDefinitions.collectAsState()

    var showMapPicker by remember { mutableStateOf(false) }

    // Find the KFZWOP map based on preference
    val kfzwopPair = remember(mapList) { findMap(mapList, KfzwopPreferences) }
    val inputKfzwop = kfzwopPair?.second

    // Editable X-axis (KFMIOP/KFZWOP load axis)
    var editedXAxis by remember(inputKfzwop) {
        mutableStateOf(
            if (inputKfzwop != null) arrayOf(inputKfzwop.xAxis.copyOf())
            else arrayOf(emptyArray<Double>())
        )
    }

    // Editable input map
    var editedInputMap by remember(inputKfzwop) {
        mutableStateOf(inputKfzwop?.let { Map3d(it) })
    }

    // Calculate the rescaled KFZWOP output
    val outputKfzwop = remember(editedInputMap, editedXAxis) {
        val input = editedInputMap
        if (input != null && editedXAxis.isNotEmpty() && editedXAxis[0].isNotEmpty() && editedXAxis[0][0] != null) {
            val newXAxis = editedXAxis[0]
            val newZAxis = Kfzw.generateKfzw(input.xAxis, input.zAxis, newXAxis)
            Map3d(newXAxis, input.yAxis, newZAxis)
        } else null
    }

    // Write confirmation dialog
    var showWriteConfirmation by remember { mutableStateOf(false) }

    if (showMapPicker) {
        MapPickerDialog(
            title = "Select KFZWOP Map",
            tableDefinitions = tableDefinitions,
            initialValue = kfzwopPair?.first,
            onSelected = { KfzwopPreferences.setSelectedMap(it) },
            onDismiss = { showMapPicker = false }
        )
    }

    if (showWriteConfirmation) {
        AlertDialog(
            onDismissRequest = { showWriteConfirmation = false },
            title = { Text("Write KFZWOP") },
            text = { Text("Are you sure you want to write KFZWOP to the binary?") },
            confirmButton = {
                TextButton(onClick = {
                    showWriteConfirmation = false
                    val tableDef = kfzwopPair?.first
                    if (outputKfzwop != null && tableDef != null) {
                        try {
                            BinWriter.write(BinFilePreferences.getStoredFile(), tableDef, outputKfzwop)
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
        // Left side: KFZWOP Input
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("KFMIOP/KFZWOP X-Axis (Input)", style = MaterialTheme.typography.titleMedium)
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

            Text("KFZWOP (Input)", style = MaterialTheme.typography.titleMedium)
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
                text = kfzwopPair?.first?.tableName ?: "No Map Selected",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { showMapPicker = true }) {
                Text("Select KFZWOP Map")
            }
        }

        // Right side: KFZWOP Output
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("KFZWOP (Output)", style = MaterialTheme.typography.titleMedium)
            if (outputKfzwop != null) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)) {
                    MapTable(map = outputKfzwop, editable = false)
                }
            } else {
                Text("No data", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showWriteConfirmation = true },
                enabled = outputKfzwop != null && kfzwopPair != null
            ) {
                Text("Write KFZWOP")
            }
        }
    }
}
