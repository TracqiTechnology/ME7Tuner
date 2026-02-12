package data.preferences.filechooser

import java.util.prefs.Preferences

object BinFileChooserPreferences {
    private val prefs = Preferences.userNodeForPackage(BinFileChooserPreferences::class.java)

    var lastDirectory: String
        get() = prefs.get("last_directory", "")
        set(value) = prefs.put("last_directory", value)

    fun clear() { runCatching { prefs.clear() } }
}
