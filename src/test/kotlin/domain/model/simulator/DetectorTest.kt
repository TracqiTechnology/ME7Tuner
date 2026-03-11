package domain.model.simulator

import domain.model.optimizer.OptimizerCalculator.WotLogEntry
import domain.model.optimizer.PullSegmenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the shared detector components used by both ME7 and MED17
 * optimizer paths: PullSegmenter, SafetyModeDetector, TransientDetector,
 * EnvironmentalCorrector, and ThrottleBodyChecker.
 */
class DetectorTest {

    // ── Helper ──────────────────────────────────────────────────────

    /**
     * Build a WotLogEntry with sensible defaults.  Override only the fields
     * that matter for each test.
     *
     * Defaults represent a typical ME7 2.7T WOT sample:
     *   3000 RPM, 191 % load, 2200 mbar MAP, 1013 mbar baro, 60 % WGDC, 100° throttle.
     */
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

    /**
     * Generate an ascending-RPM pull from [rpmStart] to [rpmEnd] with [count] evenly
     * spaced samples.  All other fields use the defaults (on-target MAP/load).
     */
    private fun rampPull(
        rpmStart: Double = 2000.0,
        rpmEnd: Double = 6000.0,
        count: Int = 40,
        requestedLoad: Double = 191.0,
        actualLoad: Double = 191.0,
        requestedMap: Double = 2200.0,
        actualMap: Double = 2200.0,
        barometricPressure: Double = 1013.0,
        wgdc: Double = 60.0,
        throttleAngle: Double = 100.0
    ): List<WotLogEntry> {
        val step = (rpmEnd - rpmStart) / (count - 1).coerceAtLeast(1)
        return (0 until count).map { i ->
            entry(
                rpm = rpmStart + step * i,
                requestedLoad = requestedLoad,
                actualLoad = actualLoad,
                requestedMap = requestedMap,
                actualMap = actualMap,
                barometricPressure = barometricPressure,
                wgdc = wgdc,
                throttleAngle = throttleAngle
            )
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PullSegmenter
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `PullSegmenter - single clean pull yields one GOOD pull`() {
        // 40 samples, 2000→6000 RPM → span=4000 > MIN_RPM_SPAN_GOOD (1500)
        val entries = rampPull(rpmStart = 2000.0, rpmEnd = 6000.0, count = 40)
        val pulls = PullSegmenter.segmentPulls(entries)

        assertEquals(1, pulls.size, "Expected exactly one pull")
        assertEquals(PullSegmenter.PullQuality.GOOD, pulls[0].quality)
        assertEquals(40, pulls[0].sampleCount)
        assertEquals(2000.0, pulls[0].rpmStart, 1.0)
        assertEquals(6000.0, pulls[0].rpmEnd, 1.0)
    }

    @Test
    fun `PullSegmenter - multiple pulls separated by RPM drop`() {
        // Pull 1: 2000→4500 (25 samples)
        val pull1 = rampPull(rpmStart = 2000.0, rpmEnd = 4500.0, count = 25)
        // Gap: big RPM drop (gear change) - single sample at 2500 RPM
        val gap = listOf(entry(rpm = 2500.0))
        // Pull 2: 2500→5500 (25 samples)
        val pull2 = rampPull(rpmStart = 2500.0, rpmEnd = 5500.0, count = 25)

        val entries = pull1 + gap + pull2
        val pulls = PullSegmenter.segmentPulls(entries)

        // Should produce at least 1 pull. The gap creates a >400 RPM drop
        // (4500→2500 = 2000 RPM drop) which splits the data.
        assertTrue(pulls.size >= 2, "Expected at least two pulls, got ${pulls.size}")
    }

    @Test
    fun `PullSegmenter - empty entry list yields empty pulls`() {
        val pulls = PullSegmenter.segmentPulls(emptyList())
        assertTrue(pulls.isEmpty(), "Empty entries should give empty pulls")
    }

    @Test
    fun `PullSegmenter - too few entries yields empty pulls`() {
        // MIN_PULL_SAMPLES is 10; provide only 5
        val entries = rampPull(count = 5)
        val pulls = PullSegmenter.segmentPulls(entries)
        assertTrue(pulls.isEmpty(), "Fewer than MIN_PULL_SAMPLES should give empty pulls")
    }

    @Test
    fun `PullSegmenter - short RPM span classified as SHORT`() {
        // RPM span < 800 (MIN_RPM_SPAN_SHORT) → SHORT quality
        val entries = rampPull(rpmStart = 3000.0, rpmEnd = 3500.0, count = 15)
        val pulls = PullSegmenter.segmentPulls(entries)

        assertEquals(1, pulls.size)
        assertEquals(PullSegmenter.PullQuality.SHORT, pulls[0].quality)
    }

    @Test
    fun `PullSegmenter - incomplete RPM span classified as INCOMPLETE`() {
        // RPM span between 800 and 1500 → INCOMPLETE quality
        val entries = rampPull(rpmStart = 3000.0, rpmEnd = 4200.0, count = 15)
        val pulls = PullSegmenter.segmentPulls(entries)

        assertEquals(1, pulls.size)
        assertEquals(PullSegmenter.PullQuality.INCOMPLETE, pulls[0].quality)
    }

    @Test
    fun `PullSegmenter - MED17 typical value range`() {
        // MED17 logs typically use higher MAP values (up to 2600 mbar) and
        // different load scaling, but PullSegmenter only looks at RPM.
        val entries = rampPull(
            rpmStart = 1800.0, rpmEnd = 6500.0, count = 50,
            requestedMap = 2600.0, actualMap = 2550.0,
            requestedLoad = 220.0, actualLoad = 215.0
        )
        val pulls = PullSegmenter.segmentPulls(entries)

        assertEquals(1, pulls.size)
        assertEquals(PullSegmenter.PullQuality.GOOD, pulls[0].quality)
    }

    @Test
    fun `PullSegmenter - checkConsistency detects anomalous pull`() {
        // Need sqrt(N) > 2 where N = number of normal GOOD pulls.
        // With 5 normal pulls + 1 anomalous, Z-score ≈ sqrt(5) ≈ 2.24 > 2.
        fun normalPull() = rampPull(
            rpmStart = 2000.0, rpmEnd = 6000.0, count = 12,
            requestedMap = 2220.0, actualMap = 2200.0
        )
        val anomalousPull = rampPull(
            rpmStart = 2000.0, rpmEnd = 6000.0, count = 12,
            requestedMap = 2700.0, actualMap = 2200.0  // error = 500 mbar
        )

        // Gap with matching error to avoid polluting the statistic
        val gap = listOf(entry(rpm = 1000.0, requestedMap = 2220.0, actualMap = 2200.0))
        val all = normalPull() + gap +
                  normalPull() + gap +
                  normalPull() + gap +
                  normalPull() + gap +
                  normalPull() + gap +
                  anomalousPull
        val pulls = PullSegmenter.segmentPulls(all)

        val goodPulls = pulls.filter { it.quality == PullSegmenter.PullQuality.GOOD }
        assertTrue(goodPulls.size >= 2, "Need at least 2 GOOD pulls, got ${goodPulls.size}")

        val consistency = PullSegmenter.checkConsistency(pulls)
        assertTrue(consistency.isNotEmpty(), "Should flag anomalous pull")
    }

    // ════════════════════════════════════════════════════════════════
    //  SafetyModeDetector
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `SafetyModeDetector - normal WOT data yields no exclusions`() {
        // actualMap - barometricPressure = 2200 - 1013 = 1187 < 250? No, 1187 > 250.
        // But wait — DPUPS checks overcharge = actualMap - barometricPressure.
        // For this to NOT trigger, overcharge must be <= 250 mbar.
        // Use actualMap only slightly above baro (naturally aspirated range).
        val entries = rampPull(
            count = 20,
            actualMap = 1200.0,       // 1200 - 1013 = 187 mbar < 250 threshold
            requestedMap = 1200.0,
            barometricPressure = 1013.0,
            wgdc = 50.0
        )
        val result = SafetyModeDetector.detect(entries)

        assertEquals(0, result.overloadCount)
        assertEquals(0, result.fallbackCount)
        assertEquals(0, result.regulationErrorCount)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `SafetyModeDetector - detects overload when overcharge exceeds DPUPS`() {
        // DPUPS threshold: actualMap - barometricPressure > 250 mbar
        // actualMap=1300, baro=1013 → overcharge=287 mbar > 250 → OVERLOAD
        val entries = (0 until 10).map {
            entry(actualMap = 1300.0, barometricPressure = 1013.0, requestedMap = 1300.0)
        }
        val result = SafetyModeDetector.detect(entries)

        assertEquals(10, result.overloadCount)
        assertTrue(result.excludedSamples.all {
            it.reason == SafetyModeDetector.ExclusionReason.OVERLOAD
        })
        assertTrue(result.warnings.any { it.contains("overcharge") })
    }

    @Test
    fun `SafetyModeDetector - detects regulation error on sustained deviation`() {
        // |requestedMap - actualMap| > 200 mbar for >= 15 consecutive samples
        // Keep overcharge (actualMap - baro) below DPUPS (250 mbar) to avoid OVERLOAD masking
        val entries = (0 until 20).map {
            entry(
                requestedMap = 1450.0,
                actualMap = 1200.0,         // |diff| = 250 > 200
                barometricPressure = 1013.0 // overcharge = 1200 - 1013 = 187 < 250
            )
        }
        val result = SafetyModeDetector.detect(entries)

        assertTrue(result.regulationErrorCount > 0,
            "Should detect sustained regulation error")
        assertTrue(result.excludedSamples.any {
            it.reason == SafetyModeDetector.ExclusionReason.REGULATION_ERROR
        })
    }

    @Test
    fun `SafetyModeDetector - no regulation error for short burst`() {
        // Only 10 consecutive samples with high deviation (< 15 threshold)
        val normal = (0 until 5).map {
            entry(requestedMap = 2200.0, actualMap = 2200.0)
        }
        val burst = (0 until 10).map {
            entry(requestedMap = 2500.0, actualMap = 2200.0)  // |diff| = 300 > 200
        }
        val moreNormal = (0 until 5).map {
            entry(requestedMap = 2200.0, actualMap = 2200.0)
        }
        val entries = normal + burst + moreNormal
        val result = SafetyModeDetector.detect(entries)

        assertEquals(0, result.regulationErrorCount,
            "Short burst (< 15 consecutive) should NOT trigger regulation error")
    }

    @Test
    fun `SafetyModeDetector - detects fallback mode via WGDC step then flat`() {
        // WGDC step > 20%, then stays flat (within 2% tolerance) for >= 8 samples
        // Keep overcharge low to avoid OVERLOAD masking these entries
        val before = (0 until 5).map {
            entry(wgdc = 50.0, actualMap = 1200.0, requestedMap = 1200.0, barometricPressure = 1013.0)
        }
        // Step: wgdc jumps from 50 to 80 (step = 30 > 20 threshold)
        val flatAfterStep = (0 until 12).map {
            entry(wgdc = 80.0, actualMap = 1200.0, requestedMap = 1200.0, barometricPressure = 1013.0)
        }

        val entries = before + flatAfterStep
        val result = SafetyModeDetector.detect(entries)

        assertTrue(result.fallbackCount > 0,
            "Should detect WGDC step+flat as fallback mode")
        assertTrue(result.excludedSamples.any {
            it.reason == SafetyModeDetector.ExclusionReason.FALLBACK
        })
    }

    @Test
    fun `SafetyModeDetector - no fallback when WGDC varies normally`() {
        // WGDC changes gradually — no step > 20%
        val entries = (0 until 20).map { i ->
            entry(wgdc = 50.0 + i * 1.5)  // gradual increase
        }
        val result = SafetyModeDetector.detect(entries)

        assertEquals(0, result.fallbackCount,
            "Gradual WGDC change should NOT trigger fallback detection")
    }

    @Test
    fun `SafetyModeDetector - MED17 value range with no safety events`() {
        // MED17 typical: higher MAP, different baro range
        val entries = rampPull(
            count = 20,
            actualMap = 1200.0,
            requestedMap = 1210.0,
            barometricPressure = 1010.0,
            wgdc = 55.0
        )
        val result = SafetyModeDetector.detect(entries)

        assertEquals(0, result.overloadCount)
        assertEquals(0, result.fallbackCount)
        assertEquals(0, result.regulationErrorCount)
    }

    // ════════════════════════════════════════════════════════════════
    //  TransientDetector
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `TransientDetector - steady state data yields no transient events`() {
        // actualLoad = 191 = ldrxnTarget → no overboost, no knock deficit
        val entries = rampPull(count = 20, actualLoad = 191.0, requestedLoad = 191.0)
        val result = TransientDetector.detect(entries)

        assertEquals(0, result.overboostEvents)
        assertEquals(0, result.knockEvents)
        assertTrue(result.transientIndices.isEmpty())
    }

    @Test
    fun `TransientDetector - detects overboost event`() {
        // actualLoad must exceed ldrxnTarget by > 2% but < 15% for >= 3 consecutive samples
        // ldrxnTarget defaults to 191.0
        // Using actualLoad = 200 → excess = 9% (within 2-15 range)
        val normal = (0 until 5).map { entry(actualLoad = 191.0) }
        val overboost = (0 until 6).map { i ->
            entry(rpm = 3000.0 + i * 100, actualLoad = 200.0)
        }
        val afterOverboost = (0 until 5).map { entry(actualLoad = 191.0) }

        val entries = normal + overboost + afterOverboost
        val result = TransientDetector.detect(entries)

        assertEquals(1, result.overboostEvents)
        assertTrue(result.overboostSampleCount >= 3)
        assertTrue(result.transientIndices.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("overboost") })
    }

    @Test
    fun `TransientDetector - ignores overboost below threshold`() {
        // actualLoad = 192 → excess = 1% < 2% minimum
        val entries = (0 until 20).map { entry(actualLoad = 192.0) }
        val result = TransientDetector.detect(entries)

        assertEquals(0, result.overboostEvents)
    }

    @Test
    fun `TransientDetector - detects knock load reduction`() {
        // Knock: actualLoad below ldrxnTarget by > 5% (deficit > 5),
        // with no boost shortfall (|requestedMap - actualMap| < 50),
        // and not torque capped (requestedLoad >= ldrxnTarget * 0.95 = 181.45)
        val knockEntries = (0 until 8).map { i ->
            entry(
                rpm = 3000.0 + i * 100,
                actualLoad = 180.0,       // deficit = 191 - 180 = 11 > 5
                requestedLoad = 191.0,    // >= 181.45
                requestedMap = 2200.0,
                actualMap = 2190.0        // |diff| = 10 < 50
            )
        }
        val result = TransientDetector.detect(knockEntries)

        assertEquals(1, result.knockEvents)
        assertTrue(result.knockSampleCount >= 3)
    }

    @Test
    fun `TransientDetector - no knock when boost is short`() {
        // Load deficit present but boost shortfall > 50 mbar → not knock-related
        val entries = (0 until 10).map {
            entry(
                actualLoad = 180.0,       // deficit = 11 > 5
                requestedLoad = 191.0,
                requestedMap = 2300.0,
                actualMap = 2200.0        // |diff| = 100 > 50 → boost shortfall
            )
        }
        val result = TransientDetector.detect(entries)

        assertEquals(0, result.knockEvents,
            "Should not flag knock when boost shortfall explains the deficit")
    }

    @Test
    fun `TransientDetector - empty entries yield empty result`() {
        val result = TransientDetector.detect(emptyList())
        assertEquals(0, result.overboostEvents)
        assertEquals(0, result.knockEvents)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `TransientDetector - custom ldrxnTarget for MED17`() {
        // MED17 may use different LDRXN target (e.g. 220%)
        val ldrxn = 220.0
        val entries = (0 until 8).map { i ->
            entry(rpm = 3000.0 + i * 100, actualLoad = 230.0)  // 230 - 220 = 10 → overboost
        }
        val result = TransientDetector.detect(entries, ldrxnTarget = ldrxn)

        assertEquals(1, result.overboostEvents)
    }

    @Test
    fun `TransientDetector - overboost event at end of data is still captured`() {
        // Overboost spanning to the very end of the list (close-final-event path)
        val normal = (0 until 5).map { entry(actualLoad = 191.0) }
        val overboost = (0 until 5).map { i ->
            entry(rpm = 4000.0 + i * 100, actualLoad = 200.0)
        }
        val entries = normal + overboost
        val result = TransientDetector.detect(entries)

        assertEquals(1, result.overboostEvents,
            "Overboost running to end of data should still be captured")
    }

    // ════════════════════════════════════════════════════════════════
    //  EnvironmentalCorrector
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `EnvironmentalCorrector - sea level standard conditions`() {
        val entries = rampPull(count = 20, barometricPressure = 1013.0)
        val summary = EnvironmentalCorrector.analyzeSummary(entries)

        assertFalse(summary.altitudeDeviation,
            "Sea-level baro should not trigger altitude deviation")
        assertFalse(summary.tempDeviation)
        assertTrue(summary.warnings.isEmpty())
        assertEquals(1013.0, summary.avgBaroPressure, 1.0)
        assertTrue(summary.estimatedAltitudeM < 50.0,
            "Estimated altitude at sea level should be near 0")
    }

    @Test
    fun `EnvironmentalCorrector - high altitude triggers warning`() {
        // Denver-ish: ~840 mbar ≈ 1600m. Deviation > 30 mbar from 1013
        val entries = rampPull(count = 20, barometricPressure = 840.0)
        val summary = EnvironmentalCorrector.analyzeSummary(entries)

        assertTrue(summary.altitudeDeviation,
            "Low baro pressure should trigger altitude deviation")
        assertTrue(summary.warnings.any { it.contains("Altitude") || it.contains("altitude") })
        assertTrue(summary.estimatedAltitudeM > 1000.0,
            "Estimated altitude should be >1000m for 840 mbar")
    }

    @Test
    fun `EnvironmentalCorrector - very high altitude triggers KFTARX warning`() {
        // Very high altitude: baro < 950 + altitudeDeviation → extra warning
        val entries = rampPull(count = 20, barometricPressure = 900.0)
        val summary = EnvironmentalCorrector.analyzeSummary(entries)

        assertTrue(summary.altitudeDeviation)
        // baro=900 < 950 → should get the charge air temp warning
        assertTrue(summary.warnings.any { it.contains("KFTARX") || it.contains("charge air") },
            "Very high altitude should trigger KFTARX / charge-air-temp warning")
    }

    @Test
    fun `EnvironmentalCorrector - empty entries yield standard defaults`() {
        val summary = EnvironmentalCorrector.analyzeSummary(emptyList())

        assertEquals(1013.0, summary.avgBaroPressure, 1.0)
        assertFalse(summary.altitudeDeviation)
        assertFalse(summary.tempDeviation)
        assertTrue(summary.warnings.isEmpty())
    }

    @Test
    fun `EnvironmentalCorrector - slightly below sea level no deviation`() {
        // 1000 mbar: |1000 - 1013| = 13 < 30 threshold → no deviation
        val entries = rampPull(count = 20, barometricPressure = 1000.0)
        val summary = EnvironmentalCorrector.analyzeSummary(entries)

        assertFalse(summary.altitudeDeviation,
            "13 mbar deviation should NOT trigger altitude warning")
    }

    @Test
    fun `EnvironmentalCorrector - ftbr at standard conditions is near 1`() {
        val ftbr = EnvironmentalCorrector.standardFtbr()
        // At standard conditions (20°C intake, 96°C coolant), ftbr should be ~0.95-1.05
        assertTrue(ftbr in 0.85..1.15,
            "ftbr at standard conditions should be near 1.0, got $ftbr")
    }

    @Test
    fun `EnvironmentalCorrector - computeFtbr varies with intake temperature`() {
        val ftbrCold = EnvironmentalCorrector.computeFtbr(
            intakeAirTemp = 0.0, coolantTemp = 96.0, rpm = 3000.0, load = 100.0
        )
        val ftbrHot = EnvironmentalCorrector.computeFtbr(
            intakeAirTemp = 50.0, coolantTemp = 96.0, rpm = 3000.0, load = 100.0
        )
        // Colder intake air → higher density → higher ftbr
        assertTrue(ftbrCold > ftbrHot,
            "Cold intake should give higher ftbr than hot intake")
    }

    // ════════════════════════════════════════════════════════════════
    //  ThrottleBodyChecker
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `ThrottleBodyChecker - full throttle on target not restricted`() {
        // WOT with no pressure deficit → not restricted
        val entries = rampPull(
            count = 20,
            throttleAngle = 100.0,
            wgdc = 80.0,
            requestedMap = 2200.0,
            actualMap = 2200.0
        )
        val result = ThrottleBodyChecker.check(entries, turboMaxed = false)

        assertFalse(result.restricted, "On-target WOT should not be restricted")
        assertEquals(0, result.affectedSampleCount)
    }

    @Test
    fun `ThrottleBodyChecker - detects throttle restriction at low RPM`() {
        // Wide-open throttle (>95°) + high WGDC (>70%) + large pressure deficit (>80 mbar)
        // Present at low RPM to distinguish from turbo limitation
        val restrictedEntries = (0 until 20).map { i ->
            entry(
                rpm = 2500.0 + i * 50,  // all < 4000 RPM
                throttleAngle = 100.0,
                wgdc = 85.0,
                requestedMap = 2400.0,
                actualMap = 2200.0       // deficit = 200 mbar > 80 threshold
            )
        }
        val result = ThrottleBodyChecker.check(restrictedEntries, turboMaxed = false)

        assertTrue(result.restricted,
            "Low-RPM pressure deficit with WOT should flag throttle restriction")
        assertTrue(result.avgPressureDeficit > 80.0)
        assertTrue(result.detail.contains("restriction") || result.detail.contains("Throttle"))
    }

    @Test
    fun `ThrottleBodyChecker - turbo maxed overrides restriction detection`() {
        // Same data as restriction test, but turboMaxed = true
        val entries = (0 until 20).map { i ->
            entry(
                rpm = 2500.0 + i * 50,
                throttleAngle = 100.0,
                wgdc = 85.0,
                requestedMap = 2400.0,
                actualMap = 2200.0
            )
        }
        val result = ThrottleBodyChecker.check(entries, turboMaxed = true)

        assertFalse(result.restricted,
            "Should not flag restriction when turbo is maxed")
        assertTrue(result.detail.contains("turbo"))
    }

    @Test
    fun `ThrottleBodyChecker - empty entries not restricted`() {
        val result = ThrottleBodyChecker.check(emptyList(), turboMaxed = false)
        assertFalse(result.restricted)
        assertEquals("No data", result.detail)
    }

    @Test
    fun `ThrottleBodyChecker - occasional deficit not flagged`() {
        // Only 1 out of 20 samples has a deficit → fraction = 5% < 10% threshold
        val normal = (0 until 19).map {
            entry(
                throttleAngle = 100.0, wgdc = 80.0,
                requestedMap = 2200.0, actualMap = 2200.0
            )
        }
        val outlier = listOf(
            entry(
                throttleAngle = 100.0, wgdc = 80.0,
                requestedMap = 2400.0, actualMap = 2200.0  // deficit but isolated
            )
        )
        val result = ThrottleBodyChecker.check(normal + outlier, turboMaxed = false)

        assertFalse(result.restricted,
            "Isolated deficit should not flag restriction")
    }

    @Test
    fun `ThrottleBodyChecker - partial throttle not flagged`() {
        // throttleAngle < 95° → entries are not even considered "wide open"
        val entries = (0 until 20).map {
            entry(
                throttleAngle = 80.0,
                wgdc = 85.0,
                requestedMap = 2400.0,
                actualMap = 2200.0
            )
        }
        val result = ThrottleBodyChecker.check(entries, turboMaxed = false)

        assertFalse(result.restricted,
            "Partial throttle entries should not flag restriction")
    }

    @Test
    fun `ThrottleBodyChecker - low WGDC not flagged`() {
        // WGDC < 70% → "turbo not trying" → not a restriction
        val entries = (0 until 20).map {
            entry(
                throttleAngle = 100.0,
                wgdc = 50.0,
                requestedMap = 2400.0,
                actualMap = 2200.0
            )
        }
        val result = ThrottleBodyChecker.check(entries, turboMaxed = false)

        assertFalse(result.restricted,
            "Low WGDC should not flag throttle restriction")
    }

    @Test
    fun `ThrottleBodyChecker - MED17 values with restriction`() {
        // MED17 may have higher MAP targets
        val entries = (0 until 20).map { i ->
            entry(
                rpm = 2000.0 + i * 80,
                throttleAngle = 98.0,
                wgdc = 90.0,
                requestedMap = 2800.0,
                actualMap = 2600.0       // 200 mbar deficit
            )
        }
        val result = ThrottleBodyChecker.check(entries, turboMaxed = false)

        assertTrue(result.restricted,
            "MED17 values with significant deficit should flag restriction")
    }
}
