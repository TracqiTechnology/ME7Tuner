package ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class NavigationState {
    var railDestination by mutableStateOf(RailDestination.CONFIGURATION)
        private set

    var calibrationTab by mutableStateOf(CalibrationTab.FUELING)
        private set

    fun navigateTo(destination: RailDestination) {
        railDestination = destination
    }

    fun navigateToCalibration(tab: CalibrationTab = calibrationTab) {
        calibrationTab = tab
        railDestination = RailDestination.CALIBRATION
    }

    fun navigateToOptimizer() {
        railDestination = RailDestination.OPTIMIZER
    }

    fun selectCalibrationTab(tab: CalibrationTab) {
        calibrationTab = tab
    }
}
