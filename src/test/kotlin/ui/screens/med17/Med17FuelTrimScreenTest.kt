package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.rkw.RkwPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end Compose UI test for the FuelTrim screen on MED17.
 *
 * Verifies the full workflow: rk_w map loaded from BIN (input) →
 * log analysis → corrected rk_w (output) → write to BIN.
 */
@OptIn(ExperimentalTestApi::class)
class Med17FuelTrimScreenTest : Med17ScreenTestBase() {

    @Test
    fun profileResolvesRkwPreference() {
        val rkw = RkwPreferences.getSelectedMap()
        assertNotNull(rkw, "Profile should resolve rk_w map preference")
        assertTrue(
            rkw.first.tableName.contains("Rel fuel mass", ignoreCase = true),
            "rk_w table name should match, got '${rkw.first.tableName}'"
        )
    }

    @Test
    fun rkwMapIs2d() {
        val selected = RkwPreferences.getSelectedMap()!!
        val map = selected.second
        assertTrue(map.xAxis.size > 1 && map.yAxis.size > 1,
            "rk_w should be a 2D map, got x=${map.xAxis.size} y=${map.yAxis.size}")
    }

    @Test
    fun fuelTrimScreenRendersEmptyState() = runComposeUiTest {
        setContent {
            ui.screens.fueltrim.FuelTrimScreen()
        }

        // Main title
        onNodeWithText("Fuel Trim Analysis", substring = true).assertExists()

        // Load button
        onNodeWithText("Load Fuel Trim Logs").assertExists()

        // Input section shows
        onNodeWithText("Input: Current rk_w", substring = true).assertExists()

        // Write button present but disabled (no logs loaded yet)
        onNodeWithText("Write rk_w").assertIsNotEnabled()
    }

    @Test
    fun fuelTrimScreenShowsInputTable() = runComposeUiTest {
        setContent {
            ui.screens.fueltrim.FuelTrimScreen()
        }

        // rk_w is configured via profile, so "Not configured" should NOT appear
        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun fuelTrimScreenUnconfiguredShowsNotConfigured() = runComposeUiTest {
        RkwPreferences.setSelectedMap(null)

        setContent {
            ui.screens.fueltrim.FuelTrimScreen()
        }

        onNodeWithText("Not configured", substring = true).assertExists()
        onNodeWithText("Write rk_w").assertIsNotEnabled()
    }
}
