package data.preferences.filechooser

import java.util.prefs.Preferences

object XdfFileChooserPreferences {
    private val prefs = Preferences.userNodeForPackage(XdfFileChooserPreferences::class.java)

    var lastDirectory: String
        get() = prefs.get("last_directory", "")
        set(value) = prefs.put("last_directory", value)

    fun clear() { runCatching { prefs.clear() } }
}
