package data.preferences.wdkugdn

import data.preferences.MapPreference
import java.util.prefs.Preferences

object WdkugdnPreferences : MapPreference("wdkugdn_title_preference", "wdkugdn_description_preference", "wdkugdn_unit_preference") {
    private val prefs = Preferences.userNodeForPackage(WdkugdnPreferences::class.java)

    var displacement: Double
        get() = prefs.get("displacement_preference", "2.7").toDouble()
        set(value) = prefs.put("displacement_preference", value.toString())
}
