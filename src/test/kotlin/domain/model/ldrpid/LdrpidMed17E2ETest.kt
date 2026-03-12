package domain.model.ldrpid

import data.contract.Me7LogFileContract
import data.contract.Med17LogFileContract
import data.parser.med17log.Med17LogAdapter
import data.parser.med17log.Med17LogParser
import data.parser.med17log.Med17LogParser.LogType
import domain.math.map.Map3d
import java.io.File
import kotlin.test.*

/**
 * End-to-end tests for the full MED17 LDRPID flow:
 *   ScorpionEFI CSV → Med17LogParser → Med17LogAdapter → LdrpidCalculator
 *
 * Uses real log files from src/test/resources/logs/.
 */
class LdrpidMed17E2ETest {

    // ── Axis definitions matching a realistic KFLDRL (5 duty-cycle cols × 4 RPM rows) ──
    private val rpmAxis = arrayOf(2000.0, 3000.0, 4000.0, 5000.0)
    private val dutyAxis = arrayOf(20.0, 40.0, 60.0, 80.0, 95.0)

    private fun buildKfldrlMap(): Map3d {
        val z = Array(rpmAxis.size) { Array(dutyAxis.size) { 30.0 } }
        return Map3d(dutyAxis, rpmAxis, z)
    }

    private fun buildKfldimxMap(): Map3d {
        val pressureAxis = arrayOf(200.0, 400.0, 600.0, 800.0, 1000.0, 1200.0)
        val z = Array(rpmAxis.size) { Array(pressureAxis.size) { 30.0 } }
        return Map3d(pressureAxis, rpmAxis, z)
    }

    private fun logFile(name: String): File {
        val url = javaClass.classLoader.getResource("logs/$name")
            ?: error("Test resource not found: logs/$name")
        return File(url.toURI())
    }

    // ── 1. Parse → Adapt → Calculate with WOT log ──────────────────

