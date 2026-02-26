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

    // Start with the existing selection, or auto-select the first filtered result
    // when the filter is pre-populated and there is no prior selection.
    var selectedItem by remember(filteredDefinitions) {
        mutableStateOf(
            initialValue ?: if (initialFilter.isNotBlank()) filteredDefinitions.firstOrNull() else null
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
        title = { Text(title) },
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
                        ListItem(
                            headlineContent = { Text(definition.toString()) },
                            modifier = Modifier.clickable {
                                selectedItem = definition
                            },
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
