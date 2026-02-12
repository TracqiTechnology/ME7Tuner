package ui.screens.wdkugdn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.parser.bin.BinParser
import data.preferences.bin.BinFilePreferences
import data.preferences.kfwdkmsn.KfwdkmsnPreferences
import data.preferences.wdkugdn.WdkugdnPreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import domain.model.wdkugdn.Wdkugdn
import ui.components.MapTable

@Composable
fun WdkugdnScreen() {
    val mapList by BinParser.mapList.collectAsState()

    var displacementText by remember { mutableStateOf(WdkugdnPreferences.displacement.toString()) }

    // Track map preference changes
    var wdkugdnVersion by remember { mutableStateOf(0) }
    var kfwdkmsnVersion by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        WdkugdnPreferences.mapChanged.collect {
            wdkugdnVersion++
        }
    }

    LaunchedEffect(Unit) {
        KfwdkmsnPreferences.mapChanged.collect {
            kfwdkmsnVersion++
        }
    }

    // Calculate the WDKUGDN output map
    val wdkugdnResult: Pair<String?, Map3d?> = remember(mapList, wdkugdnVersion, kfwdkmsnVersion, displacementText) {
        val wdkugdnPair = WdkugdnPreferences.getSelectedMap()
        val kfwdkmsnPair = KfwdkmsnPreferences.getSelectedMap()
        val disp = displacementText.toDoubleOrNull() ?: WdkugdnPreferences.displacement

        val title = wdkugdnPair?.first?.tableName

        if (wdkugdnPair != null && kfwdkmsnPair != null) {
            val result = Wdkugdn.calculateWdkugdn(wdkugdnPair.second, kfwdkmsnPair.second, disp)
            title to result
        } else {
            title to null
        }
    }

    val definitionTitle = wdkugdnResult.first
    val outputMap = wdkugdnResult.second

    var showWriteConfirmation by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Engine Displacement panel
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Engine Displacement",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Engine Displacement:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = displacementText,
                        onValueChange = { newValue ->
                            displacementText = newValue
                            newValue.toDoubleOrNull()?.let {
                                WdkugdnPreferences.displacement = it
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(120.dp).height(48.dp)
                    )
                    Text(
                        text = "Liters",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // WDKUGDN Output label
        Text(
            text = "WDKUGDN (Output)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Map table
        if (outputMap != null && outputMap.zAxis.isNotEmpty()) {
            Box(modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth()) {
                MapTable(
                    map = outputMap,
                    editable = false
                )
            }
        } else {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().height(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "No data available. Ensure WDKUGDN and KFWDKMSN map definitions are configured.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status label
        Text(
            text = definitionTitle ?: "No Table Defined",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Write button
        Button(
            onClick = { showWriteConfirmation = true },
            enabled = outputMap != null
        ) {
            Text("Write WDKUGDN")
        }
    }

    if (showWriteConfirmation) {
        AlertDialog(
            onDismissRequest = { showWriteConfirmation = false },
            title = { Text("Write WDKUGDN") },
            text = { Text("Are you sure you want to write WDKUGDN to the binary?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWriteConfirmation = false
                        val wdkugdnPair = WdkugdnPreferences.getSelectedMap()
                        if (wdkugdnPair != null && outputMap != null) {
                            BinWriter.write(
                                BinFilePreferences.file.value,
                                wdkugdnPair.first,
                                outputMap
                            )
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWriteConfirmation = false }) {
                    Text("No")
                }
            }
        )
    }
}
