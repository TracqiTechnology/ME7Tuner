package ui.screens.wdkugdn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.parser.bin.BinParser
import data.parser.me7log.AlphaNLogParser
import data.parser.xdf.TableDefinition
import data.preferences.bin.BinFilePreferences
import data.preferences.kfpbrk.KfpbrkPreferences
import data.preferences.kfwdkmsn.KfwdkmsnPreferences
import data.preferences.plsol.PlsolPreferences
import data.preferences.wdkugdn.WdkugdnPreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import domain.model.alphan.AlphaNDiagnostic
import domain.model.wdkugdn.Wdkugdn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.components.MapTable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

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
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("WDKUGDN") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("Alpha-N Diagnostic") })
        }

        when (selectedTab) {
            0 -> WdkugdnTab(
                outputMap = outputMap,
                definitionTitle = definitionTitle,
                displacementText = displacementText,
                onDisplacementChange = { displacementText = it },
                wdkugdnConfigured = wdkugdnConfigured,
                kfwdkmsnConfigured = kfwdkmsnConfigured,
                binLoaded = binLoaded,
                binFileName = if (binLoaded) binFile.name else "Not loaded",
                onWriteClick = { showWriteConfirmation = true },
                writeStatus = writeStatus
            )
            1 -> AlphaNDiagnosticTab()
        }
    }
}

// ── Tab 1: WDKUGDN ───────────────────────────────────────────────────

