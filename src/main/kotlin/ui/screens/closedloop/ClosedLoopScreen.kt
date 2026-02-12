package ui.screens.closedloop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.contract.Me7LogFileContract
import data.parser.bin.BinParser
import data.parser.me7log.ClosedLoopLogParser
import data.parser.xdf.TableDefinition
import data.preferences.bin.BinFilePreferences
import data.preferences.closedloopfueling.ClosedLoopFuelingLogPreferences
import data.preferences.mlhfm.MlhfmPreferences
import data.writer.BinWriter
import domain.derivative.Derivative
import domain.math.map.Map3d
import domain.model.closedloopfueling.ClosedLoopFuelingCorrection
import domain.model.closedloopfueling.ClosedLoopFuelingCorrectionManager
import domain.model.mlhfm.MlhfmFitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.math3.stat.descriptive.moment.Mean
import ui.components.*
import ui.theme.ChartBlue
import ui.theme.ChartGreen
import ui.theme.ChartMagenta
import ui.theme.ChartRed
import ui.theme.Primary
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import java.text.DecimalFormat

private fun findMlhfmMap(mapList: List<Pair<TableDefinition, Map3d>>): Pair<TableDefinition, Map3d>? {
    val selected = MlhfmPreferences.getSelectedMap()
    return if (selected != null) {
        mapList.find { it.first.tableName == selected.first.tableName }
    } else null
}

@Composable
fun ClosedLoopScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val mlhfmPair = remember(mapList) { findMlhfmMap(mapList) }

    var me7LogMap by remember { mutableStateOf<Map<Me7LogFileContract.Header, List<Double>>?>(null) }
    var correction by remember { mutableStateOf<ClosedLoopFuelingCorrection?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ClosedLoopLogParser.logs.collect { logs ->
            me7LogMap = logs
            val pair = findMlhfmMap(BinParser.mapList.value)
            if (pair != null && logs.isNotEmpty()) {
                withContext(Dispatchers.Default) {
                    val manager = ClosedLoopFuelingCorrectionManager(
                        ClosedLoopFuelingLogPreferences.minThrottleAngle,
                        ClosedLoopFuelingLogPreferences.minRpm,
                        ClosedLoopFuelingLogPreferences.maxDerivative
                    )
                    manager.correct(logs, pair.second)
                    correction = manager.closedLoopMlhfmCorrection
                }
            }
        }
    }

    val hasLogs = me7LogMap != null && me7LogMap!!.isNotEmpty()
    val hasCorrection = correction != null
    val hasMap = mlhfmPair != null

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("ME7 Logs", "Correction", "MLHFM", "Help")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                val enabled = when (index) {
                    0 -> hasMap
                    1 -> hasMap && hasLogs && hasCorrection
                    2 -> hasMap && hasCorrection
                    3 -> true
                    else -> true
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { if (enabled) selectedTab = index },
                    enabled = enabled,
                    text = {
                        Text(
                            title,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> ClosedLoopLogsTab(
                me7LogMap = me7LogMap,
                mlhfm = mlhfmPair?.second,
                onLogsLoaded = { selectedTab = if (correction != null) 1 else 0 }
            )
            1 -> ClosedLoopCorrectionTab(
                correction = correction,
                onCorrectionUpdated = { correction = it }
            )
            2 -> ClosedLoopMlhfmTab(correction = correction)
            3 -> ClosedLoopHelpTab()
        }
    }
}

// -- ME7 Logs Tab --

