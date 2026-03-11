package domain.model.optimizer

import data.contract.Me7LogFileContract
import domain.model.simulator.Me7Simulator
import kotlin.test.*

/**
 * Unit tests for the MED17 simplified chain diagnosis logic
 * embedded in [OptimizerCalculator.analyzeMed17].
 *
 * MED17 can only diagnose Link 1 (torque → rlsol) and Link 3 (KFLDRL → boost);
 * Links 2 and 4 (VE model via KFURL/KFPBRK) are always reported as 0%.
 */
class ChainDiagnosisTest {

    companion object {
        private const val DEFAULT_LDRXN = 191.0
        private const val MIN_THROTTLE = 80.0

        /** Build a synthetic ME7-format log map that analyzeMed17 can consume. */
        private fun buildLogData(
            entries: List<TestEntry>
        ): Map<Me7LogFileContract.Header, List<Double>> {
            return mapOf(
                Me7LogFileContract.Header.RPM_COLUMN_HEADER to entries.map { it.rpm },
                Me7LogFileContract.Header.REQUESTED_LOAD_HEADER to entries.map { it.requestedLoad },
                Me7LogFileContract.Header.ENGINE_LOAD_HEADER to entries.map { it.actualLoad },
                Me7LogFileContract.Header.REQUESTED_PRESSURE_HEADER to entries.map { it.requestedMap },
                Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER to entries.map { it.actualMap },
                Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER to entries.map { it.barometricPressure },
                Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER to entries.map { it.wgdc },
                Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER to entries.map { it.throttleAngle }
            )
        }

        /**
         * Convenience entry builder. All entries default to WOT (throttle=90),
         * high RPM (4000), on-target load, and matching boost pressures.
         */
        private fun onTarget(
            rpm: Double = 4000.0,
            requestedLoad: Double = DEFAULT_LDRXN,
            actualLoad: Double = DEFAULT_LDRXN,
            requestedMap: Double = 2000.0,
            actualMap: Double = 2000.0,
            barometricPressure: Double = 1013.0,
            wgdc: Double = 50.0,
            throttleAngle: Double = 90.0
        ) = TestEntry(rpm, requestedLoad, actualLoad, requestedMap, actualMap, barometricPressure, wgdc, throttleAngle)

        /** Entry that triggers torque capping: RPM >= 3500 and requestedLoad < ldrxn * 0.95 */
        private fun torqueCapped(
            rpm: Double = 4000.0,
            requestedLoad: Double = DEFAULT_LDRXN * 0.80,
            actualLoad: Double = DEFAULT_LDRXN * 0.80
        ) = onTarget(rpm = rpm, requestedLoad = requestedLoad, actualLoad = actualLoad)

        /** Entry that triggers boost shortfall: actualMap < requestedMap - 50 */
        private fun boostShort(
            requestedMap: Double = 2200.0,
            actualMap: Double = 2100.0 // deficit = 100 > 50
        ) = onTarget(requestedMap = requestedMap, actualMap = actualMap)
    }

    private data class TestEntry(
        val rpm: Double,
        val requestedLoad: Double,
        val actualLoad: Double,
        val requestedMap: Double,
        val actualMap: Double,
        val barometricPressure: Double,
        val wgdc: Double,
        val throttleAngle: Double
    )

    private fun analyze(
        entries: List<TestEntry>,
        ldrxnTarget: Double = DEFAULT_LDRXN
    ): OptimizerCalculator.OptimizerResult {
        return OptimizerCalculator.analyzeMed17(
            values = buildLogData(entries),
            kfldrlMap = null,
            kfldimxMap = null,
            ldrxnTarget = ldrxnTarget,
            minThrottleAngle = MIN_THROTTLE
        )
    }

    // ── Test 1: All entries on-target ────────────────────────────────

    @Test
    fun `all on-target entries produce 100 percent onTargetPercent and ON_TARGET dominant`() {
        val entries = List(20) { onTarget() }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(0.0, diag.torqueCappedPercent, 0.01)
        assertEquals(0.0, diag.boostShortfallPercent, 0.01)
        assertEquals(100.0, diag.onTargetPercent, 0.01)
        assertEquals(Me7Simulator.ErrorSource.ON_TARGET, diag.dominantError)
        assertTrue(diag.recommendations.any { it.startsWith("OK:") },
            "Expected OK recommendation, got: ${diag.recommendations}")
    }