@Composable
private fun WdkugdnTab(
    outputMap: Map3d?,
    definitionTitle: String?,
    displacementText: String,
    onDisplacementChange: (String) -> Unit,
    wdkugdnConfigured: Boolean,
    kfwdkmsnConfigured: Boolean,
    binLoaded: Boolean,
    binFileName: String,
    onWriteClick: () -> Unit,
    writeStatus: WriteStatus
) {
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
                            onDisplacementChange(newValue)
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
                    detail = if (binLoaded) binFileName else "Not loaded",
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
                        onClick = onWriteClick,
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


// ── Tab 2: Alpha-N Diagnostic ─────────────────────────────────────────

@Composable
private fun AlphaNDiagnosticTab() {
    val scope = rememberCoroutineScope()
    val mapList by BinParser.mapList.collectAsState()
    var alphaNResult by remember { mutableStateOf<AlphaNDiagnostic.AlphaNResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadedFileName by remember { mutableStateOf<String?>(null) }

    // Read current KFURL, displacement, and KFPBRK map from preferences/BIN
    val currentKfurl = PlsolPreferences.kfurl
    val displacement = PlsolPreferences.displacement

    // Resolve KFURL Pair<TableDefinition, Map3d> from BIN
    val kfurlPair: Pair<TableDefinition, Map3d>? = remember(mapList) {
        mapList.find {
            it.first.tableName.equals("KFURL", ignoreCase = true) ||
                it.first.tableName.contains("KFURL", ignoreCase = true)
        }
    }
    val currentKfurlMap = kfurlPair?.second
    val kfurlTableDef = kfurlPair?.first

    // Resolve KFPBRK Pair<TableDefinition, Map3d> from preferences
    val kfpbrkPair: Pair<TableDefinition, Map3d>? = remember(mapList) {
        KfpbrkPreferences.getSelectedMap()
    }
    val kfpbrkMap = kfpbrkPair?.second
    val kfpbrkTableDef = kfpbrkPair?.first

    // BIN file state
    val binFile by BinFilePreferences.file.collectAsState()
    val binLoaded = binFile.exists() && binFile.isFile

    // Write state for KFURL
    var showWriteKfurlConfirmation by remember { mutableStateOf(false) }
    var writeKfurlStatus by remember { mutableStateOf(WriteStatus.Idle) }

    // Write state for KFPBRK
    var showWriteKfpbrkConfirmation by remember { mutableStateOf(false) }
    var writeKfpbrkStatus by remember { mutableStateOf(WriteStatus.Idle) }

    LaunchedEffect(writeKfurlStatus) {
        if (writeKfurlStatus != WriteStatus.Idle) { delay(3000); writeKfurlStatus = WriteStatus.Idle }
    }
    LaunchedEffect(writeKfpbrkStatus) {
        if (writeKfpbrkStatus != WriteStatus.Idle) { delay(3000); writeKfpbrkStatus = WriteStatus.Idle }
    }

    // Write confirmation dialogs
    if (showWriteKfurlConfirmation) {
        AlertDialog(
            onDismissRequest = { showWriteKfurlConfirmation = false },
            title = { Text("Write KFURL") },
            text = { Text("Are you sure you want to write the suggested KFURL to the binary?") },
            confirmButton = {
                TextButton(onClick = {
                    showWriteKfurlConfirmation = false
                    val suggestedMap = alphaNResult?.suggestedKfurlMap
                    if (suggestedMap != null && kfurlTableDef != null) {
                        try {
                            BinWriter.write(BinFilePreferences.file.value, kfurlTableDef, suggestedMap)
                            writeKfurlStatus = WriteStatus.Success
                        } catch (e: Exception) {
                            e.printStackTrace()
                            writeKfurlStatus = WriteStatus.Error
                        }
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showWriteKfurlConfirmation = false }) { Text("No") }
            }
        )
    }

    if (showWriteKfpbrkConfirmation) {
        AlertDialog(
            onDismissRequest = { showWriteKfpbrkConfirmation = false },
            title = { Text("Write KFPBRK") },
            text = { Text("Are you sure you want to write the suggested KFPBRK to the binary?") },
            confirmButton = {
                TextButton(onClick = {
                    showWriteKfpbrkConfirmation = false
                    val suggestedMap = alphaNResult?.suggestedKfpbrkMap
                    if (suggestedMap != null && kfpbrkTableDef != null) {
                        try {
                            BinWriter.write(BinFilePreferences.file.value, kfpbrkTableDef, suggestedMap)
                            writeKfpbrkStatus = WriteStatus.Success
                        } catch (e: Exception) {
                            e.printStackTrace()
                            writeKfpbrkStatus = WriteStatus.Error
                        }
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showWriteKfpbrkConfirmation = false }) { Text("No") }
            }
        )
    }

    // Helper to run analysis with VE solver parameters
    fun runAnalysis(logData: Map<data.contract.Me7LogFileContract.Header, List<Double>>) {
        alphaNResult = AlphaNDiagnostic.analyze(
            logData = logData,
            currentKfurl = currentKfurl,
            displacement = displacement,
            kfpbrkMap = kfpbrkMap,
            currentKfurlMap = currentKfurlMap
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header / explanation card
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Alpha-N Diagnostic",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Compares MAF-measured airflow (mshfm_w) against the throttle model estimate (msdk_w) " +
                        "to assess how well the car will run with the MAF unplugged (alpha-n / speed-density mode). " +
                        "Log both mshfm_w and msdk_w simultaneously across various RPM and load conditions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Also log pvdks_w and pus_w to enable VE model suggestions (KFURL, KFPRG, KFPBRK).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Warning, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "WDKUGDN defines the throttle body choke point — do NOT adjust it to fix alpha-n " +
                            "accuracy. See the Alpha-N Calibration Guide for the correct approach.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Load buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val dialog = FileDialog(null as Frame?, "Select Alpha-N Log File", FileDialog.LOAD)
                    dialog.file = "*.csv"
                    dialog.isVisible = true
                    val dir = dialog.directory
                    val file = dialog.file
                    if (dir != null && file != null) {
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            val logFile = File(dir, file)
                            loadedFileName = logFile.name
                            val logData = AlphaNLogParser.parseLogFile(logFile)
                            runAnalysis(logData)
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text("Load Log File")
            }

            Button(
                onClick = {
                    val dialog = FileDialog(null as Frame?, "Select Alpha-N Log Directory", FileDialog.LOAD)
                    System.setProperty("apple.awt.fileDialogForDirectories", "true")
                    dialog.isVisible = true
                    System.setProperty("apple.awt.fileDialogForDirectories", "false")
                    val dir = dialog.directory
                    val file = dialog.file
                    if (dir != null) {
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            val directory = File(dir, file ?: "")
                            val targetDir = if (directory.isDirectory) directory else File(dir)
                            loadedFileName = targetDir.name + "/"
                            val logData = AlphaNLogParser.parseLogDirectory(targetDir)
                            runAnalysis(logData)
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text("Load Log Directory")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        if (loadedFileName != null && alphaNResult == null && !isLoading) {
            Text(
                text = "Could not analyze: log must contain nmot, mshfm_w, and msdk_w columns.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Results
        alphaNResult?.let { result ->
            // Severity card
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (severityIcon, severityColor) = when (result.severity) {
                            AlphaNDiagnostic.Severity.GOOD ->
                                Icons.Filled.CheckCircle to MaterialTheme.colorScheme.tertiary
                            AlphaNDiagnostic.Severity.WARNING ->
                                Icons.Filled.Warning to MaterialTheme.colorScheme.primary
                            AlphaNDiagnostic.Severity.CRITICAL ->
                                Icons.Filled.Cancel to MaterialTheme.colorScheme.error
                        }
                        Icon(
                            imageVector = severityIcon,
                            contentDescription = result.severity.name,
                            tint = severityColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Alpha-N Readiness: ${result.severity.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = severityColor
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Error type: ${result.errorType.name}  •  Pressure data: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = if (result.hasPressureData) Icons.Filled.CheckCircle
                                                  else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (result.hasPressureData) Color(0xFF4CAF50)
                                           else Color(0xFFF44336),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Samples", result.totalSamples.toString())
                        StatItem("Avg Error", String.format("%.1f%%", result.avgErrorPercent))
                        StatItem("RMS Error", String.format("%.1f%%", result.rmsErrorPercent))
                        StatItem("Max Error", String.format("%.1f%%", result.maxErrorPercent))
                    }

                    if (result.estimatedMultiplicativeCorrection != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Estimated multiplicative correction: ${String.format("%.3f", result.estimatedMultiplicativeCorrection)} " +
                                "(${String.format("%+.1f%%", (result.estimatedMultiplicativeCorrection - 1.0) * 100)})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (result.estimatedAdditiveCorrection != null) {
                        Text(
                            text = "Estimated additive correction: ${String.format("%.1f", result.estimatedAdditiveCorrection)} kg/h",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Per-RPM breakdown table
            if (result.rpmBins.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Per-RPM Breakdown",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            TableHeader("RPM", Modifier.weight(1f))
                            TableHeader("Samples", Modifier.weight(1f))
                            TableHeader("Avg mshfm", Modifier.weight(1f))
                            TableHeader("Avg msdk", Modifier.weight(1f))
                            TableHeader("Error %", Modifier.weight(1f))
                            TableHeader("RMS %", Modifier.weight(1f))
                        }
                        HorizontalDivider()

                        for (bin in result.rpmBins) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                TableCell(bin.rpmCenter.toInt().toString(), Modifier.weight(1f))
                                TableCell(bin.sampleCount.toString(), Modifier.weight(1f))
                                TableCell(String.format("%.0f", bin.avgMshfm), Modifier.weight(1f))
                                TableCell(String.format("%.0f", bin.avgMsdk), Modifier.weight(1f))
                                TableCell(
                                    String.format("%+.1f", bin.avgErrorPercent),
                                    Modifier.weight(1f),
                                    color = when {
                                        kotlin.math.abs(bin.avgErrorPercent) <= 5.0 -> MaterialTheme.colorScheme.tertiary
                                        kotlin.math.abs(bin.avgErrorPercent) <= 15.0 -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                                TableCell(
                                    String.format("%.1f", bin.rmsErrorPercent),
                                    Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ── KFURL Map Table Card ──────────────────────────────────
            if (result.suggestedKfurlMap != null) {
                result.kfurlSuggestion?.let { suggestion ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "KFURL — VE Slope (Suggested Correction)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("Current RMSE", String.format("%.2f%%", suggestion.currentRmse))
                                StatItem("Suggested RMSE", String.format("%.2f%%", suggestion.suggestedRmse))
                                StatItem("Reduction", String.format("%.1f%%", suggestion.errorReductionPercent))
                            }

                            // Current (input) table
                            result.currentKfurlMap?.let { inputMap ->
                                if (inputMap.zAxis.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "KFURL (Current)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                        MapTable(map = inputMap, editable = false)
                                    }
                                }
                            }

                            // Suggested (output) table
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "KFURL (Suggested)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                MapTable(map = result.suggestedKfurlMap, editable = false)
                            }

                            // Write action
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showWriteKfurlConfirmation = true },
                                    enabled = binLoaded && kfurlTableDef != null
                                ) {
                                    Text("Write KFURL to BIN")
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                AnimatedVisibility(visible = writeKfurlStatus != WriteStatus.Idle) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (writeKfurlStatus == WriteStatus.Success) Icons.Default.Check else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (writeKfurlStatus == WriteStatus.Success) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (writeKfurlStatus == WriteStatus.Success) "Written successfully" else "Write failed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (writeKfurlStatus == WriteStatus.Success) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            if (kfurlTableDef == null) {
                                Text(
                                    text = "KFURL not found in BIN/XDF. Ensure the binary contains a KFURL table definition.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── KFPBRK Map Table Card ─────────────────────────────────
            if (result.suggestedKfpbrkMap != null) {
                result.kfpbrkCorrections?.let { corrections ->
                    if (corrections.any { kotlin.math.abs(it.correctionFactor - 1.0) > 0.02 }) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "KFPBRK — Combustion Chamber Correction (Suggested)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Per-RPM correction factors summary
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    TableHeader("RPM", Modifier.weight(1f))
                                    TableHeader("Factor", Modifier.weight(1f))
                                    TableHeader("Samples", Modifier.weight(1f))
                                    TableHeader("Confidence", Modifier.weight(1f))
                                }
                                HorizontalDivider()
                                for (c in corrections) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                        TableCell(c.rpmCenter.toInt().toString(), Modifier.weight(1f))
                                        TableCell(
                                            "×${String.format("%.3f", c.correctionFactor)}",
                                            Modifier.weight(1f),
                                            color = when {
                                                kotlin.math.abs(c.correctionFactor - 1.0) <= 0.02 -> MaterialTheme.colorScheme.tertiary
                                                kotlin.math.abs(c.correctionFactor - 1.0) <= 0.05 -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                        TableCell(c.sampleCount.toString(), Modifier.weight(1f))
                                        TableCell(
                                            c.confidence.name,
                                            Modifier.weight(1f),
                                            color = when (c.confidence) {
                                                AlphaNDiagnostic.Confidence.HIGH -> MaterialTheme.colorScheme.tertiary
                                                AlphaNDiagnostic.Confidence.MEDIUM -> MaterialTheme.colorScheme.primary
                                                AlphaNDiagnostic.Confidence.LOW -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                }

                                // Current (input) table
                                result.currentKfpbrkMap?.let { inputMap ->
                                    if (inputMap.zAxis.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "KFPBRK (Current)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                            MapTable(map = inputMap, editable = false)
                                        }
                                    }
                                }

                                // Suggested (output) table
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "KFPBRK (Suggested)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                    MapTable(map = result.suggestedKfpbrkMap, editable = false)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.Warning, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp).padding(top = 1.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "KFPBRK suggestions are preliminary — verify with Optimizer WOT logs for higher confidence.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                // Write action
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { showWriteKfpbrkConfirmation = true },
                                        enabled = binLoaded && kfpbrkTableDef != null
                                    ) {
                                        Text("Write KFPBRK to BIN")
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    AnimatedVisibility(visible = writeKfpbrkStatus != WriteStatus.Idle) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (writeKfpbrkStatus == WriteStatus.Success) Icons.Default.Check else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (writeKfpbrkStatus == WriteStatus.Success) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (writeKfpbrkStatus == WriteStatus.Success) "Written successfully" else "Write failed",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (writeKfpbrkStatus == WriteStatus.Success) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                if (kfpbrkTableDef == null) {
                                    Text(
                                        text = "KFPBRK not configured. Select the KFPBRK map in the Configuration screen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── KFPRG Suggestion Card (text-only — scalar/per-RPM) ────
            result.kfprgSuggestion?.let { kfprg ->
                if (kfprg.errorReductionPercent > 2.0) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "KFPRG — Residual Gas Offset (Suggested)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("Current RMSE", String.format("%.2f%%", kfprg.currentRmse))
                                StatItem("Suggested", String.format("%.0f hPa", kfprg.optimalKfprg))
                                StatItem("New RMSE", String.format("%.2f%%", kfprg.suggestedRmse))
                                StatItem("Reduction", String.format("%.1f%%", kfprg.errorReductionPercent))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Info, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp).padding(top = 1.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Part-throttle data improves KFPRG accuracy — residual gas dominates at low load.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            kfprg.perRpmValues?.let { perRpm ->
                                if (perRpm.size >= 2) {
                                    val range = perRpm.maxOf { it.second } - perRpm.minOf { it.second }
                                    if (range > 5.0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Per-RPM KFPRG (varies by ${String.format("%.0f", range)} hPa):",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            TableHeader("RPM", Modifier.weight(1f))
                                            TableHeader("KFPRG (hPa)", Modifier.weight(1f))
                                        }
                                        HorizontalDivider()
                                        for ((rpm, value) in perRpm) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                                TableCell(rpm.toInt().toString(), Modifier.weight(1f))
                                                TableCell(String.format("%.0f", value), Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recommendations card
            if (result.recommendations.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Recommendations",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        for (rec in result.recommendations) {
                            if (rec.isNotEmpty()) {
                                Text(
                                    text = rec,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun TableCell(
    text: String,
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier.padding(vertical = 2.dp)
    )
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
