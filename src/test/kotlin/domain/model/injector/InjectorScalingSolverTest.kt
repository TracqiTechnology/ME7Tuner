package domain.model.injector

import kotlin.math.sqrt
import kotlin.test.*

/**
 * Tests for [InjectorScalingSolver] — KRKTE scaling and TVUB dead time computation.
 */
class InjectorScalingSolverTest {

    // ── computeKrkteScaling ─────────────────────────────────────────────

    @Test
    fun `same injectors produce scale factor of 1`() {
        val spec = InjectorSpec(flowRateCcPerMin = 310.0, fuelPressureBar = 3.0, deadTimeMs = 1.0)
        val result = InjectorScalingSolver.computeKrkteScaling(spec, spec)

        assertEquals(1.0, result.scaleFactor, 1e-9)
        assertEquals(1.0, result.flowRatio, 1e-9)
        assertEquals(1.0, result.pressureCorrection, 1e-9)
        assertTrue(result.warnings.isEmpty(), "No warnings expected for identical injectors")
    }

    @Test
    fun `upgrade to larger injector at same pressure`() {
        val old = InjectorSpec(flowRateCcPerMin = 310.0, fuelPressureBar = 3.0, deadTimeMs = 0.8)
        val new = InjectorSpec(flowRateCcPerMin = 630.0, fuelPressureBar = 3.0, deadTimeMs = 1.1)
        val result = InjectorScalingSolver.computeKrkteScaling(old, new)

        val expectedFlowRatio = 310.0 / 630.0
        assertEquals(expectedFlowRatio, result.flowRatio, 1e-4)
        assertEquals(1.0, result.pressureCorrection, 1e-9)
        assertEquals(expectedFlowRatio, result.scaleFactor, 1e-4)
        assertTrue(result.scaleFactor < 1.0, "Larger injector → scaleFactor < 1.0")
    }

    @Test
    fun `upgrade flow with pressure change applies Bernoulli correction`() {
        val old = InjectorSpec(flowRateCcPerMin = 310.0, fuelPressureBar = 3.0, deadTimeMs = 0.8)
        val new = InjectorSpec(flowRateCcPerMin = 630.0, fuelPressureBar = 4.0, deadTimeMs = 1.1)
        val result = InjectorScalingSolver.computeKrkteScaling(old, new)

        val expectedFlowRatio = 310.0 / 630.0
        val expectedPressureCorrection = sqrt(4.0 / 3.0)
        val expectedScale = expectedFlowRatio * expectedPressureCorrection

        assertEquals(expectedFlowRatio, result.flowRatio, 1e-4)
        assertEquals(expectedPressureCorrection, result.pressureCorrection, 1e-4)
        assertEquals(expectedScale, result.scaleFactor, 1e-4)
    }

    @Test
    fun `downgrade to smaller injector produces scale factor greater than 1`() {
        val old = InjectorSpec(flowRateCcPerMin = 630.0, fuelPressureBar = 3.0, deadTimeMs = 1.0)
        val new = InjectorSpec(flowRateCcPerMin = 310.0, fuelPressureBar = 3.0, deadTimeMs = 0.8)
        val result = InjectorScalingSolver.computeKrkteScaling(old, new)

        assertTrue(result.scaleFactor > 1.0,
            "Smaller injector should produce scaleFactor > 1.0, got ${result.scaleFactor}")
        assertEquals(630.0 / 310.0, result.flowRatio, 1e-4)
    }

    @Test
    fun `extreme scale factor generates warning`() {
        // 200 → 1000 cc: scaleFactor = 0.2, which is < 0.3
        val old = InjectorSpec(flowRateCcPerMin = 200.0, deadTimeMs = 0.5)
        val new = InjectorSpec(flowRateCcPerMin = 1000.0, deadTimeMs = 1.2)
        val result = InjectorScalingSolver.computeKrkteScaling(old, new)

        assertEquals(0.2, result.scaleFactor, 1e-4)
        assertTrue(result.warnings.any { "extreme" in it.lowercase() },
            "Expected extreme scale factor warning, got: ${result.warnings}")
    }

