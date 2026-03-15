package ui.screens.med17

import androidx.compose.ui.test.*
import data.model.EcuPlatform
import data.preferences.platform.EcuPlatformPreference
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compose UI test for the PLSOL screen with real MED17 data.
 */
@OptIn(ExperimentalTestApi::class)
class Med17PlsolScreenTest : Med17ScreenTestBase() {

    @Test
    fun plsolScreenRendersWithTabs() = runComposeUiTest {
        setContent {
            ui.screens.plsol.PlsolScreen()
        }

        // Three tabs
        onNodeWithText("Load").assertExists()
        onNodeWithText("Airflow").assertExists()
        onNodeWithText("Power").assertExists()
    }

    @Test
    fun plsolScreenShowsMed17Label() = runComposeUiTest {
        setContent {
            ui.screens.plsol.PlsolScreen()
        }

        // MED17-specific label for KFURL (exact text from semantics tree)
        assertTrue(
            onAllNodesWithText("fupsrl_w", substring = true).fetchSemanticsNodes().isNotEmpty(),
            "MED17-specific fupsrl_w label should be visible"
        )
    }

    @Test
    fun plsolScreenShowsParameterFields() = runComposeUiTest {
        setContent {
            ui.screens.plsol.PlsolScreen()
        }

        // Parameter fields (with colons, as shown in semantics tree)
        onNodeWithText("Barometric Pressure:").assertExists()
        onNodeWithText("Intake Air Temperature:").assertExists()
        onNodeWithText("Displacement:").assertExists()
        onNodeWithText("RPM:").assertExists()
    }

    @Test
    fun plsolScreenTabSwitchWorks() = runComposeUiTest {
        setContent {
            ui.screens.plsol.PlsolScreen()
        }

        // Click Airflow tab
        onNodeWithText("Airflow").performClick()
        waitForIdle()

        // Click Power tab
        onNodeWithText("Power").performClick()
        waitForIdle()

        // Click Load tab (back to start)
        onNodeWithText("Load").performClick()
        waitForIdle()
    }

    @Test
    fun plsolScreenShowsKfurlForMe7() = runComposeUiTest {
        // Temporarily switch to ME7
        EcuPlatformPreference.platform = EcuPlatform.ME7

        setContent {
            ui.screens.plsol.PlsolScreen()
        }

        // ME7 label should be "KFURL:" not "fupsrl_w"
        onNodeWithText("KFURL:", substring = true).assertExists()

        // Restore MED17 for other tests
        EcuPlatformPreference.platform = EcuPlatform.MED17
    }
}
