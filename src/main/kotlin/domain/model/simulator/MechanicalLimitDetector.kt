package domain.model.simulator

import domain.model.optimizer.OptimizerCalculator
import kotlin.math.abs

/**
 * Detects mechanical/hardware limits from WOT log data that cannot be
 * corrected via calibration changes alone.
 *
 * All ME7 analog sensors output 0–5 V. When a sensor saturates at ~5 V
 * the ECU can no longer measure the real value — the reading clips at
 * the top of the transfer function. Common casualties on tuned engines:
 *
 * | Sensor | Signal | Stock Max | 5 V = |
 * |--------|--------|-----------|-------|
 * | MAF (HFM5) | uhfm_w | ~4.96 V at ~370 g/s | MLHFM top bin |
 * | MAP (3-bar) | upvdks | ~4.65 V at ~2550 mbar | pvdks plateau |
 * | MAP (4-bar) | upvdks | ~4.65 V at ~3500 mbar | pvdks plateau |
 * | MAP (5-bar) | upvdks | ~4.65 V at ~4500 mbar | pvdks plateau |
 *
 * @see documentation/me7-boost-control.md §8
 * @see documentation/me7-maps-reference.md §Pressure Sensor Limits
 */
object MechanicalLimitDetector {

    // ── Data classes ─────────────────────────────────────────────────

    /**
     * Per-sensor saturation detail.
     */
    data class SensorSaturationWarning(
        val sensorName: String,
        val maxValue: Double,
        val unit: String,
        val plateauPercent: Double,
        val recommendation: String,
        /** RPM ranges where saturation occurs (start to end). */
        val affectedRpmRanges: List<Pair<Double, Double>> = emptyList()
    )

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
        // Sensor voltage saturation
        val mafVoltageMaxed: Boolean = false,
        val mafMaxVoltage: Double = 0.0,
        val mapSensorType: String = "Unknown",
        val sensorSaturationWarnings: List<SensorSaturationWarning> = emptyList(),
        /** When true, high-load solver suggestions (KFURL/KFPBRK) may be unreliable. */
        val dataReliabilityCompromised: Boolean = false,
        val dataReliabilityDetail: String = "",
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

    // Sensor voltage thresholds
    /** Voltage at which any ME7 0–5 V sensor is considered saturated. */
    private const val SENSOR_VOLTAGE_CEILING = 4.8           // V
    /** Fraction of WOT samples at ceiling to flag saturation. */
    private const val VOLTAGE_PLATEAU_FRACTION = 0.10        // 10%
    /** Voltage band for plateau detection. */
    private const val VOLTAGE_PLATEAU_BAND = 0.05            // V

    // Known MAP sensor pressure ceilings (approximate mbar values at 5 V output)
    private val MAP_SENSOR_TYPES = listOf(
        MapSensorSpec("Stock 3-bar", maxMbar = 2550.0, upgradeAdvice = "Upgrade to 4-bar MAP sensor. Update KFVPDKSD/KFVPDKSE transfer function in BIN."),
        MapSensorSpec("4-bar", maxMbar = 3500.0, upgradeAdvice = "Upgrade to 5-bar MAP sensor. Update KFVPDKSD/KFVPDKSE transfer function in BIN."),
        MapSensorSpec("5-bar", maxMbar = 4500.0, upgradeAdvice = "5-bar MAP sensor limit reached. Consider 6-bar or dual-sensor setup."),
        MapSensorSpec("6-bar", maxMbar = 5500.0, upgradeAdvice = "6-bar MAP sensor limit reached.")
    )

    private data class MapSensorSpec(val name: String, val maxMbar: Double, val upgradeAdvice: String)

    // ── Detection ────────────────────────────────────────────────────

