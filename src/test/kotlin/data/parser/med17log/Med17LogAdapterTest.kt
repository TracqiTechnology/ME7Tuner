package data.parser.med17log

import data.contract.Me7LogFileContract
import data.contract.Med17LogFileContract
import java.io.File
import kotlin.test.*

/**
 * Tests for [Med17LogAdapter] — verifies signal mapping from MED17 to ME7 format.
 */
class Med17LogAdapterTest {

    private val parser = Med17LogParser()

    private fun logFile(name: String): File {
        val url = javaClass.classLoader.getResource("logs/$name")
            ?: error("Test resource not found: logs/$name")
        return File(url.toURI())
    }

    private fun parsedOptimizerData(): Map<Med17LogFileContract.Header, List<Double>> {
        return parser.parseLogFile(Med17LogParser.LogType.OPTIMIZER, logFile("2025-01-21_16.24.32_log(1).csv"))
    }

    private fun parsedLdrpidData(): Map<Med17LogFileContract.Header, List<Double>> {
        return parser.parseLogFile(Med17LogParser.LogType.LDRPID, logFile("2025-01-21_16.24.32_log(1).csv"))
    }

    // ── T4: Optimizer mapping ───────────────────────────────────────

    @Test
    fun `toMe7OptimizerFormat populates required ME7 headers`() {
        val med17Data = parsedOptimizerData()
        val me7Data = Med17LogAdapter.toMe7OptimizerFormat(med17Data)

        val requiredHeaders = listOf(
            Me7LogFileContract.Header.RPM_COLUMN_HEADER,
            Me7LogFileContract.Header.ENGINE_LOAD_HEADER,
            Me7LogFileContract.Header.REQUESTED_LOAD_HEADER,
            Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER,
            Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER,
            Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER,
            Me7LogFileContract.Header.REQUESTED_PRESSURE_HEADER,
            Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER,
            Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER,
        )

        for (header in requiredHeaders) {
            val list = me7Data[header]
            assertNotNull(list, "ME7 header ${header.name} should be present")
            assertTrue(list.isNotEmpty(), "ME7 header ${header.name} should have data")
        }
    }

    @Test
    fun `toMe7OptimizerFormat preserves signal values through mapping`() {
        val med17Data = parsedOptimizerData()
        val me7Data = Med17LogAdapter.toMe7OptimizerFormat(med17Data)

        // RPM: MED17 nmot_w → ME7 RPM — values should be identical
        val med17Rpms = med17Data[Med17LogFileContract.Header.RPM_COLUMN_HEADER]!!
        val me7Rpms = me7Data[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!
        assertEquals(med17Rpms.size, me7Rpms.size, "RPM list sizes should match")
        assertEquals(med17Rpms[0], me7Rpms[0], 0.001, "First RPM value should pass through unchanged")

        // Engine load: MED17 rl_w → ME7 ENGINE_LOAD
        val med17Load = med17Data[Med17LogFileContract.Header.ENGINE_LOAD_HEADER]!!
        val me7Load = me7Data[Me7LogFileContract.Header.ENGINE_LOAD_HEADER]!!
        assertEquals(med17Load[0], me7Load[0], 0.001, "Engine load should pass through unchanged")

        // Boost pressure: MED17 psrg_w → ME7 ABSOLUTE_BOOST_PRESSURE_ACTUAL
        val med17Boost = med17Data[Med17LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!
        val me7Boost = me7Data[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!
        assertEquals(med17Boost[0], me7Boost[0], 0.001, "Boost pressure should pass through unchanged")
    }

    @Test
    fun `toMe7OptimizerFormat maps rl_w to both ENGINE_LOAD and ACTUAL_LOAD`() {
        val med17Data = parsedOptimizerData()
        val me7Data = Med17LogAdapter.toMe7OptimizerFormat(med17Data)

        val engineLoad = me7Data[Me7LogFileContract.Header.ENGINE_LOAD_HEADER]!!
        val actualLoad = me7Data[Me7LogFileContract.Header.ACTUAL_LOAD_HEADER]!!

        assertEquals(engineLoad.size, actualLoad.size, "Both load lists should have same size")
        assertEquals(engineLoad[0], actualLoad[0], 0.001, "rl_w should be mapped to both ENGINE_LOAD and ACTUAL_LOAD")
    }

    @Test
    fun `toMe7OptimizerFormat leaves MAF signals empty`() {
        val med17Data = parsedOptimizerData()
        val me7Data = Med17LogAdapter.toMe7OptimizerFormat(med17Data)

        // MED17 has no MAF sensor — these should be empty lists
        val mafVoltage = me7Data[Me7LogFileContract.Header.MAF_VOLTAGE_HEADER]!!
        val mafGrams = me7Data[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER]!!

        assertTrue(mafVoltage.isEmpty(), "MAF voltage should be empty for MED17 (speed-density)")
        assertTrue(mafGrams.isEmpty(), "MAF g/s should be empty for MED17 (speed-density)")
    }

    @Test
    fun `toMe7OptimizerFormat produces correct row count`() {
        val med17Data = parsedOptimizerData()
        val me7Data = Med17LogAdapter.toMe7OptimizerFormat(med17Data)

        assertEquals(949, me7Data[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!.size)
    }

    // ── T5: LDRPID mapping ──────────────────────────────────────────

    @Test
    fun `toMe7LdrpidFormat populates required headers`() {
        val med17Data = parsedLdrpidData()
        val me7Data = Med17LogAdapter.toMe7LdrpidFormat(med17Data)

        val requiredHeaders = listOf(
            Me7LogFileContract.Header.RPM_COLUMN_HEADER,
            Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER,
            Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER,
            Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER,
            Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER,
            Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER,
        )

        for (header in requiredHeaders) {
            val list = me7Data[header]
            assertNotNull(list, "ME7 LDRPID header ${header.name} should be present")
            assertTrue(list.isNotEmpty(), "ME7 LDRPID header ${header.name} should have data")
        }
    }

    @Test
    fun `toMe7LdrpidFormat preserves values`() {
        val med17Data = parsedLdrpidData()
        val me7Data = Med17LogAdapter.toMe7LdrpidFormat(med17Data)

        val med17Rpms = med17Data[Med17LogFileContract.Header.RPM_COLUMN_HEADER]!!
        val me7Rpms = me7Data[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!
        assertEquals(med17Rpms[0], me7Rpms[0], 0.001, "RPM should pass through unchanged")
        assertEquals(949, me7Rpms.size, "Should have 949 LDRPID rows")
    }
}
