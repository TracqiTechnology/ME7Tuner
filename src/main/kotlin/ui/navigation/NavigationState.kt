package ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class NavigationState {
    var railDestination by mutableStateOf(RailDestination.CONFIGURATION)
        private set

    var calibrationTab by mutableStateOf(CalibrationTab.FUELING)
        private set

    // Sub-tab state (used by screenshot harness; screens read for initial values)
    var closedLoopTab by mutableStateOf(0)
    var closedLoopCorrectionSubTab by mutableStateOf(0)
    var autoFitDegree: Int? by mutableStateOf(null)
    var openLoopTab by mutableStateOf(0)
    var openLoopLogSubTab by mutableStateOf(0)
    var openLoopCorrectionSubTab by mutableStateOf(0)
    var plsolTab by mutableStateOf(0)

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
