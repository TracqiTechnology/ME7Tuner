package domain.model.med17

import data.contract.Med17LogFileContract.Header
import domain.model.pfi.PfiShareCalculator
import java.io.File
import kotlin.test.*

/**
 * End-to-end tests for PFI share calculation with real MED17 log data.
 *
 * Validates that:
 *  - Default RPM-dependent curve follows ramp → plateau → decline pattern
 *  - Log refinement produces sensible RPM→PFI curve from real data
 *  - Edge cases (cruise-only, WOT-only) don't crash
 */
class PfiShareMed17E2ETest {

    private fun logFile(name: String): File {
        val url = javaClass.classLoader.getResource("logs/$name")
            ?: error("Test resource not found: logs/$name")
        return File(url.toURI())
    }

    /**
     * Parse PFI-relevant columns (RPM + InjSys_facPrtnPfiTar) directly from the CSV.
     * Med17LogParser.FUEL_TRIM doesn't include PFI split factor, so we parse it manually.
     */
    private fun parsePfiLog(filename: String): Map<Header, List<Double>> {
        val file = logFile(filename)
        val rpmList = mutableListOf<Double>()
        val pfiList = mutableListOf<Double>()

        val lines = file.readLines()
        if (lines.size < 3) return emptyMap()

        // Row 1 is the signal header row (row 0 is firmware info)
        val headers = lines[1].split(",")
        var rpmIdx = -1
        var pfiIdx = -1
        for ((i, h) in headers.withIndex()) {
            when {
                h.contains("nmot_w", ignoreCase = true) -> rpmIdx = i
                h.contains("InjSys_facPrtnPfiTar", ignoreCase = true) -> pfiIdx = i
            }
        }

        if (rpmIdx < 0 || pfiIdx < 0) return emptyMap()

        for (i in 2 until lines.size) {
            val cols = lines[i].split(",")
            if (cols.size <= maxOf(rpmIdx, pfiIdx)) continue
            val rpm = cols[rpmIdx].trim().toDoubleOrNull() ?: continue
            val pfi = cols[pfiIdx].trim().toDoubleOrNull() ?: continue
            if (rpm > 0) {
                rpmList.add(rpm)
                pfiList.add(pfi)
            }
        }

        return mapOf(
            Header.RPM_COLUMN_HEADER to rpmList.toList(),
            Header.PFI_SPLIT_FACTOR_HEADER to pfiList.toList()
        )
    }

    // ── 1. Default Curve Tests ──────────────────────────────────────

    @Test
    fun `default curve has correct ramp-plateau-decline shape`() {
        val result = PfiShareCalculator.calculateRpmDependentShare()

        assertTrue(result.rpmAxis.isNotEmpty(), "Should have RPM axis")
        assertTrue(result.pfiSharePercent.isNotEmpty(), "Should have PFI share values")
        assertEquals(result.rpmAxis.size, result.pfiSharePercent.size)

        // Find peak: should be around 3500-5500 RPM (torque peak area)
        val peakIdx = result.pfiSharePercent.indices.maxByOrNull { result.pfiSharePercent[it] }!!
        val peakRpm = result.rpmAxis[peakIdx]
        val peakPfi = result.pfiSharePercent[peakIdx]

        println("Peak PFI share: $peakPfi% at $peakRpm RPM")
        assertTrue(peakRpm in 2500.0..5500.0, "Peak should be mid RPM, got $peakRpm")
        assertTrue(peakPfi > 30.0, "Peak PFI should be >30%, got $peakPfi")

        // Low RPM should be below peak (ramping up)
        assertTrue(result.pfiSharePercent.first() < peakPfi,
            "Low RPM PFI should be < peak")

        // High RPM should be below peak (declining)
        assertTrue(result.pfiSharePercent.last() < peakPfi,
            "High RPM PFI should be < peak")
    }

    @Test
    fun `default curve values are within 0-100 percent`() {
        val result = PfiShareCalculator.calculateRpmDependentShare()
        for (i in result.pfiSharePercent.indices) {
            assertTrue(result.pfiSharePercent[i] in 0.0..100.0,
                "PFI at RPM ${result.rpmAxis[i]}: ${result.pfiSharePercent[i]}%")
        }
    }

