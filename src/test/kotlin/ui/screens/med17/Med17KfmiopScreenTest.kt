package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfmiop.KfmiopPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * End-to-end Compose UI test for the KFMIOP screen on MED17.
 *
 * With the corrected profile mapping (KFMIOP → "Opt eng tq"), KFMIOP is a
 * 14×16 2D table on standard (non-DS1) XDFs.  The full pressure calculator
 * and comparison area are shown, just like ME7.
 *
 * When a DS1 XDF is loaded where DS1 has collapsed KFMIOP to a 1×1 scalar,
 * the isScalar detection still triggers the simplified scalar UI automatically.
 */
@OptIn(ExperimentalTestApi::class)
class Med17KfmiopScreenTest : Med17ScreenTestBase() {

    @Test
    fun profileResolvesKfmiopPreference() {
        val selected = KfmiopPreferences.getSelectedMap()
        assertNotNull(selected, "Profile should resolve KFMIOP map preference")
        assertEquals(
            KFMIOP_TITLE, selected.first.tableName,
            "KFMIOP table name should be '$KFMIOP_TITLE', got '${selected.first.tableName}'"
        )
    }

    @Test
    fun kfmiopMapIs2DOnStandardXdf() {
        val selected = KfmiopPreferences.getSelectedMap()!!
        val map = selected.second
        assertTrue(
            map.xAxis.isNotEmpty() && map.yAxis.isNotEmpty(),
            "KFMIOP on a standard (non-DS1) XDF should be 2D, got x=${map.xAxis.size} y=${map.yAxis.size}"
        )
        // 404E has 14 x-axis breakpoints (load) and 16 y-axis breakpoints (RPM)
        assertEquals(14, map.xAxis.size, "KFMIOP x-axis (load) should have 14 breakpoints")
        assertEquals(16, map.yAxis.size, "KFMIOP y-axis (RPM) should have 16 breakpoints")
    }

    @Test
    fun kfmiopScreenConfiguredShowsNoNotConfigured() = runComposeUiTest {
        setContent { ui.screens.kfmiop.KfmiopScreen() }
        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun kfmiopScreen2DModeShowsSelectMap() = runComposeUiTest {
        setContent { ui.screens.kfmiop.KfmiopScreen() }
        // 2D mode: Select Map button is visible for manual map selection
        onAllNodesWithText("Select Map").assertCountEquals(1)
    }

    @Test
    fun kfmiopScreen2DModeShowsPressureCalculator() = runComposeUiTest {
        setContent { ui.screens.kfmiop.KfmiopScreen() }
        // 2D mode shows the MAP sensor / boost pressure fields (current + target columns)
        assertTrue(
            onAllNodesWithText("MAP Sensor Max:", substring = true).fetchSemanticsNodes().isNotEmpty(),
            "Should show MAP Sensor Max fields"
        )
        assertTrue(
            onAllNodesWithText("Boost Pressure Max:", substring = true).fetchSemanticsNodes().isNotEmpty(),
            "Should show Boost Pressure Max fields"
        )
    }

    @Test
    fun kfmiopWriteButtonEnabledFor2DMode() = runComposeUiTest {
        setContent { ui.screens.kfmiop.KfmiopScreen() }
        // 2D mode: write button uses the platform-aware label
        onNodeWithText("Write KFLMIOP").assertIsEnabled()
    }

    @Test
    fun kfmiopWriteProducesValidBinaryOutput() = runComposeUiTest {
        setContent { ui.screens.kfmiop.KfmiopScreen() }

        val kfmiopPair = KfmiopPreferences.getSelectedMap()!!
        val address = kfmiopPair.first.zAxis.address.toLong()
        val totalBytes = (kfmiopPair.first.zAxis.sizeBits / 8) *
            kfmiopPair.second.xAxis.size * kfmiopPair.second.yAxis.size

        // Click Write KFLMIOP
        onNodeWithText("Write KFLMIOP").performClick()
        onNodeWithText("Are you sure you want to write KFLMIOP to the binary?").assertExists()
        onNodeWithText("Yes").performClick()
        waitForIdle()

        val newBytes = readBinBytes(address, totalBytes)
        assertTrue(
            newBytes.any { it != 0.toByte() },
            "Written KFMIOP 2D table bytes should be non-zero at address $address"
        )
    }

    @Test
    fun kfmiopScreenUnconfiguredShowsNotConfigured() = runComposeUiTest {
        KfmiopPreferences.setSelectedMap(null)
        setContent { ui.screens.kfmiop.KfmiopScreen() }
        onNodeWithText("Not configured").assertExists()
        onNodeWithText("Write KFLMIOP").assertIsNotEnabled()
    }

    @Test
    fun kfmiopScreenShowsDs1Banner() = runComposeUiTest {
        setContent { ui.screens.kfmiop.KfmiopScreen() }
        onNodeWithText("DS1 Note", substring = true).assertExists()
    }

    @Test
    fun kfmiopScreenShowsPlatformAwareLabel() = runComposeUiTest {
        setContent { ui.screens.kfmiop.KfmiopScreen() }
        // MED17 platform shows KFLMIOP in banner, write section, etc.
        val nodes = onAllNodesWithText("KFLMIOP", substring = true)
        // At least: DS1 banner mention, write prereq label, write button
        assertTrue(
            nodes.fetchSemanticsNodes().size >= 3,
            "Expected at least 3 nodes containing 'KFLMIOP', got ${nodes.fetchSemanticsNodes().size}"
        )
    }
}
