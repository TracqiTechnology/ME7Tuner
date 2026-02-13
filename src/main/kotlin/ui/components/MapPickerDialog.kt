package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.parser.xdf.TableDefinition

@Composable
fun MapPickerDialog(
    title: String,
    tableDefinitions: List<TableDefinition>,
    initialValue: TableDefinition?,
    onSelected: (TableDefinition) -> Unit,
    onDismiss: () -> Unit
) {
    var filterText by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf(initialValue) }

    val filteredDefinitions = remember(filterText, tableDefinitions) {
        if (filterText.isBlank()) tableDefinitions
        else tableDefinitions.filter {
            it.tableName.lowercase().contains(filterText.lowercase())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.width(500.dp).height(400.dp)) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    label = { Text("Filter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
