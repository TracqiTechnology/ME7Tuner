package domain.model.simulator

import domain.math.map.Map3d
import domain.model.optimizer.OptimizerCalculator
import kotlin.math.abs
import kotlin.math.sign

/**
 * Phase 15: Full PID dynamics simulator for ME7 boost control.
 *
 * Simulates the LDRPID controller using the actual ME7 PID formulas to predict
 * transient boost behavior. This enables detection of oscillation, I-term windup,
 * slow convergence, and overshoot.
 *
 * ME7 Reference: LDRPID 25.10 (me7-raw.txt line 143411–144456)
 *
 * PID formulas (line 144361–144363):
 * ```
 * lde = plsol − pvdks_w                           (control deviation)
 * P: ldptv  = (LDRQ0DY − KFLDRQ2(rpm,p)) × lde
 * I: lditv  = lditv(i-1) + KFLDRQ1(rpm,p) × lde(i-1)   bounded by [0, KFLDIMX]
 * D: ldrdtv = (lde − lde(i-1)) × KFLDRQ2(rpm,p)
 * ldtv = KFLDRL(ldptv + lditv + ldrdtv)
 * ```
 *
 * @see documentation/me7-boost-control.md §4
 * @see documentation/me7-maps-reference.md — PID Gain Maps section
 */
object PidSimulator {

    /** Typical ME7 LDR controller cycle time in milliseconds. */
    private const val DEFAULT_SAMPLE_INTERVAL_MS = 20.0

    /** Default P-gain constant (LDRQ0DY). See me7-raw.txt line 144447. */
    private const val DEFAULT_LDRQ0DY = 1.0  // 100% / emax, user should calibrate

    /** Maximum WGDC output. */
    private const val MAX_WGDC = 95.0

    /** Minimum WGDC output. */
    private const val MIN_WGDC = 0.0

    // ── Data classes ────────────────────────────────────────────────

    /** State of the PID controller at a single time step. */
    data class PidState(
        val timeMs: Double,
        val rpm: Double,
        val plsol: Double,              // Target pressure (before throttle)
        val pvdks: Double,              // Actual pressure (from log)
        val lde: Double,                // Control deviation (plsol − pvdks)
        val ldptv: Double,              // P-term contribution
        val lditv: Double,              // I-term accumulator
        val ldrdtv: Double,             // D-term contribution
        val ldtv: Double,               // Total duty cycle before linearization
        val ldtvLinearized: Double,     // Output WGDC after KFLDRL linearization
        val actualWgdc: Double          // Actual WGDC from log for comparison
    )

    /** Diagnostic results from analyzing PID behavior. */
    data class PidDiagnosis(
        val oscillationDetected: Boolean,
        val oscillationCount: Int,          // Number of sign changes in lde
        val windupDetected: Boolean,
        val windupDurationMs: Double,       // Time spent at I-term limit
        val slowConvergence: Boolean,
        val convergenceTimeMs: Double,      // Time to reach |lde| < 20 mbar
        val overshootDetected: Boolean,
        val overshootMagnitude: Double,     // Max overshoot in mbar
        val avgAbsLde: Double,              // Average absolute control deviation
        val recommendations: List<String>
    )

    /** Complete PID simulation result for one WOT pull. */
    data class PidSimulationResult(
        val states: List<PidState>,
        val diagnosis: PidDiagnosis
    )

    // ── Simulation ──────────────────────────────────────────────────

