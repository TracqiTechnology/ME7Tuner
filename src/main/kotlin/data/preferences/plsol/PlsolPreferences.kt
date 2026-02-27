package data.preferences.plsol

import java.util.prefs.Preferences

object PlsolPreferences {
    private val prefs = Preferences.userNodeForPackage(PlsolPreferences::class.java)

    var barometricPressure: Double
        get() = prefs.get("barometric_pressure_preference", "1013").toDouble()
        set(value) = prefs.put("barometric_pressure_preference", value.toString())

    var intakeAirTemperature: Double
        get() = prefs.get("intake_air_temperature_preference", "20").toDouble()
        set(value) = prefs.put("intake_air_temperature_preference", value.toString())

    var kfurl: Double
        get() = prefs.get("kfurl_preference", "0.1037").toDouble()
        set(value) = prefs.put("kfurl_preference", value.toString())

    var displacement: Double
        get() = prefs.get("displacement_preference", "2.7").toDouble()
        set(value) = prefs.put("displacement_preference", value.toString())

    var rpm: Int
        get() = prefs.get("rpm_preference", "6000").toInt()
        set(value) = prefs.put("rpm_preference", value.toString())
}
