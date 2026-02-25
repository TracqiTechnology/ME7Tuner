package domain.model.simulator

import domain.math.map.Map3d
import domain.model.optimizer.OptimizerCalculator
import domain.model.plsol.Plsol
import domain.model.rlsol.Rlsol
import kotlin.math.abs

/**
 * ME7 ECU simulator that models the full load/pressure control chain.
 *
 * **Core Goal:** For a given LDRXN target, diagnose which link in the
 * 4-link signal chain is causing actual_load ≠ LDRXN:
 *
 * ```
 * Link 1: LDRXN → rlsol    (torque structure: KFMIOP/KFMIRL/KFMIZUFIL)
 * Link 2: rlsol → pssol    (VE model forward: KFURL/KFPBRK via PLSOL)
 * Link 3: pssol → pvdks    (boost control: KFLDRL/KFLDIMX/PID)
 * Link 4: pvdks → rl_w     (VE model reverse: KFPBRK/KFURL)
 * ```
 *
 * If all 4 links are calibrated correctly, the user only needs to set LDRXN
 * and the ECU will automatically achieve that load with the correct pressure.
 *
 * @see documentation/me7-boost-control.md
 * @see documentation/TECHNICAL_BREAKDOWN.md
 */
object Me7Simulator {

    /** Conversion factor: 1 mbar = 0.0145038 PSI */
    private const val MBAR_TO_PSI = 0.0145038

    /** Pressure ratio vpsspls_w from Plsol.kt (me7-raw.txt line 142841) */
    private const val VPSSPLS = 1.016

    // ── Chain diagnosis thresholds ──────────────────────────────────

    /** Link 1: rlsol must be within this fraction of LDRXN to be "on target" */
    private const val TORQUE_CAP_THRESHOLD = 0.95

    /** Link 2: |simulatedPssol − actualPssol| above this → VE model error (mbar) */
    private const val PSSOL_ERROR_THRESHOLD_MBAR = 30.0

    /** Link 3: actualPssol − actualPvdks above this → boost shortfall (mbar) */
    private const val BOOST_ERROR_THRESHOLD_MBAR = 50.0

    /** Link 4: |kfpbrkCorrection − 1.0| above this → VE mismatch */
    private const val VE_MISMATCH_THRESHOLD = 0.03

    // ── Data classes ────────────────────────────────────────────────

    /** Operating conditions for a single sample point. */
    data class OperatingPoint(
        val rpm: Double,
        val barometricPressure: Double = 1013.0,   // pus_w (mbar)
        val intakeAirTemp: Double = 20.0,           // tans (°C)
        val coolantTemp: Double = 96.0              // tmot (°C)
    )

    /** All calibration maps needed for simulation. */
    data class CalibrationSet(
        val kfpbrk: Map3d? = null,         // VE correction map
        val kfldrl: Map3d? = null,         // Feedforward WGDC map
        val kfldimx: Map3d? = null,        // PID I-term limiter
        val kfmiop: Map3d? = null,         // Load → torque
        val kfmirl: Map3d? = null,         // Torque → load (inverse)
        val kfurl: Double = 0.106,         // Basic VE constant
        val ldrxn: Double = 191.0          // Max specified load
    )

    /** Which link in the chain is the primary source of error. */
    enum class ErrorSource {
        /** Link 1: rlsol doesn't reach LDRXN (torque limiters active) */
        TORQUE_CAPPED,
        /** Link 2: PLSOL model requests wrong pressure for the load */
        PSSOL_WRONG,
        /** Link 3: Wastegate/turbo can't deliver the requested pressure */
        BOOST_SHORTFALL,
        /** Link 4: VE model (KFPBRK) reads pressure as wrong load */
        VE_MISMATCH,
        /** All links OK — actual load matches LDRXN */
        ON_TARGET
    }

