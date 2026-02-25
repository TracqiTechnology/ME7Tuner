package ui.screens.optimizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.parser.bin.BinParser
import data.parser.me7log.Me7LogParser
import data.parser.xdf.TableDefinition
import data.preferences.bin.BinFilePreferences
import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import data.preferences.kfpbrk.KfpbrkPreferences
import data.preferences.kfpbrknw.KfpbrknwPreferences
import data.preferences.optimizer.OptimizerPreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import domain.model.optimizer.OptimizerCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.components.ChartSeries
import ui.components.LineChart
import ui.components.MapTable
import ui.theme.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

private fun findMap(
    mapList: List<Pair<TableDefinition, Map3d>>,
    pref: data.preferences.MapPreference
): Pair<TableDefinition, Map3d>? {
    val selected = pref.getSelectedMap()
    return if (selected != null) {
        mapList.find { it.first.tableName == selected.first.tableName }
    } else null
}

@Composable
fun OptimizerScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Resolve current maps from preferences
    val kfldrlPair = remember(mapList) { findMap(mapList, KfldrlPreferences) }
    val kfldimxPair = remember(mapList) { findMap(mapList, KfldimxPreferences) }
    val kfpbrkPair = remember(mapList) { findMap(mapList, KfpbrkPreferences) }
    val kfpbrknwPair = remember(mapList) { findMap(mapList, KfpbrknwPreferences) }

    // Preference-backed input state
    var toleranceMbar by remember { mutableStateOf(OptimizerPreferences.mapToleranceMbar.toString()) }
    var ldrxnTarget by remember { mutableStateOf(OptimizerPreferences.ldrxnTarget.toString()) }
    var kfldimxOverhead by remember { mutableStateOf(OptimizerPreferences.kfldimxOverheadPercent.toString()) }
    var minThrottleAngle by remember { mutableStateOf(OptimizerPreferences.minThrottleAngle.toString()) }

    // Result state
    var result by remember { mutableStateOf<OptimizerCalculator.OptimizerResult?>(null) }
    var logFileName by remember { mutableStateOf("No Log Selected") }
    var showProgress by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    val tabTitles = listOf("Boost Control", "VE Model", "Charts", "Warnings")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        // ── Parameters row ────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Optimizer Parameters", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ParameterField(
                        value = toleranceMbar,
                        onValueChange = {
                            toleranceMbar = it
                            it.toDoubleOrNull()?.let { v -> OptimizerPreferences.mapToleranceMbar = v }
                        },
                        label = "MAP Tolerance (mbar)",
                        tooltip = "Maximum allowed deviation between requested boost pressure (pssol) and actual boost pressure (pvdks_w). " +
                            "Log entries within this tolerance are considered \"on-target\" and used for KFLDRL/KFPBRK suggestions. " +
                            "Typical values: 20\u201350 mbar. Lower = stricter matching, fewer data points.",
                        modifier = Modifier.weight(1f)
                    )
                    ParameterField(
                        value = ldrxnTarget,
                        onValueChange = {
                            ldrxnTarget = it
                            it.toDoubleOrNull()?.let { v -> OptimizerPreferences.ldrxnTarget = v }
                        },
                        label = "LDRXN Target (%)",
                        tooltip = "The maximum specified engine load (LDRXN) your tune is targeting. " +
                            "Used by the Intervention Watchdog to detect if rlsol is being capped below this value by torque limiters (KFMIOP/KFMIZUFIL). " +
                            "Set this to match your KFMIRL maximum load axis value.",
                        modifier = Modifier.weight(1f)
                    )
                    ParameterField(
                        value = kfldimxOverhead,
                        onValueChange = {
                            kfldimxOverhead = it
                            it.toDoubleOrNull()?.let { v -> OptimizerPreferences.kfldimxOverheadPercent = v }
                        },
                        label = "KFLDIMX Overhead (%)",
                        tooltip = "Percentage headroom added on top of the suggested KFLDRL values to derive KFLDIMX (the PID I-regulator limit). " +
                            "This gives the PID controller room to make corrections without winding up. " +
                            "Typical values: 5\u201310%. Higher = more PID authority, but risk of overshoot.",
                        modifier = Modifier.weight(1f)
                    )
                    ParameterField(
                        value = minThrottleAngle,
                        onValueChange = {
                            minThrottleAngle = it
                            it.toDoubleOrNull()?.let { v -> OptimizerPreferences.minThrottleAngle = v }
                        },
                        label = "Min Throttle Angle",
                        tooltip = "Minimum throttle plate angle (wdkba) to qualify a log entry as Wide-Open Throttle (WOT). " +
                            "Only log rows above this threshold are used for analysis. " +
                            "Typical value: 80\u00B0. Lower values include partial-throttle data which may skew results.",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Load buttons ──────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Button(onClick = {
                val dialog = FileDialog(Frame(), "Select ME7 Log File", FileDialog.LOAD)
                val lastDir = OptimizerPreferences.lastDirectory
                if (lastDir.isNotEmpty()) dialog.directory = lastDir
                dialog.isVisible = true
                val dir = dialog.directory
                val file = dialog.file
                if (dir != null && file != null) {
                    val selectedFile = File(dir, file)
                    OptimizerPreferences.lastDirectory = dir
                    logFileName = selectedFile.name
                    showProgress = true

                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val parser = Me7LogParser()
                            val values = parser.parseLogFile(Me7LogParser.LogType.OPTIMIZER, selectedFile)

                            val analysisResult = OptimizerCalculator.analyze(
                                values = values,
                                kfldrlMap = kfldrlPair?.second,
                                kfldimxMap = kfldimxPair?.second,
                                kfpbrkMap = kfpbrkPair?.second,
                                ldrxnTarget = ldrxnTarget.toDoubleOrNull() ?: 191.0,
                                toleranceMbar = toleranceMbar.toDoubleOrNull() ?: 30.0,
                                minThrottleAngle = minThrottleAngle.toDoubleOrNull() ?: 80.0,
                                kfldimxOverheadPercent = kfldimxOverhead.toDoubleOrNull() ?: 8.0
                            )

                            withContext(Dispatchers.Main) {
                                result = analysisResult
                                showProgress = false
                            }
                        }
                    }
                }
            }) {
                Text("Load ME7 Log File")
            }

            Button(onClick = {
                val dialog = FileDialog(Frame(), "Select Log Directory", FileDialog.LOAD)
                System.setProperty("apple.awt.fileDialogForDirectories", "true")
                val lastDir = OptimizerPreferences.lastDirectory
                if (lastDir.isNotEmpty()) dialog.directory = lastDir
                dialog.isVisible = true
                System.setProperty("apple.awt.fileDialogForDirectories", "false")
                val dir = dialog.directory
                val file = dialog.file
                if (dir != null && file != null) {
                    val selectedDir = File(dir, file)
                    OptimizerPreferences.lastDirectory = selectedDir.parent ?: dir
                    logFileName = selectedDir.path
                    showProgress = true

                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val parser = Me7LogParser()
                            val values = parser.parseLogDirectory(
                                Me7LogParser.LogType.OPTIMIZER,
                                selectedDir
                            ) { _, _ -> }

                            val analysisResult = OptimizerCalculator.analyze(
                                values = values,
                                kfldrlMap = kfldrlPair?.second,
                                kfldimxMap = kfldimxPair?.second,
                                kfpbrkMap = kfpbrkPair?.second,
                                ldrxnTarget = ldrxnTarget.toDoubleOrNull() ?: 191.0,
                                toleranceMbar = toleranceMbar.toDoubleOrNull() ?: 30.0,
                                minThrottleAngle = minThrottleAngle.toDoubleOrNull() ?: 80.0,
                                kfldimxOverheadPercent = kfldimxOverhead.toDoubleOrNull() ?: 8.0
                            )

                            withContext(Dispatchers.Main) {
                                result = analysisResult
                                showProgress = false
                            }
                        }
                    }
                }
            }) {
                Text("Load ME7 Log Directory")
            }

            Text(logFileName, style = MaterialTheme.typography.bodySmall)

            if (showProgress) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        // ── Status summary ────────────────────────────────────────────
        result?.let { r ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Analysis: ${r.wotEntries.size} WOT data points",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (r.wotEntries.isEmpty()) {
                        Text(
                            "No WOT data found. Ensure your log contains the required headers " +
                                "(pssol_w, rlsol_w, rl_w, pvdks_w, ldtvm, wdkba, nmot, pus_w) " +
                                "and that throttle angle exceeds the minimum threshold.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // ── Tabbed results ────────────────────────────────────────────
        if (result != null && result!!.wotEntries.isNotEmpty()) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                0 -> BoostControlTab(result!!, kfldrlPair, kfldimxPair)
                1 -> VeModelTab(result!!, kfpbrkPair, kfpbrknwPair)
                2 -> ChartsTab(result!!)
                3 -> WarningsTab(result!!)
            }
        }
    }
}

