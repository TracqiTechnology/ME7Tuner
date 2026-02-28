package data.preferences.eula

import java.util.prefs.Preferences

object EulaPreferences {
    private val prefs = Preferences.userNodeForPackage(EulaPreferences::class.java)
    private const val EULA_ACCEPTED_KEY = "eula_accepted"

    var accepted: Boolean
        get() = prefs.getBoolean(EULA_ACCEPTED_KEY, false)
        set(value) = prefs.putBoolean(EULA_ACCEPTED_KEY, value)
}
