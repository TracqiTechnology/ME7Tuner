package data.preferences.closedloopfueling

import java.util.prefs.Preferences

object ClosedLoopFuelingLogPreferences {
    private val prefs = Preferences.userNodeForPackage(ClosedLoopFuelingLogPreferences::class.java)

    var minThrottleAngle: Double
        get() = prefs.get("min_throttle_angle_preference", "0").toDouble()
        set(value) = prefs.put("min_throttle_angle_preference", value.toString())

    var minRpm: Double
        get() = prefs.get("min_rpm_preference", "0").toDouble()
        set(value) = prefs.put("min_rpm_preference", value.toString())

    var maxDerivative: Double
        get() = prefs.get("max_derivative_preference", "50").toDouble()
        set(value) = prefs.put("max_derivative_preference", value.toString())

    var lastDirectory: String
        get() = prefs.get("last_directory_preference", "")
        set(value) = prefs.put("last_directory_preference", value)
}
