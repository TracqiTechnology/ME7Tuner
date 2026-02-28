package ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.screens.closedloop.ClosedLoopScreen
import ui.screens.fueling.FuelingScreen
import ui.screens.kfmiop.KfmiopScreen
import ui.screens.kfmirl.KfmirlScreen
import ui.screens.kfvpdksd.KfvpdksdScreen
import ui.screens.kfzw.KfzwScreen
import ui.screens.kfzwop.KfzwopScreen
import ui.screens.ldrpid.LdrpidScreen
import ui.screens.openloop.OpenLoopScreen
import ui.screens.plsol.PlsolScreen
import ui.screens.wdkugdn.WdkugdnScreen

@Composable
fun CalibrationContent(navState: NavigationState) {
    val selectedTab = navState.calibrationTab

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal
        ) {
            CalibrationTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { navState.selectCalibrationTab(tab) },
                    text = { Text(tab.label) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (selectedTab) {
                CalibrationTab.FUELING -> FuelingScreen()
                CalibrationTab.CLOSED_LOOP -> ClosedLoopScreen(
                    initialTab = navState.closedLoopTab,
                    initialCorrectionSubTab = navState.closedLoopCorrectionSubTab,
                    autoFitDegree = navState.autoFitDegree
                )
                CalibrationTab.OPEN_LOOP -> OpenLoopScreen(
                    initialTab = navState.openLoopTab,
                    initialLogSubTab = navState.openLoopLogSubTab,
                    initialCorrectionSubTab = navState.openLoopCorrectionSubTab,
                    autoFitDegree = navState.autoFitDegree
                )
                CalibrationTab.PLSOL -> PlsolScreen(initialTab = navState.plsolTab)
                CalibrationTab.KFMIOP -> KfmiopScreen()
                CalibrationTab.KFMIRL -> KfmirlScreen()
                CalibrationTab.KFZWOP -> KfzwopScreen()
                CalibrationTab.KFZW -> KfzwScreen()
                CalibrationTab.KFVPDKSD -> KfvpdksdScreen()
                CalibrationTab.WDKUGDN -> WdkugdnScreen()
                CalibrationTab.LDRPID -> LdrpidScreen()
            }
        }
    }
}
