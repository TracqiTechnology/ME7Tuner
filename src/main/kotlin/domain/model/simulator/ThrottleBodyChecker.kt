package domain.model.simulator

import domain.model.optimizer.OptimizerCalculator
import kotlin.math.abs

/**
 * Phase 19: KLAF outflow verification — throttle body restriction detection.
 *
 * At WOT the throttle should be fully open. If pvdks is significantly below pssol
 * despite high WGDC and wide-open throttle, the throttle body may be restricted.
 * This is distinct from turbo limitation (which grows with RPM).
 *
 * ME7 Reference:
 * - KLAF: me7-raw.txt line 52933–52997
 * - pspvdk > 0.95 → high sensitivity zone (line 52937)
 * - KFMSNWDK: throttle angle → airflow (line 52919)
 *
 * @see documentation/me7-boost-control.md §7
 */
object ThrottleBodyChecker {

    /** Result of throttle body check. */
    data class ThrottleCheckResult(
        val restricted: Boolean,
        val affectedSampleCount: Int,
        val affectedRpmRange: String?,
        val avgPressureDeficit: Double,
        val severityPercent: Double,
        val detail: String
    )

    // ── Thresholds ─────────────────────────────────────────────────

    /** Minimum throttle angle to consider "wide open" */
    private const val WOT_THROTTLE_ANGLE = 95.0

    /** WGDC must be above this to rule out "turbo not trying" */
    private const val MIN_WGDC_FOR_CHECK = 70.0

    /** Pressure deficit (pssol − pvdks) above this triggers check */
    private const val PRESSURE_DEFICIT_THRESHOLD_MBAR = 80.0

    /** Minimum fraction of WOT samples with restriction to flag */
    private const val MIN_AFFECTED_FRACTION = 0.10

    // ── Detection ──────────────────────────────────────────────────

    fun check(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        turboMaxed: Boolean
    ): ThrottleCheckResult {
        if (wotEntries.isEmpty()) {
            return ThrottleCheckResult(false, 0, null, 0.0, 0.0, "No data")
        }

        // Look for samples where: wide-open throttle + high WGDC + significant pressure deficit
        val suspects = wotEntries.filter { entry ->
            entry.throttleAngle >= WOT_THROTTLE_ANGLE &&
                entry.wgdc >= MIN_WGDC_FOR_CHECK &&
                (entry.requestedMap - entry.actualMap) > PRESSURE_DEFICIT_THRESHOLD_MBAR
        }

        if (suspects.isEmpty()) {
            return ThrottleCheckResult(false, 0, null, 0.0, 0.0, "No restriction detected")
        }

        val affectedFraction = suspects.size.toDouble() / wotEntries.size
        if (affectedFraction < MIN_AFFECTED_FRACTION) {
            return ThrottleCheckResult(false, suspects.size, null, 0.0, 0.0,
                "Occasional pressure deficit (${suspects.size} samples) — likely transient, not restriction")
        }

        // If the turbo is maxed, the deficit is from turbo, not throttle
        if (turboMaxed) {
            return ThrottleCheckResult(false, suspects.size, null, 0.0, 0.0,
                "Pressure deficit detected but turbo is maxed — deficit is from turbo limitation, not throttle")
        }

        // Distinguish throttle restriction from turbo limitation:
        // Turbo limitation → deficit grows with RPM (turbo runs out of flow)
        // Throttle restriction → deficit present even at moderate RPM
        val lowRpmSuspects = suspects.filter { it.rpm < 4000 }
        val highRpmSuspects = suspects.filter { it.rpm >= 4000 }

        val lowRpmAvgDeficit = if (lowRpmSuspects.isNotEmpty()) {
            lowRpmSuspects.map { it.requestedMap - it.actualMap }.average()
        } else 0.0

        val highRpmAvgDeficit = if (highRpmSuspects.isNotEmpty()) {
            highRpmSuspects.map { it.requestedMap - it.actualMap }.average()
        } else 0.0

        // If deficit is similar at low and high RPM → throttle restriction
        // If deficit grows significantly with RPM → turbo limitation
        val isThrottleRestriction = lowRpmSuspects.isNotEmpty() && lowRpmAvgDeficit > PRESSURE_DEFICIT_THRESHOLD_MBAR

        val avgDeficit = suspects.map { it.requestedMap - it.actualMap }.average()
        val rpmRange = "${suspects.minOf { it.rpm }.toInt()} – ${suspects.maxOf { it.rpm }.toInt()}"
        val severity = (avgDeficit / (suspects.map { it.requestedMap }.average())) * 100

        return ThrottleCheckResult(
            restricted = isThrottleRestriction,
            affectedSampleCount = suspects.size,
            affectedRpmRange = rpmRange,
            avgPressureDeficit = avgDeficit,
            severityPercent = severity,
            detail = if (isThrottleRestriction) {
                "Throttle restriction detected: ${String.format("%.0f", avgDeficit)} mbar avg deficit across $rpmRange RPM " +
                    "(${suspects.size} samples, ${String.format("%.0f", affectedFraction * 100)}% of WOT). " +
                    "Low-RPM deficit (${String.format("%.0f", lowRpmAvgDeficit)} mbar) suggests intake restriction, not turbo limit."
            } else {
                "Pressure deficit of ${String.format("%.0f", avgDeficit)} mbar detected but grows with RPM — " +
                    "likely turbo limitation, not throttle restriction."
            }
        )
    }
}

