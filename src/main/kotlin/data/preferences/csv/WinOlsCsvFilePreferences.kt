package data.preferences.csv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.prefs.Preferences

/**
 * Persists the path to the last-loaded WinOLS CSV export file.
 *
 * Parallel to [data.preferences.kp.KpFilePreferences] but for the richer
 * WinOLS CSV export (File → Export → Map data as CSV in WinOLS).
 */
object WinOlsCsvFilePreferences {
    private const val FILE_PATH_KEY = "winols_csv_file_path_key"
    private val prefs = Preferences.userRoot().node(WinOlsCsvFilePreferences::class.java.name)

    private val _file = MutableStateFlow(getStoredFile())
    val file: StateFlow<File> = _file.asStateFlow()

    fun clear() {
        runCatching { prefs.remove(FILE_PATH_KEY) }
        _file.value = File("")
    }

    fun getStoredFile(): File = File(prefs.get(FILE_PATH_KEY, ""))

    fun setFile(file: File) {
        prefs.put(FILE_PATH_KEY, file.absolutePath)
        _file.value = file
    }
}

