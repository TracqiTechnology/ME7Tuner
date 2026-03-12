package ui.screens.med17

import androidx.compose.ui.test.*
import kotlin.test.Test

/**
 * Compose UI test for the DualInjection screen with real MED17 data.
 */
@OptIn(ExperimentalTestApi::class)
class Med17DualInjectionScreenTest : Med17ScreenTestBase() {

    @Test
    fun dualInjectionScreenRendersWithTabs() = runComposeUiTest {
        setContent {
            ui.screens.dualinjection.DualInjectionScreen()
        }

        // Three tabs
        onNodeWithText("Port Injector").assertExists()
        onNodeWithText("Direct Injector").assertExists()
        onNodeWithText("Split Calculator").assertExists()
    }

    @Test
    fun portInjectorTabShowsFields() = runComposeUiTest {
        setContent {
            ui.screens.dualinjection.DualInjectionScreen()
        }

        // Port Injector tab is default — shows PFI scaling content
        onNodeWithText("Port Injector (PFI) Scaling").assertExists()

        // Flow rate and pressure fields (from semantics tree)
        onAllNodesWithText("Flow Rate (cc/min)").assertCountEquals(2) // Stock + New
        onAllNodesWithText("Fuel Pressure (bar, gauge)").assertCountEquals(2)

        // Stock/New sections
        onNodeWithText("Stock Port Injector").assertExists()
        onNodeWithText("New Port Injector").assertExists()

        // Calculate button
        onNodeWithText("Calculate Port KRKTE Scale Factor").assertExists()
    }

    @Test
    fun directInjectorTabRenders() = runComposeUiTest {
        setContent {
            ui.screens.dualinjection.DualInjectionScreen()
        }

        // Switch to Direct Injector tab
        onNodeWithText("Direct Injector").performClick()
        waitForIdle()

        // Should show DI scaling content — "Direct Injector (GDI) Scaling" or similar
        onNodeWithText("Direct Injector (GDI) Scaling", substring = true).assertExists()
    }

    @Test
    fun splitCalculatorTabRenders() = runComposeUiTest {
        setContent {
            ui.screens.dualinjection.DualInjectionScreen()
        }

        // Switch to Split Calculator tab
        onNodeWithText("Split Calculator").performClick()
        waitForIdle()

        // Should show split calculator title
        onNodeWithText("RPM-Dependent PFI Split Calculator").assertExists()
    }

    @Test
    fun presetsSection() = runComposeUiTest {
        setContent {
            ui.screens.dualinjection.DualInjectionScreen()
        }

        // Presets section visible on Port Injector tab
        onNodeWithText("Presets").assertExists()
    }
}
