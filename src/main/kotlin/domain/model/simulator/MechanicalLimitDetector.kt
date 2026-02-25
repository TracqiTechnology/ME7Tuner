package domain.model.simulator

import domain.model.optimizer.OptimizerCalculator
import kotlin.math.abs

/**
 * Detects mechanical/hardware limits from WOT log data that cannot be
 * corrected via calibration changes alone.
 *
 * @see documentation/me7-boost-control.md §8
 * @see documentation/me7-maps-reference.md §Pressure Sensor Limits
 */
object MechanicalLimitDetector {

    /** Summary of detected mechanical limits. */
    data class MechanicalLimits(
        val mafMaxed: Boolean = false,
        val mafMaxValue: Double = 0.0,
        val injectorMaxed: Boolean = false,
        val injectorMaxDutyCycle: Double = 0.0,
        val turboMaxed: Boolean = false,
        val turboMaxWgdc: Double = 0.0,
        val mapSensorMaxed: Boolean = false,
        val mapSensorMaxValue: Double = 0.0,
        val warnings: List<String> = emptyList()
    )

    // ── Thresholds ───────────────────────────────────────────────────

    private const val TURBO_WGDC_THRESHOLD = 90.0           // %
    private const val TURBO_PRESSURE_DEFICIT_MBAR = 100.0    // mbar
    private const val MAP_SENSOR_PLATEAU_FRACTION = 0.30     // 30% of samples
    private const val MAP_SENSOR_PLATEAU_BAND_MBAR = 10.0    // mbar
    private const val MAP_SENSOR_MIN_MAX_MBAR = 2500.0       // mbar
    private const val INJECTOR_DC_THRESHOLD = 0.85           // 85%
    private const val MAF_PLATEAU_FRACTION = 0.20            // 20% of samples
    private const val MAF_PLATEAU_BAND_PERCENT = 0.02        // 2%
    private const val MAF_MIN_MAX_GS = 200.0                 // g/s

    // ── Detection ────────────────────────────────────────────────────

    /**
     * Analyze WOT log data for mechanical limits.
     *
     * @param wotEntries  WOT-filtered log entries (always available)
     * @param mafValues   Optional: mshfm_w values corresponding to WOT entries
     * @param injectorOnTimes Optional: ti_b1 values corresponding to WOT entries
     * @param rpms        Optional: RPM values for injector DC calculation
     *                    (uses wotEntries RPM if null)
     */
    fun detect(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        mafValues: List<Double>? = null,
        injectorOnTimes: List<Double>? = null,
        rpms: List<Double>? = null
    ): MechanicalLimits {
        if (wotEntries.isEmpty()) return MechanicalLimits()

        val warnings = mutableListOf<String>()

        // ── Turbo maxed ──────────────────────────────────────────
        val turboResult = detectTurboMaxed(wotEntries)
        if (turboResult.first) {
            warnings.add(turboResult.second)
        }

        // ── MAP sensor maxed ─────────────────────────────────────
        val mapResult = detectMapSensorMaxed(wotEntries)
        if (mapResult.first) {
            warnings.add(mapResult.second)
        }

        // ── Injector maxed ───────────────────────────────────────
        val injectorResult = detectInjectorMaxed(
            wotEntries, injectorOnTimes, rpms
        )

        if (injectorResult.first) {
            warnings.add(injectorResult.second)
        }

        // ── MAF maxed ────────────────────────────────────────────
        val mafResult = detectMafMaxed(mafValues)
        if (mafResult.first) {
            warnings.add(mafResult.second)
        }

        return MechanicalLimits(
            mafMaxed = mafResult.first,
            mafMaxValue = mafResult.third,
            injectorMaxed = injectorResult.first,
            injectorMaxDutyCycle = injectorResult.third,
            turboMaxed = turboResult.first,
            turboMaxWgdc = wotEntries.maxOf { it.wgdc },
            mapSensorMaxed = mapResult.first,
            mapSensorMaxValue = mapResult.third,
            warnings = warnings
        )
    }

    // ── Individual detectors ─────────────────────────────────────────

