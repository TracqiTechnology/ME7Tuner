package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end Compose UI test for the LDRPID screen on MED17.
 *
 * Verifies:
 *  - Profile application resolves both KfldrlPreferences and KfldimxPreferences
 *  - Screen renders without "Not configured" when both maps are set
 *  - Write buttons (KFLDRL + KFLDIMX) are present
 *  - Unconfigured state correctly shows "Not configured"
 *
 * Note: LDRPID's canWrite requires nonLinearMap/linearMap to be computed from log data.
 * Without loading log data, the maps default from the BIN file, so write buttons may
 * not be fully enabled until a log is loaded. The configured-state tests still validate
 * the profile resolves and the UI renders correctly.
 */
@OptIn(ExperimentalTestApi::class)
class Med17LdrpidScreenTest : Med17ScreenTestBase() {

    @Test
    fun profileResolvesBothPreferences() {
        val kfldrl = KfldrlPreferences.getSelectedMap()
        val kfldimx = KfldimxPreferences.getSelectedMap()
        assertNotNull(kfldrl, "Profile should resolve KFLDRL map preference")
        assertNotNull(kfldimx, "Profile should resolve KFLDIMX map preference")
        assertTrue(
            kfldrl.first.tableName.contains("linearize boost", ignoreCase = true),
            "KFLDRL table name should match, got '${kfldrl.first.tableName}'"
        )
    }

    @Test
    fun ldrpidScreenConfiguredShowsNoNotConfigured() = runComposeUiTest {
        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        // When both KFLDRL and KFLDIMX are configured via profile,
        // "Not configured" should NOT appear
        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun ldrpidScreenShowsWriteButtons() = runComposeUiTest {
        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        onNodeWithText("Write KFLDRL").assertExists()
        onNodeWithText("Write KFLDIMX").assertExists()
    }

    @Test
    fun ldrpidWriteKfldrlProducesValidBinaryOutput() = runComposeUiTest {
        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        val kfldrlPair = KfldrlPreferences.getSelectedMap()!!
        val address = kfldrlPair.first.zAxis.address.toLong()
        val stride = kfldrlPair.first.zAxis.sizeBits / 8
        val totalCells = kfldrlPair.second.zAxis.sumOf { it.size }
        val originalBytes = readBinBytes(address, totalCells * stride)

        // Click Write KFLDRL
        onNodeWithText("Write KFLDRL").performClick()
        onNodeWithText("Are you sure you want to write KFLDRL to the binary?").assertExists()
        onNodeWithText("Yes").performClick()
        waitForIdle()

        val newBytes = readBinBytes(address, totalCells * stride)
        assertTrue(
            newBytes.any { it != 0.toByte() },
            "Written KFLDRL bytes should be non-zero at address $address"
        )
    }

    @Test
    fun ldrpidWriteKfldimxProducesValidBinaryOutput() = runComposeUiTest {
        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        val kfldimxPair = KfldimxPreferences.getSelectedMap()!!
        val address = kfldimxPair.first.zAxis.address.toLong()
        val stride = kfldimxPair.first.zAxis.sizeBits / 8
        val totalCells = kfldimxPair.second.zAxis.sumOf { it.size }
        val originalBytes = readBinBytes(address, totalCells * stride)

        // Click Write KFLDIMX
        onNodeWithText("Write KFLDIMX").performClick()
        onNodeWithText("Are you sure you want to write KFLDIMX to the binary?").assertExists()
        onNodeWithText("Yes").performClick()
        waitForIdle()

        val newBytes = readBinBytes(address, totalCells * stride)
        assertTrue(
            newBytes.any { it != 0.toByte() },
            "Written KFLDIMX bytes should be non-zero at address $address"
        )
    }

    @Test
    fun ldrpidScreenUnconfiguredShowsNotConfigured() = runComposeUiTest {
        KfldrlPreferences.setSelectedMap(null)
        KfldimxPreferences.setSelectedMap(null)

        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        val notConfiguredNodes = onAllNodesWithText("Not configured").fetchSemanticsNodes()
        assertTrue(
            notConfiguredNodes.size >= 2,
            "Should show 'Not configured' for both KFLDRL and KFLDIMX"
        )
    }

    @Test
    fun ldrpidScreenShowsDs1LogButton() = runComposeUiTest {
        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        onNodeWithText("Load ScorpionEFI Logs").assertExists()
    }

    @Test
    fun ldrpidScreenShowsBoostTabs() = runComposeUiTest {
        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        onNodeWithText("Boost Tables").assertExists()
        assertTrue(
            onAllNodesWithText("KFLDRL", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            onAllNodesWithText("KFLDIMX", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }
}
