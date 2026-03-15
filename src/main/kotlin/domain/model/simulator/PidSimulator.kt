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
 * **Finding 1 — Dual Operating Modes (line 144370–144380):**
 * The PID operates in two modes based on control deviation magnitude:
 * - **Dynamic (`B_lddy`):** Full PID with LDRQ0DY/KFLDRQ1/KFLDRQ2 — active when
 *   |lde| exceeds threshold (strong control for fast convergence)
 * - **Quasi-stationary (`!B_lddy`):** PI-only with LDRQ0S/LDRQ1ST — active when
 *   pvdks crosses plsol (lde sign change), avoids actuator noise
 *
 * PID formulas (line 144361–144363):
 * ```
 * lde = plsol − pvdks_w                           (control deviation)
 * P: ldptv  = (LDRQ0DY − KFLDRQ2(rpm,p)) × lde   [dynamic mode]
 *    ldptv  = LDRQ0S × lde                         [static mode]
 * I: lditv  = lditv(i-1) + KFLDRQ1(rpm,p) × lde(i-1)   bounded by [0, KFLDIMX]
 * D: ldrdtv = (lde − lde(i-1)) × KFLDRQ2(rpm,p)   [dynamic only, 0 in static]
 * ldtv = KFLDRL(ldptv + lditv + ldrdtv)
 * ```
 *
 * **Finding 10 — KFLDRQ2 Guidance (line 144453):**
 * D-gain should be zero below 2500 RPM and max 0.6 × LDRQ0DY above 2500 RPM.
 *
 * @see documentation/me7-boost-control.md §4
 * @see documentation/me7-maps-reference.md — PID Gain Maps section
 * @see documentation/optimizer-algorithms.md Findings 1, 10
 */
object PidSimulator {

    /** Typical ME7 LDR controller cycle time in milliseconds. */
    private const val DEFAULT_SAMPLE_INTERVAL_MS = 20.0

    /** Default P-gain constant (LDRQ0DY). See me7-raw.txt line 144447. */
    private const val DEFAULT_LDRQ0DY = 1.0  // 100% / emax, user should calibrate

    /**
     * Default quasi-stationary P-gain (LDRQ0S).
     * From line 144456: LDRQ0S = 0.4 × LDRQ0S_krit.
     * Default: 0.4 (assumes LDRQ0S_krit ≈ 1.0)
     */
    private const val DEFAULT_LDRQ0S = 0.4

    /**
     * Default quasi-stationary I-gain (LDRQ1ST).
     * From line 144458: LDRQ1ST = 0.5 × LDRQ0S_krit × T0 / Tkrit
     * Default: 0.05 (assumes Tkrit ≈ 0.5s, T0 = 0.05s)
     */
    private const val DEFAULT_LDRQ1ST = 0.05

    /** Maximum WGDC output. */
    private const val MAX_WGDC = 95.0

    /** Minimum WGDC output. */
    private const val MIN_WGDC = 0.0

    // ── Data classes ────────────────────────────────────────────────

