package domain.model.optimizer

import data.contract.Me7LogFileContract
import domain.math.Index
import domain.math.map.Map3d
import kotlin.math.abs

/**
 * Suggestion engine that analyzes WOT logs and recommends corrections to
 * KFLDRL (wastegate base duty cycle), KFLDIMX (PID I-limiter), and
 * KFPBRK/KFPBRKNW (volumetric efficiency) so that actual pressure tracks
 * pssol and actual load tracks LDRXN.
 */
object OptimizerCalculator {

    /** Conversion factor: 1 mbar = 0.0145038 PSI */
    private const val MBAR_TO_PSI = 0.0145038

    /** Single row of relevant WOT data extracted from a log. */
    data class WotLogEntry(
        val rpm: Double,
        val requestedLoad: Double,      // rlsol_w
        val actualLoad: Double,         // rl_w (or rl)
        val requestedMap: Double,       // pssol_w (mbar absolute)
        val actualMap: Double,          // pvdks_w (mbar absolute)
        val barometricPressure: Double, // pus_w (mbar)
        val wgdc: Double,               // ldtvm (%)
        val throttleAngle: Double       // wdkba
    ) {
        /** Actual relative boost pressure in PSI (above atmospheric). */
        val relativeBoostPsi: Double get() = (actualMap - barometricPressure) * MBAR_TO_PSI
    }

    /** Complete result bundle from the optimizer. */
    data class OptimizerResult(
        val suggestedKfldrl: Map3d?,
        val suggestedKfldimx: Map3d?,
        val kfpbrkMultipliers: Map3d?,
        val pressureErrors: List<Pair<Double, Double>>,
        val loadErrors: List<Pair<Double, Double>>,
        val warnings: List<String>,
        val wotEntries: List<WotLogEntry>
    )

    // ── Helpers ────────────────────────────────────────────────────────

    fun filterWotEntries(
        values: Map<Me7LogFileContract.Header, List<Double>>,
        minThrottleAngle: Double = 80.0
    ): List<WotLogEntry> {
        val rpms = values[Me7LogFileContract.Header.RPM_COLUMN_HEADER] ?: return emptyList()
        val requestedLoads = values[Me7LogFileContract.Header.REQUESTED_LOAD_HEADER] ?: return emptyList()
        val actualLoads = values[Me7LogFileContract.Header.ENGINE_LOAD_HEADER] ?: return emptyList()
        val requestedMaps = values[Me7LogFileContract.Header.REQUESTED_PRESSURE_HEADER] ?: return emptyList()
        val actualMaps = values[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER] ?: return emptyList()
        val wgdcs = values[Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER] ?: return emptyList()
        val throttleAngles = values[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER] ?: return emptyList()
        val baroPressures = values[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER] ?: return emptyList()

        val altLoads = values[Me7LogFileContract.Header.ACTUAL_LOAD_HEADER]
        val useAltLoads = altLoads != null && altLoads.size == rpms.size

        val entries = mutableListOf<WotLogEntry>()
        for (i in rpms.indices) {
            if (throttleAngles[i] >= minThrottleAngle) {
                entries.add(
                    WotLogEntry(
                        rpm = rpms[i],
                        requestedLoad = requestedLoads[i],
                        actualLoad = if (useAltLoads) altLoads!![i] else actualLoads[i],
                        requestedMap = requestedMaps[i],
                        actualMap = actualMaps[i],
                        barometricPressure = baroPressures[i],
                        wgdc = wgdcs[i],
                        throttleAngle = throttleAngles[i]
                    )
                )
            }
        }
        return entries
    }

    // ── Phase 1 : Boost Control ───────────────────────────────────────

