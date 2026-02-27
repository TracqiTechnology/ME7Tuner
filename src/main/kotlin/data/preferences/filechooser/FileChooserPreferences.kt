package data.preferences.filechooser

import java.util.prefs.Preferences

object FileChooserPreferences {
    private val prefs = Preferences.userNodeForPackage(FileChooserPreferences::class.java)

    var lastDirectory: String
        get() = prefs.get("last_directory", "")
        set(value) = prefs.put("last_directory", value)
}