// ── Tab: Boost Control ────────────────────────────────────────────────

@Composable
private fun BoostControlTab(
    result: OptimizerCalculator.OptimizerResult,
    kfldrlPair: Pair<TableDefinition, Map3d>?,
    kfldimxPair: Pair<TableDefinition, Map3d>?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current KFLDRL
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Current KFLDRL", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                if (kfldrlPair != null) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(map = kfldrlPair.second, editable = false)
                    }
                } else {
                    Text("KFLDRL not configured", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Suggested KFLDRL
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Suggested KFLDRL", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                if (result.suggestedKfldrl != null) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(map = result.suggestedKfldrl, editable = false)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val kfldrlDef = KfldrlPreferences.getSelectedMap()
                        if (kfldrlDef != null) {
                            val binFile = BinFilePreferences.getStoredFile()
                            if (binFile.exists()) {
                                BinWriter.write(binFile, kfldrlDef.first, result.suggestedKfldrl)
                            }
                        }
                    }) {
                        Text("Write KFLDRL")
                    }
                } else {
                    Text("No suggestion (KFLDRL not configured)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current KFLDIMX
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Current KFLDIMX", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                if (kfldimxPair != null) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(map = kfldimxPair.second, editable = false)
                    }
                } else {
                    Text("KFLDIMX not configured", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Suggested KFLDIMX
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Suggested KFLDIMX", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                if (result.suggestedKfldimx != null) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(map = result.suggestedKfldimx, editable = false)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val kfldimxDef = KfldimxPreferences.getSelectedMap()
                        if (kfldimxDef != null) {
                            val binFile = BinFilePreferences.getStoredFile()
                            if (binFile.exists()) {
                                BinWriter.write(binFile, kfldimxDef.first, result.suggestedKfldimx)
                            }
                        }
                    }) {
                        Text("Write KFLDIMX")
                    }
                } else {
                    Text("No suggestion (KFLDIMX not configured)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ── Tab: VE Model ─────────────────────────────────────────────────────

@Composable
private fun VeModelTab(
    result: OptimizerCalculator.OptimizerResult,
    kfpbrkPair: Pair<TableDefinition, Map3d>?,
    kfpbrknwPair: Pair<TableDefinition, Map3d>?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "VE Model Corrections (KFPBRK)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "These corrections assume boost is on-target (Phase 1 complete). " +
                "The suggested KFPBRK values are the original map cells multiplied by " +
                "the average (requestedLoad / actualLoad) ratio at each RPM breakpoint.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current KFPBRK
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Current KFPBRK", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                if (kfpbrkPair != null) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(map = kfpbrkPair.second, editable = false)
                    }
                } else {
                    Text("KFPBRK not configured in Configuration tab", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Suggested KFPBRK
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Suggested KFPBRK", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                if (result.kfpbrkMultipliers != null) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(map = result.kfpbrkMultipliers, editable = false)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val kfpbrkDef = KfpbrkPreferences.getSelectedMap()
                        if (kfpbrkDef != null) {
                            val binFile = BinFilePreferences.getStoredFile()
                            if (binFile.exists()) {
                                BinWriter.write(binFile, kfpbrkDef.first, result.kfpbrkMultipliers)
                            }
                        }
                    }) {
                        Text("Write KFPBRK")
                    }
                } else {
                    Text(
                        "No VE corrections available. Either KFPBRK is not configured " +
                            "or there is insufficient data where boost was on-target.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // KFPBRKNW section (informational)
        if (kfpbrknwPair != null) {
            Text(
                "KFPBRKNW (Variable Cam Timing Active)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "KFPBRKNW corrections require cam state (nw_w) in the log. " +
                    "Currently the optimizer applies corrections to KFPBRK only. " +
                    "If your vehicle has active variable cam timing, the same ratio " +
                    "approach can be applied manually to KFPBRKNW.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                MapTable(map = kfpbrknwPair.second, editable = false)
            }
        }
    }
}

