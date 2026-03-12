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
 * With the corrected profile (KFMIOP → "Opt eng tq"), KFMIOP is a full 14×16
 * 2D table on standard (non-DS1) XDFs.  KFMIRL uses the ME7-style inverse
 * calculator path: it reads KFMIOP's x-axis (load axis) and computes the
 * inverse relationship to generate output KFMIRL values.
 *
 * When a DS1 XDF is loaded where KFMIOP is collapsed to a 1×1 scalar,
 * the scalar detection automatically switches to the self-axis rescale path.
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
        setContent { ui.screens.kfmirl.KfmirlScreen() }
        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun kfmirlScreenShowsSelectMap() = runComposeUiTest {
        setContent { ui.screens.kfmirl.KfmirlScreen() }
        // 2D mode: Select Map buttons are visible for KFMIOP and KFMIRL
        val selectMapNodes = onAllNodesWithText("Select Map").fetchSemanticsNodes()
        assertTrue(
            selectMapNodes.size >= 1,
            "Expected at least 1 'Select Map' button, got ${selectMapNodes.size}"
        )
    }

    @Test
    fun kfmirlWriteButtonEnabled() = runComposeUiTest {
        setContent { ui.screens.kfmirl.KfmirlScreen() }
        onNodeWithText("Write KFLMIRL").assertIsEnabled()
    }

    @Test
    fun kfmirlWriteProducesValidBinaryOutput() = runComposeUiTest {
        setContent { ui.screens.kfmirl.KfmirlScreen() }

        val kfmirlPair = KfmirlPreferences.getSelectedMap()!!
        val address = kfmirlPair.first.zAxis.address.toLong()
        val stride = kfmirlPair.first.zAxis.sizeBits / 8
        val totalCells = kfmirlPair.second.zAxis.sumOf { it.size }

        // Click Write
        onNodeWithText("Write KFLMIRL").performClick()
        onNodeWithText("Are you sure you want to write KFLMIRL to the binary?").assertExists()
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
        setContent { ui.screens.kfmirl.KfmirlScreen() }

        val notConfiguredNodes = onAllNodesWithText("Not configured").fetchSemanticsNodes()
        assertTrue(
            notConfiguredNodes.isNotEmpty(),
            "Should show 'Not configured' when KFMIRL is not set"
        )
        onNodeWithText("Write KFLMIRL").assertIsNotEnabled()
    }

    @Test
    fun kfmirlScreenShowsDs1Banner() = runComposeUiTest {
        setContent { ui.screens.kfmirl.KfmirlScreen() }
        onNodeWithText("DS1 Note", substring = true).assertExists()
    }

    @Test
    fun kfmirlScreenShowsPlatformAwareLabel() = runComposeUiTest {
        setContent { ui.screens.kfmirl.KfmirlScreen() }
        // MED17 platform should show KFLMIRL consistently
        onAllNodesWithText("KFLMIRL", substring = true)
            .fetchSemanticsNodes().isNotEmpty().let {
                assertTrue(it, "MED17 KFMIRL screen should display KFLMIRL labels")
            }
    }
}
