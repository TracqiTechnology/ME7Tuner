package data.preferences.ldrpid

import java.util.prefs.Preferences

object LdrpidPreferences {
    private val prefs = Preferences.userNodeForPackage(LdrpidPreferences::class.java)

    var lastDirectory: String
        get() = prefs.get("last_directory", "")
        set(value) = prefs.put("last_directory", value)
}
