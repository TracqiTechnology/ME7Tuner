package data.parser.med17log

import data.contract.Med17LogFileContract.Header as H
import domain.model.fueltrim.FuelTrimAnalyzer
import domain.model.fueltrim.FuelTrimResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Edge-case tests for MED17 fuel trim parsing and analysis.
 *
 * Bug 3 regression: LTFT double-count when fra_w is missing and longft1_w is used.
 * Bug 4 regression: zero-correction results must still produce valid Map3d output.
 */
class Med17FuelTrimParserEdgeCaseTest {

    private val rpmBins = doubleArrayOf(1000.0, 2000.0, 3000.0, 4000.0)
    private val loadBins = doubleArrayOf(25.0, 50.0, 75.0, 100.0)

    // ── Bug 3: Double-count regression ──────────────────────────────

    @Test
    fun `log with only longft1_w - LTFT list has exactly N entries for N data rows`() {
        val n = 20
        val logData = mapOf(
            H.RPM_COLUMN_HEADER to List(n) { 2000.0 },
            H.ENGINE_LOAD_HEADER to List(n) { 50.0 },
            H.LONG_TERM_FT_HEADER to List(n) { 1.05 },  // longft1_w fallback
            // No fra_w (LTFT_COLUMN_HEADER) — triggers fallback path
        )

        val result = FuelTrimAnalyzer.analyzeMed17Trims(logData, rpmBins, loadBins, minSamples = 1)

        // The RPM=2000 Load=50 bin should have exactly n samples worth of trim data
        val rpmIdx = 1  // nearest to 2000
        val loadIdx = 1  // nearest to 50
        // Average should be exactly 5.0% (from 1.05 - 1.0 = 0.05 → 5%)
        // If double-counted, it would be 10% or some other wrong value
        assertEquals(5.0, result.avgTrims[rpmIdx][loadIdx], 0.01,
            "LTFT average should be 5.0% from longft1_w=1.05, not double-counted")
    }

    @Test
    fun `log with only longft1_w - LTFT values match input without doubling`() {
        val logData = mapOf(
            H.RPM_COLUMN_HEADER to List(10) { 3000.0 },
            H.ENGINE_LOAD_HEADER to List(10) { 75.0 },
            H.LONG_TERM_FT_HEADER to List(10) { 0.97 },  // -3% trim
        )

        val result = FuelTrimAnalyzer.analyzeMed17Trims(logData, rpmBins, loadBins, minSamples = 1)

        val rpmIdx = 2  // nearest to 3000
        val loadIdx = 2  // nearest to 75
        // Single LTFT of 0.97 → -3.0% average trim
        assertEquals(-3.0, result.avgTrims[rpmIdx][loadIdx], 0.01,
            "LTFT should be -3.0% from longft1_w=0.97 without doubling")
    }

    @Test
    fun `log with fra_w AND longft1_w - fra_w used as LTFT`() {
        val logData = mapOf(
            H.RPM_COLUMN_HEADER to List(10) { 2000.0 },
            H.ENGINE_LOAD_HEADER to List(10) { 50.0 },
            H.LTFT_COLUMN_HEADER to List(10) { 1.03 },     // fra_w (preferred)
            H.LONG_TERM_FT_HEADER to List(10) { 1.10 },    // longft1_w (fallback, should be ignored)
        )

        val result = FuelTrimAnalyzer.analyzeMed17Trims(logData, rpmBins, loadBins, minSamples = 1)

        val rpmIdx = 1
        val loadIdx = 1
        // Should use fra_w (3.0%), not longft1_w (10.0%)
        assertEquals(3.0, result.avgTrims[rpmIdx][loadIdx], 0.01,
            "Should prefer fra_w (3%) over longft1_w (10%) as LTFT source")
    }

    @Test
    fun `log with fra_w only - LTFT list equals fra_w list exactly`() {
        val logData = mapOf(
            H.RPM_COLUMN_HEADER to List(10) { 4000.0 },
            H.ENGINE_LOAD_HEADER to List(10) { 100.0 },
            H.LTFT_COLUMN_HEADER to List(10) { 1.02 },     // fra_w only
        )

        val result = FuelTrimAnalyzer.analyzeMed17Trims(logData, rpmBins, loadBins, minSamples = 1)

        val rpmIdx = 3  // nearest to 4000
        val loadIdx = 3  // nearest to 100
        assertEquals(2.0, result.avgTrims[rpmIdx][loadIdx], 0.01,
            "Should use fra_w=1.02 → 2.0% trim")
    }

    @Test
    fun `sample count parity - RPM load and LTFT lists use consistent count`() {
        // Mismatched list sizes — analyzer should use the minimum
        val logData = mapOf(
            H.RPM_COLUMN_HEADER to List(15) { 2000.0 },
            H.ENGINE_LOAD_HEADER to List(12) { 50.0 },
            H.LONG_TERM_FT_HEADER to List(10) { 1.05 },
        )

        val result = FuelTrimAnalyzer.analyzeMed17Trims(logData, rpmBins, loadBins, minSamples = 1)

        // Should process min(15, 12, 10) = 10 samples without IndexOutOfBounds
        val rpmIdx = 1
        val loadIdx = 1
        assertEquals(5.0, result.avgTrims[rpmIdx][loadIdx], 0.01,
            "Should process 10 samples (minimum list size) correctly")
    }

