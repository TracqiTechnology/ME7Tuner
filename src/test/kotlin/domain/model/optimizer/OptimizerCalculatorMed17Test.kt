package domain.model.optimizer

import data.contract.Me7LogFileContract
import data.contract.Med17LogFileContract
import data.parser.med17log.Med17LogAdapter
import data.parser.med17log.Med17LogParser
import java.io.File
import kotlin.test.*

/**
 * Integration tests for [OptimizerCalculator] with real MED17 log data.
 *
 * Tests WOT filtering, analyzeMed17, and cruise/idle edge cases.
 */
class OptimizerCalculatorMed17Test {

    private val parser = Med17LogParser()

    private fun logFile(name: String): File {
        val url = javaClass.classLoader.getResource("logs/$name")
            ?: error("Test resource not found: logs/$name")
        return File(url.toURI())
    }

    private fun parseAndAdapt(filename: String): Map<Me7LogFileContract.Header, List<Double>> {
        val med17Data = parser.parseLogFile(Med17LogParser.LogType.OPTIMIZER, logFile(filename))
        return Med17LogAdapter.toMe7OptimizerFormat(med17Data)
    }

    // ── T6: WOT filtering ───────────────────────────────────────────

    @Test
    fun `filterWotEntries with 2025 log extracts approximately 530 WOT entries`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        // Python analysis found 530 WOT rows; allow small tolerance for edge parsing
        assertTrue(wotEntries.size in 520..540, "Expected ~530 WOT entries, got ${wotEntries.size}")
    }

    @Test
    fun `all WOT entries have throttle angle at least 80 degrees`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        for (entry in wotEntries) {
            assertTrue(
                entry.throttleAngle >= 80.0,
                "WOT entry at RPM=${entry.rpm} has throttle=${entry.throttleAngle} < 80"
            )
        }
    }

    @Test
    fun `WOT entries have sane RPM range`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        assertTrue(wotEntries.isNotEmpty())
        for (entry in wotEntries) {
            assertTrue(entry.rpm in 1000.0..8500.0, "RPM ${entry.rpm} out of range")
        }
    }

    @Test
    fun `WOT entries have sane boost pressure values`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        assertTrue(wotEntries.isNotEmpty())
        for (entry in wotEntries) {
            assertTrue(entry.actualMap in 300.0..5000.0,
                "Actual MAP ${entry.actualMap} out of range at RPM=${entry.rpm}")
            assertTrue(entry.barometricPressure in 900.0..1100.0,
                "Baro ${entry.barometricPressure} out of range")
        }
    }

    @Test
    fun `WOT entries have sane load values`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        assertTrue(wotEntries.isNotEmpty())
        for (entry in wotEntries) {
            assertTrue(entry.requestedLoad in 0.0..500.0,
                "Requested load ${entry.requestedLoad} out of range")
            assertTrue(entry.actualLoad in 0.0..500.0,
                "Actual load ${entry.actualLoad} out of range")
        }
    }

    @Test
    fun `first WOT entry from 2025 log has expected values`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        assertTrue(wotEntries.isNotEmpty())
        val first = wotEntries[0]
        // From Python analysis, first WOT row: rpm=4432.5, rl=114.82, rlsol=176.32, wdkba=83.1
        assertEquals(4432.5, first.rpm, 1.0, "First WOT RPM")
        assertEquals(114.82, first.actualLoad, 1.0, "First WOT actual load")
        assertEquals(176.32, first.requestedLoad, 1.0, "First WOT requested load")
        assertEquals(83.1, first.throttleAngle, 0.5, "First WOT throttle angle")
    }

    // ── T7: analyzeMed17 basic sanity ───────────────────────────────

    @Test
    fun `analyzeMed17 without maps does not crash`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")

        val result = OptimizerCalculator.analyzeMed17(
            values = me7Data,
            kfldrlMap = null,
            kfldimxMap = null
        )

        assertNotNull(result, "analyzeMed17 should return non-null result")
    }

    @Test
    fun `analyzeMed17 produces WOT entries`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")

        val result = OptimizerCalculator.analyzeMed17(
            values = me7Data,
            kfldrlMap = null,
            kfldimxMap = null
        )

        assertTrue(result.wotEntries.isNotEmpty(), "analyzeMed17 should produce WOT entries")
        assertTrue(result.wotEntries.size in 520..540,
            "Expected ~530 WOT entries, got ${result.wotEntries.size}")
    }

    @Test
    fun `analyzeMed17 result has null KFPBRK (MED17 mode)`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")

        val result = OptimizerCalculator.analyzeMed17(
            values = me7Data,
            kfldrlMap = null,
            kfldimxMap = null
        )

        assertNull(result.kfpbrkMultipliers, "KFPBRK should be null for MED17")
    }

    @Test
    fun `analyzeMed17 result has null kfurlSolverResult (MED17 mode)`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")

        val result = OptimizerCalculator.analyzeMed17(
            values = me7Data,
            kfldrlMap = null,
            kfldimxMap = null
        )

        assertNull(result.kfurlSolverResult, "kfurlSolverResult should be null for MED17")
    }

    // ── T8: Cruise/idle log — zero WOT ──────────────────────────────

    @Test
    fun `2023-21h08 cruise log produces zero WOT entries`() {
        val me7Data = parseAndAdapt("2023-05-19_21.08.33_log.csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        assertEquals(0, wotEntries.size, "Cruise/idle log should produce 0 WOT entries")
    }

    @Test
    fun `2023-21h31 log produces WOT entries`() {
        val me7Data = parseAndAdapt("2023-05-19_21.31.12_log.csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        assertTrue(wotEntries.size in 180..240,
            "Expected ~208 WOT entries from 2023-21h31 log, got ${wotEntries.size}")
    }

    @Test
    fun `analyzeMed17 with cruise-only log still returns valid result`() {
        val me7Data = parseAndAdapt("2023-05-19_21.08.33_log.csv")

        val result = OptimizerCalculator.analyzeMed17(
            values = me7Data,
            kfldrlMap = null,
            kfldimxMap = null
        )

        assertNotNull(result)
        assertEquals(0, result.wotEntries.size, "No WOT entries from cruise log")
    }
}