    /**
     * Analyze WOT log data for mechanical limits including sensor voltage saturation.
     *
     * @param wotEntries       WOT-filtered log entries (always available)
     * @param mafValues        Optional: mshfm_w values corresponding to WOT entries
     * @param injectorOnTimes  Optional: ti_b1 values corresponding to WOT entries
     * @param rpms             Optional: RPM values for injector DC calculation
     * @param mafVoltages      Optional: uhfm_w values corresponding to WOT entries
     */
    fun detect(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        mafValues: List<Double>? = null,
        injectorOnTimes: List<Double>? = null,
        rpms: List<Double>? = null,
        mafVoltages: List<Double>? = null
    ): MechanicalLimits {
        if (wotEntries.isEmpty()) return MechanicalLimits()

        val warnings = mutableListOf<String>()
        val saturationWarnings = mutableListOf<SensorSaturationWarning>()

        // ── Turbo maxed ──────────────────────────────────────────
        val turboResult = detectTurboMaxed(wotEntries)
        if (turboResult.first) {
            warnings.add(turboResult.second)
        }

        // ── MAP sensor maxed (pressure plateau) ──────────────────
        val mapResult = detectMapSensorMaxed(wotEntries)
        if (mapResult.first) {
            warnings.add(mapResult.second)
        }

        // ── MAP sensor type auto-detect ──────────────────────────
        val mapSensorType = detectMapSensorType(wotEntries)

        // ── MAP sensor saturation (from pressure plateau) ────────
        val mapSaturation = detectMapSensorSaturation(wotEntries, mapSensorType)
        if (mapSaturation != null) {
            saturationWarnings.add(mapSaturation)
            if (!mapResult.first) {
                // If the pressure plateau detector didn't catch it but
                // we detected saturation via sensor type analysis, warn anyway
                warnings.add(mapSaturation.recommendation)
            }
        }

        // ── Injector maxed ───────────────────────────────────────
        val injectorResult = detectInjectorMaxed(wotEntries, injectorOnTimes, rpms)
        if (injectorResult.first) {
            warnings.add(injectorResult.second)
        }

        // ── MAF maxed (airflow plateau) ──────────────────────────
        val mafResult = detectMafMaxed(mafValues)
        if (mafResult.first) {
            warnings.add(mafResult.second)
        }

        // ── MAF voltage saturation ───────────────────────────────
        val mafVoltageResult = detectMafVoltageSaturation(mafVoltages, wotEntries)
        if (mafVoltageResult.first) {
            warnings.add(mafVoltageResult.second)
            saturationWarnings.add(mafVoltageResult.fourth)
        }

        // ── Data reliability assessment ──────────────────────────
        val (reliabilityCompromised, reliabilityDetail) = assessDataReliability(
            mafVoltageMaxed = mafVoltageResult.first,
            mafMaxed = mafResult.first,
            mapSensorMaxed = mapResult.first || (mapSaturation != null),
            wotEntries = wotEntries
        )
        if (reliabilityCompromised) {
            warnings.add(reliabilityDetail)
        }

        return MechanicalLimits(
            mafMaxed = mafResult.first,
            mafMaxValue = mafResult.third,
            injectorMaxed = injectorResult.first,
            injectorMaxDutyCycle = injectorResult.third,
            turboMaxed = turboResult.first,
            turboMaxWgdc = wotEntries.maxOf { it.wgdc },
            mapSensorMaxed = mapResult.first || (mapSaturation != null),
            mapSensorMaxValue = mapResult.third,
            mafVoltageMaxed = mafVoltageResult.first,
            mafMaxVoltage = mafVoltageResult.third,
            mapSensorType = mapSensorType.name,
            sensorSaturationWarnings = saturationWarnings,
            dataReliabilityCompromised = reliabilityCompromised,
            dataReliabilityDetail = reliabilityDetail,
            warnings = warnings
        )
    }

    // ── Individual detectors ─────────────────────────────────────────

    /**
     * Turbo maxed: WGDC > 90% but pressure still significantly below target.
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
     * Auto-detect MAP sensor type from observed maximum pressure.
     *
     * The pressure transfer function for each sensor type is roughly linear
     * from ~0.2 V to ~4.65 V. When pvdks_w plateaus near a known sensor
     * ceiling, we can infer the installed sensor type.
     */
    private fun detectMapSensorType(
        wotEntries: List<OptimizerCalculator.WotLogEntry>
    ): MapSensorSpec {
        val maxPressure = wotEntries.maxOf { it.actualMap }

        // Find the sensor type whose ceiling is closest to (and >= ) the observed max
        // Allow 200 mbar tolerance below the ceiling for plateau detection
        for (spec in MAP_SENSOR_TYPES) {
            if (maxPressure <= spec.maxMbar + 50) {
                return spec
            }
        }

        // If pressure exceeds all known types, it's an unknown high-range sensor
        return MapSensorSpec(
            "Unknown (>${String.format("%.0f", maxPressure)} mbar)",
            maxPressure + 500.0,
            "Sensor type could not be auto-detected."
        )
    }

