package domain.model.simulator

import domain.model.optimizer.OptimizerCalculator
import kotlin.math.abs

/**
 * Phase 16: Detect transient events — overboost (drlmaxo) and knock load reduction.
 *
 * During WOT, the ECU may temporarily allow higher load (overboost) or reduce
 * load due to knock detection (KFFLLDE). These are not steady-state conditions
 * and should be identified separately from normal calibration data.
 *
 * ME7 Reference:
 * - Overboost: LDROBG (me7-raw.txt line 143196), LDRLMX (line 142471)
 * - Knock: KFFLLDE (line 142037, 142475), RLKRLDA (line 142047, 142512)
 */
object TransientDetector {

    /** Type of transient event. */
    enum class EventType {
        /** Normal steady-state WOT */
        NORMAL,
        /** Load exceeds LDRXN temporarily (drlmaxo active) */
        OVERBOOST,
        /** Load reduced below LDRXN due to knock adaptation */
        KNOCK_REDUCTION,
        /** Overload protection active (LDORXN) */
        OVERLOAD
    }

    /** A detected transient event spanning multiple samples. */
    data class TransientEvent(
        val type: EventType,
        val startIndex: Int,
        val endIndex: Int,
        val sampleCount: Int,
        val peakMagnitude: Double,    // peak drlmaxo or peak load reduction
        val avgRpm: Double,
        val description: String
    )

    /** Summary of all transient event detection. */
    data class TransientResult(
        val events: List<TransientEvent>,
        val overboostEvents: Int,
        val knockEvents: Int,
        val overboostSampleCount: Int,
        val knockSampleCount: Int,
        /** Indices of samples in transient events (for chart highlighting). */
        val transientIndices: Set<Int>,
        val warnings: List<String>,
        val recommendations: List<String>
    )

    // ── Thresholds ─────────────────────────────────────────────────

    /** Overboost: actual load exceeds LDRXN by at least this much */
    private const val OVERBOOST_MIN_LOAD_EXCESS = 2.0  // % load

    /** Overboost: max allowed excess (above this, something is wrong) */
    private const val OVERBOOST_MAX_LOAD_EXCESS = 15.0  // % load (~15% per ME7 spec)

    /** Knock detection: load deficit below LDRXN that suggests knock reduction */
    private const val KNOCK_LOAD_DEFICIT_THRESHOLD = 5.0  // % load

    /** Minimum consecutive samples to form an event */
    private const val MIN_EVENT_SAMPLES = 3

    // ── Detection ──────────────────────────────────────────────────

