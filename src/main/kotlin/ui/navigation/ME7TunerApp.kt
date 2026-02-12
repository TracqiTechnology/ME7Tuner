package ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ui.screens.closedloop.ClosedLoopScreen
import ui.screens.configuration.ConfigurationScreen
import ui.screens.kfmiop.KfmiopScreen
import ui.screens.kfmirl.KfmirlScreen
import ui.screens.kfvpdksd.KfvpdksdScreen
import ui.screens.kfzw.KfzwScreen
import ui.screens.kfzwop.KfzwopScreen
import ui.screens.krkte.KrkteScreen
import ui.screens.ldrpid.LdrpidScreen
import ui.screens.mlhfm.MlhfmScreen
import ui.screens.openloop.OpenLoopScreen
import ui.screens.plsol.PlsolScreen
import ui.screens.wdkugdn.WdkugdnScreen

enum class Tab(val title: String, val tooltip: String) {
    CONFIGURATION("Configuration", "Table Definition Configuration"),
    KRKTE("KRKTE", "KRKTE Calculator"),
    CLOSED_LOOP("Closed Loop Fueling", "Closed Loop MLHFM Compensation"),
    OPEN_LOOP("Open Loop Fueling", "Open Loop MLHFM Compensation"),
    PLSOL("PLSOL", "Requested Boost"),
    KFMIOP("KFMIOP", "KFMIOP Calculator"),
    KFMIRL("KFMIRL", "KFMIRL Calculator"),
    KFZWOP("KFZWOP", "KFZWOP Calculator"),
    KFZW("KFZW", "KFZW Calculator"),
    KFVPDKSD("KFVPDKSD/E", "KFVPDKSD/E Calculator"),
    WDKUGDN("WDKUGDN", "KFURL"),
    LDRPID("LDRPID", "LDRPID")
}

@Composable
fun ME7TunerApp() {
    var selectedTab by remember { mutableStateOf(Tab.CONFIGURATION) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal
            ) {
                Tab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (selectedTab) {
                    Tab.CONFIGURATION -> ConfigurationScreen()
                    Tab.KRKTE -> KrkteScreen()
                    Tab.CLOSED_LOOP -> ClosedLoopScreen()
                    Tab.OPEN_LOOP -> OpenLoopScreen()
                    Tab.PLSOL -> PlsolScreen()
                    Tab.KFMIOP -> KfmiopScreen()
                    Tab.KFMIRL -> KfmirlScreen()
                    Tab.KFZWOP -> KfzwopScreen()
                    Tab.KFZW -> KfzwScreen()
                    Tab.KFVPDKSD -> KfvpdksdScreen()
                    Tab.WDKUGDN -> WdkugdnScreen()
                    Tab.LDRPID -> LdrpidScreen()
                }
            }
        }
    }
}
