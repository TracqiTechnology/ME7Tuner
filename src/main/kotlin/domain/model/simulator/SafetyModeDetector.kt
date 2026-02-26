package domain.model.simulator

import domain.model.optimizer.OptimizerCalculator
import kotlin.math.abs

/**
 * Phase 17: Detect ECU safety modes — overload protection and fallback to open-loop.
 *
 * When the ECU detects anomalous conditions (overcharge, regulation errors), it
 * falls back to protective modes that change WOT behavior. Samples taken during
 * these modes should be excluded from calibration suggestions.
 *
 * ME7 Reference:
 * - Overload (LDORXN): me7-raw.txt line 142495–142502
 * - Overcharge (DPUPS): me7-raw.txt line 130362, 130642
 * - Fallback (B_lds → KFTVLDRE): me7-raw.txt line 141595–141601, 144717
 *
 * @see documentation/me7-boost-control.md §4, §8
 */
object SafetyModeDetector {

    /** Why a sample was excluded from calibration. */
    enum class ExclusionReason {
        /** pvdks − pus exceeds DPUPS threshold → overcharge protection */
        OVERLOAD,
        /** WGDC shows step-change pattern indicating B_lds fallback mode */
        FALLBACK,
        /** Extended regulation error (|pssol − pvdks| > threshold for too long) */
        REGULATION_ERROR
    }

    /** A sample excluded from calibration with the reason. */
    data class ExcludedSample(
        val index: Int,
        val reason: ExclusionReason,
        val detail: String
    )

    /** Summary of all safety mode detections. */
    data class SafetyModeResult(
        val excludedSamples: List<ExcludedSample>,
        val overloadCount: Int,
        val fallbackCount: Int,
        val regulationErrorCount: Int,
        val warnings: List<String>
    )

    // ── Thresholds ─────────────────────────────────────────────────

    /** DPUPS threshold: pvdks − pus ≥ 250 hPa (me7-raw.txt line 130642) */
    private const val DPUPS_THRESHOLD_MBAR = 250.0

    /** Sustained regulation error: |pssol − pvdks| > this for consecutive samples */
    private const val REGULATION_ERROR_THRESHOLD_MBAR = 200.0

    /** Number of consecutive samples with regulation error to flag */
    private const val REGULATION_ERROR_CONSECUTIVE = 15

    /** WGDC step-change detection: |WGDC[i] − WGDC[i-1]| > this AND then stays flat */
    private const val WGDC_STEP_THRESHOLD = 20.0

    /** Number of consecutive flat WGDC samples to confirm fallback */
    private const val WGDC_FLAT_CONSECUTIVE = 8

    /** WGDC flatness tolerance */
    private const val WGDC_FLAT_TOLERANCE = 2.0

    // ── Detection ──────────────────────────────────────────────────

    fun detect(wotEntries: List<OptimizerCalculator.WotLogEntry>): SafetyModeResult {
        val excluded = mutableListOf<ExcludedSample>()
        val warnings = mutableListOf<String>()

        // ── Overload detection ─────────────────────────────────
        var overloadCount = 0
        for ((i, entry) in wotEntries.withIndex()) {
            val overcharge = entry.actualMap - entry.barometricPressure
            if (overcharge > DPUPS_THRESHOLD_MBAR) {
                excluded.add(ExcludedSample(
                    index = i,
                    reason = ExclusionReason.OVERLOAD,
                    detail = "pvdks − pus = ${String.format("%.0f", overcharge)} mbar > DPUPS ($DPUPS_THRESHOLD_MBAR)"
                ))
                overloadCount++
            }
        }
        if (overloadCount > 0) {
            warnings.add("WARNING: $overloadCount samples show overcharge (pvdks - pus > ${DPUPS_THRESHOLD_MBAR.toInt()} mbar). " +
                "These may trigger LDORXN protection and are excluded from calibration.")
        }

        // ── Regulation error detection ─────────────────────────
        var regErrorCount = 0
        var consecutiveRegError = 0
        for ((i, entry) in wotEntries.withIndex()) {
            val regError = abs(entry.requestedMap - entry.actualMap)
            if (regError > REGULATION_ERROR_THRESHOLD_MBAR) {
                consecutiveRegError++
                if (consecutiveRegError >= REGULATION_ERROR_CONSECUTIVE) {
                    val alreadyExcluded = excluded.any { it.index == i }
                    if (!alreadyExcluded) {
                        excluded.add(ExcludedSample(
                            index = i,
                            reason = ExclusionReason.REGULATION_ERROR,
                            detail = "|pssol − pvdks| = ${String.format("%.0f", regError)} mbar for $consecutiveRegError+ samples"
                        ))
                        regErrorCount++
                    }
                }
            } else {
                consecutiveRegError = 0
            }
        }
        if (regErrorCount > 0) {
            warnings.add("WARNING: $regErrorCount samples show sustained regulation error (>$REGULATION_ERROR_CONSECUTIVE consecutive samples " +
                "with |pssol - pvdks| > ${REGULATION_ERROR_THRESHOLD_MBAR.toInt()} mbar). Possible B_ldra condition.")
        }

        // ── Fallback mode detection (B_lds → KFTVLDRE) ─────────
        var fallbackCount = 0
        if (wotEntries.size > WGDC_FLAT_CONSECUTIVE + 1) {
            var i = 1
            while (i < wotEntries.size) {
                val wgdcStep = abs(wotEntries[i].wgdc - wotEntries[i - 1].wgdc)
                if (wgdcStep > WGDC_STEP_THRESHOLD) {
                    // Check if WGDC stays flat after the step
                    val flatStart = i
                    var flatCount = 0
                    val baseWgdc = wotEntries[i].wgdc
                    var j = i
                    while (j < wotEntries.size && abs(wotEntries[j].wgdc - baseWgdc) < WGDC_FLAT_TOLERANCE) {
                        flatCount++
                        j++
                    }
                    if (flatCount >= WGDC_FLAT_CONSECUTIVE) {
                        // This looks like a fallback to KFTVLDRE
                        for (k in flatStart until (flatStart + flatCount).coerceAtMost(wotEntries.size)) {
                            val alreadyExcluded = excluded.any { it.index == k }
                            if (!alreadyExcluded) {
                                excluded.add(ExcludedSample(
                                    index = k,
                                    reason = ExclusionReason.FALLBACK,
                                    detail = "WGDC step to ${String.format("%.1f", baseWgdc)}% then flat for $flatCount samples (B_lds suspected)"
                                ))
                                fallbackCount++
                            }
                        }
                        i = flatStart + flatCount
                        continue
                    }
                }
                i++
            }
        }
        if (fallbackCount > 0) {
            warnings.add("WARNING: $fallbackCount samples appear to be in B_lds fallback mode (open-loop KFTVLDRE). " +
                "These are excluded from KFLDRL/KFLDIMX suggestions.")
        }

        return SafetyModeResult(
            excludedSamples = excluded.sortedBy { it.index },
            overloadCount = overloadCount,
            fallbackCount = fallbackCount,
            regulationErrorCount = regErrorCount,
            warnings = warnings
        )
    }
}

