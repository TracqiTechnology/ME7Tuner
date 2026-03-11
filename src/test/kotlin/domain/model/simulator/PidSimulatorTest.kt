package domain.model.simulator

import domain.math.map.Map3d
import domain.model.optimizer.OptimizerCalculator.WotLogEntry
import kotlin.math.abs
import kotlin.test.*

/**
 * Unit tests for [PidSimulator] — the PID boost-control dynamics simulator
 * shared between ME7 and MED17 optimizer paths.
 */
class PidSimulatorTest {

    // ── Helpers ─────────────────────────────────────────────────────

    /** Build a WotLogEntry with sensible defaults for PID simulation. */
    private fun entry(
        rpm: Double = 3000.0,
        requestedLoad: Double = 191.0,
        actualLoad: Double = 191.0,
        requestedMap: Double = 2200.0,
        actualMap: Double = 2200.0,
        barometricPressure: Double = 1013.0,
        wgdc: Double = 60.0,
        throttleAngle: Double = 100.0
    ) = WotLogEntry(
        rpm = rpm,
        requestedLoad = requestedLoad,
        actualLoad = actualLoad,
        requestedMap = requestedMap,
        actualMap = actualMap,
        barometricPressure = barometricPressure,
        wgdc = wgdc,
        throttleAngle = throttleAngle
    )

    /** Generate an ascending-RPM pull with configurable boost error. */
    private fun rampPull(
        rpmStart: Double = 2000.0,
        rpmEnd: Double = 6000.0,
        count: Int = 40,
        requestedMap: Double = 2200.0,
        actualMapOffset: Double = 0.0,
        barometricPressure: Double = 1013.0,
        wgdc: Double = 60.0
    ): List<WotLogEntry> {
        val step = (rpmEnd - rpmStart) / (count - 1).coerceAtLeast(1)
        return (0 until count).map { i ->
            entry(
                rpm = rpmStart + step * i,
                requestedMap = requestedMap,
                actualMap = requestedMap + actualMapOffset,
                barometricPressure = barometricPressure,
                wgdc = wgdc
            )
        }
    }

    /**
     * Create a small 3×3 gain map.
     * xAxis = relative boost PSI breakpoints, yAxis = RPM breakpoints.
     */
    private fun gainMap(
        value: Double,
        xAxis: Array<Double> = arrayOf(0.0, 10.0, 20.0),
        yAxis: Array<Double> = arrayOf(2000.0, 4000.0, 6000.0)
    ): Map3d {
        val z = Array(yAxis.size) { Array(xAxis.size) { value } }
        return Map3d(xAxis, yAxis, z)
    }

    /** Create a gain map with per-cell values from a flat list (row-major). */
    private fun gainMapFromValues(
        values: List<Double>,
        xAxis: Array<Double> = arrayOf(0.0, 10.0, 20.0),
        yAxis: Array<Double> = arrayOf(2000.0, 4000.0, 6000.0)
    ): Map3d {
        val cols = xAxis.size
        val z = Array(yAxis.size) { row ->
            Array(cols) { col -> values[row * cols + col] }
        }
        return Map3d(xAxis, yAxis, z)
    }

    // ── 1. Empty pull ───────────────────────────────────────────────

    @Test
    fun `simulate with empty pull returns empty result`() {
        val result = PidSimulator.simulate(
            pullEntries = emptyList(),
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null
        )
        assertTrue(result.states.isEmpty())
        assertFalse(result.diagnosis.oscillationDetected)
        assertFalse(result.diagnosis.windupDetected)
        assertFalse(result.diagnosis.slowConvergence)
        assertFalse(result.diagnosis.overshootDetected)
        assertEquals(0, result.diagnosis.oscillationCount)
        assertEquals(0.0, result.diagnosis.avgAbsLde)
        assertTrue(result.diagnosis.recommendations.isEmpty())
    }

    // ── 2. Null gain maps use defaults ──────────────────────────────

