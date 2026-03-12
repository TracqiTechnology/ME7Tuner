package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfzw.KfzwPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Compose UI test for the KFZW screen with real MED17 data.
 */
@OptIn(ExperimentalTestApi::class)
class Med17KfzwScreenTest : Med17ScreenTestBase() {

    @Test
    fun kfzwScreenRendersWithBothMaps() = runComposeUiTest {
        // Find the largest ignition-related map for KFZW
        val kfzwPair = findLargestMapContaining("Delta ignition during warm-up")
            ?: findLargestMapContaining("ignition")
        assertNotNull(kfzwPair, "KFZW ignition map not found")

        val kfmiopPair = findMap(KFMIOP_TITLE)
        assertNotNull(kfmiopPair, "KFMIOP map not found")

        KfzwPreferences.setSelectedMap(kfzwPair.first)
        KfmiopPreferences.setSelectedMap(kfmiopPair.first)

        setContent {
            ui.screens.kfzw.KfzwScreen()
        }

        // DS1 banner
        onNodeWithText("DS1 Note", substring = true).assertExists()

        // Calculator title
        onNodeWithText("KFZW Calculator").assertExists()

        // Comparison tabs
        onNodeWithText("KFZW (Input)").assertExists()
        onNodeWithText("KFZW (Comparison)").assertExists()
        onNodeWithText("Timing Comparison").assertExists()

        // Write button
        onNodeWithText("Write KFZW").assertExists()
    }

    @Test
    fun kfzwScreenRendersWithoutMaps() = runComposeUiTest {
        KfzwPreferences.setSelectedMap(null)
        KfmiopPreferences.setSelectedMap(null)

        setContent {
            ui.screens.kfzw.KfzwScreen()
        }

        // Should not crash
        onNodeWithText("KFZW Calculator").assertExists()
        onNodeWithText("Write KFZW").assertExists()
    }
}
