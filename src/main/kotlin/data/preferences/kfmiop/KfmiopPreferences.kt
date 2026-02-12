package data.preferences.kfmiop

import data.preferences.MapPreference
import java.util.prefs.Preferences

object KfmiopPreferences : MapPreference("kfmiop_title_preference", "kfmiop_description_preference", "kfmiop_unit_preference") {
    private val prefs = Preferences.userNodeForPackage(KfmiopPreferences::class.java)

    var maxMapPressure: Double
        get() = prefs.get("max_map_pressure_preference", "2550").toDouble()
        set(value) = prefs.put("max_map_pressure_preference", value.toString())

    var maxBoostPressure: Double
        get() = prefs.get("max_boost_pressure_preference", "2100").toDouble()
        set(value) = prefs.put("max_boost_pressure_preference", value.toString())
}
