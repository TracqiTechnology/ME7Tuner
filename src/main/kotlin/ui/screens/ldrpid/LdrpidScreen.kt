package ui.screens.ldrpid

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.contract.Me7LogFileContract
import data.parser.bin.BinParser
import data.parser.me7log.KfvpdksdLogParser
import data.parser.me7log.Me7LogParser
import data.parser.xdf.TableDefinition
import data.preferences.bin.BinFilePreferences
import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import data.preferences.ldrpid.LdrpidPreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import domain.model.ldrpid.LdrpidCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.components.MapAxis
import ui.components.MapTable
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
fun LdrpidScreen() {
    val mapList by BinParser.mapList.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val kfldrlPair = remember(mapList) { findMap(mapList, KfldrlPreferences) }
    val kfldimxPair = remember(mapList) { findMap(mapList, KfldimxPreferences) }

    // State for the 4 map tables
    var nonLinearMap by remember { mutableStateOf<Map3d?>(null) }
    var linearMap by remember { mutableStateOf<Map3d?>(null) }
    var kfldrlMap by remember { mutableStateOf<Map3d?>(null) }
    var kfldimxMap by remember { mutableStateOf<Map3d?>(null) }
    var kfldimxXAxis by remember { mutableStateOf<Array<Array<Double>>?>(null) }

    // Initialize maps from preferences
    LaunchedEffect(kfldrlPair, kfldimxPair) {
        if (kfldrlPair != null) {
            val kfldrl = kfldrlPair.second
            kfldrlMap = kfldrl

            // Initialize non-linear map with same structure but zeroed z-axis
            val emptyZ = Array(kfldrl.zAxis.size) { Array(kfldrl.zAxis[0].size) { 0.0 } }
            if (nonLinearMap == null) {
                nonLinearMap = Map3d(kfldrl.xAxis, kfldrl.yAxis, emptyZ)
            }
            // Initialize linear map similarly
            val emptyLZ = Array(kfldrl.zAxis.size) { Array(kfldrl.zAxis[0].size) { 0.0 } }
            if (linearMap == null) {
                linearMap = Map3d(kfldrl.xAxis, kfldrl.yAxis, emptyLZ)
            }
        }
        if (kfldimxPair != null) {
            kfldimxMap = kfldimxPair.second
            kfldimxXAxis = arrayOf(kfldimxPair.second.xAxis)
        }
    }

    // Recompute dependent maps when non-linear table is edited
    fun recomputeFromNonLinear(editedNonLinear: Map3d) {
        val kfldrlDef = kfldrlPair ?: return
        val kfldimxDef = kfldimxPair ?: return

        nonLinearMap = editedNonLinear
        val linear = LdrpidCalculator.calculateLinearTable(editedNonLinear.zAxis, kfldrlDef.second)
        linearMap = linear
        val newKfldrl = LdrpidCalculator.calculateKfldrl(editedNonLinear.zAxis, linear.zAxis, kfldrlDef.second)
        kfldrlMap = newKfldrl
        val newKfldimx = LdrpidCalculator.calculateKfldimx(editedNonLinear.zAxis, linear.zAxis, kfldrlDef.second, kfldimxDef.second)
        kfldimxMap = newKfldimx
        kfldimxXAxis = arrayOf(newKfldimx.xAxis)
    }

    // Progress bar state
    var progressValue by remember { mutableStateOf(0) }
    var progressMax by remember { mutableStateOf(1) }
    var showProgress by remember { mutableStateOf(false) }
    var logDirName by remember { mutableStateOf("No Directory Selected") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top section: 4 MapTables in a 2x2 grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Non-Linear Boost (editable)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Non Linear Boost",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (nonLinearMap != null && nonLinearMap!!.zAxis.isNotEmpty()) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(
                            map = nonLinearMap!!,
                            editable = true,
                            onMapChanged = { edited -> recomputeFromNonLinear(edited) }
                        )
                    }
                } else {
                    Text("No map data", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Linear Boost (read-only)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Linear Boost",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (linearMap != null && linearMap!!.zAxis.isNotEmpty()) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(map = linearMap!!, editable = false)
                    }
                } else {
                    Text("No map data", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // KFLDRL
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "KFLDRL",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (kfldrlMap != null && kfldrlMap!!.zAxis.isNotEmpty()) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(map = kfldrlMap!!, editable = false)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val kfldrlDef = KfldrlPreferences.getSelectedMap()
                        if (kfldrlDef != null && kfldrlMap != null) {
                            val binFile = BinFilePreferences.getStoredFile()
                            if (binFile.exists()) {
                                BinWriter.write(binFile, kfldrlDef.first, kfldrlMap!!)
                            }
                        }
                    }) {
                        Text("Write KFLDRL")
                    }
                } else {
                    Text("No map data", style = MaterialTheme.typography.bodySmall)
                }
            }

            // KFLDIMX
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "KFLDIMX X-Axis",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (kfldimxXAxis != null) {
                    Box(modifier = Modifier.heightIn(max = 40.dp)) {
                        MapAxis(data = kfldimxXAxis!!, editable = false)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "KFLDIMX",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (kfldimxMap != null && kfldimxMap!!.zAxis.isNotEmpty()) {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        MapTable(map = kfldimxMap!!, editable = false)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val kfldimxDef = KfldimxPreferences.getSelectedMap()
                        if (kfldimxDef != null && kfldimxMap != null) {
                            val binFile = BinFilePreferences.getStoredFile()
                            if (binFile.exists()) {
                                BinWriter.write(binFile, kfldimxDef.first, kfldimxMap!!)
                            }
                        }
                    }) {
                        Text("Write KFLDIMX")
                    }
                } else {
                    Text("No map data", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Bottom section: Load log directory + progress
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                val dialog = FileDialog(Frame(), "Select Log Directory", FileDialog.LOAD)
                System.setProperty("apple.awt.fileDialogForDirectories", "true")
                val lastDir = LdrpidPreferences.lastDirectory
                if (lastDir.isNotEmpty()) dialog.directory = lastDir
                dialog.isVisible = true
                System.setProperty("apple.awt.fileDialogForDirectories", "false")
                val dir = dialog.directory
                val file = dialog.file
                if (dir != null && file != null) {
                    val selectedDir = File(dir, file)
                    LdrpidPreferences.lastDirectory = selectedDir.parent ?: dir
                    logDirName = selectedDir.path

                    showProgress = true
                    progressValue = 0

                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val parser = Me7LogParser()
                            val values = parser.parseLogDirectory(
                                Me7LogParser.LogType.LDRPID,
                                selectedDir
                            ) { value, max ->
                                progressValue = value
                                progressMax = max
                                showProgress = value < max - 1
                            }

                            val kfldimxDef = KfldimxPreferences.getSelectedMap()
                            val kfldrlDef = KfldrlPreferences.getSelectedMap()

                            if (kfldimxDef != null && kfldrlDef != null) {
                                val result = LdrpidCalculator.calculateLdrpid(
                                    values,
                                    kfldrlDef.second,
                                    kfldimxDef.second
                                )

                                withContext(Dispatchers.Main) {
                                    nonLinearMap = result.nonLinearOutput
                                    linearMap = result.linearOutput
                                    kfldrlMap = result.kfldrl
                                    kfldimxMap = result.kfldimx
                                    kfldimxXAxis = arrayOf(result.kfldimx.xAxis)
                                    showProgress = false
                                }
                            }
                        }
                    }
                }
            }) {
                Text("Load ME7 Logs")
            }

            Spacer(Modifier.height(8.dp))

            if (showProgress) {
                LinearProgressIndicator(
                    progress = { if (progressMax > 0) progressValue.toFloat() / progressMax.toFloat() else 0f },
                    modifier = Modifier.width(300.dp).height(8.dp)
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = logDirName,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
