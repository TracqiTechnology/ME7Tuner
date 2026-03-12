package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfmiop.KfmiopPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end Compose UI test for the KFMIOP screen on MED17.
 *
 * On MED17 (DS1), KFMIOP is a 1×1 scalar (single max torque value),
 * NOT a full 2D map like ME7. The KFMIOP calculator expects a 2D map
 * and returns null for scalar inputs, so the Write button is correctly
 * disabled. Tests verify:
 *  - Profile resolves the KFMIOP preference
 *  - "Not configured" does NOT appear (map IS set, just scalar)
 *  - Write button is disabled (calculator returns null for scalar)
 *  - DS1 banner is visible
 */
@OptIn(ExperimentalTestApi::class)
class Med17KfmiopScreenTest : Med17ScreenTestBase() {

    @Test
    fun profileResolvesKfmiopPreference() {
        val selected = KfmiopPreferences.getSelectedMap()
        assertNotNull(selected, "Profile should resolve KFMIOP map preference")
        assertTrue(
            selected.first.tableName == KFMIOP_TITLE,
            "KFMIOP table name should be '$KFMIOP_TITLE', got '${selected.first.tableName}'"
        )
    }

    @Test
    fun kfmiopMapIsScalarOnMed17() {
        val selected = KfmiopPreferences.getSelectedMap()!!
        val map = selected.second
        // MED17 DS1 KFMIOP is a 1×1 scalar — document this reality
        assertTrue(
            map.xAxis.isEmpty() && map.yAxis.isEmpty(),
            "MED17 KFMIOP should be a scalar (empty axes), got x=${map.xAxis.size} y=${map.yAxis.size}"
        )
        assertTrue(
            map.zAxis.isNotEmpty() && map.zAxis[0].isNotEmpty(),
            "KFMIOP scalar should have a z-value"
        )
    }

    @Test
    fun kfmiopScreenConfiguredShowsNoNotConfigured() = runComposeUiTest {
        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        // Map IS configured (preference resolves), even though it's a scalar.
        // "Not configured" should NOT appear.
        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun kfmiopWriteButtonDisabledForScalarMap() = runComposeUiTest {
        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        // Write is disabled because the calculator returns null for scalar KFMIOP.
        // This is the correct behavior — the 2D rescaling calculator can't work
        // with a 1×1 scalar input.
        onNodeWithText("Write KFMIOP").assertIsNotEnabled()
    }

    @Test
    fun kfmiopScreenUnconfiguredShowsNotConfigured() = runComposeUiTest {
        KfmiopPreferences.setSelectedMap(null)

        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        onNodeWithText("Not configured").assertExists()
        onNodeWithText("Write KFMIOP").assertIsNotEnabled()
    }

    @Test
    fun kfmiopScreenShowsDs1Banner() = runComposeUiTest {
        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        onNodeWithText("DS1 Note", substring = true).assertExists()
    }

    @Test
    fun kfmiopScreenShowsComparisonTabs() = runComposeUiTest {
        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        onNodeWithText("Torque").assertExists()
        onNodeWithText("Boost Comparison").assertExists()
    }
}
