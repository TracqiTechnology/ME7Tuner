package ui.screens.med17

import androidx.compose.ui.test.*
import kotlin.test.Test

/**
 * Compose UI test for the KRKTE screen with real MED17 data.
 */
@OptIn(ExperimentalTestApi::class)
class Med17KrkteScreenTest : Med17ScreenTestBase() {

    @Test
    fun krkteScreenRendersWithDefaults() = runComposeUiTest {
        setContent {
            ui.screens.krkte.KrkteScreen()
        }

        // Title
        onNodeWithText("Calculated KRKTE").assertExists()

        // Parameter fields (with colons as shown in semantics tree)
        onNodeWithText("Air Density:").assertExists()
        onNodeWithText("Engine Displacement:").assertExists()
        onNodeWithText("Number of Cylinders:").assertExists()
        onNodeWithText("Gasoline Density:").assertExists()
        onNodeWithText("Stoichiometric A/F Ratio:").assertExists()
        onNodeWithText("Injector Size:").assertExists()

        // Result value should be present (non-zero)
        onNodeWithText("ms/%").assertExists()

        // Write button
        onNodeWithText("Write KRKTE").assertExists()
    }

    @Test
    fun krkteScreenShowsEnginePresetButton() = runComposeUiTest {
        setContent {
            ui.screens.krkte.KrkteScreen()
        }

        onNodeWithText("Apply Engine Preset").assertExists()
    }

    @Test
    fun krkteScreenShowsFuelPresetButton() = runComposeUiTest {
        setContent {
            ui.screens.krkte.KrkteScreen()
        }

        onNodeWithText("Apply Fuel Preset").assertExists()
    }

    @Test
    fun krkteScreenShowsSectionTitles() = runComposeUiTest {
        setContent {
            ui.screens.krkte.KrkteScreen()
        }

        onNodeWithText("Engine Parameters").assertExists()
        onNodeWithText("Fuel Properties").assertExists()
        onNodeWithText("Fuel Injector").assertExists()
    }
}
