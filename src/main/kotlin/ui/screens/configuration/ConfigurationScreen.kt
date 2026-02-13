package ui.screens.configuration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.contract.Me7LogFileContract
import data.parser.xdf.TableDefinition
import data.parser.xdf.XdfParser
import data.preferences.MapPreference
import data.preferences.bin.BinFilePreferences
import data.preferences.xdf.XdfFilePreferences
import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfmirl.KfmirlPreferences
import data.preferences.kfvpdksd.KfvpdksdPreferences
import data.preferences.kfwdkmsn.KfwdkmsnPreferences
import data.preferences.kfzw.KfzwPreferences
import data.preferences.kfzwop.KfzwopPreferences
import data.preferences.krkte.KrktePreferences
import data.preferences.logheaderdefinition.LogHeaderPreference
import data.preferences.mlhfm.MlhfmPreferences
import data.preferences.wdkugdn.WdkugdnPreferences
import data.profile.ProfileManager
import ui.components.MapPickerDialog
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

private data class MapDefinitionEntry(
    val title: String,
    val preference: MapPreference
)

private val mapDefinitions = listOf(
    MapDefinitionEntry("KRKTE", KrktePreferences),
    MapDefinitionEntry("MLHFM", MlhfmPreferences),
    MapDefinitionEntry("KFMIOP", KfmiopPreferences),
    MapDefinitionEntry("KFMIRL", KfmirlPreferences),
    MapDefinitionEntry("KFZWOP", KfzwopPreferences),
    MapDefinitionEntry("KFZW", KfzwPreferences),
    MapDefinitionEntry("KFVPDKSD", KfvpdksdPreferences),
    MapDefinitionEntry("WDKUGDN", WdkugdnPreferences),
    MapDefinitionEntry("KFWDKMSN", KfwdkmsnPreferences),
    MapDefinitionEntry("KFLDRL", KfldrlPreferences),
    MapDefinitionEntry("KFLDIMX", KfldimxPreferences),
)

