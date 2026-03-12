package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfmirl.KfmirlPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Compose UI test for the KFMIRL screen with real MED17 data.
 */
@OptIn(ExperimentalTestApi::class)
class Med17KfmirlScreenTest : Med17ScreenTestBase() {

    @Test
    fun kfmirlScreenRendersWithBothMaps() = runComposeUiTest {
        val kfmiopPair = findMap(KFMIOP_TITLE)
        assertNotNull(kfmiopPair, "KFMIOP map not found")
        val kfmirlPair = findMapContaining("Tgt filling")
        assertNotNull(kfmirlPair, "KFMIRL map not found")

        KfmiopPreferences.setSelectedMap(kfmiopPair.first)
        KfmirlPreferences.setSelectedMap(kfmirlPair.first)

        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        // DS1 banner
        onNodeWithText("DS1 Note", substring = true).assertExists()

        // Calculator title
        onNodeWithText("KFMIRL Calculator").assertExists()

        // Map config labels
        onNodeWithText("KFMIOP (Input):").assertExists()
        onNodeWithText("KFMIRL (Target):").assertExists()

        // Comparison tabs
        onNodeWithText("KFMIOP (Input)").assertExists()
        onNodeWithText("KFMIRL (Comparison)").assertExists()
        onNodeWithText("Load Comparison").assertExists()

        // Write button
        onNodeWithText("Write KFMIRL").assertExists()

        // Map names visible (may appear multiple times)
        assertTrue(
            onAllNodesWithText("Tgt filling", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun kfmirlScreenRendersWithoutMaps() = runComposeUiTest {
        KfmiopPreferences.setSelectedMap(null)
        KfmirlPreferences.setSelectedMap(null)

        setContent {
            ui.screens.kfmirl.KfmirlScreen()
        }

        // Should not crash
        onNodeWithText("KFMIRL Calculator").assertExists()
        onNodeWithText("Write KFMIRL").assertExists()
    }
}