    // ── Test 2: Torque-capped dominant ──────────────────────────────

    @Test
    fun `torque capped majority produces TORQUE_CAPPED dominant with WARNING`() {
        // 16 torque-capped + 4 on-target = 80% capped
        val entries = List(16) { torqueCapped() } + List(4) { onTarget() }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(80.0, diag.torqueCappedPercent, 0.01)
        assertTrue(diag.torqueCappedPercent > 20)
        assertEquals(Me7Simulator.ErrorSource.TORQUE_CAPPED, diag.dominantError)
        assertTrue(diag.recommendations.any { it.startsWith("WARNING:") },
            "Expected WARNING about torque capping, got: ${diag.recommendations}")
    }

    // ── Test 3: Boost shortfall dominant ─────────────────────────────

    @Test
    fun `boost shortfall majority produces BOOST_SHORTFALL dominant with BOOST rec`() {
        // 12 boost-short + 8 on-target = 60% boost shortfall, 0% torque capped
        val entries = List(12) { boostShort() } + List(8) { onTarget() }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(60.0, diag.boostShortfallPercent, 0.01)
        assertEquals(0.0, diag.torqueCappedPercent, 0.01)
        assertTrue(diag.boostShortfallPercent > 10)
        assertEquals(Me7Simulator.ErrorSource.BOOST_SHORTFALL, diag.dominantError)
        assertTrue(diag.recommendations.any { it.startsWith("BOOST:") },
            "Expected BOOST recommendation, got: ${diag.recommendations}")
    }

    // ── Test 4: Mixed torque and boost issues ───────────────────────

    @Test
    fun `mixed torque and boost issues have correct percentage sum`() {
        // 5 torque-capped + 3 boost-short + 2 on-target = 10 total
        // torquePct = 50%, boostPct = 30%, okPct = 20%
        val entries = List(5) { torqueCapped() } +
            List(3) { boostShort() } +
            List(2) { onTarget() }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(50.0, diag.torqueCappedPercent, 0.01)
        assertEquals(30.0, diag.boostShortfallPercent, 0.01)
        assertEquals(20.0, diag.onTargetPercent, 0.01)
        // torquePct (50) > boostPct (30) && torquePct > 20 → TORQUE_CAPPED
        assertEquals(Me7Simulator.ErrorSource.TORQUE_CAPPED, diag.dominantError)
    }

    // ── Test 5: Low RPM entries not counted as torque-capped ────────

    @Test
    fun `entries below 3500 RPM are not counted as torque capped`() {
        // All entries at RPM 3000 with low requestedLoad — should NOT be torque capped
        val lowRpmCapped = onTarget(
            rpm = 3000.0,
            requestedLoad = DEFAULT_LDRXN * 0.50,
            actualLoad = DEFAULT_LDRXN * 0.50
        )
        val entries = List(20) { lowRpmCapped }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(0.0, diag.torqueCappedPercent, 0.01,
            "Entries below 3500 RPM should not count as torque capped")
        assertEquals(Me7Simulator.ErrorSource.ON_TARGET, diag.dominantError)
    }

    // ── Test 6: MED17 always has VE model INFO message ──────────────

    @Test
    fun `MED17 chain diagnosis always includes VE model INFO message`() {
        val diag = analyze(List(10) { onTarget() }).chainDiagnosis

        assertTrue(
            diag.recommendations.any { it.contains("MED17 mode") && it.startsWith("INFO:") },
            "Expected INFO about MED17 mode, got: ${diag.recommendations}"
        )
    }

    @Test
    fun `MED17 INFO message present even with torque capping`() {
        val entries = List(20) { torqueCapped() }
        val diag = analyze(entries).chainDiagnosis

        assertTrue(
            diag.recommendations.any { it.contains("MED17 mode") && it.startsWith("INFO:") },
            "MED17 INFO message should always be present"
        )
    }

    @Test
    fun `MED17 INFO message present even with boost shortfall`() {
        val entries = List(20) { boostShort() }
        val diag = analyze(entries).chainDiagnosis

        assertTrue(
            diag.recommendations.any { it.contains("MED17 mode") && it.startsWith("INFO:") },
            "MED17 INFO message should always be present"
        )
    }

    // ── Test 7: pssolErrorPercent and veMismatchPercent always 0 ────

