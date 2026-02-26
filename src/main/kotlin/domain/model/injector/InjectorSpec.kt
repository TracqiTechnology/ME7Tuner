package domain.model.injector

/**
 * Injector specification data for scaling calculations.
 *
 * @param flowRateCcPerMin Static flow rate at reference pressure (cc/min)
 * @param fuelPressureBar Reference fuel pressure (bar), typically 3.0 or 4.0
 * @param deadTimeMs Dead time at 14.0V (ms)
 * @param deadTimeVoltageTable Optional: voltage (V) → dead time (ms) pairs for TVUB
 */
data class InjectorSpec(
    val flowRateCcPerMin: Double,
    val fuelPressureBar: Double = 3.0,
    val deadTimeMs: Double = 0.0,
    val deadTimeVoltageTable: Map<Double, Double> = emptyMap()
)

/**
 * Result of KRKTE scaling calculation.
 *
 * In ME7, injection time is: te = rk_w × KRKTE + TVUB(ubat)
 * (me7-raw.txt line 257427). KRKTE is a scalar constant [ms/%], not a 2D map.
 * There is no "KFTI" injection time Kennfeld in ME7.
 *
 * When changing injectors, KRKTE scales by the flow ratio:
 *   KRKTE_new = KRKTE_old × (old_flow / new_flow) × √(new_press / old_press)
 *
 * @param scaleFactor The computed KRKTE scale factor (old_flow / new_flow), pressure-corrected
 * @param flowRatio Raw flow ratio before pressure correction
 * @param pressureCorrection Fuel pressure correction factor √(new_press / old_press)
 * @param oldDeadTimeMs Old TVUB dead time at nominal voltage (ms)
 * @param newDeadTimeMs New TVUB dead time at nominal voltage (ms)
 * @param warnings Any warnings about the scaling
 */
data class KrkteScalingResult(
    val scaleFactor: Double,
    val flowRatio: Double,
    val pressureCorrection: Double,
    val oldDeadTimeMs: Double,
    val newDeadTimeMs: Double,
    val warnings: List<String>
)

/**
 * Result of TVUB (dead time) computation.
 *
 * TVUB (Ventilverzugszeit als f(Ubat)) is a 1D Kennlinie in ME7:
 * battery voltage (V) → injector dead time (ms).
 * XDF description: "Injector on time offset voltage correction".
 * Confirmed in me7-raw.txt line 183466 and example XDF (5 voltage points).
 *
 * @param voltageAxis Voltage axis values (V)
 * @param deadTimes Dead time values (ms) corresponding to each voltage
 */
data class TvubResult(
    val voltageAxis: DoubleArray,
    val deadTimes: DoubleArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TvubResult) return false
        return voltageAxis.contentEquals(other.voltageAxis) &&
                deadTimes.contentEquals(other.deadTimes)
    }

    override fun hashCode(): Int {
        var result = voltageAxis.contentHashCode()
        result = 31 * result + deadTimes.contentHashCode()
        return result
    }
}
