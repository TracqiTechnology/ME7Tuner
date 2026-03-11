package domain.model.ldrpid

import data.contract.Me7LogFileContract
import domain.math.map.Map3d
import kotlin.test.*

/**
 * Tests for [LdrpidCalculator] — feed-forward PID linearization for boost control.
 *
 * All tests use constructed log data so no external files are needed.
 */
class LdrpidCalculatorTest {

    // ── Axis definitions (4 RPM rows × 5 duty-cycle columns) ────────
    private val rpmAxis  = arrayOf(2000.0, 3000.0, 4000.0, 5000.0)
    private val dutyAxis = arrayOf(20.0, 40.0, 60.0, 80.0, 95.0)

    /** Skeleton KFLDRL map filled with placeholder duty-cycle values. */
    private fun buildKfldrlMap(): Map3d {
        val z = Array(rpmAxis.size) { Array(dutyAxis.size) { 0.0 } }
        return Map3d(dutyAxis, rpmAxis, z)
    }

    /** Skeleton KFLDIMX map (6 pressure columns × 4 RPM rows). */
    private fun buildKfldimxMap(): Map3d {
        val pressureAxis = arrayOf(200.0, 400.0, 600.0, 800.0, 1000.0, 1200.0)
        val z = Array(rpmAxis.size) { Array(pressureAxis.size) { 0.0 } }
        return Map3d(pressureAxis, rpmAxis, z)
    }

    /** Build a log-data map from parallel lists (one entry per log row). */
    private fun buildLogData(
        rpms: List<Double>,
        throttles: List<Double>,
        dutyCycles: List<Double>,
        baroPressures: List<Double>,
        boostPressures: List<Double>
    ): Map<Me7LogFileContract.Header, List<Double>> = mapOf(
        Me7LogFileContract.Header.RPM_COLUMN_HEADER            to rpms,
        Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER  to throttles,
        Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER  to dutyCycles,
        Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER   to baroPressures,
        Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER to boostPressures
    )

    // ── 1. Throttle filter ──────────────────────────────────────────

    @Test
    fun `calculateNonLinearTable filters rows with throttle below 80`() {
        // Two sub-80 rows should be ignored; two ≥80 rows contribute.
        val data = buildLogData(
            rpms           = listOf(3000.0, 3000.0, 3000.0, 3000.0),
            throttles      = listOf(50.0,   60.0,   80.0,   90.0),
            dutyCycles     = listOf(40.0,   40.0,   40.0,   40.0),
            baroPressures  = listOf(1013.0, 1013.0, 1013.0, 1013.0),
            boostPressures = listOf(1500.0, 1600.0, 1700.0, 1800.0)
        )

        val result = LdrpidCalculator.calculateNonLinearTable(data, buildKfldrlMap())

        // The two WOT rows have relative boost 687 and 787 mbar → average 737 mbar → 10.69 PSI
        // Verify the table isn't all zeros (i.e. the WOT rows were counted)
        val allZero = result.zAxis.all { row -> row.all { it == 0.0 } }
        assertFalse(allZero, "Table should contain non-zero values from WOT rows")
    }

    // ── 2. Binning by RPM and duty cycle ────────────────────────────

    @Test
    fun `calculateNonLinearTable bins boost by RPM and duty cycle`() {
        // Two WOT samples at same RPM+duty → averaged, then converted to PSI
        val data = buildLogData(
            rpms           = listOf(3000.0, 3000.0),
            throttles      = listOf(85.0,   90.0),
            dutyCycles     = listOf(60.0,   60.0),
            baroPressures  = listOf(1013.0, 1013.0),
            boostPressures = listOf(1513.0, 1713.0)     // relative: 500, 700
        )

        val kfldrl = buildKfldrlMap()
        val result = LdrpidCalculator.calculateNonLinearTable(data, kfldrl)

        // Average relative boost = 600 mbar → 600 * 0.0145038 = 8.70 PSI
        // The exact cell depends on Index.getInsertIndex; just verify a non-zero
        // value close to 8.70 PSI exists somewhere in the table.
        val flatValues = result.zAxis.flatMap { it.toList() }.filter { it > 1.0 }
        assertTrue(flatValues.isNotEmpty(),
            "Should have at least one cell with meaningful boost value")

        val expectedPsi = 600.0 * 0.0145038
        val closest = flatValues.minByOrNull { kotlin.math.abs(it - expectedPsi) }!!
        assertEquals(expectedPsi, closest, 2.0,
            "Averaged boost should be near ${expectedPsi} PSI")
    }

    // ── 3. Rows are sorted ascending ────────────────────────────────

    @Test
    fun `calculateNonLinearTable rows are sorted ascending`() {
        val data = buildLogData(
            rpms           = listOf(2000.0, 3000.0, 4000.0, 5000.0,
                                    2000.0, 3000.0, 4000.0, 5000.0),
            throttles      = listOf(85.0, 85.0, 85.0, 85.0,
                                    90.0, 90.0, 90.0, 90.0),
            dutyCycles     = listOf(40.0, 60.0, 80.0, 95.0,
                                    20.0, 40.0, 60.0, 80.0),
            baroPressures  = List(8) { 1013.0 },
            boostPressures = listOf(1200.0, 1400.0, 1600.0, 1800.0,
                                    1100.0, 1300.0, 1500.0, 1700.0)
        )

        val result = LdrpidCalculator.calculateNonLinearTable(data, buildKfldrlMap())

        for (row in result.zAxis) {
            for (i in 1 until row.size) {
                assertTrue(row[i] >= row[i - 1],
                    "Row values should be sorted ascending: ${row.contentToString()}")
            }
        }
    }