    /**
     * Detect MAP sensor saturation based on pressure plateau near known sensor ceiling.
     *
     * This is more informative than the generic plateau detector because it:
     * 1. Identifies the specific sensor type installed
     * 2. Recommends the correct upgrade path
     * 3. Warns about KFVPDKSD/KFVPDKSE transfer function changes needed
     */
    private fun detectMapSensorSaturation(
        wotEntries: List<OptimizerCalculator.WotLogEntry>,
        sensorType: MapSensorSpec
    ): SensorSaturationWarning? {
        val maxPressure = wotEntries.maxOf { it.actualMap }
        val ceilingProximityMbar = 100.0 // within 100 mbar of sensor ceiling

        if (maxPressure < sensorType.maxMbar - ceilingProximityMbar) return null

        val nearCeiling = wotEntries.filter {
            it.actualMap >= sensorType.maxMbar - ceilingProximityMbar
        }
        val fraction = nearCeiling.size.toDouble() / wotEntries.size

        if (fraction < 0.05) return null // Less than 5% of samples near ceiling — not saturated

        // Find affected RPM ranges
        val affectedRpms = nearCeiling.map { it.rpm }.sorted()
        val rpmRanges = buildRpmRanges(affectedRpms)

        return SensorSaturationWarning(
            sensorName = "MAP Sensor (${sensorType.name})",
            maxValue = maxPressure,
            unit = "mbar",
            plateauPercent = fraction * 100.0,
            recommendation = "${sensorType.name} MAP sensor near ceiling at " +
                "${String.format("%.0f", maxPressure)} mbar " +
                "(${String.format("%.0f", fraction * 100)}% of WOT at limit). " +
                sensorType.upgradeAdvice,
            affectedRpmRanges = rpmRanges
        )
    }

    /**
     * Injector maxed: duty cycle exceeds threshold.
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

    // ── Sensor voltage saturation detectors ──────────────────────────

    /**
     * Result type for MAF voltage saturation: (detected, warning, maxVoltage, saturationDetail)
     */
    private data class MafVoltageResult(
        val first: Boolean,
        val second: String,
        val third: Double,
        val fourth: SensorSaturationWarning
    )