    /**
     * Simulate the PID controller for a sequence of WOT samples (one pull).
     *
     * @param pullEntries Time-ordered WOT entries from a single pull
     * @param kfldrq0 P-gain map (RPM × pressure → gain)
     * @param kfldrq1 I-gain map (RPM × pressure → gain)
     * @param kfldrq2 D-gain map (RPM × pressure → gain)
     * @param kfldrl Feedforward linearization map
     * @param kfldimx I-term limiter map
     * @param ldrq0dy P-gain constant (LDRQ0DY)
     * @param sampleIntervalMs Controller sample interval
     * @return PID states and diagnostic analysis
     */
    fun simulate(
        pullEntries: List<OptimizerCalculator.WotLogEntry>,
        kfldrq0: Map3d?,
        kfldrq1: Map3d?,
        kfldrq2: Map3d?,
        kfldrl: Map3d?,
        kfldimx: Map3d?,
        ldrq0dy: Double = DEFAULT_LDRQ0DY,
        sampleIntervalMs: Double = DEFAULT_SAMPLE_INTERVAL_MS
    ): PidSimulationResult {
        if (pullEntries.isEmpty()) {
            return PidSimulationResult(emptyList(), emptyDiagnosis())
        }

        val states = mutableListOf<PidState>()
        var previousLde = 0.0
        var lditv = 0.0  // I-term accumulator

        for ((i, entry) in pullEntries.withIndex()) {
            val timeMs = i * sampleIntervalMs
            val rpm = entry.rpm

            // Compute plsol = pssol / vpsspls_w (≈1.016)
            val plsol = entry.requestedMap / 1.016

            // Control deviation
            val lde = plsol - entry.actualMap

            // Look up gains at this operating point
            val relativePsi = (entry.actualMap - entry.barometricPressure) * 0.0145038
            val q0 = kfldrq0?.lookup(relativePsi.coerceAtLeast(0.0), rpm) ?: 0.5
            val q1 = kfldrq1?.lookup(relativePsi.coerceAtLeast(0.0), rpm) ?: 0.1
            val q2 = kfldrq2?.lookup(relativePsi.coerceAtLeast(0.0), rpm) ?: 0.0

            // P-term: (LDRQ0DY − Q2) × lde
            val effectivePGain = ldrq0dy - q2
            val ldptv = effectivePGain * lde / 100.0  // Scale: lde in mbar → % duty

            // I-term: accumulate
            if (i > 0) {
                lditv += q1 * previousLde / 100.0
            }

            // Clamp I-term by KFLDIMX
            val imaxLimit = if (kfldimx != null && relativePsi > 0) {
                kfldimx.lookup(relativePsi, rpm)
            } else MAX_WGDC

            lditv = lditv.coerceIn(0.0, imaxLimit)

            // D-term: (lde − lde_prev) × Q2
            val ldrdtv = if (i > 0) {
                q2 * (lde - previousLde) / 100.0
            } else 0.0

            // Total PID output
            val ldtv = (ldptv + lditv + ldrdtv).coerceIn(MIN_WGDC, MAX_WGDC)

            // Linearize through KFLDRL
            val ldtvLinearized = if (kfldrl != null && relativePsi > 0) {
                kfldrl.lookup(relativePsi, rpm)
            } else ldtv

            states.add(PidState(
                timeMs = timeMs,
                rpm = rpm,
                plsol = plsol,
                pvdks = entry.actualMap,
                lde = lde,
                ldptv = ldptv,
                lditv = lditv,
                ldrdtv = ldrdtv,
                ldtv = ldtv,
                ldtvLinearized = ldtvLinearized,
                actualWgdc = entry.wgdc
            ))

            previousLde = lde
        }

        val diagnosis = analyzePidBehavior(states, sampleIntervalMs)
        return PidSimulationResult(states, diagnosis)
    }

    // ── Diagnostic analysis ─────────────────────────────────────────

