package ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.preferences.bin.BinFilePreferences
import data.preferences.xdf.XdfFilePreferences
import ui.screens.configuration.ConfigurationScreen
import ui.screens.optimizer.OptimizerScreen

@Composable
fun ME7TunerApp() {
    val navState = remember { NavigationState() }

    val xdfFile by XdfFilePreferences.file.collectAsState()
    val binFile by BinFilePreferences.file.collectAsState()

    val isConfigured = xdfFile.exists() && xdfFile.isFile &&
        binFile.exists() && binFile.isFile

    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                RailDestination.entries.forEach { destination ->
                    val enabled = destination == RailDestination.CONFIGURATION || isConfigured

                    NavigationRailItem(
                        selected = navState.railDestination == destination,
                        onClick = {
                            if (enabled) navState.navigateTo(destination)
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        enabled = enabled
                    )
                }
            }

            VerticalDivider()

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (navState.railDestination) {
                    RailDestination.CONFIGURATION -> {
                        ConfigurationScreen(
                            trailingContent = {
                                WorkflowGuidanceCards(
                                    onStartCalibration = { navState.navigateToCalibration() },
                                    onStartOptimizer = { navState.navigateToOptimizer() }
                                )
                            }
                        )
                    }
                    RailDestination.CALIBRATION -> {
                        if (isConfigured) {
                            CalibrationContent(
                                selectedTab = navState.calibrationTab,
                                onTabSelected = { navState.selectCalibrationTab(it) }
                            )
                        } else {
                            ConfigurationRequiredPlaceholder()
                        }
                    }
                    RailDestination.OPTIMIZER -> {
                        if (isConfigured) {
                            OptimizerScreen()
                        } else {
                            ConfigurationRequiredPlaceholder()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigurationRequiredPlaceholder() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Configuration Required",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Load an XDF and BIN file in Configuration to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