    fun detect(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        ldrxnTarget: Double = 191.0
    ): TransientResult {
        if (wotEntries.isEmpty()) {
            return TransientResult(emptyList(), 0, 0, 0, 0, emptySet(), emptyList(), emptyList())
        }

        val events = mutableListOf<TransientEvent>()
        val transientIndices = mutableSetOf<Int>()
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        // ── Overboost detection ────────────────────────────────
        var overbStart = -1
        var overbPeak = 0.0
        for (i in wotEntries.indices) {
            val excess = wotEntries[i].actualLoad - ldrxnTarget
            if (excess > OVERBOOST_MIN_LOAD_EXCESS && excess < OVERBOOST_MAX_LOAD_EXCESS) {
                if (overbStart == -1) overbStart = i
                overbPeak = maxOf(overbPeak, excess)
            } else {
                if (overbStart != -1 && (i - overbStart) >= MIN_EVENT_SAMPLES) {
                    val entries = wotEntries.subList(overbStart, i)
                    events.add(TransientEvent(
                        type = EventType.OVERBOOST,
                        startIndex = overbStart,
                        endIndex = i - 1,
                        sampleCount = i - overbStart,
                        peakMagnitude = overbPeak,
                        avgRpm = entries.map { it.rpm }.average(),
                        description = "Overboost: +${String.format("%.1f", overbPeak)}% load for ${i - overbStart} samples"
                    ))
                    for (j in overbStart until i) transientIndices.add(j)
                }
                overbStart = -1
                overbPeak = 0.0
            }
        }
        // Close final event
        if (overbStart != -1 && (wotEntries.size - overbStart) >= MIN_EVENT_SAMPLES) {
            val entries = wotEntries.subList(overbStart, wotEntries.size)
            events.add(TransientEvent(
                type = EventType.OVERBOOST,
                startIndex = overbStart,
                endIndex = wotEntries.size - 1,
                sampleCount = wotEntries.size - overbStart,
                peakMagnitude = overbPeak,
                avgRpm = entries.map { it.rpm }.average(),
                description = "Overboost: +${String.format("%.1f", overbPeak)}% load for ${wotEntries.size - overbStart} samples"
            ))
            for (j in overbStart until wotEntries.size) transientIndices.add(j)
        }

        // ── Knock load reduction detection ─────────────────────
        // Detect RPM ranges where load is consistently below LDRXN despite
        // WOT conditions and no torque cap or boost shortfall
        var knockStart = -1
        var knockPeak = 0.0
        for (i in wotEntries.indices) {
            val entry = wotEntries[i]
            val deficit = ldrxnTarget - entry.actualLoad
            val noBoostShortfall = abs(entry.requestedMap - entry.actualMap) < 50
            val notTorqueCapped = entry.requestedLoad >= ldrxnTarget * 0.95
            val knockSuspect = deficit > KNOCK_LOAD_DEFICIT_THRESHOLD && noBoostShortfall && notTorqueCapped

            if (knockSuspect) {
                if (knockStart == -1) knockStart = i
                knockPeak = maxOf(knockPeak, deficit)
            } else {
                if (knockStart != -1 && (i - knockStart) >= MIN_EVENT_SAMPLES) {
                    val entries = wotEntries.subList(knockStart, i)
                    events.add(TransientEvent(
                        type = EventType.KNOCK_REDUCTION,
                        startIndex = knockStart,
                        endIndex = i - 1,
                        sampleCount = i - knockStart,
                        peakMagnitude = knockPeak,
                        avgRpm = entries.map { it.rpm }.average(),
                        description = "Knock reduction: −${String.format("%.1f", knockPeak)}% load for ${i - knockStart} samples"
                    ))
                    for (j in knockStart until i) transientIndices.add(j)
                }
                knockStart = -1
                knockPeak = 0.0
            }
        }
        // Close final event
        if (knockStart != -1 && (wotEntries.size - knockStart) >= MIN_EVENT_SAMPLES) {
            val entries = wotEntries.subList(knockStart, wotEntries.size)
            events.add(TransientEvent(
                type = EventType.KNOCK_REDUCTION,
                startIndex = knockStart,
                endIndex = wotEntries.size - 1,
                sampleCount = wotEntries.size - knockStart,
                peakMagnitude = knockPeak,
                avgRpm = entries.map { it.rpm }.average(),
                description = "Knock reduction: −${String.format("%.1f", knockPeak)}% load for ${wotEntries.size - knockStart} samples"
            ))
            for (j in knockStart until wotEntries.size) transientIndices.add(j)
        }

        // ── Summary ────────────────────────────────────────────
        val overboostEvents = events.count { it.type == EventType.OVERBOOST }
        val knockEvents = events.count { it.type == EventType.KNOCK_REDUCTION }
        val overboostSamples = events.filter { it.type == EventType.OVERBOOST }.sumOf { it.sampleCount }
        val knockSamples = events.filter { it.type == EventType.KNOCK_REDUCTION }.sumOf { it.sampleCount }

        if (overboostEvents > 0) {
            val peakOverboost = events.filter { it.type == EventType.OVERBOOST }.maxOf { it.peakMagnitude }
            warnings.add("🔶 $overboostEvents overboost event(s) detected ($overboostSamples samples, peak +${String.format("%.1f", peakOverboost)}% load). " +
                "These samples are excluded from steady-state corrections.")
        }

        if (knockEvents > 0) {
            val peakKnock = events.filter { it.type == EventType.KNOCK_REDUCTION }.maxOf { it.peakMagnitude }
            val knockPct = (knockSamples.toDouble() / wotEntries.size * 100)
            warnings.add("🔴 $knockEvents knock load reduction event(s) detected ($knockSamples samples, peak −${String.format("%.1f", peakKnock)}% load).")
            if (knockPct > 20) {
                recommendations.add("Reduce LDRXN by ${String.format("%.0f", peakKnock)}% or improve ignition timing — " +
                    "knock reduction is active in ${String.format("%.0f", knockPct)}% of WOT samples.")
            }
        }

        return TransientResult(
            events = events,
            overboostEvents = overboostEvents,
            knockEvents = knockEvents,
            overboostSampleCount = overboostSamples,
            knockSampleCount = knockSamples,
            transientIndices = transientIndices,
            warnings = warnings,
            recommendations = recommendations
        )
    }
}