    /** Result of simulating a single WOT entry through the full 4-link chain. */
    data class SimulationResult(
        val rpm: Double,

        // ── Link 1: LDRXN → rlsol ─────────────
        val ldrxnTarget: Double,             // What load we WANT
        val rlsol: Double,                   // What the ECU actually requested (from log)
        val torqueLimited: Boolean,          // rlsol < ldrxnTarget * threshold
        val torqueHeadroom: Double,          // ldrxnTarget - rlsol (how much is lost)

        // ── Link 2: rlsol → pssol ─────────────
        val simulatedPssol: Double,          // What pressure SHOULD be for this rlsol
        val actualPssol: Double,             // What pressure the ECU requested (pssol_w from log)
        val pssolError: Double,              // simulatedPssol - actualPssol

        // ── Link 3: pssol → pvdks ─────────────
        val simulatedPlsol: Double,          // Boost controller setpoint: pssol / vpsspls_w
        val actualPvdks: Double,             // Actual pressure achieved (pvdks_w from log)
        val boostError: Double,              // actualPssol - actualPvdks
        val predictedWgdc: Double,           // What KFLDRL says WGDC should be
        val actualWgdc: Double,              // What WGDC actually was (ldtvm from log)
        val kfldrlCorrection: Double,        // actualWgdc - predictedWgdc

        // ── Link 4: pvdks → rl_w ──────────────
        val simulatedRlFromPressure: Double, // What VE model predicts for this pressure
        val actualRl: Double,                // What the ECU measured (rl_w from log)
        val kfpbrkCorrectionFactor: Double,  // Multiplier to fix VE model

        // ── Chain summary ─────────────────────
        val dominantError: ErrorSource,      // Which link is the primary problem
        val totalLoadDeficit: Double         // ldrxnTarget - actualRl (end-to-end)
    ) {
        // Backward-compatible aliases used by existing OptimizerCalculator code
        val pressureError: Double get() = simulatedPssol - actualPvdks
        val loadError: Double get() = rlsol - simulatedRlFromPressure
    }

    // ── Forward path: rlsol → pssol ──────────────────────────────────

    /**
     * Compute pssol from rlsol using the ME7 PLSOL algorithm.
     * Delegates to [Plsol.plsol] which implements the exact ME7 math.
     *
     * @see documentation/me7-boost-control.md §3
     */
    fun computePssol(
        rlsol: Double,
        op: OperatingPoint,
        kfurl: Double = 0.106,
        previousPressure: Double? = null
    ): Double {
        val ps = previousPressure ?: op.barometricPressure
        return Plsol.plsol(
            op.barometricPressure,
            ps,
            op.intakeAirTemp,
            op.coolantTemp,
            kfurl,
            rlsol
        )
    }

    // ── Forward path: pssol → plsol (LDRPLS) ────────────────────────

    /**
     * Compute the boost controller setpoint (plsol) from pssol.
     *
     * From me7-raw.txt line 142841–142843:
     * > "plsol = pssol / vpsspls_w"
     *
     * This is what the LDR PID controller actually targets — it's the
     * pressure before the throttle, not the manifold pressure.
     *
     * @see documentation/me7-boost-control.md §3b
     */
    fun computePlsol(pssol: Double): Double {
        return pssol / VPSSPLS
    }

    // ── Reverse path: pvdks → rl_w ───────────────────────────────────

    /**
     * Compute rl_w from actual pressure using the ME7 VE model.
     * Delegates to [Rlsol.rlsol] which implements the exact ME7 math.
     *
     * @see documentation/me7-boost-control.md §5
     */
    fun computeRlFromPressure(
        pressure: Double,
        op: OperatingPoint,
        kfurl: Double = 0.106,
        previousPressure: Double? = null
    ): Double {
        val ps = previousPressure ?: op.barometricPressure
        return Rlsol.rlsol(
            op.barometricPressure,
            ps,
            op.intakeAirTemp,
            op.coolantTemp,
            kfurl,
            pressure
        )
    }

    // ── Boost control: predict WGDC ──────────────────────────────────

    /**
     * Look up expected wastegate duty cycle from KFLDRL for a given
     * RPM and relative boost pressure.
     *
     * @see documentation/me7-boost-control.md §4
     */
    fun predictWgdc(
        rpm: Double,
        relativePressurePsi: Double,
        kfldrl: Map3d
    ): Double {
        return kfldrl.lookup(relativePressurePsi, rpm)
    }

    // ── Chain diagnosis ──────────────────────────────────────────────

    /**
     * Determine which link in the signal chain is the dominant source
     * of error for a given simulation result.
     *
     * Priority order (check from Link 1 down — earlier links mask later):
     * 1. If torque structure caps rlsol → nothing downstream can help
     * 2. If PLSOL model is wrong → pssol target is wrong for the load
     * 3. If boost control can't deliver pssol → pressure is wrong
     * 4. If VE model misreads pressure → reported load is wrong
     */
    fun diagnoseChain(
        torqueLimited: Boolean,
        pssolError: Double,
        boostError: Double,
        kfpbrkCorrectionFactor: Double
    ): ErrorSource {
        if (torqueLimited) return ErrorSource.TORQUE_CAPPED
        if (abs(pssolError) > PSSOL_ERROR_THRESHOLD_MBAR) return ErrorSource.PSSOL_WRONG
        if (boostError > BOOST_ERROR_THRESHOLD_MBAR) return ErrorSource.BOOST_SHORTFALL
        if (abs(kfpbrkCorrectionFactor - 1.0) > VE_MISMATCH_THRESHOLD) return ErrorSource.VE_MISMATCH
        return ErrorSource.ON_TARGET
    }