    // ── Bug 4: Zero corrections ─────────────────────────────────────

    @Test
    fun `FuelTrimResult with all-zero corrections - isEmpty is true`() {
        val result = FuelTrimResult(
            rpmBins = rpmBins,
            loadBins = loadBins,
            avgTrims = Array(rpmBins.size) { DoubleArray(loadBins.size) },
            corrections = Array(rpmBins.size) { DoubleArray(loadBins.size) },
            warnings = emptyList()
        )
        assertTrue(result.isEmpty, "All-zero corrections should report isEmpty=true")
    }

    @Test
    fun `FuelTrimResult with all-zero corrections - toCorrectionsMap3d returns valid Map3d`() {
        val result = FuelTrimResult(
            rpmBins = rpmBins,
            loadBins = loadBins,
            avgTrims = Array(rpmBins.size) { DoubleArray(loadBins.size) },
            corrections = Array(rpmBins.size) { DoubleArray(loadBins.size) },
            warnings = emptyList()
        )

        val map3d = result.toCorrectionsMap3d()
        assertEquals(loadBins.size, map3d.xAxis.size, "xAxis should be load bins")
        assertEquals(rpmBins.size, map3d.yAxis.size, "yAxis should be RPM bins")
        assertEquals(rpmBins.size, map3d.zAxis.size, "zAxis row count should match RPM bins")
        for (row in map3d.zAxis) {
            assertEquals(loadBins.size, row.size, "zAxis column count should match load bins")
        }
    }

    @Test
    fun `FuelTrimResult with all-zero corrections - toAvgTrimsMap3d returns valid Map3d`() {
        val result = FuelTrimResult(
            rpmBins = rpmBins,
            loadBins = loadBins,
            avgTrims = Array(rpmBins.size) { DoubleArray(loadBins.size) },
            corrections = Array(rpmBins.size) { DoubleArray(loadBins.size) },
            warnings = emptyList()
        )

        val map3d = result.toAvgTrimsMap3d()
        assertEquals(loadBins.size, map3d.xAxis.size)
        assertEquals(rpmBins.size, map3d.yAxis.size)
        assertTrue(map3d.zAxis.all { row -> row.all { it == 0.0 } },
            "Zero-trim Map3d should have all zero values")
    }

    @Test
    fun `analyzer with perfectly trimmed log produces zero corrections`() {
        // All trims at exactly 1.0 — no correction needed
        val logData = mapOf(
            H.RPM_COLUMN_HEADER to List(20) { 2000.0 },
            H.ENGINE_LOAD_HEADER to List(20) { 50.0 },
            H.LTFT_COLUMN_HEADER to List(20) { 1.0 },      // perfect trim
            H.STFT_MIXED_COLUMN_HEADER to List(20) { 1.0 }, // perfect trim
        )

        val result = FuelTrimAnalyzer.analyzeMed17Trims(logData, rpmBins, loadBins, minSamples = 1)

        assertTrue(result.isEmpty, "Perfectly trimmed log should produce zero corrections")
        // Even with isEmpty=true, Map3d conversion should still work
        val map3d = result.toCorrectionsMap3d()
        assertFalse(map3d.xAxis.isEmpty(), "Corrections Map3d xAxis should not be empty")
    }

    @Test
    fun `zero-correction Map3d has correct dimensions even when empty`() {
        val result = FuelTrimResult(
            rpmBins = FuelTrimAnalyzer.DEFAULT_RPM_BINS,
            loadBins = FuelTrimAnalyzer.DEFAULT_LOAD_BINS,
            avgTrims = Array(FuelTrimAnalyzer.DEFAULT_RPM_BINS.size) {
                DoubleArray(FuelTrimAnalyzer.DEFAULT_LOAD_BINS.size)
            },
            corrections = Array(FuelTrimAnalyzer.DEFAULT_RPM_BINS.size) {
                DoubleArray(FuelTrimAnalyzer.DEFAULT_LOAD_BINS.size)
            },
            warnings = emptyList()
        )

        val corrMap = result.toCorrectionsMap3d()
        val trimMap = result.toAvgTrimsMap3d()

        assertEquals(FuelTrimAnalyzer.DEFAULT_LOAD_BINS.size, corrMap.xAxis.size)
        assertEquals(FuelTrimAnalyzer.DEFAULT_RPM_BINS.size, corrMap.yAxis.size)
        assertEquals(FuelTrimAnalyzer.DEFAULT_LOAD_BINS.size, trimMap.xAxis.size)
        assertEquals(FuelTrimAnalyzer.DEFAULT_RPM_BINS.size, trimMap.yAxis.size)
    }
}