    private fun analyzePidBehavior(
        states: List<PidState>,
        sampleIntervalMs: Double
    ): PidDiagnosis {
        if (states.isEmpty()) return emptyDiagnosis()

        // ── Oscillation detection ──────────────────────────
        // Count sign changes in lde (excluding near-zero values)
        var signChanges = 0
        var lastSign = 0
        for (state in states) {
            if (abs(state.lde) > 10) {  // Ignore noise below 10 mbar
                val currentSign = state.lde.sign.toInt()
                if (currentSign != 0 && lastSign != 0 && currentSign != lastSign) {
                    signChanges++
                }
                if (currentSign != 0) lastSign = currentSign
            }
        }
        // Oscillation: >3 reversals per 500ms of data
        val durationMs = states.size * sampleIntervalMs
        val oscillationsPerSecond = if (durationMs > 0) signChanges / (durationMs / 1000.0) else 0.0
        val oscillationDetected = oscillationsPerSecond > 6.0  // >3 per 500ms = >6 per second

        // ── I-term windup detection ────────────────────────
        var windupSamples = 0
        for (state in states) {
            // Detect when lditv is at its maximum and lde is still positive (still trying to increase)
            if (state.lditv > 0 && state.lde > 20) {
                windupSamples++
            }
        }
        val windupDurationMs = windupSamples * sampleIntervalMs
        val windupDetected = windupDurationMs > 200  // >200ms at limit

        // ── Convergence time ───────────────────────────────
        // Time to reach |lde| < 20 mbar after initial error
        var convergenceIdx = -1
        val initialError = states.firstOrNull()?.lde?.let { abs(it) } ?: 0.0
        if (initialError > 20) {
            for ((i, state) in states.withIndex()) {
                if (abs(state.lde) < 20) {
                    convergenceIdx = i
                    break
                }
            }
        }
        val convergenceTimeMs = if (convergenceIdx >= 0) convergenceIdx * sampleIntervalMs else durationMs
        val slowConvergence = convergenceTimeMs > 1500  // >1.5 seconds

        // ── Overshoot detection ────────────────────────────
        // After reaching near-zero lde, does it go negative (pvdks > plsol)?
        var overshootMagnitude = 0.0
        var reachedTarget = false
        for (state in states) {
            if (abs(state.lde) < 20) reachedTarget = true
            if (reachedTarget && state.lde < -30) {
                overshootMagnitude = maxOf(overshootMagnitude, abs(state.lde))
            }
        }
        val overshootDetected = overshootMagnitude > 30

        // ── Average error ──────────────────────────────────
        val avgAbsLde = states.map { abs(it.lde) }.average()

        // ── Recommendations ────────────────────────────────
        val recommendations = mutableListOf<String>()
        if (oscillationDetected) {
            recommendations.add("Reduce KFLDRQ0 P-gain by 10–20% to dampen oscillation (${String.format("%.1f", oscillationsPerSecond)} reversals/sec)")
        }
        if (windupDetected) {
            recommendations.add("Increase KFLDIMX I-term ceiling — windup detected for ${String.format("%.0f", windupDurationMs)}ms")
        }
        if (slowConvergence) {
            recommendations.add("Increase KFLDRQ1 I-gain by 15–25% — convergence takes ${String.format("%.0f", convergenceTimeMs)}ms (target: <1500ms)")
        }
        if (overshootDetected) {
            recommendations.add("Increase KFLDRQ2 D-gain — overshoot of ${String.format("%.0f", overshootMagnitude)} mbar detected")
        }

        return PidDiagnosis(
            oscillationDetected = oscillationDetected,
            oscillationCount = signChanges,
            windupDetected = windupDetected,
            windupDurationMs = windupDurationMs,
            slowConvergence = slowConvergence,
            convergenceTimeMs = convergenceTimeMs,
            overshootDetected = overshootDetected,
            overshootMagnitude = overshootMagnitude,
            avgAbsLde = avgAbsLde,
            recommendations = recommendations
        )
    }

    private fun emptyDiagnosis() = PidDiagnosis(
        oscillationDetected = false, oscillationCount = 0,
        windupDetected = false, windupDurationMs = 0.0,
        slowConvergence = false, convergenceTimeMs = 0.0,
        overshootDetected = false, overshootMagnitude = 0.0,
        avgAbsLde = 0.0, recommendations = emptyList()
    )
}