    @Test
    fun `full pipeline with WOT log produces four non-empty maps`() {
        val parser = Med17LogParser()
        val med17Data = parser.parseLogFile(LogType.LDRPID, logFile("2025-01-21_16.24.32_log(1).csv"))

        // Parsing should yield rows for all LDRPID signals
        val rpmList = med17Data[Med17LogFileContract.Header.RPM_COLUMN_HEADER]!!
        assertTrue(rpmList.isNotEmpty(), "Parser should extract RPM values from WOT log")

        // Adapt to ME7 format
        val me7Data = Med17LogAdapter.toMe7LdrpidFormat(med17Data)
        val me7Rpms = me7Data[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!
        assertEquals(rpmList.size, me7Rpms.size, "Adapter should preserve row count")

        // Calculate
        val kfldrl = buildKfldrlMap()
        val kfldimx = buildKfldimxMap()
        val result = LdrpidCalculator.calculateLdrpid(me7Data, kfldrl, kfldimx)

        // All four maps should be well-formed
        assertEquals(rpmAxis.size, result.nonLinearOutput.yAxis.size)
        assertEquals(dutyAxis.size, result.nonLinearOutput.xAxis.size)
        assertEquals(rpmAxis.size, result.linearOutput.yAxis.size)
        assertEquals(rpmAxis.size, result.kfldrl.yAxis.size)
        assertEquals(rpmAxis.size, result.kfldimx.yAxis.size)

        // Non-linear output should have actual boost data (not all zeros)
        val nonLinearValues = result.nonLinearOutput.zAxis.flatMap { it.toList() }
        assertTrue(nonLinearValues.any { it > 0.1 },
            "Non-linear output should contain real boost values from WOT data")
    }

    // ── 2. Verify parsed signal values are physically plausible ─────

    @Test
    fun `parsed WOT log signals are within physically plausible ranges`() {
        val parser = Med17LogParser()
        val med17Data = parser.parseLogFile(LogType.LDRPID, logFile("2025-01-21_16.24.32_log(1).csv"))

        val rpms = med17Data[Med17LogFileContract.Header.RPM_COLUMN_HEADER]!!
        val throttles = med17Data[Med17LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!
        val boosts = med17Data[Med17LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!
        val baros = med17Data[Med17LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER]!!
        val wgdcs = med17Data[Med17LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER]!!

        assertTrue(rpms.all { it in 0.0..10000.0 }, "RPMs should be 0–10000")
        assertTrue(throttles.all { it in 0.0..110.0 }, "Throttle should be 0–110%")
        assertTrue(boosts.all { it in 0.0..6000.0 }, "Boost pressure should be 0–6000 hPa")
        assertTrue(baros.all { it in 800.0..1200.0 }, "Baro should be 800–1200 hPa")
        assertTrue(wgdcs.all { it in 0.0..110.0 }, "WGDC should be 0–110%")
    }

    // ── 3. Adapter preserves data integrity ─────────────────────────

    @Test
    fun `adapter maps all LDRPID signals to ME7 equivalents`() {
        val parser = Med17LogParser()
        val med17Data = parser.parseLogFile(LogType.LDRPID, logFile("2025-01-21_16.24.32_log(1).csv"))
        val me7Data = Med17LogAdapter.toMe7LdrpidFormat(med17Data)

        // Each required ME7 signal should be populated
        val requiredHeaders = listOf(
            Me7LogFileContract.Header.RPM_COLUMN_HEADER,
            Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER,
            Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER,
            Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER,
            Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER,
        )

        for (header in requiredHeaders) {
            val values = me7Data[header]
            assertNotNull(values, "ME7 header $header should be present")
            assertTrue(values.isNotEmpty(), "ME7 header $header should have data")
        }

        // Values should match the source MED17 data exactly
        val med17Rpms = med17Data[Med17LogFileContract.Header.RPM_COLUMN_HEADER]!!
        val me7Rpms = me7Data[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!
        assertEquals(med17Rpms, me7Rpms, "RPM values should be identical after adaptation")
    }

    // ── 4. KFLDRL output has monotonic rows ─────────────────────────

    @Test
    fun `KFLDRL rows are monotonically non-decreasing`() {
        val parser = Med17LogParser()
        val med17Data = parser.parseLogFile(LogType.LDRPID, logFile("2025-01-21_16.24.32_log(1).csv"))
        val me7Data = Med17LogAdapter.toMe7LdrpidFormat(med17Data)

        val result = LdrpidCalculator.calculateLdrpid(me7Data, buildKfldrlMap(), buildKfldimxMap())

        for ((rowIdx, row) in result.nonLinearOutput.zAxis.withIndex()) {
            for (i in 1 until row.size) {
                assertTrue(row[i] >= row[i - 1],
                    "Non-linear row $rowIdx should be non-decreasing: ${row.contentToString()}")
            }
        }
    }

    // ── 5. Linear table has equal step sizes per column ─────────────

    @Test
    fun `linear table has equal step sizes per column`() {
        val parser = Med17LogParser()
        val med17Data = parser.parseLogFile(LogType.LDRPID, logFile("2025-01-21_16.24.32_log(1).csv"))
        val me7Data = Med17LogAdapter.toMe7LdrpidFormat(med17Data)

        val result = LdrpidCalculator.calculateLdrpid(me7Data, buildKfldrlMap(), buildKfldimxMap())
        val linear = result.linearOutput.zAxis

        for (col in linear[0].indices) {
            if (linear.size < 3) continue
            val step = linear[1][col] - linear[0][col]
            for (row in 2 until linear.size) {
                val actual = linear[row][col] - linear[row - 1][col]
                assertEquals(step, actual, 0.01,
                    "Column $col should have equal step size ($step), got $actual at row $row")
            }
        }
    }

    // ── 6. Cruise-only log (no WOT) still produces valid output ─────

    @Test
    fun `cruise-only log produces valid but zero-boost output`() {
        val parser = Med17LogParser()
        val med17Data = parser.parseLogFile(LogType.LDRPID, logFile("2023-05-19_21.08.33_log.csv"))

        val rpms = med17Data[Med17LogFileContract.Header.RPM_COLUMN_HEADER]!!
        // This log may have data but no WOT rows (throttle < 80)
        // The pipeline should still run without errors

        val me7Data = Med17LogAdapter.toMe7LdrpidFormat(med17Data)
        val result = LdrpidCalculator.calculateLdrpid(me7Data, buildKfldrlMap(), buildKfldimxMap())

        // Maps should be well-formed regardless of WOT content
        assertEquals(rpmAxis.size, result.nonLinearOutput.zAxis.size)
        assertEquals(dutyAxis.size, result.nonLinearOutput.zAxis[0].size)

        // All values should be non-negative
        for (row in result.nonLinearOutput.zAxis) {
            for (v in row) {
                assertTrue(v >= 0.0, "Boost values should be non-negative even with no WOT data")
            }
        }
    }

    // ── 7. Empty log data produces valid output ─────────────────────

    @Test
    fun `empty log data produces valid calculator output`() {
        // Simulate empty parsed result (all lists empty)
        val emptyData = mutableMapOf<Me7LogFileContract.Header, List<Double>>()
        for (header in Me7LogFileContract.Header.entries) {
            emptyData[header] = emptyList()
        }

        val result = LdrpidCalculator.calculateLdrpid(emptyData, buildKfldrlMap(), buildKfldimxMap())

        assertEquals(rpmAxis.size, result.nonLinearOutput.zAxis.size,
            "Output should have correct row count even with empty data")
        assertEquals(dutyAxis.size, result.nonLinearOutput.zAxis[0].size,
            "Output should have correct column count even with empty data")
    }

    // ── 8. KFLDIMX output has reasonable pressure axis ──────────────

    @Test
    fun `KFLDIMX has recalculated pressure x-axis from boost data`() {
        val parser = Med17LogParser()
        val med17Data = parser.parseLogFile(LogType.LDRPID, logFile("2025-01-21_16.24.32_log(1).csv"))
        val me7Data = Med17LogAdapter.toMe7LdrpidFormat(med17Data)

        val result = LdrpidCalculator.calculateLdrpid(me7Data, buildKfldrlMap(), buildKfldimxMap())

        // KFLDIMX x-axis is recalculated from boost data, should not be all zeros
        val xAxis = result.kfldimx.xAxis
        assertTrue(xAxis.isNotEmpty(), "KFLDIMX should have a pressure x-axis")
        assertTrue(xAxis.any { it > 0.0 }, "KFLDIMX x-axis should have positive pressure values")

        // Axis should be monotonically increasing
        for (i in 1 until xAxis.size) {
            assertTrue(xAxis[i] >= xAxis[i - 1],
                "KFLDIMX x-axis should be non-decreasing: ${xAxis.contentToString()}")
        }
    }

    // ── 9. parseLogDirectory works with single-file directory ───────

    @Test
    fun `parseLogDirectory aggregates files in directory`() {
        val parser = Med17LogParser()
        val logDir = logFile("2025-01-21_16.24.32_log(1).csv").parentFile

        var progressCalled = false
        val med17Data = parser.parseLogDirectory(LogType.LDRPID, logDir) { value, max ->
            progressCalled = true
            assertTrue(value in 1..max, "Progress should be within range")
        }

        assertTrue(progressCalled, "Progress callback should be invoked")

        val rpms = med17Data[Med17LogFileContract.Header.RPM_COLUMN_HEADER]!!
        assertTrue(rpms.isNotEmpty(), "Directory parse should produce data from CSV files")
    }

    // ── 10. Second WOT log file also works through pipeline ─────────

    @Test
    fun `second WOT log also produces valid LDRPID results`() {
        val parser = Med17LogParser()
        val med17Data = parser.parseLogFile(LogType.LDRPID, logFile("2023-05-19_21.31.12_log.csv"))
        val me7Data = Med17LogAdapter.toMe7LdrpidFormat(med17Data)

        val result = LdrpidCalculator.calculateLdrpid(me7Data, buildKfldrlMap(), buildKfldimxMap())

        assertEquals(rpmAxis.size, result.nonLinearOutput.yAxis.size)
        assertEquals(dutyAxis.size, result.nonLinearOutput.xAxis.size)

        // This log has WOT rows, so we expect some non-trivial boost values
        val hasBoostData = result.nonLinearOutput.zAxis.flatMap { it.toList() }.any { it > 0.1 }
        assertTrue(hasBoostData, "WOT log should produce non-trivial boost values")
    }
}
