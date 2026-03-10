package ui.screens.dualinjection

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
import domain.model.injector.InjectorScalingSolver
import domain.model.injector.InjectorSpec
import domain.model.injector.KrkteScalingResult
import domain.model.injector.TvubResult

/**
 * MED17-only screen for dual injection (port + direct) calibration.
 *
 * Three tabs:
 * 1. Port Injector Scaling — KRKTE_PFI ratio calculator + TVUB_PFI dead time
 * 2. Direct Injector Scaling — KRKTE_GDI ratio calculator
 * 3. Split Calculator — port vs DI fuel share at given load/RPM
 */
@Composable
fun DualInjectionScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Port Injector", "Direct Injector", "Split Calculator")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> PortInjectorTab()
            1 -> DirectInjectorTab()
            2 -> SplitCalculatorTab()
        }
    }
}

// ── Port Injector Scaling Tab ─────────────────────────────────────────

@Composable
private fun PortInjectorTab() {
    val scrollState = rememberScrollState()

    var oldFlowRate by remember { mutableStateOf("") }
    var oldPressure by remember { mutableStateOf("4.0") }
    var oldDeadTime by remember { mutableStateOf("") }
    var newFlowRate by remember { mutableStateOf("") }
    var newPressure by remember { mutableStateOf("4.0") }
    var newDeadTime by remember { mutableStateOf("") }

    var scalingResult by remember { mutableStateOf<KrkteScalingResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Port Injector (PFI) Scaling", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "MED17 2.5T dual-fuel: port injectors deliver fuel into the intake manifold. " +
                        "Compute KRKTE_PFI scale factor when upgrading port injectors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "te_pfi = rk_w × KRKTE_PFI × portShare + TVUB_PFI(ubat)",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Old injector
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Stock Port Injector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = oldFlowRate, onValueChange = { oldFlowRate = it }, label = { Text("Flow Rate (cc/min)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = oldPressure, onValueChange = { oldPressure = it }, label = { Text("Fuel Pressure (bar)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = oldDeadTime, onValueChange = { oldDeadTime = it }, label = { Text("Dead Time @ 14V (ms)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }

        // New injector
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("New Port Injector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newFlowRate, onValueChange = { newFlowRate = it }, label = { Text("Flow Rate (cc/min)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = newPressure, onValueChange = { newPressure = it }, label = { Text("Fuel Pressure (bar)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = newDeadTime, onValueChange = { newDeadTime = it }, label = { Text("Dead Time @ 14V (ms)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }

        // Calculate button
        Button(onClick = {
            errorMessage = null
            scalingResult = null
            try {
                val oldSpec = InjectorSpec(
                    flowRateCcPerMin = oldFlowRate.toDouble(),
                    fuelPressureBar = oldPressure.toDouble(),
                    deadTimeMs = oldDeadTime.toDoubleOrNull() ?: 0.0
                )
                val newSpec = InjectorSpec(
                    flowRateCcPerMin = newFlowRate.toDouble(),
                    fuelPressureBar = newPressure.toDouble(),
                    deadTimeMs = newDeadTime.toDoubleOrNull() ?: 0.0
                )
                scalingResult = InjectorScalingSolver.computeKrkteScaling(oldSpec, newSpec)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Calculation error"
            }
        }) {
            Text("Calculate Port KRKTE Scale Factor")
        }

        // Results
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        scalingResult?.let { result ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Result", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "KRKTE_PFI scale factor: %.6f".format(result.scaleFactor),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "KRKTE_PFI_new = KRKTE_PFI_old × %.6f".format(result.scaleFactor),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    if (result.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        result.warnings.forEach { warning ->
                            Text(warning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// ── Direct Injector Scaling Tab ───────────────────────────────────────

@Composable
private fun DirectInjectorTab() {
    val scrollState = rememberScrollState()

    var oldFlowRate by remember { mutableStateOf("") }
    var oldPressure by remember { mutableStateOf("200.0") }
    var newFlowRate by remember { mutableStateOf("") }
    var newPressure by remember { mutableStateOf("200.0") }

    var scalingResult by remember { mutableStateOf<KrkteScalingResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Direct Injector (GDI) Scaling", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "MED17 2.5T dual-fuel: direct injectors deliver fuel into the combustion chamber " +
                        "at high pressure (~200 bar). Compute KRKTE_GDI scale factor when upgrading DI injectors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "te_gdi = rk_w × KRKTE_GDI × (1 − portShare) + TVUB_GDI(ubat)",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Note: DI dead time (TVUB_GDI) may not be tunable on MED17.1.62. " +
                        "The DI driver firmware handles injector timing internally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Old DI injector
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Stock Direct Injector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = oldFlowRate, onValueChange = { oldFlowRate = it }, label = { Text("Flow Rate (cc/min)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = oldPressure, onValueChange = { oldPressure = it }, label = { Text("Fuel Pressure (bar)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }

        // New DI injector
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("New Direct Injector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newFlowRate, onValueChange = { newFlowRate = it }, label = { Text("Flow Rate (cc/min)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = newPressure, onValueChange = { newPressure = it }, label = { Text("Fuel Pressure (bar)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }

        // Calculate
        Button(onClick = {
            errorMessage = null
            scalingResult = null
            try {
                val oldSpec = InjectorSpec(
                    flowRateCcPerMin = oldFlowRate.toDouble(),
                    fuelPressureBar = oldPressure.toDouble(),
                    deadTimeMs = 0.0
                )
                val newSpec = InjectorSpec(
                    flowRateCcPerMin = newFlowRate.toDouble(),
                    fuelPressureBar = newPressure.toDouble(),
                    deadTimeMs = 0.0
                )
                scalingResult = InjectorScalingSolver.computeKrkteScaling(oldSpec, newSpec)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Calculation error"
            }
        }) {
            Text("Calculate DI KRKTE Scale Factor")
        }

        // Results
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        scalingResult?.let { result ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Result", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "KRKTE_GDI scale factor: %.6f".format(result.scaleFactor),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "KRKTE_GDI_new = KRKTE_GDI_old × %.6f".format(result.scaleFactor),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    if (result.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        result.warnings.forEach { warning ->
                            Text(warning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// ── Split Calculator Tab ──────────────────────────────────────────────

@Composable
private fun SplitCalculatorTab() {
    val scrollState = rememberScrollState()

    var portKrkte by remember { mutableStateOf("") }
    var diKrkte by remember { mutableStateOf("") }
    var desiredPortShare by remember { mutableStateOf("30.0") }
    var targetLoad by remember { mutableStateOf("150.0") }

    var portOnTime by remember { mutableStateOf<Double?>(null) }
    var diOnTime by remember { mutableStateOf<Double?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Dual Injection Split Calculator", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "The 2.5T EA855 EVO runs dual fuel injection from the factory. " +
                        "Port injectors (PFI) and direct injectors (GDI) share the total fuel delivery. " +
                        "This calculator computes injection on-times for a given split ratio and load target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "te_pfi = targetLoad × portShare% × KRKTE_PFI\n" +
                        "te_gdi = targetLoad × (1 − portShare%) × KRKTE_GDI",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Inputs
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Parameters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = portKrkte, onValueChange = { portKrkte = it }, label = { Text("KRKTE_PFI (ms/%)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = diKrkte, onValueChange = { diKrkte = it }, label = { Text("KRKTE_GDI (ms/%)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = desiredPortShare, onValueChange = { desiredPortShare = it }, label = { Text("Port Share (%)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = targetLoad, onValueChange = { targetLoad = it }, label = { Text("Target Load (%)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }

        // Calculate
        Button(onClick = {
            errorMessage = null
            portOnTime = null
            diOnTime = null
            try {
                val pKrkte = portKrkte.toDouble()
                val dKrkte = diKrkte.toDouble()
                val share = desiredPortShare.toDouble() / 100.0
                val load = targetLoad.toDouble()

                require(pKrkte > 0) { "KRKTE_PFI must be positive" }
                require(dKrkte > 0) { "KRKTE_GDI must be positive" }
                require(share in 0.0..1.0) { "Port share must be 0–100%" }
                require(load > 0) { "Target load must be positive" }

                portOnTime = load * share * pKrkte
                diOnTime = load * (1.0 - share) * dKrkte
            } catch (e: Exception) {
                errorMessage = e.message ?: "Calculation error"
            }
        }) {
            Text("Calculate Split")
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (portOnTime != null && diOnTime != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Result", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Port (PFI) on-time:   %.4f ms".format(portOnTime),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Direct (GDI) on-time: %.4f ms".format(diOnTime),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Total on-time:        %.4f ms".format((portOnTime ?: 0.0) + (diOnTime ?: 0.0)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val share = desiredPortShare.toDoubleOrNull() ?: 30.0
                    if (share < 10.0 || share > 90.0) {
                        Text(
                            "Warning: Extreme split ratio (%.0f%% port). Verify injector sizing.".format(share),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}


