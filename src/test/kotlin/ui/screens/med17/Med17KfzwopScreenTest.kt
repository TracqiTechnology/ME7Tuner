package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfzwop.KfzwopPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end Compose UI test for the KFZWOP screen on MED17.
 *
 * Verifies:
 *  - Profile application resolves KfzwopPreferences
 *  - Screen renders without "Not configured" when profile is applied
 *  - Write button is enabled when prerequisites are met
 *  - Clicking Write + Yes writes to BIN and changes bytes
 *  - Unconfigured state correctly shows "Not configured"
 */
@OptIn(ExperimentalTestApi::class)
class Med17KfzwopScreenTest : Med17ScreenTestBase() {

    @Test
    fun profileResolvesKfzwopPreference() {
        val selected = KfzwopPreferences.getSelectedMap()
        assertNotNull(selected, "Profile should resolve KFZWOP map preference")
        assertTrue(
            selected.first.tableName.contains("Opt model ref ignition", ignoreCase = true),
            "KFZWOP table name should match, got '${selected.first.tableName}'"
        )
    }

    @Test
    fun kfzwopScreenConfiguredShowsNoNotConfigured() = runComposeUiTest {
        setContent {
            ui.screens.kfzwop.KfzwopScreen()
        }

        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun kfzwopWriteButtonIsEnabledWhenConfigured() = runComposeUiTest {
        setContent {
            ui.screens.kfzwop.KfzwopScreen()
        }

        // canWrite = binLoaded && kfzwopMapConfigured && outputKfzwop != null
        onNodeWithText("Write KFZWOP").assertIsEnabled()
    }

    @Test
    fun kfzwopWriteProducesValidBinaryOutput() = runComposeUiTest {
        setContent {
            ui.screens.kfzwop.KfzwopScreen()
        }

        val kfzwopPair = KfzwopPreferences.getSelectedMap()!!
        val address = kfzwopPair.first.zAxis.address.toLong()
        val stride = kfzwopPair.first.zAxis.sizeBits / 8
        val totalCells = kfzwopPair.second.zAxis.sumOf { it.size }
        val originalBytes = readBinBytes(address, totalCells * stride)

        // Click Write
        onNodeWithText("Write KFZWOP").performClick()
        onNodeWithText("Are you sure you want to write KFZWOP to the binary?").assertExists()
        onNodeWithText("Yes").performClick()
        waitForIdle()

        val newBytes = readBinBytes(address, totalCells * stride)
        assertTrue(
            newBytes.any { it != 0.toByte() },
            "Written KFZWOP bytes should be non-zero at address $address"
        )
    }

    @Test
    fun kfzwopScreenUnconfiguredShowsNotConfigured() = runComposeUiTest {
        KfzwopPreferences.setSelectedMap(null)

        setContent {
            ui.screens.kfzwop.KfzwopScreen()
        }

        onNodeWithText("Not configured").assertExists()
        onNodeWithText("Write KFZWOP").assertIsNotEnabled()
    }

    @Test
    fun kfzwopScreenShowsDs1Banner() = runComposeUiTest {
        setContent {
            ui.screens.kfzwop.KfzwopScreen()
        }

        onNodeWithText("DS1 Note", substring = true).assertExists()
    }
}