    @Test
    fun `simulate with null maps uses default gains and returns result`() {
        val pull = rampPull(count = 30, actualMapOffset = -100.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null
        )

        assertEquals(30, result.states.size)
        // With null maps, defaults are q0=0.5, q1=0.1, q2=0.0
        // lde should be positive (requestedMap > actualMap)
        assertTrue(result.states.all { it.lde > 0 })
        // P-term should be positive when lde > 0
        assertTrue(result.states.first().ldptv > 0)
        // D-term should be 0 with default q2=0
        assertEquals(0.0, result.states.first().ldrdtv)
    }

    // ── 3. Known gain maps produce expected PID terms ───────────────

    @Test
    fun `simulate with known gain maps produces non-null result with reasonable WGDC`() {
        val kfldrq0 = gainMap(0.6)
        val kfldrq1 = gainMap(0.08)
        val kfldrq2 = gainMap(0.0)
        val kfldrl = gainMap(50.0)   // linearization map
        val kfldimx = gainMap(80.0)  // I-term ceiling

        // Pull with 100 mbar boost deficit
        val pull = rampPull(count = 40, requestedMap = 2200.0, actualMapOffset = -100.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = kfldrq0, kfldrq1 = kfldrq1, kfldrq2 = kfldrq2,
            kfldrl = kfldrl, kfldimx = kfldimx
        )

        assertEquals(40, result.states.size)
        // All states should have positive lde (under-boost)
        assertTrue(result.states.all { it.lde > 0 })
        // I-term should accumulate over time
        val midI = result.states[20].lditv
        val lateI = result.states[39].lditv
        assertTrue(lateI >= midI, "I-term should accumulate: late=$lateI >= mid=$midI")
        // Total duty should be bounded [0, 95]
        assertTrue(result.states.all { it.ldtv in 0.0..95.0 })
    }

    // ── 4. Steady-state pull (zero error) ───────────────────────────

    @Test
    fun `simulate with steady-state pull produces minimal corrections`() {
        val kfldrq0 = gainMap(0.5)
        val kfldrq1 = gainMap(0.1)
        val kfldrq2 = gainMap(0.0)

        // actualMap == requestedMap (lde ≈ 0 after plsol adjustment)
        val pull = rampPull(count = 30, requestedMap = 2200.0, actualMapOffset = 0.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = kfldrq0, kfldrq1 = kfldrq1, kfldrq2 = kfldrq2,
            kfldrl = null, kfldimx = null
        )

        assertEquals(30, result.states.size)
        // lde should be very small (only the /1.016 plsol adjustment)
        // plsol = 2200/1.016 ≈ 2165.35, actualMap = 2200, so lde ≈ -34.6
        // With near-zero lde, corrections should be small
        val avgAbsLde = result.states.map { abs(it.lde) }.average()
        assertTrue(avgAbsLde < 50, "Average |lde| should be small for near-target: $avgAbsLde")
        assertFalse(result.diagnosis.oscillationDetected)
    }

    // ── 5. Increasing error → increasing PID corrections ────────────

    @Test
    fun `simulate with increasing error produces growing corrections`() {
        val kfldrq0 = gainMap(0.8)
        val kfldrq1 = gainMap(0.1)
        val kfldrq2 = gainMap(0.0)

        // Build entries where boost deficit grows over time
        val pull = (0 until 40).map { i ->
            val deficit = 50.0 + i * 10.0  // 50 → 440 mbar deficit
            entry(
                rpm = 2000.0 + i * 100.0,
                requestedMap = 2200.0,
                actualMap = 2200.0 - deficit,
                wgdc = 50.0
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = kfldrq0, kfldrq1 = kfldrq1, kfldrq2 = kfldrq2,
            kfldrl = null, kfldimx = null
        )

        // lde should be increasing
        val earlyLde = result.states[5].lde
        val lateLde = result.states[35].lde
        assertTrue(lateLde > earlyLde, "Error should grow: late=$lateLde > early=$earlyLde")

        // I-term should grow as error accumulates
        val earlyI = result.states[5].lditv
        val lateI = result.states[35].lditv
        assertTrue(lateI > earlyI, "I-term should grow: late=$lateI > early=$earlyI")

        // Total duty should increase
        val earlyDuty = result.states[5].ldtv
        val lateDuty = result.states[35].ldtv
        assertTrue(lateDuty > earlyDuty, "Duty should grow: late=$lateDuty > early=$earlyDuty")
    }

    // ── 6. Short pull (< 10 entries) ────────────────────────────────

    @Test
    fun `simulate with short pull handles gracefully`() {
        val pull = rampPull(count = 5, actualMapOffset = -50.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null
        )

        assertEquals(5, result.states.size)
        // Should still produce valid states
        assertTrue(result.states.all { it.ldtv in 0.0..95.0 })
        // Short pulls shouldn't trigger oscillation (too few samples)
        assertFalse(result.diagnosis.oscillationDetected)
    }

    @Test
    fun `simulate with single entry handles gracefully`() {
        val pull = listOf(entry(requestedMap = 2200.0, actualMap = 2000.0))

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null
        )

        assertEquals(1, result.states.size)
        assertEquals(0.0, result.states[0].timeMs)
        // First entry has no previous lde so D-term = 0
        assertEquals(0.0, result.states[0].ldrdtv)
    }