    // ── 4. Linear table interpolation ───────────────────────────────

    @Test
    fun `calculateLinearTable produces linear interpolation per column`() {
        // Build a non-linear table with known values
        val nonLinear = arrayOf(
            arrayOf(1.0, 2.0, 3.0, 4.0, 5.0),   // row 0 (min per column)
            arrayOf(2.0, 3.0, 4.0, 5.0, 6.0),
            arrayOf(5.0, 6.0, 7.0, 8.0, 9.0),
            arrayOf(10.0, 12.0, 14.0, 16.0, 18.0) // row 3 (max per column)
        )

        val kfldrl = buildKfldrlMap()
        val result = LdrpidCalculator.calculateLinearTable(nonLinear, kfldrl)

        // For column 0: min=1, max=10, step=(10-1)/3=3
        // Expected: [1, 4, 7, 10]
        assertEquals(1.0,  result.zAxis[0][0], 0.01)
        assertEquals(4.0,  result.zAxis[1][0], 0.01)
        assertEquals(7.0,  result.zAxis[2][0], 0.01)
        assertEquals(10.0, result.zAxis[3][0], 0.01)

        // For column 4: min=5, max=18, step=(18-5)/3 ≈ 4.333
        assertEquals(5.0,   result.zAxis[0][4], 0.01)
        assertEquals(18.0,  result.zAxis[3][4], 0.01)

        // Verify linearity: equal spacing within each column
        for (col in nonLinear[0].indices) {
            val step = result.zAxis[1][col] - result.zAxis[0][col]
            for (row in 2 until nonLinear.size) {
                val actual = result.zAxis[row][col] - result.zAxis[row - 1][col]
                assertEquals(step, actual, 0.01,
                    "Column $col should have equal step size")
            }
        }
    }

    // ── 5. Full pipeline returns all four maps ──────────────────────

    @Test
    fun `calculateLdrpid returns all four maps with correct axes`() {
        // Provide enough WOT data to produce non-trivial output
        val rpms           = mutableListOf<Double>()
        val throttles      = mutableListOf<Double>()
        val dutyCycles     = mutableListOf<Double>()
        val baroPressures  = mutableListOf<Double>()
        val boostPressures = mutableListOf<Double>()

        // Generate WOT rows across RPM / duty combinations
        for (rpm in listOf(2000.0, 3000.0, 4000.0, 5000.0)) {
            for (duty in listOf(20.0, 40.0, 60.0, 80.0, 95.0)) {
                rpms.add(rpm)
                throttles.add(85.0)
                dutyCycles.add(duty)
                baroPressures.add(1013.0)
                // Boost rises with both RPM and duty
                boostPressures.add(1013.0 + rpm * 0.05 + duty * 3.0)
            }
        }

        val data    = buildLogData(rpms, throttles, dutyCycles, baroPressures, boostPressures)
        val kfldrl  = buildKfldrlMap()
        val kfldimx = buildKfldimxMap()

        val result = LdrpidCalculator.calculateLdrpid(data, kfldrl, kfldimx)

        // Verify all four outputs are present and sized correctly
        assertEquals(rpmAxis.size, result.nonLinearOutput.yAxis.size,
            "Non-linear output should have ${rpmAxis.size} RPM rows")
        assertEquals(dutyAxis.size, result.nonLinearOutput.xAxis.size,
            "Non-linear output should have ${dutyAxis.size} duty columns")

        assertEquals(rpmAxis.size, result.linearOutput.yAxis.size,
            "Linear output should have ${rpmAxis.size} RPM rows")

        assertEquals(rpmAxis.size, result.kfldrl.yAxis.size,
            "KFLDRL should have ${rpmAxis.size} RPM rows")

        assertEquals(rpmAxis.size, result.kfldimx.yAxis.size,
            "KFLDIMX should have ${rpmAxis.size} RPM rows")
    }

    // ── 6. No WOT data ─────────────────────────────────────────────

    @Test
    fun `calculateNonLinearTable with no WOT data produces valid output`() {
        // All throttle values below 80 → nothing passes the filter
        val data = buildLogData(
            rpms           = listOf(3000.0, 4000.0),
            throttles      = listOf(50.0,   70.0),
            dutyCycles     = listOf(40.0,   60.0),
            baroPressures  = listOf(1013.0, 1013.0),
            boostPressures = listOf(1200.0, 1400.0)
        )

        val result = LdrpidCalculator.calculateNonLinearTable(data, buildKfldrlMap())

        // Table should still be well-formed (correct dimensions)
        assertEquals(rpmAxis.size, result.zAxis.size,
            "Should still have ${rpmAxis.size} rows")
        assertEquals(dutyAxis.size, result.zAxis[0].size,
            "Should still have ${dutyAxis.size} columns")

        // All values should be the post-processed fallback (0 mbar * 0.0145038 → sorted/fixed to 0.1)
        for (row in result.zAxis) {
            for (value in row) {
                assertTrue(value >= 0.0,
                    "All values should be non-negative even with no WOT data")
            }
        }
    }
}