    /**
     * Detect MAF sensor voltage saturation.
     *
     * ME7's HFM5 hot-film MAF sensor outputs 0–5 V. The MLHFM transfer function
     * maps voltage → kg/h (then scaled to g/s). When the sensor output clips at
     * ~4.8–5.0 V, the ECU uses the top bin of MLHFM — any additional airflow above
     * that point is invisible. This means:
     * - mshfm_w (MAF g/s) will plateau even if real airflow is higher
     * - All downstream calculations (load, fueling, VE model) are wrong at high flow
     * - KFURL/KFPBRK solver suggestions at high-RPM/high-load are unreliable
     *
     * Fix: Rescale MLHFM to use a larger-range MAF housing (e.g., 80mm → 85mm)
     * and extend the transfer function's top bins, or switch to a different MAF
     * sensor with higher flow capacity.
     */
    private fun detectMafVoltageSaturation(
        mafVoltages: List<Double>?,
        wotEntries: List<OptimizerCalculator.WotLogEntry>
    ): MafVoltageResult {
        val empty = MafVoltageResult(
            false, "", 0.0,
            SensorSaturationWarning("MAF Voltage", 0.0, "V", 0.0, "")
        )

        if (mafVoltages == null || mafVoltages.isEmpty()) return empty

        val maxVoltage = mafVoltages.max()
        if (maxVoltage < SENSOR_VOLTAGE_CEILING) return empty

        val nearCeiling = mafVoltages.indices.count {
            mafVoltages[it] >= SENSOR_VOLTAGE_CEILING - VOLTAGE_PLATEAU_BAND
        }
        val fraction = nearCeiling.toDouble() / mafVoltages.size

        val detected = fraction > VOLTAGE_PLATEAU_FRACTION

        // Find affected RPM ranges
        val affectedRpms = mafVoltages.indices
            .filter { mafVoltages[it] >= SENSOR_VOLTAGE_CEILING - VOLTAGE_PLATEAU_BAND }
            .filter { it < wotEntries.size }
            .map { wotEntries[it].rpm }
            .sorted()
        val rpmRanges = buildRpmRanges(affectedRpms)

        val rpmDetail = if (rpmRanges.isNotEmpty()) {
            " Affected RPM: " + rpmRanges.joinToString(", ") {
                "${it.first.toInt()}–${it.second.toInt()}"
            } + "."
        } else ""

        val warning = "WARNING: MAF sensor voltage saturated at " +
            "${String.format("%.2f", maxVoltage)} V " +
            "(${String.format("%.0f", fraction * 100)}% of WOT at ≥${SENSOR_VOLTAGE_CEILING} V ceiling).$rpmDetail " +
            "MLHFM is clipping — airflow above this point is invisible to the ECU. " +
            "Rescale MLHFM for a larger MAF housing or upgrade the MAF sensor."

        val saturation = SensorSaturationWarning(
            sensorName = "MAF Sensor (uhfm_w)",
            maxValue = maxVoltage,
            unit = "V",
            plateauPercent = fraction * 100.0,
            recommendation = "MLHFM rescaling needed: extend top voltage bins for actual airflow. " +
                "Common fix: larger MAF housing (e.g., 80mm → 85mm) with rescaled MLHFM. " +
                "All solver suggestions at high RPM/load are unreliable while MAF is clipping.",
            affectedRpmRanges = rpmRanges
        )

        return MafVoltageResult(detected, warning, maxVoltage, saturation)
    }

    // ── Data reliability assessment ──────────────────────────────────

    /**
     * Assess whether sensor saturation compromises the reliability of
     * downstream solver suggestions (KFURL, KFPBRK, KFPRG, etc.).
     *
     * When a sensor is saturated, the logged data at those operating points
     * is incorrect. Any map correction derived from saturated data will be
     * compensating for sensor error rather than actual VE characteristics.
     */
    private fun assessDataReliability(
        mafVoltageMaxed: Boolean,
        mafMaxed: Boolean,
        mapSensorMaxed: Boolean,
        @Suppress("UNUSED_PARAMETER") wotEntries: List<OptimizerCalculator.WotLogEntry>
    ): Pair<Boolean, String> {
        val issues = mutableListOf<String>()

        if (mafVoltageMaxed || mafMaxed) {
            issues.add("MAF clipping → mshfm_w unreliable at peak airflow → " +
                "KFURL/KFPBRK high-RPM suggestions may be biased")
        }

        if (mapSensorMaxed) {
            issues.add("MAP sensor saturated → pvdks_w plateau → " +
                "boost control, KFPBRK, and pressure-based calculations are unreliable at peak boost")
        }

        if (issues.isEmpty()) return Pair(false, "")

        val detail = "WARNING: Data reliability warning: ${issues.joinToString("; ")}. " +
            "Upgrade saturated sensors before trusting solver suggestions at affected operating points."

        return Pair(true, detail)
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /**
     * Build contiguous RPM ranges from a sorted list of RPM values.
     * Groups values within 500 RPM of each other into ranges.
     */
    private fun buildRpmRanges(sortedRpms: List<Double>): List<Pair<Double, Double>> {
        if (sortedRpms.isEmpty()) return emptyList()

        val ranges = mutableListOf<Pair<Double, Double>>()
        var rangeStart = sortedRpms.first()
        var rangePrev = rangeStart

        for (rpm in sortedRpms.drop(1)) {
            if (rpm - rangePrev > 500.0) {
                ranges.add(rangeStart to rangePrev)
                rangeStart = rpm
            }
            rangePrev = rpm
        }
        ranges.add(rangeStart to rangePrev)

        return ranges
    }
}