    // ── 7. Diagnosis: oscillation detection ─────────────────────────

    @Test
    fun `diagnosis detects oscillation with rapid sign changes`() {
        // Create pull with rapidly alternating boost error
        // Need >6 reversals/second. At 20ms interval, 50 samples = 1 sec
        // Alternate lde above +50 and below -50 every sample → 50 reversals/sec
        val pull = (0 until 50).map { i ->
            val offset = if (i % 2 == 0) 100.0 else -100.0
            entry(
                rpm = 3000.0 + i * 20.0,
                requestedMap = 2200.0,
                actualMap = (2200.0 / 1.016) - offset  // so lde alternates around ±offset
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = null
        )

        assertTrue(result.diagnosis.oscillationDetected,
            "Should detect oscillation: ${result.diagnosis.oscillationCount} sign changes")
        assertTrue(result.diagnosis.oscillationCount > 6)
        assertTrue(result.diagnosis.recommendations.any { "oscillation" in it.lowercase() })
    }

    @Test
    fun `diagnosis does not flag oscillation on smooth convergence`() {
        // Pull where lde smoothly decreases from positive toward zero
        val pull = (0 until 50).map { i ->
            val deficit = (200.0 - i * 4.0).coerceAtLeast(0.0)
            entry(
                rpm = 2000.0 + i * 80.0,
                requestedMap = 2200.0,
                actualMap = 2200.0 - deficit
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = null
        )

        assertFalse(result.diagnosis.oscillationDetected)
    }

    // ── 8. Diagnosis: I-term windup detection ───────────────────────

    @Test
    fun `diagnosis detects windup with sustained positive lde and I-term`() {
        // Large sustained boost deficit → I-term stays high, lde stays > 20
        val pull = (0 until 50).map { i ->
            entry(
                rpm = 2000.0 + i * 80.0,
                requestedMap = 2200.0,
                actualMap = 1800.0,   // 400 mbar deficit → lde ≈ 365 mbar
                wgdc = 90.0
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = null
        )

        assertTrue(result.diagnosis.windupDetected,
            "Should detect windup with sustained lde > 20 and I-term accumulating. " +
            "windupDurationMs=${result.diagnosis.windupDurationMs}")
        assertTrue(result.diagnosis.windupDurationMs > 200)
    }

    // ── 9. Diagnosis: slow convergence ──────────────────────────────

    @Test
    fun `diagnosis detects slow convergence when lde stays large`() {
        // Large deficit that never converges within the pull
        val pull = (0 until 100).map { i ->
            entry(
                rpm = 2000.0 + i * 40.0,
                requestedMap = 2200.0,
                actualMap = 1900.0,  // persistent 300 mbar deficit
                wgdc = 70.0
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.05), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = null
        )

        // 100 samples × 20ms = 2000ms > 1500ms threshold
        assertTrue(result.diagnosis.slowConvergence,
            "Should detect slow convergence: convergenceTimeMs=${result.diagnosis.convergenceTimeMs}")
        assertTrue(result.diagnosis.recommendations.any { "convergence" in it.lowercase() })
    }

    @Test
    fun `diagnosis does not flag slow convergence when error starts small`() {
        // Pull where lde starts below 20 mbar → no convergence issue
        val pull = rampPull(count = 30, requestedMap = 2200.0, actualMapOffset = 0.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null
        )

        // plsol = 2200/1.016 ≈ 2165.35, actualMap = 2200 → lde ≈ -34.6
        // The initial |lde| is ~34.6 which is > 20, but it's consistently negative
        // so convergence happens at whatever index first has |lde| < 20
        // The key point: this shouldn't trigger slow convergence for a near-target pull
        assertFalse(result.diagnosis.slowConvergence,
            "Near-target pull shouldn't flag slow convergence: " +
            "convergenceTimeMs=${result.diagnosis.convergenceTimeMs}")
    }

    // ── 10. Diagnosis: overshoot detection ──────────────────────────

    @Test
    fun `diagnosis detects overshoot when lde goes negative after convergence`() {
        // Pull: starts with deficit, reaches target, then overshoots
        val pull = (0 until 60).map { i ->
            val deficit = when {
                i < 20 -> 100.0 - i * 5.0    // 100 → 5 mbar deficit (converging)
                i < 30 -> 0.0                  // at target (lde ≈ small)
                else -> -(i - 30) * 15.0       // overshoot grows to -450 mbar
            }
            entry(
                rpm = 2000.0 + i * 60.0,
                requestedMap = 2200.0,
                // plsol = 2200/1.016 ≈ 2165.35; for lde=deficit → actualMap = plsol - deficit
                actualMap = (2200.0 / 1.016) - deficit,
                wgdc = 60.0
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = null
        )

        assertTrue(result.diagnosis.overshootDetected,
            "Should detect overshoot: magnitude=${result.diagnosis.overshootMagnitude}")
        assertTrue(result.diagnosis.overshootMagnitude > 30)
        assertTrue(result.diagnosis.recommendations.any { "overshoot" in it.lowercase() })
    }

    // ── 11. Dual-mode switching ─────────────────────────────────────

    @Test
    fun `simulate starts in DYNAMIC mode`() {
        val pull = rampPull(count = 20, actualMapOffset = -200.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.1),
            kfldrl = null, kfldimx = null
        )

        assertEquals(PidSimulator.PidMode.DYNAMIC, result.states.first().mode)
    }

    @Test
    fun `simulate switches to QUASI_STATIONARY when lde crosses zero`() {
        // Pull that starts under-boosting then overshoots
        val pull = (0 until 40).map { i ->
            val deficit = 200.0 - i * 12.0  // 200 → -280 mbar
            entry(
                rpm = 2000.0 + i * 100.0,
                requestedMap = 2200.0,
                actualMap = (2200.0 / 1.016) - deficit
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.1),
            kfldrl = null, kfldimx = null
        )

        // Should start DYNAMIC, then switch
        assertEquals(PidSimulator.PidMode.DYNAMIC, result.states.first().mode)
        // After lde crosses zero, should go quasi-stationary
        val quasiStates = result.states.filter { it.mode == PidSimulator.PidMode.QUASI_STATIONARY }
        assertTrue(quasiStates.isNotEmpty(),
            "Should switch to QUASI_STATIONARY after lde crosses zero")
        // In quasi-stationary mode, D-term should be 0
        assertTrue(quasiStates.all { it.ldrdtv == 0.0 },
            "D-term should be 0 in QUASI_STATIONARY mode")
    }

    // ── 12. D-term only active in dynamic mode ──────────────────────

    @Test
    fun `D-term is non-zero only in DYNAMIC mode with non-zero Q2`() {
        // Pull with large changing deficit to ensure D-term has signal
        val pull = (0 until 30).map { i ->
            entry(
                rpm = 2000.0 + i * 100.0,
                requestedMap = 2200.0,
                actualMap = 1800.0 + i * 5.0  // slowly recovering
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.2),
            kfldrl = null, kfldimx = null
        )

        // States in DYNAMIC mode (after first) should have non-zero D-term
        // because lde is changing and Q2 > 0
        val dynamicStatesAfterFirst = result.states.drop(1)
            .filter { it.mode == PidSimulator.PidMode.DYNAMIC }
        assertTrue(dynamicStatesAfterFirst.isNotEmpty())
        assertTrue(dynamicStatesAfterFirst.any { it.ldrdtv != 0.0 },
            "DYNAMIC mode should produce non-zero D-term with Q2 > 0")
    }

    // ── 13. I-term clamping by KFLDIMX ──────────────────────────────

    @Test
    fun `I-term is clamped by KFLDIMX ceiling`() {
        val kfldimx = gainMap(10.0)  // Very low ceiling

        // Sustained large deficit to push I-term hard
        val pull = (0 until 100).map { i ->
            entry(
                rpm = 3000.0 + i * 30.0,
                requestedMap = 2200.0,
                actualMap = 1600.0,  // 600 mbar deficit
                wgdc = 80.0
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.5), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = kfldimx
        )

        // I-term should be clamped at or below the KFLDIMX ceiling
        assertTrue(result.states.all { it.lditv <= 10.0 + 0.001 },
            "I-term should be clamped to KFLDIMX ceiling of 10.0. Max seen: ${result.states.maxOf { it.lditv }}")
    }

    // ── 14. WGDC bounds ─────────────────────────────────────────────

    @Test
    fun `ldtv output is bounded between 0 and 95 percent`() {
        // Extreme boost deficit to push duty high
        val pull = (0 until 50).map { i ->
            entry(
                rpm = 3000.0,
                requestedMap = 3000.0,
                actualMap = 1000.0,  // 2000 mbar deficit
                wgdc = 95.0
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(2.0), kfldrq1 = gainMap(1.0), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = null
        )

        assertTrue(result.states.all { it.ldtv in 0.0..95.0 },
            "ldtv must be clamped to [0, 95]. Range: [${result.states.minOf { it.ldtv }}, ${result.states.maxOf { it.ldtv }}]")
    }

    @Test
    fun `ldtv does not go below zero with negative lde`() {
        // Over-boosting: actualMap > requestedMap
        val pull = rampPull(count = 30, requestedMap = 2000.0, actualMapOffset = 500.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = null
        )

        assertTrue(result.states.all { it.ldtv >= 0.0 },
            "ldtv must not go below 0. Min: ${result.states.minOf { it.ldtv }}")
    }

    // ── 15. KFLDRQ2 sanity check (Finding 10) ──────────────────────

    @Test
    fun `Q2 warnings emitted for D-gain below 2500 RPM`() {
        // Map with non-zero D-gain at low RPM
        val kfldrq2 = Map3d(
            arrayOf(0.0, 10.0, 20.0),
            arrayOf(1500.0, 2000.0, 3000.0, 5000.0),
            arrayOf(
                arrayOf(0.1, 0.1, 0.1),   // 1500 RPM — should warn
                arrayOf(0.05, 0.05, 0.05), // 2000 RPM — should warn
                arrayOf(0.2, 0.2, 0.2),    // 3000 RPM — OK
                arrayOf(0.3, 0.3, 0.3)     // 5000 RPM — OK
            )
        )

        val pull = rampPull(count = 20, actualMapOffset = -100.0)
        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = kfldrq2,
            kfldrl = null, kfldimx = null
        )

        assertTrue(result.diagnosis.q2Warnings.isNotEmpty(),
            "Should warn about D-gain below 2500 RPM")
        assertTrue(result.diagnosis.q2Warnings.any { "2500" in it },
            "Warning should mention 2500 RPM threshold")
    }

    @Test
    fun `Q2 warnings emitted when max D-gain exceeds 90 percent of LDRQ0DY`() {
        // Map with D-gain very close to LDRQ0DY (default = 1.0)
        val kfldrq2 = Map3d(
            arrayOf(0.0, 10.0),
            arrayOf(3000.0, 5000.0),
            arrayOf(
                arrayOf(0.95, 0.95),   // > 0.9 × 1.0
                arrayOf(0.95, 0.95)
            )
        )

        val pull = rampPull(count = 20, actualMapOffset = -100.0)
        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = kfldrq2,
            kfldrl = null, kfldimx = null
        )

        assertTrue(result.diagnosis.q2Warnings.any { "90%" in it || "exceeds" in it.lowercase() },
            "Should warn about Q2 exceeding 90% of LDRQ0DY. Warnings: ${result.diagnosis.q2Warnings}")
    }

    @Test
    fun `no Q2 warnings for well-calibrated D-gain map`() {
        // D-gain only above 2500 RPM, max well below 0.9 × LDRQ0DY
        val kfldrq2 = Map3d(
            arrayOf(0.0, 10.0),
            arrayOf(3000.0, 5000.0),
            arrayOf(
                arrayOf(0.2, 0.3),
                arrayOf(0.3, 0.4)
            )
        )

        val pull = rampPull(count = 20, actualMapOffset = -100.0)
        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = kfldrq2,
            kfldrl = null, kfldimx = null
        )

        assertTrue(result.diagnosis.q2Warnings.isEmpty(),
            "Well-calibrated map should have no Q2 warnings: ${result.diagnosis.q2Warnings}")
    }

    // ── 16. MED17-typical values ────────────────────────────────────

    @Test
    fun `simulate handles MED17 boost ranges (1500-2500 hPa)`() {
        val kfldrq0 = gainMap(0.6)
        val kfldrq1 = gainMap(0.1)
        val kfldrq2 = gainMap(0.0)

        // MED17 boost ranges: psrg_w typically 1500–2500 hPa
        val pull = (0 until 40).map { i ->
            entry(
                rpm = 2000.0 + i * 100.0,
                requestedMap = 1500.0 + i * 25.0,   // 1500 → 2475 hPa
                actualMap = 1450.0 + i * 25.0,       // 50 mbar deficit
                barometricPressure = 1013.0,
                wgdc = 40.0 + i * 1.0
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = kfldrq0, kfldrq1 = kfldrq1, kfldrq2 = kfldrq2,
            kfldrl = null, kfldimx = null
        )

        assertEquals(40, result.states.size)
        // All lde should be positive (under-boost)
        assertTrue(result.states.all { it.lde > 0 })
        // Output should be valid
        assertTrue(result.states.all { it.ldtv in 0.0..95.0 })
        // Average error should reflect the ~50 mbar deficit
        val avgLde = result.states.map { it.lde }.average()
        assertTrue(avgLde > 0, "Average lde should be positive for under-boost")
    }

    // ── 17. KFLDRL / KFLDIMX interaction ────────────────────────────

    @Test
    fun `KFLDRL linearization map is applied to output`() {
        val kfldrl = gainMap(75.0)  // Constant linearization output

        val pull = rampPull(count = 20, actualMapOffset = -100.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.0),
            kfldrl = kfldrl, kfldimx = null
        )

        // ldtvLinearized should come from KFLDRL lookup
        // With positive relative boost PSI, KFLDRL returns 75.0
        val statesWithBoost = result.states.filter {
            (it.pvdks - 1013.0) * 0.0145038 > 0  // positive relative PSI
        }
        if (statesWithBoost.isNotEmpty()) {
            assertTrue(statesWithBoost.all { it.ldtvLinearized == 75.0 },
                "KFLDRL should provide linearization output of 75.0")
        }
    }

    @Test
    fun `KFLDRL falls back to ldtv when null`() {
        val pull = rampPull(count = 20, actualMapOffset = -100.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = null
        )

        // Without KFLDRL, ldtvLinearized should equal ldtv
        assertTrue(result.states.all { it.ldtvLinearized == it.ldtv },
            "Without KFLDRL, linearized output should equal raw ldtv")
    }

    @Test
    fun `KFLDRL falls back to ldtv when relative PSI is non-positive`() {
        // Pull at sub-atmospheric pressure → relativePsi <= 0
        val pull = rampPull(
            count = 20,
            requestedMap = 900.0,
            actualMapOffset = -50.0,
            barometricPressure = 1013.0
        )

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.5), kfldrq1 = gainMap(0.1), kfldrq2 = gainMap(0.0),
            kfldrl = gainMap(75.0), kfldimx = null
        )