    @Test
    fun `large injector idle stability warning`() {
        // 310 → 800 cc: 800 > 310 * 2.5 = 775
        val old = InjectorSpec(flowRateCcPerMin = 310.0, deadTimeMs = 0.8)
        val new = InjectorSpec(flowRateCcPerMin = 800.0, deadTimeMs = 1.2)
        val result = InjectorScalingSolver.computeKrkteScaling(old, new)

        assertTrue(result.warnings.any { "idle" in it.lowercase() || "TEMIN" in it },
            "Expected idle stability warning, got: ${result.warnings}")
    }

    @Test
    fun `both dead times zero generates warning`() {
        val old = InjectorSpec(flowRateCcPerMin = 310.0, deadTimeMs = 0.0)
        val new = InjectorSpec(flowRateCcPerMin = 630.0, deadTimeMs = 0.0)
        val result = InjectorScalingSolver.computeKrkteScaling(old, new)

        assertTrue(result.warnings.any { "dead time" in it.lowercase() || "zero" in it.lowercase() },
            "Expected dead time warning, got: ${result.warnings}")
    }

    @Test
    fun `one non-zero dead time does not trigger zero warning`() {
        val old = InjectorSpec(flowRateCcPerMin = 310.0, deadTimeMs = 0.8)
        val new = InjectorSpec(flowRateCcPerMin = 630.0, deadTimeMs = 0.0)
        val result = InjectorScalingSolver.computeKrkteScaling(old, new)

        assertFalse(result.warnings.any { "dead times are both zero" in it.lowercase() },
            "Should not warn about zero dead times when one is non-zero")
    }

    @Test
    fun `non-positive old flow rate throws`() {
        val old = InjectorSpec(flowRateCcPerMin = 0.0)
        val new = InjectorSpec(flowRateCcPerMin = 630.0)
        assertFailsWith<IllegalArgumentException> {
            InjectorScalingSolver.computeKrkteScaling(old, new)
        }
    }

    @Test
    fun `negative new flow rate throws`() {
        val old = InjectorSpec(flowRateCcPerMin = 310.0)
        val new = InjectorSpec(flowRateCcPerMin = -100.0)
        assertFailsWith<IllegalArgumentException> {
            InjectorScalingSolver.computeKrkteScaling(old, new)
        }
    }

    @Test
    fun `dead time values are passed through in result`() {
        val old = InjectorSpec(flowRateCcPerMin = 310.0, deadTimeMs = 0.85)
        val new = InjectorSpec(flowRateCcPerMin = 630.0, deadTimeMs = 1.15)
        val result = InjectorScalingSolver.computeKrkteScaling(old, new)

        assertEquals(0.85, result.oldDeadTimeMs, 1e-9)
        assertEquals(1.15, result.newDeadTimeMs, 1e-9)
    }

    @Test
    fun `pressure difference below threshold treated as equal`() {
        // 0.005 bar difference is within the 0.01 tolerance
        val old = InjectorSpec(flowRateCcPerMin = 310.0, fuelPressureBar = 3.000, deadTimeMs = 1.0)
        val new = InjectorSpec(flowRateCcPerMin = 310.0, fuelPressureBar = 3.005, deadTimeMs = 1.0)
        val result = InjectorScalingSolver.computeKrkteScaling(old, new)

        assertEquals(1.0, result.pressureCorrection, 1e-9,
            "Tiny pressure difference should not trigger Bernoulli correction")
    }

    // ── RS3 2.5T realistic scenario ─────────────────────────────────────

