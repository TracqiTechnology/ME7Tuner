package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfmirl.KfmirlPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end Compose UI test for the KFMIRL screen on MED17.
 *
 * On MED17 (DS1), KFMIOP is a 1×1 scalar. KFMIRL is a full 2D table (14×16),
 * but the inverse calculation requires KFMIOP's xAxis as input, which is empty.
 * So outputKfmirl = null and Write is disabled. Tests verify:
 *  - Profile resolves both preferences
 *  - "Not configured" does NOT appear (both maps ARE set)
 *  - Write button is disabled (inverse calc returns null)
 */
@OptIn(ExperimentalTestApi::class)
class Med17KfmirlScreenTest : Med17ScreenTestBase() {

    @Test
    fun profileResolvesBothPreferences() {
        val kfmiop = KfmiopPreferences.getSelectedMap()
        val kfmirl = KfmirlPreferences.getSelectedMap()
        assertNotNull(kfmiop, "Profile should resolve KFMIOP map preference")
        assertNotNull(kfmirl, "Profile should resolve KFMIRL map preference")
        assertTrue(
            kfmirl.first.tableName.contains("Tgt filling", ignoreCase = true),
            "KFMIRL table name should contain 'Tgt filling', got '${kfmirl.first.tableName}'"
        )
    }

    @Test
    fun kfmirlMapIs2dOnMed17() {
        val selected = KfmirlPreferences.getSelectedMap()!!
        val map = selected.second
        // KFMIRL is a real 2D table on MED17, unlike KFMIOP
        assertTrue(map.xAxis.size > 1 && map.yAxis.size > 1,
            "KFMIRL should be a 2D map, got x=${map.xAxis.size} y=${map.yAxis.size}")
    }

    @Test
    fun kfmirlScreenConfiguredShowsNoNotConfigured() = runComposeUiTest {
        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        // Both maps are configured, so "Not configured" should not appear
        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun kfmirlWriteButtonDisabledDueToScalarKfmiop() = runComposeUiTest {
        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        // Write is disabled because inverse calc needs KFMIOP xAxis (empty on MED17)
        onNodeWithText("Write KFMIRL").assertIsNotEnabled()
    }

    @Test
    fun kfmirlScreenUnconfiguredShowsNotConfigured() = runComposeUiTest {
        KfmiopPreferences.setSelectedMap(null)
        KfmirlPreferences.setSelectedMap(null)

        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        val notConfiguredNodes = onAllNodesWithText("Not configured").fetchSemanticsNodes()
        assertTrue(
            notConfiguredNodes.size >= 2,
            "Should show 'Not configured' for both KFMIOP and KFMIRL"
        )
        onNodeWithText("Write KFMIRL").assertIsNotEnabled()
    }

    @Test
    fun kfmirlScreenShowsDs1Banner() = runComposeUiTest {
        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        onNodeWithText("DS1 Note", substring = true).assertExists()
    }

    @Test
    fun kfmirlScreenShowsComparisonTabs() = runComposeUiTest {
        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        onNodeWithText("KFMIOP (Input)").assertExists()
        onNodeWithText("KFMIRL (Comparison)").assertExists()
        onNodeWithText("Load Comparison").assertExists()
    }
}
