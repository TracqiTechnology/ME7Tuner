package data.preferences.openloopfueling

import java.util.prefs.Preferences

object OpenLoopFuelingLogFilterPreferences {
    private val prefs = Preferences.userNodeForPackage(OpenLoopFuelingLogFilterPreferences::class.java)

    var minThrottleAngle: Double
        get() = prefs.get("min_throttle_angle_preference", "80").toDouble()
        set(value) = prefs.put("min_throttle_angle_preference", value.toString())

    var minRpm: Double
        get() = prefs.get("min_rpm_preference", "2000").toDouble()
        set(value) = prefs.put("min_rpm_preference", value.toString())

    var minMe7Points: Int
        get() = prefs.get("min_me7_points_preference", "75").toInt()
        set(value) = prefs.put("min_me7_points_preference", value.toString())

    var minAfrPoints: Int
        get() = prefs.get("min_afr_points_preference", "150").toInt()
        set(value) = prefs.put("min_afr_points_preference", value.toString())

    var maxAfr: Double
        get() = prefs.get("max_afr_preference", "16").toDouble()
        set(value) = prefs.put("max_afr_preference", value.toString())

    var fuelInjectorSize: Double
        get() = prefs.get("open_loop_fuel_injector_size_preference", "349").toDouble()
        set(value) = prefs.put("open_loop_fuel_injector_size_preference", value.toString())

    var gasolineGramsPerCubicCentimeter: Double
        get() = prefs.get("fuel_density_open_loop_preference", "0.7").toDouble()
        set(value) = prefs.put("fuel_density_open_loop_preference", value.toString())

    var numFuelInjectors: Double
        get() = prefs.get("num_fuel_injectors_open_loop_preference", "6").toDouble()
        set(value) = prefs.put("num_fuel_injectors_open_loop_preference", value.toString())
}
