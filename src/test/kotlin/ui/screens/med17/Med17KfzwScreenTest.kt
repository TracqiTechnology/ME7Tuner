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
 * On MED17 (DS1), KFMIOP is a 1×1 scalar. KFZW is a full 2D table (10×10).
 * The screen detects scalar KFMIOP and uses KFZW's own xAxis for rescaling
 * instead of requiring KFMIOP's (empty) xAxis. Write now works.
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
    fun kfzwScalarModeShowsRescaleUI() = runComposeUiTest {
        setContent {
            ui.screens.kfzw.KfzwScreen()
        }

        // DS1 scalar mode shows the rescale configuration
        onNodeWithText("Rescale Load Axis", substring = true).assertExists()
        onNodeWithText("Target Max Load", substring = true).assertExists()
    }

    @Test
    fun kfzwWriteButtonEnabledWithScalarKfmiop() = runComposeUiTest {
        setContent {
            ui.screens.kfzw.KfzwScreen()
        }

        // Write should now be enabled — DS1 path uses KFZW's own xAxis
        onNodeWithText("Write KFZW").assertIsEnabled()
    }

    @Test
    fun kfzwWriteProducesValidBinaryOutput() = runComposeUiTest {
        setContent {
            ui.screens.kfzw.KfzwScreen()
        }

        val kfzwPair = KfzwPreferences.getSelectedMap()!!
        val address = kfzwPair.first.zAxis.address.toLong()
        val stride = kfzwPair.first.zAxis.sizeBits / 8
        val totalCells = kfzwPair.second.zAxis.sumOf { it.size }

        // Click Write
        onNodeWithText("Write KFZW").performClick()
        onNodeWithText("Are you sure you want to write KFZW to the binary?").assertExists()
        onNodeWithText("Yes").performClick()
        waitForIdle()

        val newBytes = readBinBytes(address, totalCells * stride)
        assertTrue(
            newBytes.any { it != 0.toByte() },
            "Written KFZW bytes should be non-zero at address $address"
        )
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
            notConfiguredNodes.isNotEmpty(),
            "Should show 'Not configured' when KFZW is not set"
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
