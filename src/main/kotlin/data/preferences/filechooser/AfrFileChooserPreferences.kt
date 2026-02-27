package data.preferences.filechooser

import java.util.prefs.Preferences

object AfrFileChooserPreferences {
    private val prefs = Preferences.userNodeForPackage(AfrFileChooserPreferences::class.java)

    var lastDirectory: String
        get() = prefs.get("last_directory", "")
        set(value) = prefs.put("last_directory", value)
}
