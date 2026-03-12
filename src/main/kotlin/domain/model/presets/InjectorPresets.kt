package domain.model.presets

data class InjectorPreset(val name: String, val flowRateCcPerMin: Double, val fuelPressureBar: Double)

object InjectorPresets {
    // GDI injectors
    val DAZA_GDI = InjectorPreset("DAZA GDI (Stock RS3 FL)", 450.0, 200.0)
    val DNWA_GDI = InjectorPreset("DNWA GDI (Stock RS3 PFL)", 380.0, 200.0)
    // PFI injectors
    val DAZA_PFI = InjectorPreset("DAZA PFI (Stock RS3 FL)", 270.0, 5.0)
    val DNWA_PFI = InjectorPreset("DNWA PFI (Stock RS3 PFL)", 220.0, 5.0)

    val all = listOf(DAZA_GDI, DNWA_GDI, DAZA_PFI, DNWA_PFI)
}
