package domain.model.med17

import data.contract.Me7LogFileContract
import data.parser.bin.BinParser
import data.parser.med17log.Med17LogAdapter
import data.parser.med17log.Med17LogParser
import data.parser.med17log.Med17LogParser.LogType
import data.parser.xdf.TableDefinition
import data.parser.xdf.XdfParser
import domain.math.map.Map3d
import domain.model.optimizer.OptimizerCalculator
import java.io.File
import java.io.FileInputStream
import kotlin.test.*

/**
 * End-to-end tests for the MED17 Optimizer pipeline:
 *   XDF+BIN (maps) + ScorpionEFI CSV (log) → OptimizerCalculator
 *
 * Tests with real 404E XDF/BIN and WOT log data to verify:
 *  - WOT filtering extracts correct entries
 *  - Key maps from 404E XDF are loadable
 *  - No crashes with real MED17 table dimensions
 */
class OptimizerMed17E2ETest {

    companion object {
        private val PROJECT_ROOT = File(System.getProperty("user.dir"))
        private val XDF_FILE = File(PROJECT_ROOT, "technical/med17/Normal XDF/404E_normal.xdf")
        private val BIN_FILE = File(PROJECT_ROOT, "technical/med17/OTS tunes/404E/MED17_1_62_STOCK.bin")
    }

    private val parser = Med17LogParser()

    private fun logFile(name: String): File {
        val url = javaClass.classLoader.getResource("logs/$name")
            ?: error("Test resource not found: logs/$name")
        return File(url.toURI())
    }

    private fun parseAndAdapt(filename: String): Map<Me7LogFileContract.Header, List<Double>> {
        val med17Data = parser.parseLogFile(LogType.OPTIMIZER, logFile(filename))
        return Med17LogAdapter.toMe7OptimizerFormat(med17Data)
    }

    private fun parseXdf(xdfFile: File): List<TableDefinition> {
        val (_, defs) = XdfParser.parseToList(FileInputStream(xdfFile))
        return defs
    }

    private fun parseBin(binFile: File, defs: List<TableDefinition>): List<Pair<TableDefinition, Map3d>> =
        BinParser.parseToList(FileInputStream(binFile), defs)

    private fun findMap(maps: List<Pair<TableDefinition, Map3d>>, keyword: String): Map3d? =
        maps.firstOrNull { it.first.tableName.contains(keyword, ignoreCase = true) }?.second

    // ── 1. WOT Filtering with Maps ──────────────────────────────────

    @Test
    fun `WOT filtering extracts entries from 2025 log`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        assertTrue(wotEntries.size > 400, "Expected >400 WOT entries, got ${wotEntries.size}")
        assertTrue(wotEntries.size < 600, "Expected <600 WOT entries, got ${wotEntries.size}")

        println("WOT entries: ${wotEntries.size}")
        println("RPM range: ${wotEntries.minOf { it.rpm }} - ${wotEntries.maxOf { it.rpm }}")
    }

    @Test
    fun `WOT entries have sane boost pressure values`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        for (entry in wotEntries) {
            // MED17 boost (actualMap) in hPa/mbar, typically 1000-3500 for stock/stage1
            assertTrue(entry.actualMap > 500.0 || entry.actualMap == 0.0,
                "Boost pressure ${entry.actualMap} seems too low for MED17 (hPa)")
        }
    }

    // ── 2. Optimizer with 404E maps ────────────────────────────────

    @Test
    fun `404E XDF maps load alongside WOT log without crash`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val defs = parseXdf(XDF_FILE)
        val maps = parseBin(BIN_FILE, defs)

        val kfmiop = findMap(maps, "Max indexed eng tq")
        val kfldrl = findMap(maps, "linearize boost")

        // At least one of these key maps should exist
        assertTrue(kfmiop != null || kfldrl != null,
            "Expected KFMIOP or KFLDRL in 404E XDF")

        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)
        assertTrue(wotEntries.isNotEmpty(), "Should have WOT entries for optimizer")
    }

    @Test
    fun `optimizer WOT entries have required fields populated`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        for (entry in wotEntries) {
            assertTrue(entry.rpm > 0, "WOT entry should have RPM > 0")
            assertTrue(entry.throttleAngle >= 80.0, "WOT entry should have throttle >= 80")
        }
    }

    // ── 3. Mixed / Cruise Log Edge Cases ────────────────────────────

    @Test
    fun `mixed log has some WOT entries`() {
        val me7Data = parseAndAdapt("2023-05-19_21.31.12_log.csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        // Mixed log should have some WOT
        println("Mixed log WOT entries: ${wotEntries.size}")
        assertTrue(wotEntries.isNotEmpty(), "Mixed log should have some WOT entries")
    }

    @Test
    fun `cruise-only log produces zero or minimal WOT entries`() {
        val me7Data = parseAndAdapt("2023-05-19_21.08.33_log.csv")
        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)

        // Cruise log should have few/no WOT entries
        println("Cruise log WOT entries: ${wotEntries.size}")
        assertTrue(wotEntries.size < 50, "Cruise log should have few WOT entries, got ${wotEntries.size}")
    }

    // ── 4. Map + Log Cross-Validation ───────────────────────────────

    @Test
    fun `WOT RPM range overlaps with KFMIOP RPM axis`() {
        val me7Data = parseAndAdapt("2025-01-21_16.24.32_log(1).csv")
        val defs = parseXdf(XDF_FILE)
        val maps = parseBin(BIN_FILE, defs)

        val kfmiop = findMap(maps, "Max indexed eng tq") ?: return

        // Only run if KFMIOP has a real yAxis
        if (kfmiop.yAxis.isEmpty()) {
            println("KFMIOP has empty yAxis — skipping cross-validation")
            return
        }

        val wotEntries = OptimizerCalculator.filterWotEntries(me7Data, 80.0)
        if (wotEntries.isEmpty()) return

        val wotRpmRange = wotEntries.minOf { it.rpm }..wotEntries.maxOf { it.rpm }
        val kfmiopRpmRange = kfmiop.yAxis.min()..kfmiop.yAxis.max()

        println("WOT RPM range: $wotRpmRange")
        println("KFMIOP RPM axis range: $kfmiopRpmRange")

        val overlapping = wotEntries.count { it.rpm in kfmiopRpmRange }
        assertTrue(overlapping > 0,
            "No WOT RPMs overlap with KFMIOP axis range $kfmiopRpmRange")
        println("WOT entries overlapping KFMIOP RPM axis: $overlapping/${wotEntries.size}")
    }
}
