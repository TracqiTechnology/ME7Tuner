package data.preferences.kp

import java.util.prefs.Preferences

object KpFileChooserPreferences {
    private const val LAST_DIR_KEY = "kp_last_dir"
    private val prefs = Preferences.userRoot().node(KpFileChooserPreferences::class.java.name)

    var lastDirectory: String
        get() = prefs.get(LAST_DIR_KEY, "")
        set(value) { prefs.put(LAST_DIR_KEY, value) }

    fun clear() { runCatching { prefs.clear() } }
}

