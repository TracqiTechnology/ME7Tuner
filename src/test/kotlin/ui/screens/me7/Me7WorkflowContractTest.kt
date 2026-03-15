package ui.screens.me7

import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfmirl.KfmirlPreferences
import data.preferences.kfvpdksd.KfvpdksdPreferences
import data.preferences.kfzw.KfzwPreferences
import data.preferences.kfzwop.KfzwopPreferences
import data.preferences.mlhfm.MlhfmPreferences
import domain.math.Inverse
import domain.math.RescaleAxis
import domain.model.kfmiop.Kfmiop
import domain.model.kfzw.Kfzw
import domain.model.rlsol.Rlsol
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Workflow-level integration test for ME7.
 *
 * Validates the full pipeline for ME7: profile apply → preference resolve →
 * dimension contract → mode detection → compute validity. Mirrors
 * [Med17WorkflowContractTest] for parity.
 */
class Me7WorkflowContractTest : Me7TestBase() {

    // ── Dimension Contracts ─────────────────────────────────────────

    @Test
    fun `KFMIOP is 2D with RPM x load axes`() {
        val kfmiop = KfmiopPreferences.getSelectedMap()
        assertNotNull(kfmiop, "KFMIOP should resolve")
        assertTrue(kfmiop.second.xAxis.size > 1, "KFMIOP should be 2D (x)")
        assertTrue(kfmiop.second.yAxis.size > 1, "KFMIOP should be 2D (y)")
    }

    @Test
    fun `KFMIRL is 2D`() {
        val kfmirl = KfmirlPreferences.getSelectedMap()
        assertNotNull(kfmirl, "KFMIRL should resolve")
        assertTrue(kfmirl.second.xAxis.size > 1, "KFMIRL should be 2D (x)")
        assertTrue(kfmirl.second.yAxis.size > 1, "KFMIRL should be 2D (y)")
    }

    @Test
    fun `KFZWOP is 2D ignition table`() {
        val kfzwop = KfzwopPreferences.getSelectedMap()
        assertNotNull(kfzwop, "KFZWOP should resolve")
        assertTrue(kfzwop.second.xAxis.size > 1 && kfzwop.second.yAxis.size > 1,
            "KFZWOP should be 2D")
    }

    @Test
    fun `KFZW is 2D ignition table`() {
        val kfzw = KfzwPreferences.getSelectedMap()
        assertNotNull(kfzw, "KFZW should resolve")
        assertTrue(kfzw.second.xAxis.size > 1 && kfzw.second.yAxis.size > 1,
            "KFZW should be 2D")
    }

    @Test
    fun `MLHFM resolves and has data`() {
        val mlhfm = MlhfmPreferences.getSelectedMap()
        assertNotNull(mlhfm, "MLHFM should resolve")
        val map = mlhfm.second
        // MLHFM is a 1D curve: yAxis=voltage, zAxis[i][0]=kg/h per voltage point
        assertTrue(map.zAxis.size > 1,
            "MLHFM should have multiple voltage points, got ${map.zAxis.size}")
    }

    @Test
    fun `MLHFM is monotonically increasing`() {
        val mlhfm = MlhfmPreferences.getSelectedMap()!!
        // Each zAxis row has a single value — extract the column
        val values = mlhfm.second.zAxis.map { it[0] }
        for (i in 1 until values.size) {
            assertTrue(values[i] >= values[i - 1],
                "MLHFM should be monotonically increasing: z[$i]=${values[i]} < z[${i - 1}]=${values[i - 1]}")
        }
    }

    @Test
    fun `KFVPDKSD is 2D throttle transition map`() {
        val kfvpdksd = KfvpdksdPreferences.getSelectedMap()
        assertNotNull(kfvpdksd, "KFVPDKSD should resolve")
        assertTrue(kfvpdksd.second.xAxis.size > 1 && kfvpdksd.second.yAxis.size > 1,
            "KFVPDKSD should be 2D")
    }

    @Test
    fun `LDRPID maps resolve`() {
        val kfldrl = KfldrlPreferences.getSelectedMap()
        val kfldimx = KfldimxPreferences.getSelectedMap()
        assertNotNull(kfldrl, "KFLDRL should resolve")
        assertNotNull(kfldimx, "KFLDIMX should resolve")
        assertTrue(kfldrl.second.xAxis.size > 1, "KFLDRL should be 2D")
        assertTrue(kfldimx.second.xAxis.size > 1, "KFLDIMX should be 2D")
    }

