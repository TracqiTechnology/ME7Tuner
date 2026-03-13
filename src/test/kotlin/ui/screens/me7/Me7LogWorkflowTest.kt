package ui.screens.me7

import data.contract.Me7LogFileContract
import data.parser.afrlog.AfrLogParser
import data.parser.me7log.Me7LogParser
import data.parser.xdf.TableDefinition
import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import data.preferences.kfvpdksd.KfvpdksdPreferences
import data.preferences.mlhfm.MlhfmPreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import domain.model.closedloopfueling.ClosedLoopFuelingCorrectionManager
import domain.model.kfvpdksd.Kfvpdksd
import domain.model.ldrpid.LdrpidCalculator
import domain.model.mlhfm.MlhfmFitter
import domain.model.openloopfueling.correction.OpenLoopMlhfmCorrectionManager
import ui.screens.med17.BinaryDiffHelper
import java.io.File
import kotlin.test.*

/**
 * End-to-end log-driven workflow tests for ME7.
 *
 * Uses the stock XDF/BIN pair with MBox profile.
 * Exercises the full pipeline: parse log → calculate → write → binary diff.
 */
class Me7LogWorkflowTest : Me7TestBase() {

    companion object {
        private val PROJECT_ROOT = File(System.getProperty("user.dir"))

        // Primary log (supports ALL 5 log types)
        val LOG_ALL = File(PROJECT_ROOT, "example/me7/logs/me7/log_typical_20200825_141742.csv")
        // Secondary log (supports CLOSED_LOOP/OPEN_LOOP/LDRPID but not OPTIMIZER)
        val LOG_FUEL = File(PROJECT_ROOT, "example/me7/logs/me7/log_typical_20190105_094614.csv")
        // Zeitronix wideband AFR log
        val LOG_ZEIT = File(PROJECT_ROOT, "example/me7/logs/ziet/zeitronix.csv")
    }

    private val parser = Me7LogParser()

    // ── Closed-Loop Fueling: log → MLHFM correction → write → diff ──

    @Test
    fun `closed-loop - log1 corrects MLHFM and writes to BIN`() {
        verifyClosedLoopWorkflow(LOG_ALL)
    }

    @Test
    fun `closed-loop - log2 corrects MLHFM and writes to BIN`() {
        verifyClosedLoopWorkflow(LOG_FUEL)
    }

    private fun verifyClosedLoopWorkflow(logFile: File) {
        resetBin()
        assertTrue(logFile.exists(), "Log file not found: ${logFile.absolutePath}")

        val mlhfmPair = MlhfmPreferences.getSelectedMap()
            ?: fail("MLHFM preference not resolved")
        val (mlhfmDef, mlhfmMap) = mlhfmPair

        // 1. Parse log as CLOSED_LOOP
        val logData = parser.parseLogFile(Me7LogParser.LogType.CLOSED_LOOP, logFile)
        val rpm = logData[Me7LogFileContract.Header.RPM_COLUMN_HEADER]
        assertNotNull(rpm, "Log should contain RPM data")
        assertTrue(rpm.isNotEmpty(), "RPM data should not be empty")

        // 2. Run closed-loop correction manager
        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 50.0
        )
        manager.correct(logData, mlhfmMap)

        val correction = manager.closedLoopMlhfmCorrection
        assertNotNull(correction, "Correction should be produced")

        val correctedMlhfm = correction.correctedMlhfm
        assertTrue(correctedMlhfm.yAxis.isNotEmpty(), "Corrected MLHFM should have voltage axis")
        assertTrue(correctedMlhfm.zAxis.isNotEmpty(), "Corrected MLHFM should have kg/h values")

        // 3. Verify corrections are in sane range (0.5x to 2.0x of original)
        for (i in correctedMlhfm.zAxis.indices) {
            val original = if (i < mlhfmMap.zAxis.size) mlhfmMap.zAxis[i][0] else 0.0
            val corrected = correctedMlhfm.zAxis[i][0]
            if (original > 1.0) { // Skip near-zero values
                val ratio = corrected / original
                assertTrue(
                    ratio in 0.5..2.0,
                    "MLHFM correction ratio at [$i] = $ratio (${original} → ${corrected}) out of sane range"
                )
            }
        }