    @Test
    fun `default curve RPM axis is monotonically increasing`() {
        val result = PfiShareCalculator.calculateRpmDependentShare()
        for (i in 1 until result.rpmAxis.size) {
            assertTrue(result.rpmAxis[i] > result.rpmAxis[i - 1])
        }
    }

    // ── 2. Log Refinement Tests ─────────────────────────────────────

    @Test
    fun `refineFromLog with cruise log produces PFI curve`() {
        val logData = parsePfiLog("2023-05-19_21.08.33_log.csv")

        val rpmCount = logData[Header.RPM_COLUMN_HEADER]?.size ?: 0
        val pfiCount = logData[Header.PFI_SPLIT_FACTOR_HEADER]?.size ?: 0
        println("Cruise log: $rpmCount RPM, $pfiCount PFI rows")

        val result = PfiShareCalculator.refineFromLog(logData)
        assertTrue(result.rpmAxis.isNotEmpty(), "Should have RPM axis after refinement")
        assertTrue(result.pfiSharePercent.isNotEmpty(), "Should have PFI share")
    }

    @Test
    fun `refineFromLog with WOT log produces PFI curve`() {
        val logData = parsePfiLog("2025-01-21_16.24.32_log(1).csv")

        val result = PfiShareCalculator.refineFromLog(logData)
        assertTrue(result.rpmAxis.isNotEmpty())
        assertTrue(result.pfiSharePercent.isNotEmpty())

        if (result.loggedRpmAxis != null) {
            println("Logged PFI curve points: ${result.loggedRpmAxis!!.size}")
            for (i in result.loggedRpmAxis!!.indices) {
                println("  RPM=${result.loggedRpmAxis!![i]}, PFI=${result.loggedPfiPercent?.get(i)}%")
            }
        }
    }

    @Test
    fun `refineFromLog with mixed log produces curve with RPM spread`() {
        val logData = parsePfiLog("2023-05-19_21.31.12_log.csv")

        val result = PfiShareCalculator.refineFromLog(logData)
        assertTrue(result.rpmAxis.isNotEmpty())

        if (result.loggedRpmAxis != null && result.loggedRpmAxis!!.isNotEmpty()) {
            val span = result.loggedRpmAxis!!.max() - result.loggedRpmAxis!!.min()
            println("Mixed log RPM span: $span")
            assertTrue(span > 500, "Expected RPM span > 500, got $span")
        }
    }

    @Test
    fun `refined PFI values are within 0-100 percent`() {
        val logData = parsePfiLog("2023-05-19_21.08.33_log.csv")
        val result = PfiShareCalculator.refineFromLog(logData)

        for (i in result.pfiSharePercent.indices) {
            assertTrue(result.pfiSharePercent[i] in 0.0..100.0,
                "PFI at ${result.rpmAxis[i]} RPM: ${result.pfiSharePercent[i]}%")
        }
    }

    // ── 3. Edge Cases ───────────────────────────────────────────────

    @Test
    fun `refineFromLog with empty data returns default curve`() {
        val emptyData = mapOf<Header, List<Double>>(
            Header.RPM_COLUMN_HEADER to emptyList(),
            Header.PFI_SPLIT_FACTOR_HEADER to emptyList()
        )

        val result = PfiShareCalculator.refineFromLog(emptyData)
        assertTrue(result.rpmAxis.isNotEmpty(), "Should fall back to default")
        assertTrue(result.pfiSharePercent.isNotEmpty())
    }

    @Test
    fun `refineFromLog with missing PFI column returns default curve`() {
        val noData = mapOf<Header, List<Double>>(
            Header.RPM_COLUMN_HEADER to listOf(1000.0, 2000.0, 3000.0)
        )

        val result = PfiShareCalculator.refineFromLog(noData)
        assertTrue(result.rpmAxis.isNotEmpty(), "Should fall back to default")
    }
}