    // ── ME7-Specific Mode Detection ─────────────────────────────────

    @Test
    fun `no map is falsely detected as DS1 scalar`() {
        val maps = mapOf(
            "KFMIOP" to KfmiopPreferences,
            "KFMIRL" to KfmirlPreferences,
            "KFZWOP" to KfzwopPreferences,
            "KFZW" to KfzwPreferences,
            "KFLDRL" to KfldrlPreferences,
            "KFLDIMX" to KfldimxPreferences,
        )

        for ((name, pref) in maps) {
            val selected = pref.getSelectedMap()!!
            val isScalar = selected.second.xAxis.isEmpty() && selected.second.yAxis.isEmpty()
            assertFalse(isScalar, "$name should NOT be detected as DS1 scalar on ME7")
        }
    }

    // ── Compute Pipeline ────────────────────────────────────────────

    @Test
    fun `KFMIOP rescale produces valid output`() {
        val pair = KfmiopPreferences.getSelectedMap()!!
        val maxMapLoad = Rlsol.rlsol(1013.0, 2550.0, 0.0, 96.0, 0.106, 2550.0)
        val maxBoostLoad = Rlsol.rlsol(1013.0, 2100.0, 0.0, 96.0, 0.106, 2100.0)

        val result = Kfmiop.calculateKfmiop(pair.second, maxMapLoad, maxBoostLoad)
        assertNotNull(result, "KFMIOP calculator should not return null")

        for (row in result.outputKfmiop.zAxis) {
            for (value in row) {
                assertFalse(value.isNaN(), "KFMIOP output contains NaN")
                assertFalse(value.isInfinite(), "KFMIOP output contains Inf")
            }
        }
    }

    @Test
    fun `KFMIRL inverse is mathematically valid`() {
        val kfmiopPair = KfmiopPreferences.getSelectedMap()!!
        val kfmirlPair = KfmirlPreferences.getSelectedMap()!!

        val inverse = Inverse.calculateInverse(kfmiopPair.second, kfmirlPair.second)
        for (row in inverse.zAxis) {
            for (value in row) {
                assertFalse(value.isNaN(), "KFMIRL inverse contains NaN")
                assertFalse(value.isInfinite(), "KFMIRL inverse contains Inf")
            }
        }
    }

    @Test
    fun `KFZW rescale values in valid range`() {
        val pair = KfzwPreferences.getSelectedMap()!!
        val input = pair.second
        val rescaledXAxis = RescaleAxis.rescaleAxis(input.xAxis, 200.0)
        val newZAxis = Kfzw.generateKfzw(input.xAxis, input.zAxis, rescaledXAxis)

        for (row in newZAxis) {
            for (value in row) {
                assertFalse(value.isNaN(), "KFZW rescale contains NaN")
                assertFalse(value.isInfinite(), "KFZW rescale contains Inf")
                assertTrue(value in -15.0..60.0,
                    "KFZW timing $value outside valid range (-15 to 60 grad KW)")
            }
        }
    }

    // ── Multi-Profile ───────────────────────────────────────────────

    @Test
    fun `MBox profile resolves all expected preferences`() {
        // Already loaded via base class — just verify
        assertNotNull(KfmiopPreferences.getSelectedMap(), "MBox: KFMIOP")
        assertNotNull(KfmirlPreferences.getSelectedMap(), "MBox: KFMIRL")
        assertNotNull(KfzwopPreferences.getSelectedMap(), "MBox: KFZWOP")
        assertNotNull(KfzwPreferences.getSelectedMap(), "MBox: KFZW")
        assertNotNull(MlhfmPreferences.getSelectedMap(), "MBox: MLHFM")
        assertNotNull(KfldrlPreferences.getSelectedMap(), "MBox: KFLDRL")
        assertNotNull(KfldimxPreferences.getSelectedMap(), "MBox: KFLDIMX")
    }

    @Test
    fun `dimension contracts hold across MBox profile`() {
        val kfmiop = KfmiopPreferences.getSelectedMap()!!
        val kfmirl = KfmirlPreferences.getSelectedMap()!!

        assertTrue(kfmiop.second.xAxis.size > 1 && kfmiop.second.yAxis.size > 1,
            "MBox KFMIOP should be 2D")
        assertTrue(kfmirl.second.xAxis.size > 1 && kfmirl.second.yAxis.size > 1,
            "MBox KFMIRL should be 2D")
    }
}
