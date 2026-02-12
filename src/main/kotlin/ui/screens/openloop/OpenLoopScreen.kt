package ui.screens.openloop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.contract.AfrLogFileContract
import data.contract.Me7LogFileContract
import data.parser.afrlog.AfrLogParser
import data.parser.bin.BinParser
import data.parser.me7log.OpenLoopLogParser
import data.parser.xdf.TableDefinition
import data.preferences.bin.BinFilePreferences
import data.preferences.mlhfm.MlhfmPreferences
import data.preferences.openloopfueling.OpenLoopFuelingLogFilterPreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import domain.model.airflow.AirflowEstimation
import domain.model.airflow.AirflowEstimationManager
import domain.model.mlhfm.MlhfmFitter
import domain.model.openloopfueling.correction.OpenLoopMlhfmCorrection
import domain.model.openloopfueling.correction.OpenLoopMlhfmCorrectionManager
import domain.model.openloopfueling.util.AfrLogUtil
import domain.model.openloopfueling.util.Me7LogUtil
import kotlinx.coroutines.Dispatchers
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

private fun findMlhfmMap(mapList: List<Pair<TableDefinition, Map3d>>): Pair<TableDefinition, Map3d>? {
    val selected = MlhfmPreferences.getSelectedMap()
    return if (selected != null) {
        mapList.find { it.first.tableName == selected.first.tableName }
    } else null
}

@Composable
fun OpenLoopScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val mlhfmPair = remember(mapList) { findMlhfmMap(mapList) }

    var me7LogMap by remember { mutableStateOf<Map<Me7LogFileContract.Header, List<Double>>?>(null) }
    var afrLogMap by remember { mutableStateOf<Map<String, List<Double>>?>(null) }
    var correction by remember { mutableStateOf<OpenLoopMlhfmCorrection?>(null) }
    var airflowEstimation by remember { mutableStateOf<AirflowEstimation?>(null) }

    val scope = rememberCoroutineScope()

    // Collect ME7 logs
    LaunchedEffect(Unit) {
        OpenLoopLogParser.logs.collect { logs ->
            me7LogMap = logs
            // Check if wide band O2 is in the log and forward to AFR parser
            val wideBand = logs[Me7LogFileContract.Header.WIDE_BAND_O2_HEADER]
            if (wideBand != null && wideBand.isNotEmpty()) {
                AfrLogParser.load(logs)
            }
        }
    }

    // Collect AFR logs and compute correction + airflow
    LaunchedEffect(Unit) {
        AfrLogParser.logs.collect { logs ->
            afrLogMap = logs
            val currentMe7 = me7LogMap
            val pair = findMlhfmMap(BinParser.mapList.value)
            if (currentMe7 != null && currentMe7.isNotEmpty() && logs.isNotEmpty() && pair != null) {
                withContext(Dispatchers.Default) {
                    // Compute correction
                    val prefs = OpenLoopFuelingLogFilterPreferences
                    val manager = OpenLoopMlhfmCorrectionManager(
                        prefs.minThrottleAngle, prefs.minRpm,
                        prefs.minMe7Points, prefs.minAfrPoints, prefs.maxAfr
                    )
                    manager.correct(currentMe7, logs, pair.second)
                    correction = manager.openLoopCorrection

                    // Compute airflow estimation
                    val afManager = AirflowEstimationManager(
                        prefs.minThrottleAngle, prefs.minRpm,
                        prefs.minMe7Points, prefs.minAfrPoints,
                        prefs.maxAfr, prefs.fuelInjectorSize,
                        prefs.numFuelInjectors, prefs.gasolineGramsPerCubicCentimeter
                    )
                    afManager.estimate(currentMe7, logs)
                    airflowEstimation = afManager.airflowEstimation
                }
            }
        }
    }

    val hasMap = mlhfmPair != null
    val hasMe7Logs = me7LogMap != null && me7LogMap!!.isNotEmpty()
    val hasAfrLogs = afrLogMap != null && afrLogMap!!.isNotEmpty()
    val hasCorrection = correction != null

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("ME7 Logs", "Correction", "MLHFM", "Help")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                val enabled = when (index) {
                    0 -> hasMap
                    1 -> hasMap && hasMe7Logs && hasAfrLogs && hasCorrection
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
            0 -> OpenLoopLogsTab(
                me7LogMap = me7LogMap,
                afrLogMap = afrLogMap,
                airflowEstimation = airflowEstimation
            )
            1 -> OpenLoopCorrectionTab(
                correction = correction,
                onCorrectionUpdated = { correction = it }
            )
            2 -> OpenLoopMlhfmTab(correction = correction)
            3 -> OpenLoopHelpTab()
        }
    }
}

