package ui.screens.wdkugdn

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
import data.preferences.bin.BinFilePreferences
import data.preferences.kfwdkmsn.KfwdkmsnPreferences
import data.preferences.wdkugdn.WdkugdnPreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import domain.model.wdkugdn.Wdkugdn
import kotlinx.coroutines.delay
import ui.components.MapTable

private enum class WriteStatus { Idle, Success, Error }

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

    // Write state
    val binFile by BinFilePreferences.file.collectAsState()
    val binLoaded = binFile.exists() && binFile.isFile
    val wdkugdnConfigured = WdkugdnPreferences.getSelectedMap() != null
    val kfwdkmsnConfigured = KfwdkmsnPreferences.getSelectedMap() != null

    var showWriteConfirmation by remember { mutableStateOf(false) }
    var writeStatus by remember { mutableStateOf(WriteStatus.Idle) }

    LaunchedEffect(writeStatus) {
        if (writeStatus != WriteStatus.Idle) {
            delay(3000)
            writeStatus = WriteStatus.Idle
        }
    }

    // Write confirmation dialog
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
                            try {
                                BinWriter.write(
                                    BinFilePreferences.file.value,
                                    wdkugdnPair.first,
                                    outputMap
                                )
                                writeStatus = WriteStatus.Success
                            } catch (e: Exception) {
                                e.printStackTrace()
                                writeStatus = WriteStatus.Error
                            }
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

    // ── Main layout ───────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {
        // ── WDKUGDN table fills available space ───────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "WDKUGDN (Output)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (outputMap != null && outputMap.zAxis.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    MapTable(map = outputMap, editable = false)
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "No data available. Ensure WDKUGDN and KFWDKMSN map definitions are configured.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Action bar ────────────────────────────────────────────────
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Engine displacement input
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Prerequisites
                PrerequisiteRow(
                    label = "WDKUGDN map",
                    detail = if (wdkugdnConfigured) definitionTitle ?: "Configured" else "Not configured",
                    met = wdkugdnConfigured
                )

                PrerequisiteRow(
                    label = "KFWDKMSN map",
                    detail = if (kfwdkmsnConfigured) "Configured" else "Not configured",
                    met = kfwdkmsnConfigured
                )

                PrerequisiteRow(
                    label = "BIN file",
                    detail = if (binLoaded) binFile.name else "Not loaded",
                    met = binLoaded
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Write action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showWriteConfirmation = true },
                        enabled = binLoaded && wdkugdnConfigured && kfwdkmsnConfigured && outputMap != null
                    ) {
                        Text("Write WDKUGDN")
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

                if (!wdkugdnConfigured || !kfwdkmsnConfigured) {
                    Text(
                        text = "Configure the WDKUGDN and KFWDKMSN map definitions in the Configuration screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
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
            modifier = Modifier.width(110.dp)
        )

        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