    @Test
    fun `RS3 2_5T injector upgrade scenario`() {
        val stock = InjectorSpec(flowRateCcPerMin = 480.0, fuelPressureBar = 4.0, deadTimeMs = 0.9)
        val upgrade = InjectorSpec(flowRateCcPerMin = 630.0, fuelPressureBar = 4.0, deadTimeMs = 1.1)
        val result = InjectorScalingSolver.computeKrkteScaling(stock, upgrade)

        val expectedRatio = 480.0 / 630.0  // ≈ 0.7619
        assertEquals(expectedRatio, result.flowRatio, 1e-4)
        assertEquals(1.0, result.pressureCorrection, 1e-9, "Same pressure → no correction")
        assertEquals(expectedRatio, result.scaleFactor, 1e-4)
        assertEquals(0.9, result.oldDeadTimeMs, 1e-9)
        assertEquals(1.1, result.newDeadTimeMs, 1e-9)
        assertTrue(result.warnings.isEmpty(),
            "Moderate upgrade should not trigger warnings, got: ${result.warnings}")
    }

    // ── computeTvub ─────────────────────────────────────────────────────

    @Test
    fun `single dead time uses 1 over V relationship`() {
        val spec = InjectorSpec(flowRateCcPerMin = 630.0, deadTimeMs = 1.0)
        val result = InjectorScalingSolver.computeTvub(spec)

        // At 14V: deadTime = 1.0 × 14/14 = 1.0
        val idx14 = result.voltageAxis.indexOfFirst { it == 14.0 }
        assertTrue(idx14 >= 0, "Default axis should include 14V")
        assertEquals(1.0, result.deadTimes[idx14], 1e-9, "At 14V dead time should equal reference")

        // At 7V: deadTime = 1.0 × 14/7 = 2.0
        val idx = result.voltageAxis.indices.firstOrNull {
            kotlin.math.abs(result.voltageAxis[it] - 6.0) < 0.01
        }
        if (idx != null) {
            val expected = 1.0 * 14.0 / 6.0  // ≈ 2.333
            assertEquals(expected, result.deadTimes[idx], 1e-3)
        }
    }

    @Test
    fun `dead times decrease with increasing voltage`() {
        val spec = InjectorSpec(flowRateCcPerMin = 630.0, deadTimeMs = 1.0)
        val result = InjectorScalingSolver.computeTvub(spec)

        for (i in 1 until result.deadTimes.size) {
            assertTrue(result.deadTimes[i] <= result.deadTimes[i - 1],
                "Dead time should decrease with voltage: " +
                    "${result.voltageAxis[i - 1]}V=${result.deadTimes[i - 1]}ms > " +
                    "${result.voltageAxis[i]}V=${result.deadTimes[i]}ms")
        }
    }

    @Test
    fun `zero dead time produces all-zero TVUB`() {
        val spec = InjectorSpec(flowRateCcPerMin = 630.0, deadTimeMs = 0.0)
        val result = InjectorScalingSolver.computeTvub(spec)

        result.deadTimes.forEachIndexed { i, dt ->
            assertEquals(0.0, dt, 1e-9,
                "All dead times should be 0 when reference is 0, got $dt at ${result.voltageAxis[i]}V")
        }
    }

    @Test
    fun `multi-point voltage table interpolation`() {
        val table = mapOf(
            8.0 to 2.5,
            12.0 to 1.2,
            16.0 to 0.8
        )
        val spec = InjectorSpec(
            flowRateCcPerMin = 630.0,
            deadTimeVoltageTable = table
        )
        val result = InjectorScalingSolver.computeTvub(spec)

        // At 10V: linear interpolation between (8, 2.5) and (12, 1.2)
        // t = (10 - 8) / (12 - 8) = 0.5, deadTime = 2.5 + 0.5 * (1.2 - 2.5) = 1.85
        val idx10 = result.voltageAxis.indexOfFirst { kotlin.math.abs(it - 10.0) < 0.01 }
        assertTrue(idx10 >= 0, "Default axis should include 10V")
        assertEquals(1.85, result.deadTimes[idx10], 1e-4, "Interpolated at 10V")

        // At 14V: linear interpolation between (12, 1.2) and (16, 0.8)
        // t = (14 - 12) / (16 - 12) = 0.5, deadTime = 1.2 + 0.5 * (0.8 - 1.2) = 1.0
        val idx14 = result.voltageAxis.indexOfFirst { kotlin.math.abs(it - 14.0) < 0.01 }
        assertTrue(idx14 >= 0, "Default axis should include 14V")
        assertEquals(1.0, result.deadTimes[idx14], 1e-4, "Interpolated at 14V")
    }