    @Test
    fun `pssolErrorPercent is always zero for MED17`() {
        val scenarios = listOf(
            List(10) { onTarget() },
            List(10) { torqueCapped() },
            List(10) { boostShort() }
        )
        for (entries in scenarios) {
            val diag = analyze(entries).chainDiagnosis
            assertEquals(0.0, diag.pssolErrorPercent, 0.0,
                "pssolErrorPercent must be 0.0 for MED17")
        }
    }

    @Test
    fun `veMismatchPercent is always zero for MED17`() {
        val scenarios = listOf(
            List(10) { onTarget() },
            List(10) { torqueCapped() },
            List(10) { boostShort() }
        )
        for (entries in scenarios) {
            val diag = analyze(entries).chainDiagnosis
            assertEquals(0.0, diag.veMismatchPercent, 0.0,
                "veMismatchPercent must be 0.0 for MED17")
        }
    }

    // ── Test 8: Empty WOT entries ───────────────────────────────────

    @Test
    fun `empty log data produces valid chain diagnosis without crash`() {
        val diag = analyze(emptyList()).chainDiagnosis

        assertEquals(0.0, diag.torqueCappedPercent, 0.01)
        assertEquals(0.0, diag.boostShortfallPercent, 0.01)
        assertEquals(Me7Simulator.ErrorSource.ON_TARGET, diag.dominantError)
        // Should still have INFO rec
        assertTrue(diag.recommendations.any { it.contains("MED17 mode") },
            "Even empty results should have MED17 INFO message")
    }

    @Test
    fun `empty WOT entries via sub-threshold throttle produce valid diagnosis`() {
        // Entries with throttle < 80 are filtered out, leaving 0 WOT entries
        val entries = List(10) { onTarget(throttleAngle = 50.0) }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(0.0, diag.torqueCappedPercent, 0.01)
        assertEquals(0.0, diag.boostShortfallPercent, 0.01)
    }

    // ── Test 9: onTargetPercent is clamped at 0 ─────────────────────

    @Test
    fun `onTargetPercent is clamped to zero when torque and boost overlap`() {
        // An entry can be BOTH torque-capped (rpm>=3500, requestedLoad < ldrxn*0.95)
        // AND boost-short (actualMap < requestedMap - 50).
        // If all entries match both, torquePct=100 + boostPct=100 = 200, okPct clamped to 0.
        val bothBad = onTarget(
            rpm = 4000.0,
            requestedLoad = DEFAULT_LDRXN * 0.50,
            actualLoad = DEFAULT_LDRXN * 0.50,
            requestedMap = 2200.0,
            actualMap = 2100.0 // 100 mbar deficit > 50
        )
        val entries = List(20) { bothBad }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(100.0, diag.torqueCappedPercent, 0.01)
        assertEquals(100.0, diag.boostShortfallPercent, 0.01)
        assertEquals(0.0, diag.onTargetPercent, 0.01,
            "onTargetPercent must be clamped to 0 when sum exceeds 100")
    }

    // ── Test 10: avgTotalLoadDeficit computation ────────────────────

    @Test
    fun `avgTotalLoadDeficit equals average of ldrxnTarget minus actualLoad`() {
        val actualLoads = listOf(180.0, 170.0, 160.0, 190.0, 150.0)
        val entries = actualLoads.map { load ->
            onTarget(actualLoad = load)
        }
        val diag = analyze(entries).chainDiagnosis

        val expectedDeficit = actualLoads.map { DEFAULT_LDRXN - it }.average()
        assertEquals(expectedDeficit, diag.avgTotalLoadDeficit, 0.01,
            "avgTotalLoadDeficit should be average of (ldrxnTarget - actualLoad)")
    }

    @Test
    fun `avgTotalLoadDeficit is zero when all entries match target`() {
        val entries = List(10) { onTarget(actualLoad = DEFAULT_LDRXN) }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(0.0, diag.avgTotalLoadDeficit, 0.01)
    }

    @Test
    fun `avgTotalLoadDeficit is negative when actual exceeds target`() {
        val entries = List(10) { onTarget(actualLoad = DEFAULT_LDRXN + 20.0) }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(-20.0, diag.avgTotalLoadDeficit, 0.01,
            "Deficit should be negative when actual load exceeds target")
    }

    // ── Dominant error boundary cases ───────────────────────────────