// ── Tab: Charts ───────────────────────────────────────────────────────

@Composable
private fun ChartsTab(result: OptimizerCalculator.OptimizerResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Pressure chart: pssol vs pvdks_w over RPM
        Text("Pressure: Requested (pssol) vs Actual (pvdks_w)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        val requestedPressurePoints = result.wotEntries.map { Pair(it.rpm, it.requestedMap) }
        val actualPressurePoints = result.wotEntries.map { Pair(it.rpm, it.actualMap) }

        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            LineChart(
                series = listOf(
                    ChartSeries(
                        name = "pssol (Requested)",
                        points = requestedPressurePoints,
                        color = Primary,
                        showPoints = true,
                        showLine = false
                    ),
                    ChartSeries(
                        name = "pvdks_w (Actual)",
                        points = actualPressurePoints,
                        color = ChartRed,
                        showPoints = true,
                        showLine = false
                    )
                ),
                title = "Boost Pressure vs RPM",
                xAxisLabel = "RPM",
                yAxisLabel = "Pressure (mbar)"
            )
        }

        Spacer(Modifier.height(8.dp))

        // Pressure error chart
        Text("Pressure Error (pssol − pvdks_w)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            LineChart(
                series = listOf(
                    ChartSeries(
                        name = "Pressure Error (mbar)",
                        points = result.pressureErrors,
                        color = ChartOrange,
                        showPoints = true,
                        showLine = false
                    )
                ),
                title = "Pressure Error vs RPM",
                xAxisLabel = "RPM",
                yAxisLabel = "Error (mbar)"
            )
        }

        Spacer(Modifier.height(16.dp))

        // Load chart: rlsol vs rl over RPM
        Text("Load: Requested (rlsol) vs Actual (rl_w)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        val requestedLoadPoints = result.wotEntries.map { Pair(it.rpm, it.requestedLoad) }
        val actualLoadPoints = result.wotEntries.map { Pair(it.rpm, it.actualLoad) }

        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            LineChart(
                series = listOf(
                    ChartSeries(
                        name = "rlsol (Requested)",
                        points = requestedLoadPoints,
                        color = ChartBlue,
                        showPoints = true,
                        showLine = false
                    ),
                    ChartSeries(
                        name = "rl_w (Actual)",
                        points = actualLoadPoints,
                        color = ChartGreen,
                        showPoints = true,
                        showLine = false
                    )
                ),
                title = "Engine Load vs RPM",
                xAxisLabel = "RPM",
                yAxisLabel = "Load (%)"
            )
        }

        Spacer(Modifier.height(8.dp))

        // Load ratio chart
        if (result.loadErrors.isNotEmpty()) {
            Text("Load Ratio (rlsol / rl_w)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                LineChart(
                    series = listOf(
                        ChartSeries(
                            name = "Load Ratio (1.0 = perfect)",
                            points = result.loadErrors,
                            color = ChartMagenta,
                            showPoints = true,
                            showLine = false
                        )
                    ),
                    title = "Load Ratio vs RPM",
                    xAxisLabel = "RPM",
                    yAxisLabel = "Ratio"
                )
            }
        }
    }
}

