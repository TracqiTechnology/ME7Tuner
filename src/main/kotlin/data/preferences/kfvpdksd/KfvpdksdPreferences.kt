package data.preferences.kfvpdksd

import data.preferences.MapPreference
import java.util.prefs.Preferences

object KfvpdksdPreferences : MapPreference("kfvpdksd_title_preference", "kfvpdksd_description_preference", "kfvpdksd_unit_preference") {
    private val prefs = Preferences.userNodeForPackage(KfvpdksdPreferences::class.java)

    var maxWastegateCrackingPressure: Double
        get() = prefs.get("max_wastegate_cracking_pressure_preference", "200").toDouble()
        set(value) = prefs.put("max_wastegate_cracking_pressure_preference", value.toString())

    var lastDirectory: String
        get() = prefs.get("last_directory_preference", "")
        set(value) = prefs.put("last_directory_preference", value)
}