    @Test
    fun `dominantError is ON_TARGET when torquePct is 20 exactly`() {
        // torquePct > 20 is required for TORQUE_CAPPED, so exactly 20 → ON_TARGET
        // 2 capped out of 10 = 20%
        val entries = List(2) { torqueCapped() } + List(8) { onTarget() }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(20.0, diag.torqueCappedPercent, 0.01)
        // torquePct (20) is NOT > 20, so falls through to boostPct check (0) → ON_TARGET
        assertEquals(Me7Simulator.ErrorSource.ON_TARGET, diag.dominantError)
    }

    @Test
    fun `dominantError is BOOST_SHORTFALL when boostPct just above 10`() {
        // 3 boost-short out of 20 = 15% → BOOST_SHORTFALL
        val entries = List(3) { boostShort() } + List(17) { onTarget() }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(15.0, diag.boostShortfallPercent, 0.01)
        assertTrue(diag.boostShortfallPercent > 10)
        assertEquals(Me7Simulator.ErrorSource.BOOST_SHORTFALL, diag.dominantError)
    }

    @Test
    fun `dominantError is ON_TARGET when boostPct is exactly 10`() {
        // 1 boost-short out of 10 = 10% → NOT > 10 → ON_TARGET
        val entries = List(1) { boostShort() } + List(9) { onTarget() }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(10.0, diag.boostShortfallPercent, 0.01)
        assertEquals(Me7Simulator.ErrorSource.ON_TARGET, diag.dominantError)
    }

    @Test
    fun `TORQUE_CAPPED wins over BOOST_SHORTFALL when both above threshold and torque higher`() {
        // 6 capped + 4 boost-short out of 10 → torquePct=60 > boostPct=40, both > thresholds
        val entries = List(6) { torqueCapped() } + List(4) { boostShort() }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(Me7Simulator.ErrorSource.TORQUE_CAPPED, diag.dominantError,
            "TORQUE_CAPPED should win when torquePct > boostPct && torquePct > 20")
    }

    @Test
    fun `BOOST_SHORTFALL wins when boost higher than torque and above threshold`() {
        // 3 capped + 7 boost-short out of 10 → torquePct=30, boostPct=70
        // torquePct (30) > boostPct (70)? No → falls to boostPct > 10 → BOOST_SHORTFALL
        val entries = List(3) { torqueCapped() } + List(7) { boostShort() }
        val diag = analyze(entries).chainDiagnosis

        assertEquals(Me7Simulator.ErrorSource.BOOST_SHORTFALL, diag.dominantError,
            "BOOST_SHORTFALL should win when boostPct > torquePct")
    }

    // ── OK recommendation only when recs empty and okPct > 80 ───────

    @Test
    fun `no OK recommendation when torque warning present even if okPct above 80`() {
        // This can't happen naturally (torquePct > 20 means okPct < 80) but verify the
        // "recs.isEmpty()" guard: if torquePct > 20, WARNING is added, so OK is skipped.
        val entries = List(5) { torqueCapped() } + List(15) { onTarget() }
        val diag = analyze(entries).chainDiagnosis

        // torquePct = 25%, okPct = 75% (< 80), so OK not added anyway
        assertFalse(diag.recommendations.any { it.startsWith("OK:") },
            "OK recommendation should not appear when warnings are present")
    }

    // ── Custom ldrxnTarget ──────────────────────────────────────────

    @Test
    fun `custom ldrxnTarget changes torque capping threshold`() {
        val customTarget = 250.0
        // requestedLoad = 250 * 0.80 = 200. At ldrxn=250, threshold = 250*0.95 = 237.5
        // 200 < 237.5 → capped
        val entries = List(20) {
            onTarget(rpm = 4000.0, requestedLoad = 200.0, actualLoad = 200.0)
        }
        val diag = analyze(entries, ldrxnTarget = customTarget).chainDiagnosis

        assertEquals(100.0, diag.torqueCappedPercent, 0.01,
            "All entries should be torque-capped at ldrxn=250 with requestedLoad=200")
        assertEquals(Me7Simulator.ErrorSource.TORQUE_CAPPED, diag.dominantError)
    }

    @Test
    fun `custom ldrxnTarget affects avgTotalLoadDeficit`() {
        val customTarget = 250.0
        val entries = List(10) { onTarget(actualLoad = 200.0) }
        val diag = analyze(entries, ldrxnTarget = customTarget).chainDiagnosis

        assertEquals(50.0, diag.avgTotalLoadDeficit, 0.01,
            "Deficit should be 250 - 200 = 50")
    }
}