    @Test
    fun `clamped extrapolation at boundaries`() {
        val table = mapOf(
            10.0 to 1.8,
            14.0 to 1.0
        )
        val spec = InjectorSpec(
            flowRateCcPerMin = 630.0,
            deadTimeVoltageTable = table
        )
        val result = InjectorScalingSolver.computeTvub(spec)

        // Below table range: 6V and 8V should clamp to 10V value (1.8)
        val idx6 = result.voltageAxis.indexOfFirst { kotlin.math.abs(it - 6.0) < 0.01 }
        val idx8 = result.voltageAxis.indexOfFirst { kotlin.math.abs(it - 8.0) < 0.01 }
        if (idx6 >= 0) assertEquals(1.8, result.deadTimes[idx6], 1e-9, "6V clamped to 10V value")
        if (idx8 >= 0) assertEquals(1.8, result.deadTimes[idx8], 1e-9, "8V clamped to 10V value")

        // Above table range: 16V should clamp to 14V value (1.0)
        val idx16 = result.voltageAxis.indexOfFirst { kotlin.math.abs(it - 16.0) < 0.01 }
        if (idx16 >= 0) assertEquals(1.0, result.deadTimes[idx16], 1e-9, "16V clamped to 14V value")

        // Within range: 12V should interpolate → (12-10)/(14-10) = 0.5, 1.8 + 0.5*(1.0-1.8) = 1.4
        val idx12 = result.voltageAxis.indexOfFirst { kotlin.math.abs(it - 12.0) < 0.01 }
        if (idx12 >= 0) assertEquals(1.4, result.deadTimes[idx12], 1e-4, "12V interpolated")
    }

    @Test
    fun `custom voltage axis is respected`() {
        val spec = InjectorSpec(flowRateCcPerMin = 630.0, deadTimeMs = 1.0)
        val customAxis = doubleArrayOf(11.0, 12.0, 13.0, 14.0, 15.0)
        val result = InjectorScalingSolver.computeTvub(spec, customAxis)

        assertEquals(customAxis.size, result.voltageAxis.size)
        assertContentEquals(customAxis, result.voltageAxis)

        // Verify 1/V relationship at each custom point
        for (i in customAxis.indices) {
            val expected = 1.0 * 14.0 / customAxis[i]
            assertEquals(expected, result.deadTimes[i], 1e-6,
                "1/V relationship at ${customAxis[i]}V")
        }
    }

    @Test
    fun `default voltage axis has expected points`() {
        val spec = InjectorSpec(flowRateCcPerMin = 630.0, deadTimeMs = 1.0)
        val result = InjectorScalingSolver.computeTvub(spec)

        val expected = doubleArrayOf(6.0, 8.0, 10.0, 12.0, 14.0, 16.0)
        assertContentEquals(expected, result.voltageAxis)
    }

    @Test
    fun `multi-point table with exact axis hits returns exact values`() {
        val table = mapOf(
            6.0 to 3.0,
            10.0 to 1.5,
            14.0 to 1.0,
            16.0 to 0.8
        )
        val spec = InjectorSpec(
            flowRateCcPerMin = 630.0,
            deadTimeVoltageTable = table
        )
        // Use an axis that exactly matches the table keys
        val axis = doubleArrayOf(6.0, 10.0, 14.0, 16.0)
        val result = InjectorScalingSolver.computeTvub(spec, axis)

        assertEquals(3.0, result.deadTimes[0], 1e-9, "Exact hit at 6V")
        assertEquals(1.5, result.deadTimes[1], 1e-9, "Exact hit at 10V")
        assertEquals(1.0, result.deadTimes[2], 1e-9, "Exact hit at 14V")
        assertEquals(0.8, result.deadTimes[3], 1e-9, "Exact hit at 16V")
    }
}
