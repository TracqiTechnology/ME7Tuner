package domain.model.simulator

import domain.math.map.Map3d
import domain.model.optimizer.OptimizerCalculator
import kotlin.math.abs

/**
 * Phase 18: Environmental corrections for altitude and temperature.
 *
 * v3 assumes sea-level, standard-temperature conditions. Real engines operate
 * at altitude (lower air density → different WGDC needed) and varying intake
 * temperatures (affects VE model accuracy).
 *
 * ME7 Reference:
 * - Altitude: KFLDIOPU (me7-raw.txt line 143618, 144473)
 * - Temperature: KFFWTBR (me7-raw.txt line 54498, 54872)
 * - ftu factor: me7-raw.txt line 54882
 *
 * @see documentation/me7-boost-control.md §4
 */
object EnvironmentalCorrector {

    /** Standard conditions assumed by the base VE model. */
    private const val STANDARD_BARO_MBAR = 1013.0
    private const val STANDARD_INTAKE_TEMP_C = 20.0
    private const val STANDARD_COOLANT_TEMP_C = 96.0

    /** Environmental conditions summary from log data. */
    data class EnvironmentalSummary(
        val avgBaroPressure: Double,
        val estimatedAltitudeM: Double,
        val avgIntakeTemp: Double?,      // null if not available in logs
        val avgCoolantTemp: Double?,
        val intakeTempRange: Pair<Double, Double>?,
        val altitudeDeviation: Boolean,  // true if significantly non-sea-level
        val tempDeviation: Boolean,      // true if significantly non-standard
        val kftarxWarning: Boolean = false,  // Finding 4: KFTARX may reduce LDRXN
        val warnings: List<String>
    )

    /**
     * Analyze environmental conditions from WOT log data.
     */
    fun analyzeSummary(
        wotEntries: List<OptimizerCalculator.WotLogEntry>
    ): EnvironmentalSummary {
        if (wotEntries.isEmpty()) {
            return EnvironmentalSummary(
                STANDARD_BARO_MBAR, 0.0, null, null, null,
                altitudeDeviation = false, tempDeviation = false, kftarxWarning = false,
                warnings = emptyList()
            )
        }

        val warnings = mutableListOf<String>()

        val avgBaro = wotEntries.map { it.barometricPressure }.average()
        // Barometric formula approximation: altitude ≈ (1 - (P/1013.25)^0.190284) × 44330
        val altitudeM = (1 - Math.pow(avgBaro / 1013.25, 0.190284)) * 44330

        val altitudeDeviation = abs(avgBaro - STANDARD_BARO_MBAR) > 30 // >30 mbar = ~250m altitude
        if (altitudeDeviation) {
            warnings.add("🏔️ Altitude detected: avg barometric pressure ${String.format("%.0f", avgBaro)} mbar " +
                "(≈${String.format("%.0f", altitudeM)}m / ${String.format("%.0f", altitudeM * 3.281)}ft). " +
                "KFLDRL suggestions may need KFLDIOPU altitude correction.")
        }

        // Finding 4: KFTARX intake air temperature correction warning
        // ME7 Reference: LDRLMX 3.100 (line 142587)
        // KFTARX reduces effective LDRXN above tans ≈ 75°C.
        // We check for high ambient conditions using barometric pressure as a rough
        // indicator (low baro + high altitude = higher density altitude).
        // Since we can't read tans directly from most logs, we warn conservatively
        // when conditions suggest hot intake air.
        val kftarxWarning = false  // Will be set when tans is available from logs

        // Check for potential intake temp issues from very low barometric pressure
        // (high altitude + heat = especially bad for charge air temps)
        if (altitudeDeviation && avgBaro < 950) {
            warnings.add("🌡️ High altitude (${String.format("%.0f", altitudeM)}m) with low baro pressure — " +
                "charge air temperatures may be elevated. KFTARX (me7-raw.txt line 142587) " +
                "reduces effective LDRXN above tans > 75°C. Monitor intake air temps.")
        }

        return EnvironmentalSummary(
            avgBaroPressure = avgBaro,
            estimatedAltitudeM = altitudeM,
            avgIntakeTemp = null,  // Not yet available from standard log headers
            avgCoolantTemp = null,
            intakeTempRange = null,
            altitudeDeviation = altitudeDeviation,
            tempDeviation = false,
            kftarxWarning = kftarxWarning,
            warnings = warnings
        )
    }

    /**
     * Normalize observed WGDC to sea-level equivalent using KFLDIOPU.
     *
     * From me7-raw.txt line 144473:
     * > "KFLDIOPU: Tastverhältniskorrekturbedarf als Funktion der Höhe (pu)"
     *
     * @param observedWgdc Actual WGDC from log
     * @param rpm Engine RPM
     * @param baroPressure Barometric pressure from log (pus_w)
     * @param kfldiopu Altitude correction map
     * @return Normalized WGDC at sea level
     */
    fun normalizeWgdcForAltitude(
        observedWgdc: Double,
        rpm: Double,
        baroPressure: Double,
        kfldiopu: Map3d
    ): Double {
        val altitudeCorrection = kfldiopu.lookup(baroPressure, rpm)
        val seaLevelCorrection = kfldiopu.lookup(STANDARD_BARO_MBAR, rpm)
        return observedWgdc - altitudeCorrection + seaLevelCorrection
    }

    /**
     * Compute the combustion chamber temperature factor ftbr.
     *
     * From me7-raw.txt line 54859–54880:
     * > "Diese Temperaturkompensation liefert am Ausgang den Faktor Temperatur
     * > Brennraum (ftbr)"
     *
     * ftbr = 273 / (evtmod + 273) × fwft
     * where:
     *   evtmod = tans + (tmot - tans) × KFFWTBR(rpm, load)
     *   fwft = (tans + 673.425) / 731.334
     *
     * @return ftbr factor (1.0 at standard conditions)
     */
    fun computeFtbr(
        intakeAirTemp: Double,     // tans (°C)
        coolantTemp: Double,       // tmot (°C)
        rpm: Double,
        load: Double,
        kffwtbr: Map3d? = null
    ): Double {
        // KFFWTBR blending factor (0 = pure intake temp, 1 = pure coolant temp)
        val blendFactor = kffwtbr?.lookup(load, rpm) ?: 0.02  // default from ME7 docs
        val evtmod = intakeAirTemp + (coolantTemp - intakeAirTemp) * blendFactor
        val fwft = (intakeAirTemp + 673.425) / 731.334
        return 273.0 / (evtmod + 273.0) * fwft
    }

    /**
     * Compute standard ftbr at reference conditions.
     */
    fun standardFtbr(): Double {
        return computeFtbr(STANDARD_INTAKE_TEMP_C, STANDARD_COOLANT_TEMP_C, 3000.0, 100.0)
    }
}