    /**
     * Turbo maxed: WGDC > 90% but pressure still significantly below target.
     * Returns (detected, warning message, max wgdc).
     */
    private fun detectTurboMaxed(
        wotEntries: List<OptimizerCalculator.WotLogEntry>
    ): Triple<Boolean, String, Double> {
        val highWgdc = wotEntries.filter { it.wgdc > TURBO_WGDC_THRESHOLD }
        val maxWgdc = wotEntries.maxOf { it.wgdc }

        if (highWgdc.isEmpty()) return Triple(false, "", maxWgdc)

        val underTarget = highWgdc.filter { e ->
            e.actualMap < e.requestedMap - TURBO_PRESSURE_DEFICIT_MBAR
        }

        val detected = underTarget.isNotEmpty()
        val avgDeficit = if (underTarget.isNotEmpty()) {
            underTarget.map { it.requestedMap - it.actualMap }.average()
        } else 0.0

        val warning = "Turbo appears maxed: WGDC > ${TURBO_WGDC_THRESHOLD.toInt()}% " +
            "in ${highWgdc.size} samples but pressure is below target by " +
            "avg ${String.format("%.0f", avgDeficit)} mbar in ${underTarget.size} of those. " +
            "Consider larger turbo or check for boost leaks."

        return Triple(detected, warning, maxWgdc)
    }

    /**
     * MAP sensor maxed: pvdks_w plateaus near sensor max.
     * Returns (detected, warning message, max pressure).
     */
    private fun detectMapSensorMaxed(
        wotEntries: List<OptimizerCalculator.WotLogEntry>
    ): Triple<Boolean, String, Double> {
        val maxPressure = wotEntries.maxOf { it.actualMap }

        if (maxPressure < MAP_SENSOR_MIN_MAX_MBAR) {
            return Triple(false, "", maxPressure)
        }

        val nearMax = wotEntries.count {
            abs(it.actualMap - maxPressure) < MAP_SENSOR_PLATEAU_BAND_MBAR
        }
        val fraction = nearMax.toDouble() / wotEntries.size

        val detected = fraction > MAP_SENSOR_PLATEAU_FRACTION
        val warning = "MAP sensor appears maxed at " +
            "${String.format("%.0f", maxPressure)} mbar " +
            "(${String.format("%.0f", fraction * 100)}% of samples at ceiling). " +
            "Consider a higher-range MAP sensor."

        return Triple(detected, warning, maxPressure)
    }

    /**
     * Injector maxed: duty cycle exceeds threshold.
     * Injector DC = ti_b1 / engine_cycle_ms
     * Returns (detected, warning message, max duty cycle).
     */
    private fun detectInjectorMaxed(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        injectorOnTimes: List<Double>?,
        rpms: List<Double>?
    ): Triple<Boolean, String, Double> {
        if (injectorOnTimes == null) return Triple(false, "", 0.0)

        val rpmValues = rpms ?: wotEntries.map { it.rpm }
        if (injectorOnTimes.size != rpmValues.size) return Triple(false, "", 0.0)

        val dutyCycles = rpmValues.indices.map { i ->
            if (rpmValues[i] > 0) {
                val engineCycleMs = (1.0 / ((rpmValues[i] / 2.0) / 60.0)) * 1000.0
                injectorOnTimes[i] / engineCycleMs
            } else 0.0
        }

        val maxDc = dutyCycles.maxOrNull() ?: 0.0
        val detected = maxDc > INJECTOR_DC_THRESHOLD
        val warning = "Fuel injectors appear near capacity " +
            "(max duty cycle: ${String.format("%.1f", maxDc * 100)}%). " +
            "Consider larger injectors for more power."

        return Triple(detected, warning, maxDc)
    }

    /**
     * MAF sensor maxed: mshfm_w plateaus at sensor max.
     * Returns (detected, warning message, max MAF value).
     */
    private fun detectMafMaxed(
        mafValues: List<Double>?
    ): Triple<Boolean, String, Double> {
        if (mafValues == null || mafValues.isEmpty()) return Triple(false, "", 0.0)

        val maxMaf = mafValues.max()
        if (maxMaf < MAF_MIN_MAX_GS) return Triple(false, "", maxMaf)

        val band = maxMaf * MAF_PLATEAU_BAND_PERCENT
        val nearMax = mafValues.count { abs(it - maxMaf) < band }
        val fraction = nearMax.toDouble() / mafValues.size

        val detected = fraction > MAF_PLATEAU_FRACTION
        val warning = "MAF sensor appears maxed at " +
            "${String.format("%.1f", maxMaf)} g/s " +
            "(${String.format("%.0f", fraction * 100)}% of samples at ceiling). " +
            "MLHFM rescaling or a larger MAF housing may be needed."

        return Triple(detected, warning, maxMaf)
    }
}