    /**
     * KFLDRL structure:
     *   y-axis = RPM breakpoints
     *   x-axis = linearized relative boost pressure breakpoints (PSI)
     *   z-axis = wastegate duty cycle (%)
     *
     * For each (RPM, pressure) cell, bin WOT entries by their RPM and
     * actual relative boost pressure (PSI), then average the observed WGDC.
     * Cells without data retain their original values.
     */
    fun suggestKfldrl(
        wotEntries: List<WotLogEntry>,
        kfldrlMap: Map3d,
        toleranceMbar: Double = 30.0,
        rpmTolerance: Double = 200.0
    ): Map3d {
        val rpmAxis = kfldrlMap.yAxis
        val pressureAxis = kfldrlMap.xAxis
        val suggested = Array(rpmAxis.size) { Array(pressureAxis.size) { 0.0 } }

        val wgdcSum = Array(rpmAxis.size) { DoubleArray(pressureAxis.size) }
        val count = Array(rpmAxis.size) { IntArray(pressureAxis.size) }

        // Only use entries where boost is positive (on-boost)
        val boostEntries = wotEntries.filter { it.relativeBoostPsi > 0 }

        for (entry in boostEntries) {
            val rpmIdx = Index.getInsertIndex(rpmAxis.toList(), entry.rpm)
            val pressureIdx = Index.getInsertIndex(pressureAxis.toList(), entry.relativeBoostPsi)

            wgdcSum[rpmIdx][pressureIdx] += entry.wgdc
            count[rpmIdx][pressureIdx]++
        }

        for (rpmIdx in rpmAxis.indices) {
            for (pIdx in pressureAxis.indices) {
                if (count[rpmIdx][pIdx] > 0) {
                    suggested[rpmIdx][pIdx] = wgdcSum[rpmIdx][pIdx] / count[rpmIdx][pIdx]
                } else {
                    // No data for this cell — keep original value
                    suggested[rpmIdx][pIdx] = kfldrlMap.zAxis[rpmIdx][pIdx]
                }
            }

            // Enforce monotonicity: higher pressure should need >= duty cycle
            for (pIdx in 1 until pressureAxis.size) {
                if (suggested[rpmIdx][pIdx] < suggested[rpmIdx][pIdx - 1] && count[rpmIdx][pIdx] == 0) {
                    suggested[rpmIdx][pIdx] = suggested[rpmIdx][pIdx - 1]
                }
            }
        }

        return Map3d(pressureAxis, rpmAxis, suggested)
    }

    /**
     * Derive KFLDIMX from the suggested KFLDRL by multiplying each cell
     * by (1 + overheadPct/100).
     *
     * Handles both same-shape and different-shape KFLDIMX/KFLDRL maps.
     */
    fun suggestKfldimx(
        suggestedKfldrl: Map3d,
        kfldimxMap: Map3d,
        overheadPercent: Double = 8.0
    ): Map3d {
        val multiplier = 1.0 + overheadPercent / 100.0

        // If same dimensions, scale directly
        if (kfldimxMap.yAxis.size == suggestedKfldrl.yAxis.size &&
            kfldimxMap.xAxis.size == suggestedKfldrl.xAxis.size) {
            val suggested = Array(kfldimxMap.yAxis.size) { rpmIdx ->
                Array(kfldimxMap.xAxis.size) { pIdx ->
                    val v = suggestedKfldrl.zAxis[rpmIdx][pIdx]
                    if (v > 0) v * multiplier else kfldimxMap.zAxis[rpmIdx][pIdx]
                }
            }
            return Map3d(kfldimxMap.xAxis, kfldimxMap.yAxis, suggested)
        }

        // Different shape: interpolate from KFLDRL for each KFLDIMX cell
        val suggested = Array(kfldimxMap.yAxis.size) { rpmIdx ->
            val kfldrlRpmIdx = Index.getInsertIndex(suggestedKfldrl.yAxis.toList(), kfldimxMap.yAxis[rpmIdx])
            Array(kfldimxMap.xAxis.size) { pIdx ->
                // KFLDIMX x-axis may be in mbar; convert to PSI for KFLDRL lookup
                val pressurePsi = kfldimxMap.xAxis[pIdx] * MBAR_TO_PSI
                val kfldrlPIdx = Index.getInsertIndex(suggestedKfldrl.xAxis.toList(), pressurePsi)
                val v = suggestedKfldrl.zAxis[kfldrlRpmIdx][kfldrlPIdx]
                if (v > 0) v * multiplier else kfldimxMap.zAxis[rpmIdx][pIdx]
            }
        }
        return Map3d(kfldimxMap.xAxis, kfldimxMap.yAxis, suggested)
    }

    // ── Phase 2 : VE Model ────────────────────────────────────────────

    fun suggestKfpbrk(
        wotEntries: List<WotLogEntry>,
        kfpbrkMap: Map3d,
        toleranceMbar: Double = 30.0,
        rpmTolerance: Double = 200.0
    ): Map3d? {
        val rpmAxis = kfpbrkMap.yAxis
        val xAxis = kfpbrkMap.xAxis
        val suggested = Array(rpmAxis.size) { Array(xAxis.size) { 0.0 } }
        var hasData = false

        for (rpmIdx in rpmAxis.indices) {
            val rpmTarget = rpmAxis[rpmIdx]

            val valid = wotEntries.filter { e ->
                abs(e.rpm - rpmTarget) < rpmTolerance &&
                    abs(e.actualMap - e.requestedMap) <= toleranceMbar &&
                    e.actualLoad > 0
            }

            if (valid.isEmpty()) {
                for (xIdx in xAxis.indices) {
                    suggested[rpmIdx][xIdx] = kfpbrkMap.zAxis[rpmIdx][xIdx]
                }
                continue
            }

            hasData = true
            val avgRatio = valid.map { e -> e.requestedLoad / e.actualLoad }.average()
            val safeRatio = if (avgRatio.isFinite()) avgRatio else 1.0

            for (xIdx in xAxis.indices) {
                suggested[rpmIdx][xIdx] = kfpbrkMap.zAxis[rpmIdx][xIdx] * safeRatio
            }
        }

        return if (hasData) Map3d(xAxis, rpmAxis, suggested) else null
    }

