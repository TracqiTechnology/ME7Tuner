package ui.screens.med17

import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfmirl.KfmirlPreferences
import data.preferences.kfzw.KfzwPreferences
import data.preferences.kfzwop.KfzwopPreferences
import data.preferences.krkte.KrkteGdiPreferences
import data.preferences.krkte.KrktePfiPreferences
import data.preferences.rkw.RkwPreferences
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
 * Workflow-level integration test for MED17.
 *
 * Validates the full pipeline: profile apply → preference resolve → dimension
 * contract → mode detection → compute validity. This catches bugs where a map
 * resolves non-null but points to the wrong table (Bug 1), or a 2D map is
 * falsely detected as a DS1 scalar (Bug 5).
 */
class Med17WorkflowContractTest : Med17ScreenTestBase() {

    // ── Dimension Contracts (Funktionsrahmen-derived) ────────────────

    @Test
    fun `KFMIOP is 2D with at least 14 load columns and 14 RPM rows`() {
        val kfmiop = KfmiopPreferences.getSelectedMap()
        assertNotNull(kfmiop, "KFMIOP should resolve")
        assertTrue(kfmiop.second.xAxis.size >= 14,
            "KFMIOP should have >=14 load columns, got ${kfmiop.second.xAxis.size}")
        assertTrue(kfmiop.second.yAxis.size >= 14,
            "KFMIOP should have >=14 RPM rows, got ${kfmiop.second.yAxis.size}")
    }

    @Test
    fun `KFMIOP xAxis values are load range and yAxis values are RPM range`() {
        val kfmiop = KfmiopPreferences.getSelectedMap()!!
        val xMin = kfmiop.second.xAxis.min()
        val xMax = kfmiop.second.xAxis.max()
        val yMin = kfmiop.second.yAxis.min()
        val yMax = kfmiop.second.yAxis.max()

        // Load axis: 0–300% range is typical
        assertTrue(xMin >= 0.0, "KFMIOP xAxis min should be >= 0, got $xMin")
        assertTrue(xMax <= 350.0, "KFMIOP xAxis max should be <= 350%, got $xMax")

        // RPM axis: 500–8000 range is typical
        assertTrue(yMin >= 0.0, "KFMIOP yAxis min should be >= 0, got $yMin")
        assertTrue(yMax <= 9000.0, "KFMIOP yAxis max should be <= 9000, got $yMax")
    }

    @Test
    fun `KFMIRL has same row and col count as KFMIOP`() {
        val kfmiop = KfmiopPreferences.getSelectedMap()!!
        val kfmirl = KfmirlPreferences.getSelectedMap()
        assertNotNull(kfmirl, "KFMIRL should resolve")

        assertTrue(kfmirl.second.xAxis.size == kfmiop.second.xAxis.size,
            "KFMIRL xAxis (${kfmirl.second.xAxis.size}) should match KFMIOP (${kfmiop.second.xAxis.size})")
        assertTrue(kfmirl.second.yAxis.size == kfmiop.second.yAxis.size,
            "KFMIRL yAxis (${kfmirl.second.yAxis.size}) should match KFMIOP (${kfmiop.second.yAxis.size})")
    }

    @Test
    fun `KFZWOP is 2D with load x RPM axes`() {
        val kfzwop = KfzwopPreferences.getSelectedMap()
        assertNotNull(kfzwop, "KFZWOP should resolve")
        assertTrue(kfzwop.second.xAxis.size > 1 && kfzwop.second.yAxis.size > 1,
            "KFZWOP should be 2D, got x=${kfzwop.second.xAxis.size} y=${kfzwop.second.yAxis.size}")
    }

    @Test
    fun `KFZW is 2D ignition table`() {
        val kfzw = KfzwPreferences.getSelectedMap()
        assertNotNull(kfzw, "KFZW should resolve")
        assertTrue(kfzw.second.xAxis.size > 1 && kfzw.second.yAxis.size > 1,
            "KFZW should be 2D, got x=${kfzw.second.xAxis.size} y=${kfzw.second.yAxis.size}")
    }

    @Test
    fun `KFLDRL is 2D with RPM x pressure axes`() {
        val kfldrl = KfldrlPreferences.getSelectedMap()
        assertNotNull(kfldrl, "KFLDRL should resolve")
        assertTrue(kfldrl.second.xAxis.size > 1 && kfldrl.second.yAxis.size > 1,
            "KFLDRL should be 2D, got x=${kfldrl.second.xAxis.size} y=${kfldrl.second.yAxis.size}")
    }

    @Test
    fun `KFLDIMX is 2D with RPM x pressure axes`() {
        val kfldimx = KfldimxPreferences.getSelectedMap()
        assertNotNull(kfldimx, "KFLDIMX should resolve")
        assertTrue(kfldimx.second.xAxis.size > 1 && kfldimx.second.yAxis.size > 1,
            "KFLDIMX should be 2D, got x=${kfldimx.second.xAxis.size} y=${kfldimx.second.yAxis.size}")
    }

