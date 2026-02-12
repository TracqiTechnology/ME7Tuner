package ui.screens.krkte

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.preferences.bin.BinFilePreferences
import data.preferences.krkte.KrktePreferences
import data.preferences.primaryfueling.PrimaryFuelingPreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import domain.model.krkte.KrkteCalculator
import java.text.DecimalFormat

private val decimalFormat = DecimalFormat("#.####")

@Composable
fun KrkteScreen() {
    val scrollState = rememberScrollState()

    var airDensity by remember { mutableStateOf(PrimaryFuelingPreferences.airDensity.toString()) }
    var displacement by remember { mutableStateOf(PrimaryFuelingPreferences.displacement.toString()) }
    var numCylinders by remember { mutableStateOf(PrimaryFuelingPreferences.numCylinders.toString()) }
    var gasolineDensity by remember { mutableStateOf(PrimaryFuelingPreferences.gasolineGramsPerCubicCentimeter.toString()) }
    var stoichiometricAfr by remember { mutableStateOf(PrimaryFuelingPreferences.stoichiometricAfr.toString()) }
    var fuelInjectorSize by remember { mutableStateOf(PrimaryFuelingPreferences.fuelInjectorSize.toString()) }

    val cylinderDisplacement by remember(displacement, numCylinders) {
        derivedStateOf {
            val disp = displacement.toDoubleOrNull() ?: 0.0
            val cyl = numCylinders.toIntOrNull() ?: 1
            if (cyl > 0) disp / cyl else 0.0
        }
    }

    val krkteResult by remember(displacement, numCylinders, airDensity, gasolineDensity, stoichiometricAfr, fuelInjectorSize) {
        derivedStateOf {
            val airDensityVal = airDensity.toDoubleOrNull() ?: 0.0
            val dispVal = displacement.toDoubleOrNull() ?: 0.0
            val cylVal = numCylinders.toIntOrNull() ?: 1
            val fuelDensityVal = gasolineDensity.toDoubleOrNull() ?: 0.0
            val afrVal = stoichiometricAfr.toDoubleOrNull() ?: 0.0
            val injectorVal = fuelInjectorSize.toDoubleOrNull() ?: 0.0
            val cylDisp = if (cylVal > 0) dispVal / cylVal else 0.0
            KrkteCalculator.calculateKrkte(airDensityVal, cylDisp, injectorVal, fuelDensityVal, afrVal)
        }
    }

    var showWriteConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Constants panel
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Configure KRKTE Constants",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Air Density (read-only)
                ConstantRow(
                    label = "Air Density (0C and 1013hPa)",
                    value = airDensity,
                    unit = "g/dm^3",
                    enabled = false,
                    onValueChange = {}
                )

                // Engine Displacement
                ConstantRow(
                    label = "Engine Displacement",
                    value = displacement,
                    unit = "dm^3",
                    enabled = true,
                    onValueChange = { newValue ->
                        displacement = newValue
                        newValue.toDoubleOrNull()?.let { PrimaryFuelingPreferences.displacement = it }
                    }
                )

                // Number of Cylinders
                ConstantRow(
                    label = "Number of Cylinders",
                    value = numCylinders,
                    unit = "",
                    enabled = true,
                    onValueChange = { newValue ->
                        numCylinders = newValue
                        newValue.toIntOrNull()?.let { PrimaryFuelingPreferences.numCylinders = it }
                    }
                )

                // Cylinder Displacement (read-only, derived)
                ConstantRow(
                    label = "Cylinder Displacement",
                    value = decimalFormat.format(cylinderDisplacement),
                    unit = "dm^3",
                    enabled = false,
                    onValueChange = {}
                )

                // Gasoline Density
                ConstantRow(
                    label = "Gasoline Grams per Cubic Centimeter",
                    value = gasolineDensity,
                    unit = "g/cc^3",
                    enabled = true,
                    onValueChange = { newValue ->
                        gasolineDensity = newValue
                        newValue.toDoubleOrNull()?.let { PrimaryFuelingPreferences.gasolineGramsPerCubicCentimeter = it }
                    }
                )

                // Stoichiometric A/F Ratio
                ConstantRow(
                    label = "Stoichiometric A/F Ratio",
                    value = stoichiometricAfr,
                    unit = "",
                    enabled = true,
                    onValueChange = { newValue ->
                        stoichiometricAfr = newValue
                        newValue.toDoubleOrNull()?.let { PrimaryFuelingPreferences.stoichiometricAfr = it }
                    }
                )

                // Fuel Injector Size
                ConstantRow(
                    label = "Fuel Injector cc/min",
                    value = fuelInjectorSize,
                    unit = "cc/min",
                    enabled = true,
                    onValueChange = { newValue ->
                        fuelInjectorSize = newValue
                        newValue.toDoubleOrNull()?.let { PrimaryFuelingPreferences.fuelInjectorSize = it }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "KRKTE: ",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = decimalFormat.format(krkteResult),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ms/%",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Write button
        Button(onClick = { showWriteConfirmation = true }) {
            Text("Write KRKTE")
        }
    }

    if (showWriteConfirmation) {
        AlertDialog(
            onDismissRequest = { showWriteConfirmation = false },
            title = { Text("Write KRKTE") },
            text = { Text("Are you sure you want to write KRKTE to the binary?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWriteConfirmation = false
                        val krkteTable = KrktePreferences.getSelectedMap()
                        if (krkteTable != null) {
                            val tableDefinition = krkteTable.first
                            val map = Map3d()
                            map.zAxis = arrayOf(arrayOf(krkteResult))
                            BinWriter.write(BinFilePreferences.file.value, tableDefinition, map)
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
}

@Composable
private fun ConstantRow(
    label: String,
    value: String,
    unit: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(120.dp).height(48.dp)
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp).width(60.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(68.dp))
        }
    }
}
