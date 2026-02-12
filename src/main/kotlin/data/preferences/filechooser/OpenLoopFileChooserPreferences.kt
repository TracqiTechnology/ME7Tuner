package data.preferences.filechooser

import java.util.prefs.Preferences

object OpenLoopFileChooserPreferences {
    private val prefs = Preferences.userNodeForPackage(OpenLoopFileChooserPreferences::class.java)

    var lastDirectory: String
        get() = prefs.get("last_directory", "")
        set(value) = prefs.put("last_directory", value)
}