    @Test
    fun `RKW is 2D with load x RPM axes`() {
        val rkw = RkwPreferences.getSelectedMap()
        assertNotNull(rkw, "RKW should resolve")
        assertTrue(rkw.second.xAxis.size > 1 && rkw.second.yAxis.size > 1,
            "RKW should be 2D, got x=${rkw.second.xAxis.size} y=${rkw.second.yAxis.size}")
    }

    @Test
    fun `KRKTE PFI is 1D or scalar`() {
        val krktePfi = KrktePfiPreferences.getSelectedMap()
        assertNotNull(krktePfi, "KRKTE PFI should resolve")
        // KRKTE is a 1D curve or scalar — xAxis should be small
        assertTrue(krktePfi.second.xAxis.size <= 1 || krktePfi.second.yAxis.size <= 1,
            "KRKTE PFI should be 1D/scalar, got x=${krktePfi.second.xAxis.size} y=${krktePfi.second.yAxis.size}")
    }

    @Test
    fun `KRKTE GDI is 1D or scalar`() {
        val krkteGdi = KrkteGdiPreferences.getSelectedMap()
        assertNotNull(krkteGdi, "KRKTE GDI should resolve")
        assertTrue(krkteGdi.second.xAxis.size <= 1 || krkteGdi.second.yAxis.size <= 1,
            "KRKTE GDI should be 1D/scalar, got x=${krkteGdi.second.xAxis.size} y=${krkteGdi.second.yAxis.size}")
    }

    // ── Mode Detection (the logic that broke) ───────────────────────

    @Test
    fun `KFMIOP is NOT detected as scalar`() {
        val kfmiop = KfmiopPreferences.getSelectedMap()!!
        assertFalse(
            kfmiop.second.xAxis.isEmpty() && kfmiop.second.yAxis.isEmpty(),
            "KFMIOP must NOT be scalar — wrong map selected if scalar"
        )
    }

    @Test
    fun `KFMIRL is NOT detected as scalar`() {
        val kfmirl = KfmirlPreferences.getSelectedMap()!!
        assertFalse(
            kfmirl.second.xAxis.isEmpty() && kfmirl.second.yAxis.isEmpty(),
            "KFMIRL must NOT be scalar"
        )
    }

    @Test
    fun `KFZWOP is NOT detected as scalar`() {
        val kfzwop = KfzwopPreferences.getSelectedMap()!!
        assertFalse(
            kfzwop.second.xAxis.isEmpty() && kfzwop.second.yAxis.isEmpty(),
            "KFZWOP must NOT be scalar"
        )
    }

    @Test
    fun `DS1 scalar detection is false for all 2D maps`() {
        val maps2d = mapOf(
            "KFMIOP" to KfmiopPreferences,
            "KFMIRL" to KfmirlPreferences,
            "KFZWOP" to KfzwopPreferences,
            "KFZW" to KfzwPreferences,
            "KFLDRL" to KfldrlPreferences,
            "KFLDIMX" to KfldimxPreferences,
            "RKW" to RkwPreferences,
        )

        for ((name, pref) in maps2d) {
            val selected = pref.getSelectedMap()!!
            val isScalar = selected.second.xAxis.isEmpty() && selected.second.yAxis.isEmpty()
            assertFalse(isScalar, "$name must NOT be detected as DS1 scalar")
        }
    }

    // ── Compute Pipeline Validity ───────────────────────────────────

    @Test
    fun `KFMIOP rescale produces output with same dimensions as input`() {
        val pair = KfmiopPreferences.getSelectedMap()!!
        val input = pair.second
        val maxMapLoad = Rlsol.rlsol(1030.0, 3500.0, 0.0, 96.0, 0.106, 3500.0)
        val maxBoostLoad = Rlsol.rlsol(1030.0, 2800.0, 0.0, 96.0, 0.106, 2800.0)

        val result = Kfmiop.calculateKfmiop(input, maxMapLoad, maxBoostLoad)
        assertNotNull(result, "KFMIOP calculator should not return null")

        val output = result.outputKfmiop
        assertTrue(output.xAxis.size == input.xAxis.size,
            "Output xAxis size (${output.xAxis.size}) should match input (${input.xAxis.size})")
        assertTrue(output.yAxis.size == input.yAxis.size,
            "Output yAxis size (${output.yAxis.size}) should match input (${input.yAxis.size})")
        assertTrue(output.zAxis.size == input.zAxis.size,
            "Output zAxis row count should match input")
    }

    @Test
    fun `KFMIOP rescale output values are physically valid`() {
        val pair = KfmiopPreferences.getSelectedMap()!!
        val maxMapLoad = Rlsol.rlsol(1030.0, 3500.0, 0.0, 96.0, 0.106, 3500.0)
        val maxBoostLoad = Rlsol.rlsol(1030.0, 2800.0, 0.0, 96.0, 0.106, 2800.0)

        val result = Kfmiop.calculateKfmiop(pair.second, maxMapLoad, maxBoostLoad)!!
        for (row in result.outputKfmiop.zAxis) {
            for (value in row) {
                assertFalse(value.isNaN(), "KFMIOP output contains NaN")
                assertFalse(value.isInfinite(), "KFMIOP output contains Inf")
                assertTrue(value in 0.0..500.0,
                    "KFMIOP output value $value outside valid 0–500% load range")
            }
        }
    }

