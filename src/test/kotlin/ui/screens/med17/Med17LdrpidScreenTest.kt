package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Compose UI test for the LDRPID screen with real MED17 data.
 */
@OptIn(ExperimentalTestApi::class)
class Med17LdrpidScreenTest : Med17ScreenTestBase() {

    @Test
    fun ldrpidScreenRendersWithMaps() = runComposeUiTest {
        val kfldrlPair = findMap(KFLDRL_TITLE)
        assertNotNull(kfldrlPair, "KFLDRL map '$KFLDRL_TITLE' not found")

        val kfldimxPair = findMapContaining("LDR I controller limitation map")
        assertNotNull(kfldimxPair, "KFLDIMX map not found")

        KfldrlPreferences.setSelectedMap(kfldrlPair.first)
        KfldimxPreferences.setSelectedMap(kfldimxPair.first)

        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        // Title
        onNodeWithText("LDRPID", substring = true).assertExists()

        // Platform-aware button label for MED17
        onNodeWithText("Load ScorpionEFI Logs").assertExists()

        // Tab structure
        onNodeWithText("Boost Tables").assertExists()

        // KFLDRL and KFLDIMX tabs (exact text from semantics tree)
        assertTrue(
            onAllNodesWithText("KFLDRL", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            onAllNodesWithText("KFLDIMX", substring = true).fetchSemanticsNodes().isNotEmpty()
        )

        // Write buttons
        onNodeWithText("Write KFLDRL").assertExists()
        onNodeWithText("Write KFLDIMX").assertExists()
    }

    @Test
    fun ldrpidScreenRendersWithoutMaps() = runComposeUiTest {
        KfldrlPreferences.setSelectedMap(null)
        KfldimxPreferences.setSelectedMap(null)

        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        // Should not crash; show "Not configured" state
        onNodeWithText("Load ScorpionEFI Logs").assertExists()
        onNodeWithText("Write KFLDRL").assertExists()
        onNodeWithText("Write KFLDIMX").assertExists()
    }

    @Test
    fun ldrpidShowsEmptyBoostState() = runComposeUiTest {
        KfldrlPreferences.setSelectedMap(null)
        KfldimxPreferences.setSelectedMap(null)

        setContent {
            ui.screens.ldrpid.LdrpidScreen()
        }

        // Shows empty state text
        onNodeWithText("No boost data loaded").assertExists()
    }
}