// -- ME7 Logs Tab --

@Composable
private fun OpenLoopLogsTab(
    me7LogMap: Map<Me7LogFileContract.Header, List<Double>>?,
    afrLogMap: Map<String, List<Double>>?,
    airflowEstimation: AirflowEstimation?
) {
    var me7FileName by remember { mutableStateOf("No File Selected") }
    var afrFileName by remember { mutableStateOf("No File Selected") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var logSubTab by remember { mutableStateOf(0) }
    val logSubTabs = listOf("Fueling", "Airflow")

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tab row
        PrimaryTabRow(selectedTabIndex = logSubTab) {
            logSubTabs.forEachIndexed { index, title ->
                Tab(
                    selected = logSubTab == index,
                    onClick = { logSubTab = index },
                    text = { Text(title, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (logSubTab) {
                0 -> FuelingChart(me7LogMap, afrLogMap)
                1 -> AirflowChart(airflowEstimation)
            }
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

            // ME7 file button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    val dialog = FileDialog(Frame(), "Load ME7 Log", FileDialog.LOAD)
                    dialog.file = "*.csv"
                    dialog.isVisible = true
                    val dir = dialog.directory
                    val file = dialog.file
                    if (dir != null && file != null) {
                        val selected = File(dir, file)
                        me7FileName = selected.name
                        OpenLoopLogParser.loadFile(selected)
                    }
                }) {
                    Text("Load ME7 Logs")
                }
                Text(
                    text = me7FileName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.width(24.dp))

            // AFR file button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    val dialog = FileDialog(Frame(), "Load AFR Log", FileDialog.LOAD)
                    dialog.file = "*.csv"
                    dialog.isVisible = true
                    val dir = dialog.directory
                    val file = dialog.file
                    if (dir != null && file != null) {
                        val selected = File(dir, file)
                        afrFileName = selected.name
                        AfrLogParser.load(selected)
                    }
                }) {
                    Text("Load Zeitronix Logs")
                }
                Text(
                    text = afrFileName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (showFilterDialog) {
        OpenLoopFilterDialog(
            onDismiss = { showFilterDialog = false },
            onApply = { showFilterDialog = false }
        )
    }
}

@Composable
private fun FuelingChart(
    me7LogMap: Map<Me7LogFileContract.Header, List<Double>>?,
    afrLogMap: Map<String, List<Double>>?
) {
    val prefs = OpenLoopFuelingLogFilterPreferences

    val series = remember(me7LogMap, afrLogMap) {
        val allSeries = mutableListOf<ChartSeries>()

        if (me7LogMap != null && me7LogMap.isNotEmpty()) {
            val me7LogList = Me7LogUtil.findMe7Logs(
                me7LogMap, prefs.minThrottleAngle, 0.0, prefs.minRpm, prefs.minMe7Points
            )
            var logCount = 1
            for (log in me7LogList) {
                val requestedAfr = log[Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER] ?: continue
                val rpm = log[Me7LogFileContract.Header.RPM_COLUMN_HEADER] ?: continue
                val points = rpm.indices.map { i -> rpm[i] to requestedAfr[i] * 14.7 }
                allSeries.add(
                    ChartSeries("Desired AFR ${logCount++}", points, Primary, showLine = true, showPoints = false)
                )
            }
        }

        if (afrLogMap != null && afrLogMap.isNotEmpty()) {
            val afrLogList = AfrLogUtil.findAfrLogs(
                afrLogMap, prefs.minThrottleAngle, prefs.minRpm, prefs.maxAfr, prefs.minAfrPoints
            )
            var logCount = 1
            for (log in afrLogList) {
                val afr = log[AfrLogFileContract.AFR_HEADER] ?: continue
                val rpm = log[AfrLogFileContract.RPM_HEADER] ?: continue
                val points = rpm.indices.map { i -> rpm[i] to afr[i] }
                allSeries.add(
                    ChartSeries("Actual AFR ${logCount++}", points, ChartRed, showLine = true, showPoints = false)
                )
            }
        }

        allSeries
    }

    LineChart(
        series = series,
        title = "Fueling",
        xAxisLabel = "RPM",
        yAxisLabel = "AFR"
    )
}

@Composable
private fun AirflowChart(airflowEstimation: AirflowEstimation?) {
    val series = remember(airflowEstimation) {
        if (airflowEstimation == null) return@remember emptyList()
        val allSeries = mutableListOf<ChartSeries>()

        var logCount = 1
        for (i in airflowEstimation.measuredAirflowGramsPerSecondLogs.indices) {
            val airflow = airflowEstimation.measuredAirflowGramsPerSecondLogs[i]
            val rpm = airflowEstimation.measuredRpmLogs[i]
            val points = rpm.indices.map { j -> rpm[j] to airflow[j] }
            allSeries.add(
                ChartSeries("Measured Airflow ${logCount++}", points, Primary, showLine = true, showPoints = false)
            )
        }

        logCount = 1
        for (i in airflowEstimation.estimatedAirflowGramsPerSecondLogs.indices) {
            val airflow = airflowEstimation.estimatedAirflowGramsPerSecondLogs[i]
            val rpm = airflowEstimation.measuredRpmLogs[i]
            val points = rpm.indices.map { j -> rpm[j] to airflow[j] }
            allSeries.add(
                ChartSeries("Estimated Airflow ${logCount++}", points, ChartRed, showLine = true, showPoints = false)
            )
        }

        allSeries
    }

    LineChart(
        series = series,
        title = "Airflow",
        xAxisLabel = "RPM",
        yAxisLabel = "g/sec"
    )
}

@Composable
private fun OpenLoopFilterDialog(
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val prefs = OpenLoopFuelingLogFilterPreferences
    var minTps by remember { mutableStateOf(prefs.minThrottleAngle.toString()) }
    var minRpm by remember { mutableStateOf(prefs.minRpm.toString()) }
    var minMe7Pts by remember { mutableStateOf(prefs.minMe7Points.toString()) }
    var minAfrPts by remember { mutableStateOf(prefs.minAfrPoints.toString()) }
    var maxAfr by remember { mutableStateOf(prefs.maxAfr.toString()) }
    var fuelInjSize by remember { mutableStateOf(prefs.fuelInjectorSize.toString()) }
    var fuelDensity by remember { mutableStateOf(prefs.gasolineGramsPerCubicCentimeter.toString()) }
    var numInjectors by remember { mutableStateOf(prefs.numFuelInjectors.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Open Loop Filter") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = minTps, onValueChange = { minTps = it },
                    label = { Text("Minimum Throttle Angle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minRpm, onValueChange = { minRpm = it },
                    label = { Text("Minimum RPM") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minMe7Pts, onValueChange = { minMe7Pts = it },
                    label = { Text("Minimum ME7 Points") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minAfrPts, onValueChange = { minAfrPts = it },
                    label = { Text("Minimum AFR Points") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = maxAfr, onValueChange = { maxAfr = it },
                    label = { Text("Maximum AFR") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fuelInjSize, onValueChange = { fuelInjSize = it },
                    label = { Text("Fuel Injector Size (cc/m)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fuelDensity, onValueChange = { fuelDensity = it },
                    label = { Text("Fuel Density (g/cc)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = numInjectors, onValueChange = { numInjectors = it },
                    label = { Text("Fuel Injectors") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                prefs.minThrottleAngle = minTps.toDoubleOrNull() ?: return@TextButton
                prefs.minRpm = minRpm.toDoubleOrNull() ?: return@TextButton
                prefs.minMe7Points = minMe7Pts.toIntOrNull() ?: return@TextButton
                prefs.minAfrPoints = minAfrPts.toIntOrNull() ?: return@TextButton
                prefs.maxAfr = maxAfr.toDoubleOrNull() ?: return@TextButton
                prefs.fuelInjectorSize = fuelInjSize.toDoubleOrNull() ?: return@TextButton
                prefs.gasolineGramsPerCubicCentimeter = fuelDensity.toDoubleOrNull() ?: return@TextButton
                prefs.numFuelInjectors = numInjectors.toDoubleOrNull() ?: return@TextButton
                onApply()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -- Correction Tab --

@Composable
private fun OpenLoopCorrectionTab(
    correction: OpenLoopMlhfmCorrection?,
    onCorrectionUpdated: (OpenLoopMlhfmCorrection) -> Unit
) {
    if (correction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No correction data available.")
        }
        return
    }

    var polynomialDegree by remember { mutableStateOf(6) }
    var correctionSubTab by remember { mutableStateOf(0) }
    val correctionSubTabs = listOf("MLHFM", "AFR Correction %")

    Column(modifier = Modifier.fillMaxSize()) {
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
                0 -> OpenLoopCorrectionMlhfmChart(correction)
                1 -> OpenLoopAfrCorrectionChart(correction)
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
private fun OpenLoopCorrectionMlhfmChart(correction: OpenLoopMlhfmCorrection) {
    val inputPoints = remember(correction) {
        correction.inputMlhfm.yAxis.indices.map { i ->
            correction.inputMlhfm.yAxis[i] to correction.inputMlhfm.zAxis[i][0]
        }
    }
    val correctedPoints = remember(correction) {
        val mlhfm = correction.fitMlhfm
        if (mlhfm.yAxis.isNotEmpty()) {
            mlhfm.yAxis.indices.map { i -> mlhfm.yAxis[i] to mlhfm.zAxis[i][0] }
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

@Composable
private fun OpenLoopAfrCorrectionChart(correction: OpenLoopMlhfmCorrection) {
    val mean = remember { Mean() }

    val rawPoints = remember(correction) {
        correction.correctionsAfrMap.flatMap { (voltage, values) ->
            values.filter { !it.isNaN() }.map { voltage to it }
        }
    }
    val meanLine = remember(correction) {
        correction.meanAfrMap.filter { !it.value.isNaN() }.map { (voltage, value) -> voltage to value }
    }
    val modeLine = remember(correction) {
        correction.modeAfrMap.mapNotNull { (voltage, modes) ->
            if (modes.isEmpty()) null
            else voltage to mean.evaluate(modes, 0, modes.size)
        }
    }
    val correctedLine = remember(correction) {
        correction.correctedAfrMap.filter { !it.value.isNaN() }.map { (voltage, value) -> voltage to value }
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

// -- MLHFM Tab --

@Composable
private fun OpenLoopMlhfmTab(correction: OpenLoopMlhfmCorrection?) {
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
private fun OpenLoopHelpTab() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Open Loop MLHFM Correction",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = """
                This tool corrects the MAF sensor linearization table (MLHFM) using open loop (wide open throttle) data with an external wideband O2 sensor.

                Steps:
                1. Ensure MLHFM is configured in the Configuration tab.
                2. Load an ME7 log file (.csv) with WOT pull data.
                3. Load a Zeitronix (or similar) AFR log file (.csv) with corresponding wideband O2 data.
                4. The ME7 Logs tab will show fueling and airflow charts.
                5. The Correction tab shows the corrected MLHFM and AFR correction scatter plot.
                6. Use the polynomial fit to smooth the correction curve.
                7. Write the corrected MLHFM back to the binary.

                Required ME7 log headers: MAF Voltage, STFT, LTFT, Lambda Control, Throttle Angle, RPM, Requested Lambda, Fuel Injector On-Time, MAF g/sec.
                Required AFR log headers: Time, RPM, AFR, TPS, Boost.

                The correction compares the expected AFR (from ME7 fueling request) with the actual measured AFR
                from the wideband O2 sensor. The difference indicates MAF sensor error at each voltage point.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = {
            try {
                Desktop.getDesktop().browse(URI("https://github.com/KalebKE/ME7Tuner#open-loop"))
            } catch (_: Exception) { }
        }) {
            Text("Open Loop MLHFM User Guide")
        }
    }
}
