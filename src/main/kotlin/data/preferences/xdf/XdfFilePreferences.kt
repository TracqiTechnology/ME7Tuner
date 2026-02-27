package data.preferences.xdf

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.prefs.Preferences

object XdfFilePreferences {
    private const val FILE_PATH_KEY = "file_path_key"
    private val prefs = Preferences.userRoot().node(XdfFilePreferences::class.java.name)

    private val _file = MutableStateFlow(getStoredFile())
    val file: StateFlow<File> = _file.asStateFlow()

    fun clear() { runCatching { prefs.clear() } }

    fun getStoredFile(): File = File(prefs.get(FILE_PATH_KEY, ""))

    fun setFile(file: File) {
        prefs.put(FILE_PATH_KEY, file.absolutePath)
        _file.value = file
    }
}