    /** Which PID mode is active at a given sample. */
    enum class PidMode {
        /** Full PID with strong gains — used during transient (large lde) */
        DYNAMIC,
        /** PI-only with weaker gains — used after convergence (small lde) */
        QUASI_STATIONARY
    }

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
        val actualWgdc: Double,         // Actual WGDC from log for comparison
        val mode: PidMode = PidMode.DYNAMIC  // Which mode was active
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
        val recommendations: List<String>,
        val q2Warnings: List<String> = emptyList()  // Finding 10 sanity checks
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
     * Now implements dual-mode operation per Finding 1:
     * - Starts in DYNAMIC mode (large lde from boost ramp-up)
     * - Switches to QUASI_STATIONARY when lde crosses zero (pvdks > plsol)
     * - Switches back to DYNAMIC if |lde| exceeds threshold again
     *
     * @param pullEntries Time-ordered WOT entries from a single pull
     * @param kfldrq0 P-gain map (RPM × pressure → gain)
     * @param kfldrq1 I-gain map (RPM × pressure → gain)
     * @param kfldrq2 D-gain map (RPM × pressure → gain)
     * @param kfldrl Feedforward linearization map
     * @param kfldimx I-term limiter map
     * @param ldrq0dy P-gain constant (LDRQ0DY) for dynamic mode
     * @param ldrq0s P-gain constant (LDRQ0S) for quasi-stationary mode
     * @param ldrq1st I-gain constant (LDRQ1ST) for quasi-stationary mode
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
        ldrq0s: Double = DEFAULT_LDRQ0S,
        ldrq1st: Double = DEFAULT_LDRQ1ST,
        sampleIntervalMs: Double = DEFAULT_SAMPLE_INTERVAL_MS
    ): PidSimulationResult {
        if (pullEntries.isEmpty()) {
            return PidSimulationResult(emptyList(), emptyDiagnosis())
        }

        val states = mutableListOf<PidState>()
        var previousLde = 0.0
        var lditv = 0.0  // I-term accumulator
        var currentMode = PidMode.DYNAMIC  // Start in dynamic mode (large initial error)

        // Mode switch threshold: ~5% of max expected setpoint (Finding 1, line 144378)
        val maxPssol = pullEntries.maxOfOrNull { it.requestedMap } ?: 2000.0
        val dynamicThreshold = (maxPssol - (pullEntries.firstOrNull()?.barometricPressure ?: 1013.0)) * 0.05

        for ((i, entry) in pullEntries.withIndex()) {
            val timeMs = i * sampleIntervalMs
            val rpm = entry.rpm

            // Compute plsol = pssol / vpsspls_w (≈1.016)
            val plsol = entry.requestedMap / 1.016

            // Control deviation
            val lde = plsol - entry.actualMap

            // Mode switching logic (Finding 1, line 144378):
            // "oberhalb einer positiven Regelabweichungsschwelle wird der dynamische
            //  Regeleingriff aktiviert und erst beim Vorzeichenwechsel der
            //  Regelabweichung zurückgenommen"
            if (currentMode == PidMode.DYNAMIC) {
                // Switch to quasi-stationary when pvdks overshoots plsol (lde crosses zero)
                if (i > 0 && previousLde > 0 && lde <= 0) {
                    currentMode = PidMode.QUASI_STATIONARY
                }
            } else {
                // Switch back to dynamic if error exceeds threshold
                if (lde > dynamicThreshold) {
                    currentMode = PidMode.DYNAMIC
                }
            }

            // Look up gains at this operating point (x-axis is relative boost in mbar)
            val relativeMbar = entry.actualMap - entry.barometricPressure
            val q0 = kfldrq0?.lookup(relativeMbar.coerceAtLeast(0.0), rpm) ?: 0.5
            val q1 = kfldrq1?.lookup(relativeMbar.coerceAtLeast(0.0), rpm) ?: 0.1
            val q2 = kfldrq2?.lookup(relativeMbar.coerceAtLeast(0.0), rpm) ?: 0.0

            val ldptv: Double
            val ldrdtv: Double

            when (currentMode) {
                PidMode.DYNAMIC -> {
                    // P-term: (LDRQ0DY − Q2) × lde
                    val effectivePGain = ldrq0dy - q2
                    ldptv = effectivePGain * lde / 100.0

                    // D-term: (lde − lde_prev) × Q2
                    ldrdtv = if (i > 0) {
                        q2 * (lde - previousLde) / 100.0
                    } else 0.0
                }
                PidMode.QUASI_STATIONARY -> {
                    // P-term: LDRQ0S × lde (weaker, no D-term subtraction)
                    ldptv = ldrq0s * lde / 100.0

                    // D-term: 0 in quasi-stationary mode (line 144375:
                    // "wird der D-Anteil über den zugehörigen Parameter abgeschaltet
                    //  um unnötiges Stellsignalrauschen zu vermeiden")
                    ldrdtv = 0.0
                }
            }

            // I-term: accumulate (uses dynamic gains KFLDRQ1 in dynamic,
            // LDRQ1ST in quasi-stationary)
            if (i > 0) {
                val effectiveIGain = when (currentMode) {
                    PidMode.DYNAMIC -> q1
                    PidMode.QUASI_STATIONARY -> ldrq1st
                }
                lditv += effectiveIGain * previousLde / 100.0
            }

            // Clamp I-term by KFLDIMX (x-axis is relative boost in mbar)
            val imaxLimit = if (kfldimx != null && relativeMbar > 0) {
                kfldimx.lookup(relativeMbar, rpm)
            } else MAX_WGDC

            lditv = lditv.coerceIn(0.0, imaxLimit)

            // Total PID output
            val ldtv = (ldptv + lditv + ldrdtv).coerceIn(MIN_WGDC, MAX_WGDC)

            // Linearize through KFLDRL: x-axis is duty cycle (%), not boost pressure
            val ldtvLinearized = if (kfldrl != null) {
                kfldrl.lookup(ldtv, rpm)
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
                actualWgdc = entry.wgdc,
                mode = currentMode
            ))

            previousLde = lde
        }

        // Finding 10: KFLDRQ2 sanity checks
        val q2Warnings = checkKfldrq2Sanity(kfldrq2, ldrq0dy)

        val diagnosis = analyzePidBehavior(states, sampleIntervalMs, q2Warnings)
        return PidSimulationResult(states, diagnosis)
    }

    // ── Diagnostic analysis ─────────────────────────────────────────

    /**
     * Finding 10: Check KFLDRQ2 map for common calibration issues.
     *
     * ME7 Reference (line 144453): "KFLDRQ2: bei n < 2500/min = 0;
     * bei n > 2500 schrittweise bis auf max. 0.6 × LDRQ0DY steigern"
     */
    private fun checkKfldrq2Sanity(kfldrq2: Map3d?, ldrq0dy: Double): List<String> {
        if (kfldrq2 == null) return emptyList()
        val warnings = mutableListOf<String>()

        // Check if Q2 is non-zero below 2500 RPM
        for (rpmIdx in kfldrq2.yAxis.indices) {
            val rpm = kfldrq2.yAxis[rpmIdx]
            if (rpm < 2500) {
                for (pIdx in kfldrq2.xAxis.indices) {
                    if (kfldrq2.zAxis[rpmIdx][pIdx] > 0.001) { warnings.add("WARNING: KFLDRQ2: D-gain ${String.format("%.3f", kfldrq2.zAxis[rpmIdx][pIdx])} at " +
                            "${rpm.toInt()} RPM — ME7 recommends 0 below 2500 RPM (line 144453)")
                        break  // One warning per RPM row
                    }
                }
            }
        }

        // Check if Q2 exceeds 0.9 × LDRQ0DY at any point
        val maxQ2 = kfldrq2.zAxis.flatMap { it.toList() }.maxOrNull() ?: 0.0
        if (maxQ2 > 0.9 * ldrq0dy) {
            warnings.add("WARNING: KFLDRQ2: max D-gain ${String.format("%.3f", maxQ2)} exceeds 90% of " +
                "LDRQ0DY (${String.format("%.3f", ldrq0dy)}). ME7 recommends max 0.6 x LDRQ0DY = " +
                "${String.format("%.3f", 0.6 * ldrq0dy)} (line 144453)")
        }

        return warnings
    }

    private fun analyzePidBehavior(
        states: List<PidState>,
        sampleIntervalMs: Double,
        q2Warnings: List<String> = emptyList()
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

        // Add Q2 sanity check warnings (Finding 10)
        recommendations.addAll(q2Warnings)

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
            recommendations = recommendations,
            q2Warnings = q2Warnings
        )
    }

    private fun emptyDiagnosis() = PidDiagnosis(
        oscillationDetected = false, oscillationCount = 0,
        windupDetected = false, windupDurationMs = 0.0,
        slowConvergence = false, convergenceTimeMs = 0.0,
        overshootDetected = false, overshootMagnitude = 0.0,
        avgAbsLde = 0.0, recommendations = emptyList(),
        q2Warnings = emptyList()
    )
}