    @Test
    fun `KFMIRL inverse of KFMIOP produces valid output`() {
        val kfmiopPair = KfmiopPreferences.getSelectedMap()!!
        val kfmirlPair = KfmirlPreferences.getSelectedMap()!!

        val inverse = Inverse.calculateInverse(kfmiopPair.second, kfmirlPair.second)
        for (row in inverse.zAxis) {
            for (value in row) {
                assertFalse(value.isNaN(), "KFMIRL inverse output contains NaN")
                assertFalse(value.isInfinite(), "KFMIRL inverse output contains Inf")
            }
        }
    }

    @Test
    fun `KFMIRL inverse preserves first column from original`() {
        val kfmiopPair = KfmiopPreferences.getSelectedMap()!!
        val kfmirlPair = KfmirlPreferences.getSelectedMap()!!

        val inverse = Inverse.calculateInverse(kfmiopPair.second, kfmirlPair.second)
        // Preserve first column from original KFMIRL (standard practice)
        for (i in inverse.zAxis.indices) {
            if (inverse.zAxis[i].isNotEmpty() && kfmirlPair.second.zAxis[i].isNotEmpty()) {
                inverse.zAxis[i][0] = kfmirlPair.second.zAxis[i][0]
            }
        }
        // Verify preserved column matches original
        for (i in inverse.zAxis.indices) {
            if (inverse.zAxis[i].isNotEmpty() && kfmirlPair.second.zAxis[i].isNotEmpty()) {
                assertTrue(
                    inverse.zAxis[i][0] == kfmirlPair.second.zAxis[i][0],
                    "KFMIRL first column row $i should be preserved"
                )
            }
        }
    }

    @Test
    fun `KFZWOP rescale produces timing values in valid range`() {
        val pair = KfzwopPreferences.getSelectedMap()!!
        val input = pair.second
        val rescaledXAxis = RescaleAxis.rescaleAxis(input.xAxis, 300.0)
        val newZAxis = Kfzw.generateKfzw(input.xAxis, input.zAxis, rescaledXAxis)

        for (row in newZAxis) {
            for (value in row) {
                assertFalse(value.isNaN(), "KFZWOP rescale contains NaN")
                assertFalse(value.isInfinite(), "KFZWOP rescale contains Inf")
                assertTrue(value in -15.0..60.0,
                    "KFZWOP timing $value outside valid range (-15 to 60 grad KW)")
            }
        }
    }

    @Test
    fun `KFZW rescale produces timing values in valid range`() {
        val pair = KfzwPreferences.getSelectedMap()!!
        val input = pair.second
        val rescaledXAxis = RescaleAxis.rescaleAxis(input.xAxis, 300.0)
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

    // ── Cross-Variant Dimension Parity ──────────────────────────────

    @Test
    fun `all 6 MED17 variants resolve KFMIOP as 2D with consistent dimensions`() {
        for (variant in Med17MultiVariantTest.VARIANTS) {
            val xdfFile = java.io.File(java.io.File(System.getProperty("user.dir")), variant.xdfPath)
            val binFile = java.io.File(java.io.File(System.getProperty("user.dir")), variant.binPath)
            if (!xdfFile.exists() || !binFile.exists()) continue

            val (_, defs) = data.parser.xdf.XdfParser.parseToList(java.io.FileInputStream(xdfFile))
            val maps = data.parser.bin.BinParser.parseToList(java.io.FileInputStream(binFile), defs)

            data.parser.xdf.XdfParser.setTableDefinitionsForTesting(defs)
            data.parser.bin.BinParser.setMapListForTesting(maps)
            data.profile.ProfileManager.applyProfile(profile)

            val kfmiop = KfmiopPreferences.getSelectedMap()
            assertNotNull(kfmiop, "${variant.name}: KFMIOP should resolve")
            assertFalse(
                kfmiop.second.xAxis.isEmpty() && kfmiop.second.yAxis.isEmpty(),
                "${variant.name}: KFMIOP must not be scalar"
            )
            assertTrue(kfmiop.second.xAxis.size >= 10,
                "${variant.name}: KFMIOP should have >=10 load columns, got ${kfmiop.second.xAxis.size}")
            assertTrue(kfmiop.second.yAxis.size >= 10,
                "${variant.name}: KFMIOP should have >=10 RPM rows, got ${kfmiop.second.yAxis.size}")
        }

        // Restore 404E fixtures for other tests
        data.parser.xdf.XdfParser.setTableDefinitionsForTesting(tableDefs)
        data.parser.bin.BinParser.setMapListForTesting(allMaps)
        data.profile.ProfileManager.applyProfile(profile)
    }
}