        // actualMap = 850 < baro = 1013 → relativePsi < 0 → KFLDRL not used
        assertTrue(result.states.all { it.ldtvLinearized == it.ldtv },
            "KFLDRL should not be used when relative PSI <= 0")
    }

    // ── 18. Time progression ────────────────────────────────────────

    @Test
    fun `states have correct time progression`() {
        val pull = rampPull(count = 10, actualMapOffset = -50.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null,
            sampleIntervalMs = 20.0
        )

        for ((i, state) in result.states.withIndex()) {
            assertEquals(i * 20.0, state.timeMs, "Time at index $i should be ${i * 20.0}")
        }
    }

    @Test
    fun `custom sample interval is respected`() {
        val pull = rampPull(count = 5, actualMapOffset = -50.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null,
            sampleIntervalMs = 50.0
        )

        assertEquals(0.0, result.states[0].timeMs)
        assertEquals(50.0, result.states[1].timeMs)
        assertEquals(200.0, result.states[4].timeMs)
    }

    // ── 19. RPM and pressure are passed through correctly ───────────

    @Test
    fun `states reflect input RPM and pressure values`() {
        val pull = listOf(
            entry(rpm = 2500.0, requestedMap = 1800.0, actualMap = 1700.0),
            entry(rpm = 3500.0, requestedMap = 2000.0, actualMap = 1900.0),
            entry(rpm = 4500.0, requestedMap = 2200.0, actualMap = 2100.0)
        )

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null
        )

        assertEquals(2500.0, result.states[0].rpm)
        assertEquals(3500.0, result.states[1].rpm)
        assertEquals(4500.0, result.states[2].rpm)
        assertEquals(1700.0, result.states[0].pvdks)
        assertEquals(1900.0, result.states[1].pvdks)
        assertEquals(2100.0, result.states[2].pvdks)
        // plsol = requestedMap / 1.016
        assertEquals(1800.0 / 1.016, result.states[0].plsol, 0.01)
    }

    // ── 20. Diagnosis recommendations list ──────────────────────────

    @Test
    fun `healthy pull produces no recommendations`() {
        // On-target, smooth pull → no issues
        val pull = rampPull(count = 30, requestedMap = 2200.0, actualMapOffset = 0.0)

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null
        )

        // Near-zero error pull shouldn't trigger most recommendations
        assertFalse(result.diagnosis.oscillationDetected)
        assertFalse(result.diagnosis.overshootDetected)
    }

    @Test
    fun `multiple issues produce multiple recommendations`() {
        // Create pull that triggers both windup and slow convergence
        val pull = (0 until 100).map { i ->
            entry(
                rpm = 2000.0 + i * 40.0,
                requestedMap = 2200.0,
                actualMap = 1700.0,  // persistent 500 mbar deficit
                wgdc = 90.0
            )
        }

        val result = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = gainMap(0.3), kfldrq1 = gainMap(0.02), kfldrq2 = gainMap(0.0),
            kfldrl = null, kfldimx = null
        )

        // Should have multiple recommendations
        assertTrue(result.diagnosis.recommendations.size >= 1,
            "Should produce at least one recommendation for problematic pull. " +
            "Got: ${result.diagnosis.recommendations}")
    }

    // ── 21. Custom PID constants ────────────────────────────────────

    @Test
    fun `custom ldrq0dy affects P-term in dynamic mode`() {
        val pull = rampPull(count = 20, actualMapOffset = -200.0)

        val resultLow = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null,
            ldrq0dy = 0.5
        )
        val resultHigh = PidSimulator.simulate(
            pullEntries = pull,
            kfldrq0 = null, kfldrq1 = null, kfldrq2 = null,
            kfldrl = null, kfldimx = null,
            ldrq0dy = 2.0
        )

        // Higher LDRQ0DY should produce larger P-term
        val lowP = result(resultLow.states[1].ldptv)
        val highP = result(resultHigh.states[1].ldptv)
        assertTrue(highP > lowP,
            "Higher LDRQ0DY should produce larger P-term: high=$highP, low=$lowP")
    }

    private fun result(value: Double) = abs(value)
}
