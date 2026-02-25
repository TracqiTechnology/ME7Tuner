package data.preferences.optimizer

import java.util.prefs.Preferences

object OptimizerPreferences {
    private val prefs = Preferences.userNodeForPackage(OptimizerPreferences::class.java)

    private const val MAP_TOLERANCE_KEY = "optimizer_map_tolerance_mbar"
    private const val LDRXN_TARGET_KEY = "optimizer_ldrxn_target"
    private const val KFLDIMX_OVERHEAD_KEY = "optimizer_kfldimx_overhead_pct"
    private const val MIN_THROTTLE_ANGLE_KEY = "optimizer_min_throttle_angle"
    private const val LAST_DIRECTORY_KEY = "optimizer_last_directory"

    var mapToleranceMbar: Double
        get() = prefs.getDouble(MAP_TOLERANCE_KEY, 30.0)
        set(value) = prefs.putDouble(MAP_TOLERANCE_KEY, value)

    var ldrxnTarget: Double
        get() = prefs.getDouble(LDRXN_TARGET_KEY, 191.0)
        set(value) = prefs.putDouble(LDRXN_TARGET_KEY, value)

    var kfldimxOverheadPercent: Double
        get() = prefs.getDouble(KFLDIMX_OVERHEAD_KEY, 8.0)
        set(value) = prefs.putDouble(KFLDIMX_OVERHEAD_KEY, value)

    var minThrottleAngle: Double
        get() = prefs.getDouble(MIN_THROTTLE_ANGLE_KEY, 80.0)
        set(value) = prefs.putDouble(MIN_THROTTLE_ANGLE_KEY, value)

    var lastDirectory: String
        get() = prefs.get(LAST_DIRECTORY_KEY, "")
        set(value) = prefs.put(LAST_DIRECTORY_KEY, value)
}