    // ── Phase 3 : Intervention Check ──────────────────────────────────

    fun checkInterventions(
        wotEntries: List<WotLogEntry>,
        ldrxnTarget: Double
    ): List<String> {
        val warnings = mutableListOf<String>()
        if (wotEntries.isEmpty()) return warnings

        val capped = wotEntries.filter { e -> e.requestedLoad < ldrxnTarget * 0.95 }
        if (capped.isNotEmpty()) {
            val pct = capped.size.toDouble() / wotEntries.size * 100
            val avgRlsol = capped.map { it.requestedLoad }.average()
            warnings.add(
                "Torque Intervention Detected: rlsol is below LDRXN target (${ldrxnTarget.format()}) " +
                    "in ${String.format("%.1f", pct)}% of WOT samples (avg rlsol = ${avgRlsol.format()}). " +
                    "Check KFMIOP / KFMIZUFIL to ensure torque requests support this load."
            )
        }

        val boostMissing = wotEntries.filter { e -> e.actualMap < e.requestedMap - 50 }
        if (boostMissing.size > wotEntries.size * 0.5) {
            val avgError = boostMissing.map { it.requestedMap - it.actualMap }.average()
            warnings.add(
                "Boost Target Not Reached: Actual pressure is below requested pressure by an average of " +
                    "${avgError.format()} mbar in ${String.format("%.1f", boostMissing.size.toDouble() / wotEntries.size * 100)}% " +
                    "of WOT samples. This may indicate a mechanical limitation (turbo spool, wastegate, boost leak) " +
                    "or incorrect KFLDRL base duty cycle."
            )
        }

        return warnings
    }

    // ── Chart data ────────────────────────────────────────────────────

    fun computePressureErrors(wotEntries: List<WotLogEntry>): List<Pair<Double, Double>> {
        return wotEntries.map { e -> Pair(e.rpm, e.requestedMap - e.actualMap) }
    }

    fun computeLoadErrors(wotEntries: List<WotLogEntry>): List<Pair<Double, Double>> {
        return wotEntries.filter { it.actualLoad > 0 }
            .map { e -> Pair(e.rpm, e.requestedLoad / e.actualLoad) }
    }

    // ── Top-level entry point ─────────────────────────────────────────

    fun analyze(
        values: Map<Me7LogFileContract.Header, List<Double>>,
        kfldrlMap: Map3d?,
        kfldimxMap: Map3d?,
        kfpbrkMap: Map3d?,
        ldrxnTarget: Double = 191.0,
        toleranceMbar: Double = 30.0,
        minThrottleAngle: Double = 80.0,
        kfldimxOverheadPercent: Double = 8.0
    ): OptimizerResult {
        val wotEntries = filterWotEntries(values, minThrottleAngle)

        val suggestedKfldrl = if (kfldrlMap != null) {
            suggestKfldrl(wotEntries, kfldrlMap, toleranceMbar)
        } else null

        val suggestedKfldimx = if (suggestedKfldrl != null && kfldimxMap != null) {
            suggestKfldimx(suggestedKfldrl, kfldimxMap, kfldimxOverheadPercent)
        } else null

        val kfpbrkMultipliers = if (kfpbrkMap != null) {
            suggestKfpbrk(wotEntries, kfpbrkMap, toleranceMbar)
        } else null

        val pressureErrors = computePressureErrors(wotEntries)
        val loadErrors = computeLoadErrors(wotEntries)
        val warnings = checkInterventions(wotEntries, ldrxnTarget)

        return OptimizerResult(
            suggestedKfldrl = suggestedKfldrl,
            suggestedKfldimx = suggestedKfldimx,
            kfpbrkMultipliers = kfpbrkMultipliers,
            pressureErrors = pressureErrors,
            loadErrors = loadErrors,
            warnings = warnings,
            wotEntries = wotEntries
        )
    }

    private fun Double.format(): String = String.format("%.2f", this)
}
