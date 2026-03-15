package domain.model.presets

data class FuelPreset(val name: String, val densityGPerCc: Double, val stoichAfr: Double)

object FuelPresets {
    val E0 = FuelPreset("E0 (Gasoline)", 0.755, 14.7)
    val E85 = FuelPreset("E85", 0.78, 9.765)
    val E100 = FuelPreset("E100 (Pure Ethanol)", 0.789, 9.0)
    val RACE_GAS_100 = FuelPreset("Race Gas 100 (Leaded)", 0.74, 14.7)
    val RACE_GAS_116 = FuelPreset("Race Gas 116 (Leaded)", 0.72, 14.7)

    val all = listOf(E0, E85, E100, RACE_GAS_100, RACE_GAS_116)

    /** Linear interpolation between E0 and E100 for a given ethanol percentage (0–100). */
    fun blend(ethPct: Double, e0: FuelPreset = E0, e100: FuelPreset = E100): FuelPreset {
        val density = e0.densityGPerCc + (e100.densityGPerCc - e0.densityGPerCc) * ethPct / 100
        val afr = e0.stoichAfr + (e100.stoichAfr - e0.stoichAfr) * ethPct / 100
        return FuelPreset("E${ethPct.toInt()}", density, afr)
    }
}