    // ── Full simulation of a WOT entry ───────────────────────────────

    /**
     * Simulate a single WOT log entry through the full 4-link chain.
     *
     * Given the ECU's calibration and the actual conditions from the log,
     * this determines what the ECU *should* have computed at each stage
     * and identifies where the chain breaks down.
     */
    fun simulateEntry(
        entry: OptimizerCalculator.WotLogEntry,
        calibration: CalibrationSet
    ): SimulationResult {
        val op = OperatingPoint(
            rpm = entry.rpm,
            barometricPressure = entry.barometricPressure
        )

        // ── Link 1: LDRXN → rlsol ───────────────────────────────
        // Was the load request capped by the torque structure?
        val torqueLimited = entry.requestedLoad < calibration.ldrxn * TORQUE_CAP_THRESHOLD
        val torqueHeadroom = calibration.ldrxn - entry.requestedLoad

        // ── Link 2: rlsol → pssol ───────────────────────────────
        // What pressure should correspond to this load?
        val simulatedPssol = computePssol(
            rlsol = entry.requestedLoad,
            op = op,
            kfurl = calibration.kfurl,
            previousPressure = entry.barometricPressure
        )
        val pssolError = simulatedPssol - entry.requestedMap

        // ── Link 3: pssol → pvdks ───────────────────────────────
        // Compute what the boost controller sees as its setpoint
        val simulatedPlsol = computePlsol(entry.requestedMap)

        // Did the turbo actually deliver the requested pressure?
        val boostError = entry.requestedMap - entry.actualMap

        // What WGDC would KFLDRL predict for the actual operating point?
        val relativePsi = (entry.actualMap - entry.barometricPressure) * MBAR_TO_PSI
        val predictedWgdc = if (calibration.kfldrl != null && relativePsi > 0) {
            predictWgdc(entry.rpm, relativePsi, calibration.kfldrl)
        } else {
            entry.wgdc
        }
        val kfldrlCorrection = entry.wgdc - predictedWgdc

        // ── Link 4: pvdks → rl_w ───────────────────────────────
        // Given the actual pressure, what load does the VE model predict?
        val simulatedRl = computeRlFromPressure(
            pressure = entry.actualMap,
            op = op,
            kfurl = calibration.kfurl,
            previousPressure = entry.barometricPressure
        )

        // KFPBRK correction: how to scale VE model to match reality
        val kfpbrkCorrection = if (simulatedRl > 0 && entry.actualLoad > 0) {
            entry.actualLoad / simulatedRl
        } else 1.0

        // ── Chain diagnosis ─────────────────────────────────────
        val dominantError = diagnoseChain(
            torqueLimited = torqueLimited,
            pssolError = pssolError,
            boostError = boostError,
            kfpbrkCorrectionFactor = kfpbrkCorrection
        )
        val totalLoadDeficit = calibration.ldrxn - entry.actualLoad

        return SimulationResult(
            rpm = entry.rpm,
            // Link 1
            ldrxnTarget = calibration.ldrxn,
            rlsol = entry.requestedLoad,
            torqueLimited = torqueLimited,
            torqueHeadroom = torqueHeadroom,
            // Link 2
            simulatedPssol = simulatedPssol,
            actualPssol = entry.requestedMap,
            pssolError = pssolError,
            // Link 3
            simulatedPlsol = simulatedPlsol,
            actualPvdks = entry.actualMap,
            boostError = boostError,
            predictedWgdc = predictedWgdc,
            actualWgdc = entry.wgdc,
            kfldrlCorrection = kfldrlCorrection,
            // Link 4
            simulatedRlFromPressure = simulatedRl,
            actualRl = entry.actualLoad,
            kfpbrkCorrectionFactor = kfpbrkCorrection,
            // Chain summary
            dominantError = dominantError,
            totalLoadDeficit = totalLoadDeficit
        )
    }

    // ── Batch simulation ─────────────────────────────────────────────

    /**
     * Simulate all WOT entries and return per-entry results.
     * Each entry is simulated independently through the full 4-link chain.
     */
    fun simulateAll(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        calibration: CalibrationSet
    ): List<SimulationResult> {
        return wotEntries.map { simulateEntry(it, calibration) }
    }
}
