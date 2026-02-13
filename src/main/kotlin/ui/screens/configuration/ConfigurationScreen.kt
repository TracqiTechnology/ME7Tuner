package ui.screens.configuration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.contract.Me7LogFileContract
import data.parser.xdf.XdfParser
import data.preferences.MapPreference
import data.preferences.MapPreferenceManager
import data.preferences.bin.BinFilePreferences
import data.preferences.filechooser.BinFileChooserPreferences
import data.preferences.filechooser.XdfFileChooserPreferences
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

private val defaultHeaderValues = mapOf(
    Me7LogFileContract.Header.START_TIME_HEADER to Me7LogFileContract.START_TIME_LABEL,
    Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER to Me7LogFileContract.TIME_COLUMN_LABEL,
    Me7LogFileContract.Header.RPM_COLUMN_HEADER to Me7LogFileContract.RPM_COLUMN_LABEL,
    Me7LogFileContract.Header.STFT_COLUMN_HEADER to Me7LogFileContract.STFT_COLUMN_LABEL,
    Me7LogFileContract.Header.LTFT_COLUMN_HEADER to Me7LogFileContract.LTFT_COLUMN_LABEL,
    Me7LogFileContract.Header.MAF_VOLTAGE_HEADER to Me7LogFileContract.MAF_VOLTAGE_LABEL,
    Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER to Me7LogFileContract.MAF_GRAMS_PER_SECOND_LABEL,
    Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER to Me7LogFileContract.THROTTLE_PLATE_ANGLE_LABEL,
    Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER to Me7LogFileContract.LAMBDA_CONTROL_ACTIVE_LABEL,
    Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER to Me7LogFileContract.REQUESTED_LAMBDA_LABEL,
    Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER to Me7LogFileContract.FUEL_INJECTOR_ON_TIME_LABEL,
    Me7LogFileContract.Header.ENGINE_LOAD_HEADER to Me7LogFileContract.ENGINE_LOAD_LABEL,
    Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER to Me7LogFileContract.WASTEGATE_DUTY_CYCLE_LABEL,
    Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER to Me7LogFileContract.BAROMETRIC_PRESSURE_LABEL,
    Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER to Me7LogFileContract.ABSOLUTE_BOOST_PRESSURE_ACTUAL_LABEL,
    Me7LogFileContract.Header.SELECTED_GEAR_HEADER to Me7LogFileContract.SELECTED_GEAR_LABEL,
    Me7LogFileContract.Header.WIDE_BAND_O2_HEADER to Me7LogFileContract.WIDE_BAND_O2_LABEL,
)

