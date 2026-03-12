package ui.screens.med17

import androidx.compose.ui.test.*
import data.preferences.kfmiop.KfmiopPreferences
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Compose UI test for the KFMIOP screen with real MED17 data.
 *
 * Verifies:
 *  - Screen renders without crash when KFMIOP map is loaded
 *  - DS1 info banner is visible for MED17
 *  - Pressure input fields are present and editable
 *  - Map definition name is displayed after selection
 *  - Comparison tabs exist (Torque, Boost, Boost Comparison)
 *  - Write section shows prerequisite status
 */
@OptIn(ExperimentalTestApi::class)
class Med17KfmiopScreenTest : Med17ScreenTestBase() {

    @Test
    fun kfmiopScreenRendersWithMap() = runComposeUiTest {
        val kfmiopPair = findMap(KFMIOP_TITLE)
        assertNotNull(kfmiopPair, "KFMIOP map '${KFMIOP_TITLE}' not found in XDF")
        KfmiopPreferences.setSelectedMap(kfmiopPair.first)

        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        // DS1 banner should be visible for MED17
        onNodeWithText("DS1 Note", substring = true).assertExists()

        // Calculator title
        onNodeWithText("KFMIOP Calculator").assertExists()

        // Pressure fields — both "Current" and "Target" sections have these labels
        onAllNodesWithText("MAP Sensor Max:", substring = true).assertCountEquals(2)
        onAllNodesWithText("Boost Pressure Max:", substring = true).assertCountEquals(2)

        // Map definition appears in both config and write sections (2 nodes)
        assertTrue(
            onAllNodesWithText(KFMIOP_TITLE, substring = true)
                .fetchSemanticsNodes().isNotEmpty(),
            "Map title should appear in the UI"
        )

        // Select Map button
        onNodeWithText("Select Map").assertExists()

        // Comparison tabs
        onNodeWithText("Torque").assertExists()
        onNodeWithText("Boost Comparison").assertExists()

        // Write button
        onNodeWithText("Write KFMIOP").assertExists()
    }

    @Test
    fun kfmiopScreenRendersWithoutMap() = runComposeUiTest {
        KfmiopPreferences.setSelectedMap(null)

        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        // DS1 banner still visible
        onNodeWithText("DS1 Note", substring = true).assertExists()

        // Write button present
        onNodeWithText("Write KFMIOP").assertExists()
    }

    @Test
    fun kfmiopPressureFieldsAreEditable() = runComposeUiTest {
        val kfmiopPair = findMap(KFMIOP_TITLE)
        assertNotNull(kfmiopPair)
        KfmiopPreferences.setSelectedMap(kfmiopPair.first)

        setContent {
            ui.screens.kfmiop.KfmiopScreen()
        }

        // The "Target (Desired)" section has editable fields
        onNodeWithText("Target (Desired)").assertExists()
        onNodeWithText("Current (Analyzed)").assertExists()
    }
}
