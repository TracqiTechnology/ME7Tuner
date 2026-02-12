package ui.screens.kfmirl

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
import data.preferences.kfmirl.KfmirlPreferences
import data.writer.BinWriter
import domain.math.Inverse
import domain.math.map.Map3d
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
fun KfmirlScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val tableDefinitions by XdfParser.tableDefinitions.collectAsState()

    var showKfmiopPicker by remember { mutableStateOf(false) }
    var showKfmirlPicker by remember { mutableStateOf(false) }

    // Find maps based on preferences
    val kfmiopPair = remember(mapList) { findMap(mapList, KfmiopPreferences) }
    val kfmirlPair = remember(mapList) { findMap(mapList, KfmirlPreferences) }

    val inputKfmiop = kfmiopPair?.second

    // Editable X-axis for KFMIOP (user can change load breakpoints)
    var editedXAxis by remember(inputKfmiop) {
        mutableStateOf(
            if (inputKfmiop != null) arrayOf(inputKfmiop.xAxis.copyOf())
            else arrayOf(emptyArray<Double>())
        )
    }

    // Editable input map (user can edit KFMIOP zAxis values)
    var editedInputMap by remember(inputKfmiop) {
        mutableStateOf(inputKfmiop?.let { Map3d(it) })
    }

    // Calculate KFMIRL as inverse of KFMIOP
    val outputKfmirl = remember(editedInputMap, editedXAxis, kfmirlPair) {
        val kfmiop = editedInputMap
        val kfmirlBase = kfmirlPair?.second
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

    // Write confirmation dialog
    var showWriteConfirmation by remember { mutableStateOf(false) }

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
                            BinWriter.write(BinFilePreferences.getStoredFile(), tableDef, outputKfmirl)
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
        // Left side: KFMIOP Input with X-Axis
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("KFMIOP X-Axis (Output)", style = MaterialTheme.typography.titleMedium)
            if (editedXAxis.isNotEmpty() && editedXAxis[0].isNotEmpty()) {
                MapAxis(
                    data = editedXAxis,
                    editable = true,
                    onDataChanged = { newData ->
                        editedXAxis = newData
                        // When X-axis changes, update the input map's column headers
                        editedInputMap?.let { currentMap ->
                            editedInputMap = Map3d(newData[0], currentMap.yAxis, currentMap.zAxis)
                        }
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Text("KFMIOP (Input)", style = MaterialTheme.typography.titleMedium)
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
                text = kfmiopPair?.first?.tableName ?: "No Definition Selected",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { showKfmiopPicker = true }) {
                Text("Select KFMIOP Map")
            }
        }

        // Right side: KFMIRL Output
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("KFMIRL (Output)", style = MaterialTheme.typography.titleMedium)
            if (outputKfmirl != null) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)) {
                    MapTable(map = outputKfmirl, editable = false)
                }
            } else {
                Text("No data", style = MaterialTheme.typography.bodyMedium)
            }

            Text(
                text = kfmirlPair?.first?.tableName ?: "No Definition Selected",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { showKfmirlPicker = true }) {
                Text("Select KFMIRL Map")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showWriteConfirmation = true },
                enabled = outputKfmirl != null && kfmirlPair != null
            ) {
                Text("Write KFMIRL")
            }
        }
    }
}
