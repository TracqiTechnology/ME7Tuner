package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfzw.KfzwPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end Compose UI test for the KFZW screen on MED17.
 *
 * On MED17 (DS1), KFMIOP is a 1×1 scalar. KFZW is a full 2D table (10×10),
 * but the rescaling calculation uses KFMIOP's xAxis as the edit axis, which
 * is empty. So outputKfzw = null and Write is disabled. Tests verify:
 *  - Profile resolves both preferences
 *  - "Not configured" does NOT appear (both maps ARE set)
 *  - Write button is disabled (rescale calc returns null)
 */
@OptIn(ExperimentalTestApi::class)
class Med17KfzwScreenTest : Med17ScreenTestBase() {

    @Test
    fun profileResolvesBothPreferences() {
        val kfzw = KfzwPreferences.getSelectedMap()
        val kfmiop = KfmiopPreferences.getSelectedMap()
        assertNotNull(kfzw, "Profile should resolve KFZW map preference")
        assertNotNull(kfmiop, "Profile should resolve KFMIOP map preference")
        assertTrue(
            kfzw.first.tableName.contains("Delta ignition", ignoreCase = true),
            "KFZW table name should match, got '${kfzw.first.tableName}'"
        )
    }

    @Test
    fun kfzwMapIs2dOnMed17() {
        val selected = KfzwPreferences.getSelectedMap()!!
        val map = selected.second
        assertTrue(map.xAxis.size > 1 && map.yAxis.size > 1,
            "KFZW should be a 2D map, got x=${map.xAxis.size} y=${map.yAxis.size}")
    }

    @Test
    fun kfzwScreenConfiguredShowsNoNotConfigured() = runComposeUiTest {
        setContent {
            ui.screens.kfzw.KfzwScreen()
        }

        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun kfzwWriteButtonDisabledDueToScalarKfmiop() = runComposeUiTest {
        setContent {
            ui.screens.kfzw.KfzwScreen()
        }

        // Write is disabled because rescale uses KFMIOP xAxis (empty on MED17)
        onNodeWithText("Write KFZW").assertIsNotEnabled()
    }

    @Test
    fun kfzwScreenUnconfiguredShowsNotConfigured() = runComposeUiTest {
        KfzwPreferences.setSelectedMap(null)
        KfmiopPreferences.setSelectedMap(null)

        setContent {
            ui.screens.kfzw.KfzwScreen()
        }

        val notConfiguredNodes = onAllNodesWithText("Not configured").fetchSemanticsNodes()
        assertTrue(
            notConfiguredNodes.size >= 2,
            "Should show 'Not configured' for both KFZW and KFMIOP"
        )
        onNodeWithText("Write KFZW").assertIsNotEnabled()
    }

    @Test
    fun kfzwScreenShowsDs1Banner() = runComposeUiTest {
        setContent {
            ui.screens.kfzw.KfzwScreen()
        }

        onNodeWithText("DS1 Note", substring = true).assertExists()
    }
}
