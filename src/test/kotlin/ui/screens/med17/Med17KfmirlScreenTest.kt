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
 * On MED17 (DS1), KFMIOP is a 1×1 scalar. KFMIRL is a full 2D table (14×16).
 * The screen detects scalar KFMIOP and uses KFMIRL's own xAxis for rescaling
 * instead of requiring KFMIOP's (empty) xAxis. Write now works.
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
        assertTrue(map.xAxis.size > 1 && map.yAxis.size > 1,
            "KFMIRL should be a 2D map, got x=${map.xAxis.size} y=${map.yAxis.size}")
    }

    @Test
    fun kfmirlScreenConfiguredShowsNoNotConfigured() = runComposeUiTest {
        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun kfmirlScalarModeShowsRescaleUI() = runComposeUiTest {
        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        // DS1 scalar mode shows the rescale configuration
        onNodeWithText("Rescale Load Axis", substring = true).assertExists()
        onNodeWithText("Target Max Load", substring = true).assertExists()
    }

    @Test
    fun kfmirlWriteButtonEnabledWithScalarKfmiop() = runComposeUiTest {
        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        // Write should now be enabled — DS1 path uses KFMIRL's own xAxis
        onNodeWithText("Write KFMIRL").assertIsEnabled()
    }

    @Test
    fun kfmirlWriteProducesValidBinaryOutput() = runComposeUiTest {
        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        val kfmirlPair = KfmirlPreferences.getSelectedMap()!!
        val address = kfmirlPair.first.zAxis.address.toLong()
        val stride = kfmirlPair.first.zAxis.sizeBits / 8
        val totalCells = kfmirlPair.second.zAxis.sumOf { it.size }

        // Click Write
        onNodeWithText("Write KFMIRL").performClick()
        onNodeWithText("Are you sure you want to write KFMIRL to the binary?").assertExists()
        onNodeWithText("Yes").performClick()
        waitForIdle()

        val newBytes = readBinBytes(address, totalCells * stride)
        assertTrue(
            newBytes.any { it != 0.toByte() },
            "Written KFMIRL bytes should be non-zero at address $address"
        )
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
            notConfiguredNodes.isNotEmpty(),
            "Should show 'Not configured' when KFMIRL is not set"
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
}
