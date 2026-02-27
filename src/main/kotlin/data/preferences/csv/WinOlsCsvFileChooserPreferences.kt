package data.preferences.csv

import java.util.prefs.Preferences

object WinOlsCsvFileChooserPreferences {
    private const val LAST_DIR_KEY = "winols_csv_last_dir"
    private val prefs = Preferences.userRoot().node(WinOlsCsvFileChooserPreferences::class.java.name)

    var lastDirectory: String
        get() = prefs.get(LAST_DIR_KEY, "")
        set(value) { prefs.put(LAST_DIR_KEY, value) }

    fun clear() { runCatching { prefs.clear() } }
}