@Composable
fun ConfigurationScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        ProfileSection()

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TableDefinitionSection(modifier = Modifier.weight(1f))
            LogHeaderSection(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProfileSection() {
    val defaultProfiles by ProfileManager.defaultProfiles.collectAsState()
    val userProfiles by ProfileManager.userProfiles.collectAsState()
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column {
        Text(
            text = "Profiles",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (defaultProfiles.isNotEmpty()) {
                    Text(
                        text = "Default Profiles",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    for (profile in defaultProfiles) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile.name.ifEmpty { "Unnamed Profile" },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    ProfileManager.applyProfile(profile)
                                    statusMessage = "Applied profile: ${profile.name}"
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Apply")
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = {
                        val dialog = FileDialog(null as Frame?, "Load Profile", FileDialog.LOAD)
                        dialog.setFilenameFilter { _, name -> name.endsWith(".me7profile.json", ignoreCase = true) }
                        dialog.isVisible = true
                        val dir = dialog.directory
                        val file = dialog.file
                        if (dir != null && file != null) {
                            runCatching {
                                val profile = ProfileManager.loadFromFile(File(dir, file))
                                ProfileManager.applyProfile(profile)
                                ProfileManager.addUserProfile(profile)
                                statusMessage = "Loaded and applied profile: ${profile.name}"
                            }.onFailure {
                                statusMessage = "Failed to load profile: ${it.message}"
                            }
                        }
                    }) {
                        Text("Load Profile...")
                    }

                    Button(onClick = {
                        SwingUtilities.invokeLater {
                            val name = JOptionPane.showInputDialog(
                                null,
                                "Profile name:",
                                "Save Profile",
                                JOptionPane.PLAIN_MESSAGE
                            )
                            if (name != null && name.isNotBlank()) {
                                val dialog = FileDialog(null as Frame?, "Save Profile", FileDialog.SAVE)
                                dialog.file = "${name.replace(Regex("[^a-zA-Z0-9_ -]"), "")}.me7profile.json"
                                dialog.isVisible = true
                                val dir = dialog.directory
                                val fileName = dialog.file
                                if (dir != null && fileName != null) {
                                    runCatching {
                                        val profile = ProfileManager.exportCurrentProfile(name)
                                        val targetFile = File(dir, fileName)
                                        ProfileManager.saveToFile(profile, targetFile)
                                        statusMessage = "Saved profile: $name"
                                    }.onFailure {
                                        statusMessage = "Failed to save profile: ${it.message}"
                                    }
                                }
                            }
                        }
                    }) {
                        Text("Save Current as Profile...")
                    }
                }

                if (userProfiles.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Loaded Profiles",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    for (profile in userProfiles) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile.name.ifEmpty { "Unnamed Profile" },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    ProfileManager.applyProfile(profile)
                                    statusMessage = "Applied profile: ${profile.name}"
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Apply")
                            }
                        }
                    }
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TableDefinitionSection(modifier: Modifier = Modifier) {
    val tableDefinitions by XdfParser.tableDefinitions.collectAsState()

    var pickerDialogEntry by remember { mutableStateOf<MapDefinitionEntry?>(null) }

    // Force recomposition when maps change by collecting mapChanged for each preference
    // We use a version counter that increments whenever any map selection changes
    var version by remember { mutableStateOf(0) }

    for (entry in mapDefinitions) {
        LaunchedEffect(entry.preference) {
            entry.preference.mapChanged.collect {
                version++
            }
        }
    }

    val xdfFile by XdfFilePreferences.file.collectAsState()
    val binFile by BinFilePreferences.file.collectAsState()

    val xdfLoaded = xdfFile.exists() && xdfFile.isFile
    val binLoaded = binFile.exists() && binFile.isFile

    Column(modifier = modifier) {
        Text(
            text = "Map Definitions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "XDF: ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (xdfLoaded) xdfFile.name else "Not loaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (xdfLoaded) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        text = "BIN: ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (binLoaded) binFile.name else "Not loaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (binLoaded) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.error
                    )
                }

                if (!xdfLoaded) {
                    Text(
                        text = "Load an XDF file via File \u2192 Select XDF to populate map definitions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                // Read version to trigger recomposition
                @Suppress("UNUSED_EXPRESSION")
                version

                for (entry in mapDefinitions) {
                    val selectedMap = remember(version, tableDefinitions) {
                        entry.preference.getSelectedMap()
                    }
                    val selectedName = selectedMap?.first?.tableName ?: "Undefined"
                    val selectedUnit = selectedMap?.first?.zAxis?.unit ?: "-"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${entry.title}:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = selectedName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = selectedUnit,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(60.dp)
                        )
                        Button(
                            onClick = { pickerDialogEntry = entry },
                            modifier = Modifier.padding(start = 8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Select Definition")
                        }
                    }
                }
            }
        }
    }

    if (pickerDialogEntry != null) {
        val entry = pickerDialogEntry!!
        val currentSelection = remember { entry.preference.getSelectedMap()?.first }

        MapPickerDialog(
            title = "Select ${entry.title}",
            tableDefinitions = tableDefinitions,
            initialValue = currentSelection,
            onSelected = { tableDefinition ->
                entry.preference.setSelectedMap(tableDefinition)
            },
            onDismiss = { pickerDialogEntry = null }
        )
    }
}

@Composable
private fun LogHeaderSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Log Headers",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                for (header in Me7LogFileContract.Header.entries) {
                    LogHeaderRow(header)
                }
            }
        }
    }
}

@Composable
private fun LogHeaderRow(header: Me7LogFileContract.Header) {
    var headerValue by remember {
        mutableStateOf(LogHeaderPreference.getHeader(header))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${header.title}:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(180.dp)
        )
        OutlinedTextField(
            value = headerValue,
            onValueChange = { newValue ->
                headerValue = newValue
                LogHeaderPreference.setHeader(header, newValue)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f).height(48.dp)
        )
    }
}
