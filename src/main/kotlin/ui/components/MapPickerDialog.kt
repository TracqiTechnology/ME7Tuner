package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import data.parser.csv.WinOlsCsvParser
import data.parser.kp.KpHintParser
import data.parser.xdf.TableDefinition

@Composable
fun MapPickerDialog(
    title: String,
    tableDefinitions: List<TableDefinition>,
    initialValue: TableDefinition?,
    onSelected: (TableDefinition) -> Unit,
    onDismiss: () -> Unit,
    // Derive the default filter from the title: "Select KRKTE" → "KRKTE",
    // "Select KFMIOP Map" → "KFMIOP". Callers can override if needed.
    initialFilter: String = title
        .removePrefix("Select ")
        .removeSuffix(" Map")
        .trim()
) {
    // Observe KP hints and CSV definitions.
    val kpHints by KpHintParser.hints.collectAsState()
    val csvDefinitions by WinOlsCsvParser.definitions.collectAsState()

    // Find the KP hint (if any) that matches the map we're looking for.
    val kpHint = remember(initialFilter, kpHints) {
        kpHints.firstOrNull { it.name.equals(initialFilter, ignoreCase = true) }
    }

    // Find a matching WinOLS CSV definition for richer hint metadata.
    val csvHint = remember(initialFilter, csvDefinitions) {
        csvDefinitions.firstOrNull { it.id.equals(initialFilter, ignoreCase = true) }
    }

    // Use TextFieldValue so we can place the cursor at the end of the pre-populated text,
    // making it easy to append or clear without requiring an extra click.
    var filterField by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialFilter,
                selection = TextRange(initialFilter.length)
            )
        )
    }
    val filterText = filterField.text

    val filteredDefinitions = remember(filterText, tableDefinitions) {
        if (filterText.isBlank()) tableDefinitions
        else tableDefinitions.filter {
            it.tableName.lowercase().contains(filterText.lowercase())
        }
    }

    // When a KP hint with an address is available, prefer the XDF definition whose
    // z-axis address matches the KP AR address.
    val kpPreferredDefinition = remember(kpHint, csvHint, tableDefinitions) {
        // Prefer CSV address (more reliable, explicit column) over KP parsed address
        val preferredAddress = when {
            csvHint != null && csvHint.hasAddress -> csvHint.address
            kpHint != null && kpHint.hasAddress   -> kpHint.arAddress
            else                                  -> -1
        }
        if (preferredAddress > 0) {
            tableDefinitions.firstOrNull { def -> def.zAxis.address == preferredAddress }
        } else null
    }

    // Start with: KP-address-matched definition > existing selection > first filtered result
    var selectedItem by remember(filteredDefinitions, kpPreferredDefinition) {
        mutableStateOf(
            kpPreferredDefinition
                ?: initialValue
                ?: if (initialFilter.isNotBlank()) filteredDefinitions.firstOrNull() else null
        )
    }

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    // Auto-focus the filter field so the user can type immediately.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title)

                // WinOLS CSV hint — richer than KP because it has dimensions, units, scaling
                if (csvHint != null) {
                    val addrStr = if (csvHint.hasAddress) " @ 0x${csvHint.address.toString(16).uppercase()}" else ""
                    val dimStr = if (csvHint.is2D) " [${csvHint.dimensionString}]"
                                 else " [${maxOf(csvHint.columns, csvHint.rows)}]"
                    val unitStr = if (csvHint.units.isNotBlank()) " — ${csvHint.units}" else ""
                    val scaleStr = if (csvHint.scale != 1.0) " × ${csvHint.scale}" else ""
                    Text(
                        text = "WinOLS CSV: ${csvHint.id}$addrStr$dimStr$unitStr$scaleStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (csvHint.name.isNotBlank() && csvHint.name != "-") {
                        Text(
                            text = csvHint.name.take(80),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // KP hint badge — shown when a WinOLS KP file is loaded and has a match
                // (shown alongside CSV hint if both are available)
                if (kpHint != null && csvHint == null) {
                    val addrStr = if (kpHint.hasAddress) " @ 0x${kpHint.arAddress.toString(16).uppercase()}" else ""
                    Text(
                        text = "WinOLS KP: ${kpHint.name}$addrStr — ${kpHint.description.take(60)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.width(500.dp).height(400.dp)) {
                OutlinedTextField(
                    value = filterField,
                    onValueChange = { new ->
                        filterField = new
                        // When the user changes the filter, auto-select the first match
                        // so pressing Set immediately picks the best result.
                        selectedItem = filteredDefinitions.firstOrNull {
                            it.tableName.lowercase().contains(new.text.lowercase())
                        }
                    },
                    label = { Text("Filter") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .focusRequester(focusRequester)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    if (filteredDefinitions.isEmpty()) {
                        item {
                            val message = if (tableDefinitions.isEmpty()) {
                                "No table definitions available. Load an XDF file first (File \u2192 Select XDF...)."
                            } else {
                                "No matching definitions."
                            }
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(filteredDefinitions) { definition ->
                        // Highlight KP-address-matched definitions with a subtle indicator
                        val isKpMatch = kpPreferredDefinition == definition
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(definition.toString())
                                    if (isKpMatch) {
                                        Spacer(Modifier.width(6.dp))
                                        // Show "CSV" badge if matched via CSV, "KP" if via KP binary
                                        val badgeLabel = if (csvHint != null && csvHint.hasAddress) "CSV" else "KP"
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = badgeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.clickable { selectedItem = definition },
                            colors = if (selectedItem == definition) {
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            } else {
                                ListItemDefaults.colors()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedItem?.let { onSelected(it) }
                    onDismiss()
                },
                enabled = selectedItem != null
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