@Composable
private fun ClosedLoopLogsTab(
    me7LogMap: Map<Me7LogFileContract.Header, List<Double>>?,
    mlhfm: Map3d?,
    onLogsLoaded: () -> Unit
) {
    var logDirName by remember { mutableStateOf("No Directory Selected") }
    var showFilterDialog by remember { mutableStateOf(false) }

    val chartSeries = remember(me7LogMap, mlhfm) {
        if (me7LogMap != null && mlhfm != null) {
            val dtMap = Derivative.getMlfhm(me7LogMap, mlhfm)
            val maxFilterVoltage = ClosedLoopFuelingLogPreferences.maxDerivative
            val excludedPoints = mutableListOf<Pair<Double, Double>>()
            val includedPoints = mutableListOf<Pair<Double, Double>>()

            for (voltage in mlhfm.yAxis) {
                val values = dtMap[voltage] ?: continue
                for (value in values) {
                    if (value > maxFilterVoltage) {
                        excludedPoints.add(voltage to value)
                    } else {
                        includedPoints.add(voltage to value)
                    }
                }
            }
            listOf(
                ChartSeries("Excluded Sample", excludedPoints, ChartRed, showLine = false, showPoints = true),
                ChartSeries("Included Sample", includedPoints, ChartGreen, showLine = false, showPoints = true)
            )
        } else emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ScatterPlot(
                series = chartSeries,
                title = "Derivative",
                xAxisLabel = "MAF Voltage",
                yAxisLabel = "dMAFv/dt"
            )
        }

        // Action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { showFilterDialog = true }) {
                Text("Configure Filter")
            }

            Spacer(Modifier.width(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    val dialog = FileDialog(Frame(), "Select Log Directory", FileDialog.LOAD)
                    System.setProperty("apple.awt.fileDialogForDirectories", "true")
                    val lastDir = ClosedLoopFuelingLogPreferences.lastDirectory
                    if (lastDir.isNotEmpty()) dialog.directory = lastDir
                    dialog.isVisible = true
                    System.setProperty("apple.awt.fileDialogForDirectories", "false")
                    val dir = dialog.directory
                    val file = dialog.file
                    if (dir != null && file != null) {
                        val selectedDir = File(dir, file)
                        ClosedLoopFuelingLogPreferences.lastDirectory = selectedDir.parent ?: dir
                        logDirName = selectedDir.name
                        ClosedLoopLogParser.loadDirectory(selectedDir)
                        onLogsLoaded()
                    }
                }) {
                    Text("Load Logs")
                }
                Text(
                    text = logDirName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (showFilterDialog) {
        ClosedLoopFilterDialog(
            onDismiss = { showFilterDialog = false },
            onApply = { minTps, minRpm, maxDt ->
                ClosedLoopFuelingLogPreferences.minThrottleAngle = minTps
                ClosedLoopFuelingLogPreferences.minRpm = minRpm
                ClosedLoopFuelingLogPreferences.maxDerivative = maxDt
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun ClosedLoopFilterDialog(
    onDismiss: () -> Unit,
    onApply: (Double, Double, Double) -> Unit
) {
    var minTps by remember { mutableStateOf(ClosedLoopFuelingLogPreferences.minThrottleAngle.toString()) }
    var minRpm by remember { mutableStateOf(ClosedLoopFuelingLogPreferences.minRpm.toString()) }
    var maxDt by remember { mutableStateOf(ClosedLoopFuelingLogPreferences.maxDerivative.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Closed Loop Filter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minTps,
                    onValueChange = { minTps = it },
                    label = { Text("Minimum Throttle Angle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minRpm,
                    onValueChange = { minRpm = it },
                    label = { Text("Minimum RPM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxDt,
                    onValueChange = { maxDt = it },
                    label = { Text("Max dMAFv/dt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val tps = minTps.toDoubleOrNull() ?: return@TextButton
                val rpm = minRpm.toDoubleOrNull() ?: return@TextButton
                val dt = maxDt.toDoubleOrNull() ?: return@TextButton
                onApply(tps, rpm, dt)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -- Correction Tab --

@Composable
private fun ClosedLoopCorrectionTab(
    correction: ClosedLoopFuelingCorrection?,
    onCorrectionUpdated: (ClosedLoopFuelingCorrection) -> Unit
) {
    if (correction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No correction data available.")
        }
        return
    }

    var polynomialDegree by remember { mutableStateOf(6) }
    var correctionSubTab by remember { mutableStateOf(0) }
    val correctionSubTabs = listOf("AFR Correction %", "dMAFv/dt", "MLHFM")

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tab row
        PrimaryTabRow(selectedTabIndex = correctionSubTab) {
            correctionSubTabs.forEachIndexed { index, title ->
                Tab(
                    selected = correctionSubTab == index,
                    onClick = { correctionSubTab = index },
                    text = { Text(title, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (correctionSubTab) {
                0 -> AfrCorrectionChart(correction)
                1 -> DerivativeChart(correction)
                2 -> CorrectionMlhfmChart(correction)
            }
        }

        // Polynomial fit + write controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Polynomial Degree: ", style = MaterialTheme.typography.bodyMedium)
            var degreeText by remember { mutableStateOf(polynomialDegree.toString()) }
            OutlinedTextField(
                value = degreeText,
                onValueChange = {
                    degreeText = it
                    it.toIntOrNull()?.let { v -> polynomialDegree = v }
                },
                singleLine = true,
                modifier = Modifier.width(60.dp).height(48.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val fitMlhfm = MlhfmFitter.fitMlhfm(correction.correctedMlhfm, polynomialDegree)
                onCorrectionUpdated(
                    correction.copy(fitMlhfm = fitMlhfm)
                )
            }) {
                Text("Fit MLHFM")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                val pair = MlhfmPreferences.getSelectedMap()
                if (pair != null) {
                    val binFile = BinFilePreferences.getStoredFile()
                    if (binFile.exists()) {
                        BinWriter.write(binFile, pair.first, correction.fitMlhfm)
                    }
                }
            }) {
                Text("Write MLHFM")
            }
        }
    }
}

@Composable
private fun AfrCorrectionChart(correction: ClosedLoopFuelingCorrection) {
    val mean = remember { Mean() }

    val rawPoints = remember(correction) {
        correction.correctionsAfrMap.flatMap { (voltage, values) ->
            values.map { voltage to it }
        }
    }
    val meanLine = remember(correction) {
        correction.meanAfrMap.map { (voltage, value) -> voltage to value }
    }
    val modeLine = remember(correction) {
        correction.modeAfrMap.map { (voltage, modes) ->
            voltage to mean.evaluate(modes, 0, modes.size)
        }
    }
    val correctedLine = remember(correction) {
        correction.correctedAfrMap.map { (voltage, value) -> voltage to value }
    }

    val series = listOf(
        ChartSeries("AFR Corrections %", rawPoints, Primary, showLine = false, showPoints = true),
        ChartSeries("Final AFR Correction %", correctedLine, ChartRed, showLine = true, showPoints = false),
        ChartSeries("Mean AFR Correction %", meanLine, ChartGreen, showLine = true, showPoints = false),
        ChartSeries("Mode AFR Correction %", modeLine, ChartMagenta, showLine = true, showPoints = false)
    )

    ScatterPlot(
        series = series,
        title = "AFR Correction %",
        xAxisLabel = "Voltage",
        yAxisLabel = "Correction %"
    )
}

@Composable
private fun DerivativeChart(correction: ClosedLoopFuelingCorrection) {
    val points = remember(correction) {
        correction.filteredVoltageDt.flatMap { (voltage, values) ->
            values.map { voltage to it }
        }
    }
    val series = listOf(
        ChartSeries("dMAFv/dt", points, Primary, showLine = false, showPoints = true)
    )

    ScatterPlot(
        series = series,
        title = "Derivative",
        xAxisLabel = "Voltage",
        yAxisLabel = "dMAFv/dt"
    )
}

@Composable
private fun CorrectionMlhfmChart(correction: ClosedLoopFuelingCorrection) {
    val inputPoints = remember(correction) {
        correction.inputMlhfm.yAxis.indices.map { i ->
            correction.inputMlhfm.yAxis[i] to correction.inputMlhfm.zAxis[i][0]
        }
    }
    val correctedPoints = remember(correction) {
        val mlhfm = correction.fitMlhfm
        if (mlhfm.yAxis.isNotEmpty()) {
            mlhfm.yAxis.indices.map { i ->
                mlhfm.yAxis[i] to mlhfm.zAxis[i][0]
            }
        } else {
            correction.correctedMlhfm.yAxis.indices.map { i ->
                correction.correctedMlhfm.yAxis[i] to correction.correctedMlhfm.zAxis[i][0]
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            LineChart(
                series = listOf(
                    ChartSeries("MLHFM", inputPoints, Primary, showLine = true, showPoints = false),
                    ChartSeries("Corrected MLHFM", correctedPoints, ChartRed, showLine = true, showPoints = false)
                ),
                title = "Corrected MLHFM",
                xAxisLabel = "Voltage",
                yAxisLabel = "kg/hr"
            )
        }

        // MapTable for corrected values
        val displayMlhfm = correction.fitMlhfm.let {
            if (it.yAxis.isNotEmpty()) it else correction.correctedMlhfm
        }
        if (displayMlhfm.zAxis.isNotEmpty()) {
            Box(modifier = Modifier.width(140.dp).fillMaxHeight()) {
                MapTable(map = displayMlhfm, editable = false)
            }
        }
    }
}

// -- MLHFM Tab --

@Composable
private fun ClosedLoopMlhfmTab(correction: ClosedLoopFuelingCorrection?) {
    if (correction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No correction data available.")
        }
        return
    }

    val inputPoints = remember(correction) {
        correction.inputMlhfm.yAxis.indices.map { i ->
            correction.inputMlhfm.yAxis[i] to correction.inputMlhfm.zAxis[i][0]
        }
    }
    val correctedPoints = remember(correction) {
        correction.correctedMlhfm.yAxis.indices.map { i ->
            correction.correctedMlhfm.yAxis[i] to correction.correctedMlhfm.zAxis[i][0]
        }
    }
    val fitPoints = remember(correction) {
        if (correction.fitMlhfm.yAxis.isNotEmpty()) {
            correction.fitMlhfm.yAxis.indices.map { i ->
                correction.fitMlhfm.yAxis[i] to correction.fitMlhfm.zAxis[i][0]
            }
        } else emptyList()
    }

    val allSeries = remember(correction) {
        buildList {
            add(ChartSeries("Original MLHFM", inputPoints, Primary, showLine = true, showPoints = false))
            add(ChartSeries("Corrected MLHFM", correctedPoints, ChartRed, showLine = true, showPoints = false))
            if (fitPoints.isNotEmpty()) {
                add(ChartSeries("Fitted MLHFM", fitPoints, ChartBlue, showLine = true, showPoints = false))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                LineChart(
                    series = allSeries,
                    title = "MLHFM",
                    xAxisLabel = "Voltage",
                    yAxisLabel = "kg/hr"
                )
            }

            val displayMlhfm = correction.fitMlhfm.let {
                if (it.yAxis.isNotEmpty()) it else correction.correctedMlhfm
            }
            if (displayMlhfm.zAxis.isNotEmpty()) {
                Box(modifier = Modifier.width(140.dp).fillMaxHeight()) {
                    MapTable(map = displayMlhfm, editable = false)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                val pair = MlhfmPreferences.getSelectedMap()
                if (pair != null) {
                    val binFile = BinFilePreferences.getStoredFile()
                    if (binFile.exists()) {
                        val mlhfmToWrite = correction.fitMlhfm.let {
                            if (it.yAxis.isNotEmpty()) it else correction.correctedMlhfm
                        }
                        BinWriter.write(binFile, pair.first, mlhfmToWrite)
                    }
                }
            }) {
                Text("Write MLHFM")
            }
        }
    }
}

// -- Help Tab --

@Composable
private fun ClosedLoopHelpTab() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Closed Loop MLHFM Correction",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = """
                This tool corrects the MAF sensor linearization table (MLHFM) using closed loop fuel trim data.

                Steps:
                1. Ensure MLHFM is configured in the Configuration tab.
                2. Load a directory containing ME7 log files (.csv) with closed loop driving data.
                3. The ME7 Logs tab will show a derivative scatter plot. Use the filter to exclude noisy samples.
                4. The Correction tab shows the AFR correction percentage, derivative, and corrected MLHFM.
                5. Use the polynomial fit to smooth the correction curve.
                6. Write the corrected MLHFM back to the binary.

                Required log headers: MAF Voltage, STFT, LTFT, Lambda Control, Throttle Angle, RPM, Timestamp.

                The correction uses Short Term Fuel Trim (STFT) and Long Term Fuel Trim (LTFT) to determine
                how much the ECU is adjusting fueling to maintain stoichiometric AFR. These trims indicate
                the MAF sensor error at each voltage point.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = {
            try {
                Desktop.getDesktop().browse(URI("https://github.com/KalebKE/ME7Tuner#closed-loop-mlhfm"))
            } catch (_: Exception) { }
        }) {
            Text("Closed Loop MLHFM User Guide")
        }
    }
}
