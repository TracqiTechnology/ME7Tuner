package data.preferences.primaryfueling

import java.util.prefs.Preferences

object PrimaryFuelingPreferences {
    private val prefs = Preferences.userNodeForPackage(PrimaryFuelingPreferences::class.java)

    var airDensity: Double
        get() = prefs.get("air_density_preference", "1.2929").toDouble()
        set(value) = prefs.put("air_density_preference", value.toString())

    var displacement: Double
        get() = prefs.get("displacement_preference", "0.4496").toDouble()
        set(value) = prefs.put("displacement_preference", value.toString())

    var numCylinders: Int
        get() = prefs.get("num_cylinders_preference", "6").toInt()
        set(value) = prefs.put("num_cylinders_preference", value.toString())

    var stoichiometricAfr: Double
        get() = prefs.get("stoichiometric_afr_preference", "14.7").toDouble()
        set(value) = prefs.put("stoichiometric_afr_preference", value.toString())

    var gasolineGramsPerCubicCentimeter: Double
        get() = prefs.get("fuel_density_preference", "0.7").toDouble()
        set(value) = prefs.put("fuel_density_preference", value.toString())

    var fuelInjectorSize: Double
        get() = prefs.get("fuel_injector_size_preference", "349").toDouble()
        set(value) = prefs.put("fuel_injector_size_preference", value.toString())

    // Flex fuel E0/E100 endpoint preferences (UI-state only)
    var e0Density: Double
        get() = prefs.get("e0_density_preference", "0.755").toDouble()
        set(value) = prefs.put("e0_density_preference", value.toString())

    var e0Afr: Double
        get() = prefs.get("e0_afr_preference", "14.7").toDouble()
        set(value) = prefs.put("e0_afr_preference", value.toString())

    var e100Density: Double
        get() = prefs.get("e100_density_preference", "0.789").toDouble()
        set(value) = prefs.put("e100_density_preference", value.toString())

    var e100Afr: Double
        get() = prefs.get("e100_afr_preference", "9.0").toDouble()
        set(value) = prefs.put("e100_afr_preference", value.toString())
}
