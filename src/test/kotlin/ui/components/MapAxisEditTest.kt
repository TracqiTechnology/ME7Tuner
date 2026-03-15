package ui.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies that MapAxis cells marked editable actually accept keyboard input.
 *
 * Regression test for focus-stealing bug where the cell Box's implicit focusable
 * (from Modifier.clickable) competed with the BasicTextField, causing the
 * TextField to lose focus immediately on appearance.
 */
@OptIn(ExperimentalTestApi::class)
class MapAxisEditTest {

    @Test
    fun editableAxisCellAcceptsInput() = runComposeUiTest {
        var receivedData: Array<Array<Double>>? = null

        setContent {
            MapAxis(
                data = arrayOf(arrayOf(10.0, 20.0, 30.0)),
                editable = true,
                onDataChanged = { receivedData = it }
            )
        }

        val cell = onNodeWithTag("axis_cell_0_1")
        cell.assertExists()

        // First click selects the cell
        cell.performClick()
        waitForIdle()

        // Second click enters edit mode
        cell.performClick()
        waitForIdle()

        // The BasicTextField should now be visible
        val textFields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        assertTrue(textFields.isNotEmpty(), "BasicTextField should appear after click-to-select then click-to-edit")

        onNode(hasSetTextAction()).performTextReplacement("25.0")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }

        // Advance past the 100ms debounce
        waitForIdle()
        mainClock.advanceTimeBy(200)
        waitForIdle()

        assertNotNull(receivedData, "onDataChanged should have been called")
        assertEquals(25.0, receivedData!![0][1], "Cell [0][1] should be updated to 25.0")
    }

    @Test
    fun nonEditableAxisCellDoesNotEnterEditMode() = runComposeUiTest {
        setContent {
            MapAxis(
                data = arrayOf(arrayOf(10.0, 20.0, 30.0)),
                editable = false
            )
        }

        val cell = onNodeWithTag("axis_cell_0_1")
        cell.performClick()
        waitForIdle()
        cell.performClick()
        waitForIdle()

        onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }
}