@Composable
fun ConfigurationScreen() {
    val scrollState = rememberScrollState()

    val xdfFile by XdfFilePreferences.file.collectAsState()
    val binFile by BinFilePreferences.file.collectAsState()

    val xdfLoaded = xdfFile.exists() && xdfFile.isFile
    val binLoaded = binFile.exists() && binFile.isFile
    val filesLoaded = xdfLoaded && binLoaded

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        FileLoadSection(xdfFile, xdfLoaded, binFile, binLoaded)

        Spacer(modifier = Modifier.height(16.dp))

        if (!filesLoaded) {
            FilesNotLoadedPlaceholder()
        } else {
            QuickSetupSection()

            Spacer(modifier = Modifier.height(16.dp))

            ManualConfigDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MapDefinitionsSection(modifier = Modifier.weight(1f))
                LogHeadersSection(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FileLoadSection(xdfFile: File, xdfLoaded: Boolean, binFile: File, binLoaded: Boolean) {
    Text(
        text = "Load Files",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FileCard(
            label = "XDF Definition File",
            fileName = if (xdfLoaded) xdfFile.name else "Not loaded",
            isLoaded = xdfLoaded,
            onOpen = {
                val dialog = FileDialog(null as Frame?, "Select XDF File", FileDialog.LOAD)
                dialog.setFilenameFilter { _, name -> name.endsWith(".xdf", ignoreCase = true) }
                val lastDir = XdfFileChooserPreferences.lastDirectory
                if (lastDir.isNotEmpty()) dialog.directory = lastDir
                dialog.isVisible = true
                val dir = dialog.directory
                val file = dialog.file
                if (dir != null && file != null) {
                    val selected = File(dir, file)
                    MapPreferenceManager.clear()
                    XdfFilePreferences.setFile(selected)
                    XdfFileChooserPreferences.lastDirectory = selected.parent
                }
            },
            modifier = Modifier.weight(1f)
        )

        FileCard(
            label = "BIN ECU File",
            fileName = if (binLoaded) binFile.name else "Not loaded",
            isLoaded = binLoaded,
            onOpen = {
                val dialog = FileDialog(null as Frame?, "Open Bin File", FileDialog.LOAD)
                dialog.setFilenameFilter { _, name -> name.endsWith(".bin", ignoreCase = true) }
                val lastDir = BinFileChooserPreferences.lastDirectory
                if (lastDir.isNotEmpty()) dialog.directory = lastDir
                dialog.isVisible = true
                val dir = dialog.directory
                val file = dialog.file
                if (dir != null && file != null) {
                    val selected = File(dir, file)
                    BinFilePreferences.setFile(selected)
                    BinFileChooserPreferences.lastDirectory = selected.parent
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FileCard(
    label: String,
    fileName: String,
    isLoaded: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLoaded) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = if (isLoaded) "Loaded" else "Not loaded",
                tint = if (isLoaded) MaterialTheme.colorScheme.tertiary
                       else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = onOpen) {
                Text(if (isLoaded) "Change\u2026" else "Open\u2026")
            }
        }
    }
}

@Composable
private fun FilesNotLoadedPlaceholder() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(32.dp).fillMaxWidth()
        ) {
            Text(
                text = "Load an XDF and BIN file above to begin configuration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickSetupSection() {
    val defaultProfiles by ProfileManager.defaultProfiles.collectAsState()
    val userProfiles by ProfileManager.userProfiles.collectAsState()
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column {
        Text(
            text = "Quick Setup",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Apply a preset profile to configure all map definitions and log headers at once.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (defaultProfiles.isNotEmpty()) {
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

                if (userProfiles.isNotEmpty()) {
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = {
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
                        Text("Load Profile\u2026")
                    }

                    OutlinedButton(onClick = {
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
                        Text("Save Current as Profile\u2026")
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
private fun ManualConfigDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "or configure manually",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MapDefinitionsSection(modifier: Modifier = Modifier) {
    val tableDefinitions by XdfParser.tableDefinitions.collectAsState()

    var pickerDialogEntry by remember { mutableStateOf<MapDefinitionEntry?>(null) }

    // Force recomposition when maps change by collecting mapChanged for each preference
    var version by remember { mutableStateOf(0) }

    for (entry in mapDefinitions) {
        LaunchedEffect(entry.preference) {
            entry.preference.mapChanged.collect {
                version++
            }
        }
    }

    val configuredCount = remember(version, tableDefinitions) {
        mapDefinitions.count { it.preference.getSelectedMap() != null }
    }

    Column(modifier = modifier) {
        Text(
            text = "Map Definitions ($configuredCount of ${mapDefinitions.size} configured)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Read version to trigger recomposition
                @Suppress("UNUSED_EXPRESSION")
                version

                for (entry in mapDefinitions) {
                    val selectedMap = remember(version, tableDefinitions) {
                        entry.preference.getSelectedMap()
                    }
                    val isConfigured = selectedMap != null
                    val selectedName = selectedMap?.first?.tableName ?: "Undefined"
                    val selectedUnit = selectedMap?.first?.zAxis?.unit ?: "-"

                    MapDefinitionRow(
                        title = entry.title,
                        isConfigured = isConfigured,
                        selectedName = selectedName,
                        selectedUnit = selectedUnit,
                        onSelect = { pickerDialogEntry = entry }
                    )
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
private fun MapDefinitionRow(
    title: String,
    isConfigured: Boolean,
    selectedName: String,
    selectedUnit: String,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isConfigured) Icons.Default.Check else Icons.Default.Close,
            contentDescription = if (isConfigured) "Configured" else "Not configured",
            tint = if (isConfigured) MaterialTheme.colorScheme.tertiary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$title:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(92.dp)
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
            onClick = onSelect,
            modifier = Modifier.padding(start = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("Select Definition")
        }
    }
}

@Composable
private fun LogHeadersSection(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var headerVersion by remember { mutableStateOf(0) }

    val customizedCount = remember(headerVersion) {
        Me7LogFileContract.Header.entries.count { header ->
            LogHeaderPreference.getHeader(header) != defaultHeaderValues[header]
        }
    }

    val subtitle = if (customizedCount == 0) "(using defaults)" else "($customizedCount customized)"

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
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                                     else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        for (header in Me7LogFileContract.Header.entries) {
                            LogHeaderRow(header) { headerVersion++ }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogHeaderRow(header: Me7LogFileContract.Header, onChanged: () -> Unit) {
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
                onChanged()
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f).height(48.dp)
        )
    }
}
