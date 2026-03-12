package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfmiop.KfmiopPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end Compose UI test for the KFMIOP screen on MED17.
 *
 * On MED17 (DS1), KFMIOP is a 1×1 scalar (single max load ceiling).
 * The screen now detects scalar KFMIOP and shows a simplified editor
 * with Current/Target value fields and a working Write button.
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

        onAllNodesWithText("Not configured").assertCountEquals(0)
    }

    @Test
    fun kfmiopScalarModeShowsLoadCeiling() = runComposeUiTest {
        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        // Scalar mode shows the max load ceiling UI
        onNodeWithText("Max Load Ceiling", substring = true).assertExists()
        onNodeWithText("Current Value", substring = true).assertExists()
        onNodeWithText("Target Value", substring = true).assertExists()
    }

    @Test
    fun kfmiopWriteButtonEnabledForScalarMode() = runComposeUiTest {
        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        // Scalar mode should have Write enabled (scalarOutputMap is always non-null)
        onNodeWithText("Write KFLMIOP").assertIsEnabled()
    }

    @Test
    fun kfmiopWriteProducesValidBinaryOutput() = runComposeUiTest {
        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        val kfmiopPair = KfmiopPreferences.getSelectedMap()!!
        val address = kfmiopPair.first.zAxis.address.toLong()
        val stride = kfmiopPair.first.zAxis.sizeBits / 8

        // Click Write KFLMIOP
        onNodeWithText("Write KFLMIOP").performClick()
        onNodeWithText("Are you sure you want to write KFLMIOP to the binary?").assertExists()
        onNodeWithText("Yes").performClick()
        waitForIdle()

        val newBytes = readBinBytes(address, stride)
        assertTrue(
            newBytes.any { it != 0.toByte() },
            "Written KFMIOP scalar bytes should be non-zero at address $address"
        )
    }

    @Test
    fun kfmiopScreenUnconfiguredShowsNotConfigured() = runComposeUiTest {
        KfmiopPreferences.setSelectedMap(null)

        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        onNodeWithText("Not configured").assertExists()
        onNodeWithText("Write KFLMIOP").assertIsNotEnabled()
    }

    @Test
    fun kfmiopScreenShowsDs1Banner() = runComposeUiTest {
        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        onNodeWithText("DS1 Note", substring = true).assertExists()
    }
}
