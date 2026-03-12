package ui.screens.fueltrim

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.contract.Med17LogFileContract
import data.parser.med17log.Med17LogParser
import domain.model.fueltrim.FuelTrimAnalyzer
import domain.model.fueltrim.FuelTrimResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.math.abs

/**
 * MED17 fuel trim analysis screen.
 *
 * Loads MED17 log files containing STFT (frm_w) and/or LTFT (fra_w) data,
 * bins them into an RPM × Load grid, and displays per-bin average trims
 * plus suggested rk_w corrections.
 */
@Composable
fun FuelTrimScreen() {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var result by remember { mutableStateOf<FuelTrimResult?>(null) }
    var logStatus by remember { mutableStateOf<String?>(null) }
    var showProgress by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Fuel Trim Analysis (STFT / LTFT)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Load MED17 logs containing fuel trim data (frm_w / fra_w / longft1_w) to " +
                        "see per-bin average trims across an RPM × Load grid. Bins where the combined " +
                        "trim exceeds ±3% generate a suggested correction to rk_w (relative fuel mass). " +
                        "Positive correction = add fuel, negative = remove fuel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Log Loading
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Load Log Files", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        val dialog = FileDialog(Frame(), "Select MED17 Fuel Trim Log", FileDialog.LOAD)
                        dialog.isMultipleMode = true
                        dialog.isVisible = true
                        val dir = dialog.directory
                        val files = dialog.files
                        if (dir != null && files != null && files.isNotEmpty()) {
                            showProgress = true
                            logStatus = "Loading ${files.size} file(s)..."
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val parser = Med17LogParser()
                                        // Load and merge multiple log files
                                        var merged: Map<Med17LogFileContract.Header, List<Double>>? = null
                                        for (f in files) {
                                            val logData = parser.parseLogFile(
                                                Med17LogParser.LogType.FUEL_TRIM, f
                                            )
                                            if (merged == null) {
                                                merged = logData.toMutableMap()
                                            } else {
                                                val m = merged!!.toMutableMap()
                                                for ((key, list) in logData) {
                                                    val existing = m[key]
                                                    if (existing != null) {
                                                        m[key] = existing + list
                                                    } else {
                                                        m[key] = list
                                                    }
                                                }
                                                merged = m
                                            }
                                        }
                                        val allLogData = merged ?: emptyMap()
                                        val analyzed = FuelTrimAnalyzer.analyzeMed17Trims(allLogData)
                                        withContext(Dispatchers.Main) {
                                            result = analyzed
                                            val totalSamples = analyzed.avgTrims.sumOf { row ->
                                                row.count { it != 0.0 }
                                            }
                                            logStatus = if (analyzed.isEmpty) {
                                                "✓ Loaded ${files.size} file(s) — all trims within ±3% threshold"
                                            } else {
                                                "✓ Loaded ${files.size} file(s) — $totalSamples bins with corrections"
                                            }
                                            showProgress = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            logStatus = "Error: ${e.message}"
                                            showProgress = false
                                        }
                                    }
                                }
                            }
                        }
                    }) {
                        Text("Load Fuel Trim Logs")
                    }

                    if (result != null) {
                        OutlinedButton(onClick = {
                            result = null
                            logStatus = "Cleared"
                        }) {
                            Text("Clear")
                        }
                    }
                }
                if (showProgress) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                logStatus?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Results
        result?.let { fuelTrimResult ->
            // Average Trims Grid
            TrimGrid(
                title = "Average Combined Fuel Trim (%)",
                rpmBins = fuelTrimResult.rpmBins,
                loadBins = fuelTrimResult.loadBins,
                values = fuelTrimResult.avgTrims,
                highlightThreshold = 3.0
            )

            // Corrections Grid
            if (!fuelTrimResult.isEmpty) {
                TrimGrid(
                    title = "Suggested rk_w Corrections (%)",
                    rpmBins = fuelTrimResult.rpmBins,
                    loadBins = fuelTrimResult.loadBins,
                    values = fuelTrimResult.corrections,
                    highlightThreshold = 0.1
                )
            }

            // Warnings
            if (fuelTrimResult.warnings.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Diagnostics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        for (warning in fuelTrimResult.warnings.take(20)) {
                            Text(
                                "• $warning",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (fuelTrimResult.warnings.size > 20) {
                            Text(
                                "... and ${fuelTrimResult.warnings.size - 20} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrimGrid(
    title: String,
    rpmBins: DoubleArray,
    loadBins: DoubleArray,
    values: Array<DoubleArray>,
    highlightThreshold: Double
) {
    val hScrollState = rememberScrollState()

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "RPM ↓  /  Load % →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.horizontalScroll(hScrollState)) {
                Column {
                    // Header row: load bins
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "",
                            modifier = Modifier.width(56.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                        for (load in loadBins) {
                            Text(
                                "%.0f".format(load),
                                modifier = Modifier.width(48.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Data rows
                    for (r in rpmBins.indices) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "%.0f".format(rpmBins[r]),
                                modifier = Modifier.width(56.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            for (l in loadBins.indices) {
                                val v = values[r][l]
                                val color = when {
                                    abs(v) < highlightThreshold -> MaterialTheme.colorScheme.onSurface
                                    v > 0 -> MaterialTheme.colorScheme.error  // Rich (adding fuel)
                                    else -> MaterialTheme.colorScheme.primary  // Lean (removing fuel)
                                }
                                Text(
                                    if (abs(v) < 0.01) "·" else "%+.1f".format(v),
                                    modifier = Modifier.width(48.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