// ── Tab: Warnings ─────────────────────────────────────────────────────

@Composable
private fun WarningsTab(result: OptimizerCalculator.OptimizerResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (result.warnings.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✓ No warnings", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "No torque interventions or significant boost shortfalls were detected in the WOT data.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            result.warnings.forEachIndexed { index, warning ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Warning ${index + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            warning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Summary statistics
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Summary Statistics", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                val entries = result.wotEntries
                if (entries.isNotEmpty()) {
                    val avgPressureError = entries.map { it.requestedMap - it.actualMap }.average()
                    val maxPressureError = entries.maxOf { kotlin.math.abs(it.requestedMap - it.actualMap) }
                    val avgLoadRatio = entries.filter { it.actualLoad > 0 }
                        .map { it.requestedLoad / it.actualLoad }.average()
                    val rpmRange = "${entries.minOf { it.rpm }.toInt()} – ${entries.maxOf { it.rpm }.toInt()}"

                    Text("WOT Data Points: ${entries.size}")
                    Text("RPM Range: $rpmRange")
                    Text("Avg Pressure Error: ${String.format("%.1f", avgPressureError)} mbar")
                    Text("Max |Pressure Error|: ${String.format("%.1f", maxPressureError)} mbar")
                    if (avgLoadRatio.isFinite()) {
                        Text("Avg Load Ratio (rlsol/rl): ${String.format("%.4f", avgLoadRatio)}")
                    }
                    Text("Avg WGDC: ${String.format("%.1f", entries.map { it.wgdc }.average())}%")
                }
            }
        }
    }
}

// ── Reusable parameter field with info tooltip ────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParameterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tooltip: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        trailingIcon = {
            @Suppress("DEPRECATION")
            TooltipBox(
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                tooltip = {
                    RichTooltip(
                        title = { Text(label) }
                    ) {
                        Text(tooltip, style = MaterialTheme.typography.bodySmall)
                    }
                },
                state = rememberTooltipState(isPersistent = true)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "$label info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}
