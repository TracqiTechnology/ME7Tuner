package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfzwop.KfzwopPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Compose UI test for the KFZWOP screen with real MED17 data.
 */
@OptIn(ExperimentalTestApi::class)
class Med17KfzwopScreenTest : Med17ScreenTestBase() {

    @Test
    fun kfzwopScreenRendersWithMap() = runComposeUiTest {
        // Find the largest optimal ignition map
        val kfzwopPair = findLargestMapContaining("Opt model ref ignition")
            ?: findLargestMapContaining("opt model ref ignition")
        assertNotNull(kfzwopPair, "KFZWOP ignition map not found")
        KfzwopPreferences.setSelectedMap(kfzwopPair.first)

        setContent {
            ui.screens.kfzwop.KfzwopScreen()
        }

        // DS1 banner
        onNodeWithText("DS1 Note", substring = true).assertExists()

        // Calculator title
        onNodeWithText("KFZWOP Calculator").assertExists()

        // Editable load axis label
        onNodeWithText("KFZWOP Load Axis (Editable)").assertExists()

        // Map name visible
        assertTrue(
            onAllNodesWithText(kfzwopPair.first.tableName, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )

        // Comparison tabs
        onNodeWithText("KFZWOP (Input)").assertExists()
        onNodeWithText("KFZWOP (Comparison)").assertExists()
        onNodeWithText("Timing Comparison").assertExists()

        // Write button
        onNodeWithText("Write KFZWOP").assertExists()
    }

    @Test
    fun kfzwopScreenRendersWithoutMap() = runComposeUiTest {
        KfzwopPreferences.setSelectedMap(null)

        setContent {
            ui.screens.kfzwop.KfzwopScreen()
        }

        // Should not crash
        onNodeWithText("KFZWOP Calculator").assertExists()
        onNodeWithText("Write KFZWOP").assertExists()
    }
}