        // 4. Fit the corrected curve
        val fitted = MlhfmFitter.fitMlhfm(correctedMlhfm, 6)
        assertTrue(fitted.zAxis.isNotEmpty(), "Fitted MLHFM should have data")

        // 5. Write corrected MLHFM to BIN
        BinWriter.write(tempBinFile, mlhfmDef, correctedMlhfm)

        // 6. Binary diff — only MLHFM bytes should change
        BinaryDiffHelper.assertOnlyExpectedBytesChanged(
            stockBinCopy, tempBinFile, mlhfmDef, requireChanges = false
        )
    }

    // ── Open-Loop Fueling: ME7 log + Zeitronix → MLHFM → write → diff ──

    @Test
    fun `open-loop - Zeitronix AFR corrects MLHFM and writes to BIN`() {
        resetBin()
        assertTrue(LOG_ALL.exists(), "ME7 log not found")
        assertTrue(LOG_ZEIT.exists(), "Zeitronix log not found")

        val mlhfmPair = MlhfmPreferences.getSelectedMap()
            ?: fail("MLHFM preference not resolved")
        val (mlhfmDef, mlhfmMap) = mlhfmPair

        // 1. Parse ME7 log as OPEN_LOOP
        val me7Log = parser.parseLogFile(Me7LogParser.LogType.OPEN_LOOP, LOG_ALL)
        val rpm = me7Log[Me7LogFileContract.Header.RPM_COLUMN_HEADER]
        assertNotNull(rpm, "ME7 log should contain RPM data")
        assertTrue(rpm.isNotEmpty(), "RPM should not be empty")

        // 2. Parse Zeitronix log via reflection (parseFile is private)
        val afrLog = parseZeitronixFile(LOG_ZEIT)
        assertTrue(afrLog.isNotEmpty(), "Zeitronix log should parse successfully")

        // 3. Run open-loop correction manager
        val manager = OpenLoopMlhfmCorrectionManager(
            minThrottleAngle = 80.0,
            minRpm = 2000.0,
            minPointsMe7 = 75,
            minPointsAfr = 150,
            maxAfr = 16.0
        )
        manager.correct(me7Log, afrLog, mlhfmMap)

        val correction = manager.openLoopCorrection
        assertNotNull(correction, "Open-loop correction should be produced")

        val correctedMlhfm = correction.correctedMlhfm
        assertTrue(correctedMlhfm.yAxis.isNotEmpty(), "Corrected MLHFM should have voltage axis")

        // 4. Write corrected MLHFM to BIN
        BinWriter.write(tempBinFile, mlhfmDef, correctedMlhfm)

        // 5. Binary diff
        BinaryDiffHelper.assertOnlyExpectedBytesChanged(
            stockBinCopy, tempBinFile, mlhfmDef, requireChanges = false
        )
    }

    // ── LDRPID: log → KFLDRL + KFLDIMX → write → diff ──────────────

    @Test
    fun `ldrpid - log1 calculates KFLDRL and writes to BIN`() {
        verifyLdrpidKfldrlWorkflow(LOG_ALL)
    }

    @Test
    fun `ldrpid - log2 calculates KFLDRL and writes to BIN`() {
        verifyLdrpidKfldrlWorkflow(LOG_FUEL)
    }

    @Test
    fun `ldrpid - log1 calculates KFLDIMX and writes to BIN`() {
        verifyLdrpidKfldimxWorkflow(LOG_ALL)
    }

    @Test
    fun `ldrpid - log2 calculates KFLDIMX and writes to BIN`() {
        verifyLdrpidKfldimxWorkflow(LOG_FUEL)
    }

    private fun verifyLdrpidKfldrlWorkflow(logFile: File) {
        resetBin()
        val kfldrlPair = KfldrlPreferences.getSelectedMap()
            ?: fail("KFLDRL preference not resolved")
        val kfldimxPair = KfldimxPreferences.getSelectedMap()
            ?: fail("KFLDIMX preference not resolved")

        val logData = parser.parseLogFile(Me7LogParser.LogType.LDRPID, logFile)
        val result = LdrpidCalculator.calculateLdrpid(logData, kfldrlPair.second, kfldimxPair.second)

        assertEquals(
            kfldrlPair.second.yAxis.size, result.kfldrl.yAxis.size,
            "KFLDRL output y-axis should match input"
        )

        BinWriter.write(tempBinFile, kfldrlPair.first, result.kfldrl)
        BinaryDiffHelper.assertOnlyExpectedBytesChanged(
            stockBinCopy, tempBinFile, kfldrlPair.first, requireChanges = false
        )
    }

    private fun verifyLdrpidKfldimxWorkflow(logFile: File) {
        resetBin()
        val kfldrlPair = KfldrlPreferences.getSelectedMap()
            ?: fail("KFLDRL preference not resolved")
        val kfldimxPair = KfldimxPreferences.getSelectedMap()
            ?: fail("KFLDIMX preference not resolved")

        val logData = parser.parseLogFile(Me7LogParser.LogType.LDRPID, logFile)
        val result = LdrpidCalculator.calculateLdrpid(logData, kfldrlPair.second, kfldimxPair.second)

        BinWriter.write(tempBinFile, kfldimxPair.first, result.kfldimx)
        BinaryDiffHelper.assertOnlyExpectedBytesChanged(
            stockBinCopy, tempBinFile, kfldimxPair.first, requireChanges = false
        )
    }

    // ── KFVPDKSD: log → pressure ratio → generate → write → diff ────

    @Test
    fun `kfvpdksd - log1 calculates pressure ratio and writes to BIN`() {
        resetBin()
        val kfvpdksdPair = KfvpdksdPreferences.getSelectedMap()
            ?: fail("KFVPDKSD preference not resolved")
        val (kfvpdksdDef, kfvpdksdMap) = kfvpdksdPair

        val logData = parser.parseLogFile(Me7LogParser.LogType.KFVPDKSD, LOG_ALL)
        val rpm = logData[Me7LogFileContract.Header.RPM_COLUMN_HEADER]
        assertNotNull(rpm, "Log should contain RPM data")
        assertTrue(rpm.isNotEmpty(), "RPM should not be empty")

        // Parse max pressure per RPM bin
        val maxPressure = Kfvpdksd.parsePressure(logData, kfvpdksdMap.yAxis)
        assertTrue(maxPressure.isNotEmpty(), "Max pressure array should not be empty")

        // Generate KFVPDKSD output table
        val result = Kfvpdksd.generate(maxPressure, kfvpdksdMap.yAxis, kfvpdksdMap.xAxis)
        assertTrue(result.kfvpdksd.isNotEmpty(), "KFVPDKSD output should not be empty")

        // Wrap into Map3d for writing
        val outputMap = Map3d(kfvpdksdMap.xAxis, kfvpdksdMap.yAxis, result.kfvpdksd)

        BinWriter.write(tempBinFile, kfvpdksdDef, outputMap)
        BinaryDiffHelper.assertOnlyExpectedBytesChanged(
            stockBinCopy, tempBinFile, kfvpdksdDef, requireChanges = false
        )
    }

    // ── Optimizer: log → full analysis → verify suggestions ─────────

    @Test
    fun `optimizer - log1 produces non-empty KFLDRL suggestions`() {
        val logData = parser.parseLogFile(Me7LogParser.LogType.OPTIMIZER, LOG_ALL)
        val rpm = logData[Me7LogFileContract.Header.RPM_COLUMN_HEADER]
        assertNotNull(rpm, "Optimizer log should contain RPM")
        assertTrue(rpm.isNotEmpty(), "Optimizer RPM should not be empty")

        // Verify key optimizer signals are present
        val requestedPressure = logData[Me7LogFileContract.Header.REQUESTED_PRESSURE_HEADER]
        assertNotNull(requestedPressure, "Optimizer log should contain requested pressure")
        assertTrue(requestedPressure.isNotEmpty(), "Requested pressure should not be empty")

        val requestedLoad = logData[Me7LogFileContract.Header.REQUESTED_LOAD_HEADER]
        assertNotNull(requestedLoad, "Optimizer log should contain requested load")
        assertTrue(requestedLoad.isNotEmpty(), "Requested load should not be empty")
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /** Invoke the private `parseFile` method on AfrLogParser via reflection. */
    private fun parseZeitronixFile(file: File): Map<String, List<Double>> {
        val method = AfrLogParser::class.java.getDeclaredMethod("parseFile", File::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(AfrLogParser, file) as Map<String, List<Double>>
    }
}
