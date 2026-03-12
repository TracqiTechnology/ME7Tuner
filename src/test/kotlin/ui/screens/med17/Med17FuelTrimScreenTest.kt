package ui.screens.med17

import androidx.compose.ui.test.*
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compose UI test for the FuelTrim screen with real MED17 data.
 */
@OptIn(ExperimentalTestApi::class)
class Med17FuelTrimScreenTest : Med17ScreenTestBase() {

    @Test
    fun fuelTrimScreenRendersEmptyState() = runComposeUiTest {
        setContent {
            ui.screens.fueltrim.FuelTrimScreen()
        }

        // Main title
        onNodeWithText("Fuel Trim Analysis", substring = true).assertExists()

        // Load button
        onNodeWithText("Load Fuel Trim Logs").assertExists()
    }

    @Test
    fun fuelTrimScreenShowsDescription() = runComposeUiTest {
        setContent {
            ui.screens.fueltrim.FuelTrimScreen()
        }

        // Description text about fuel trim analysis
        assertTrue(
            onAllNodesWithText("fuel trim", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty(),
            "Fuel trim description should be visible"
        )
    }
}
