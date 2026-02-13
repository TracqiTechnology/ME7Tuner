import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import data.parser.bin.BinParser
import data.parser.xdf.XdfParser
import data.preferences.MapPreferenceManager
import data.preferences.bin.BinFilePreferences
import data.preferences.filechooser.BinFileChooserPreferences
import data.preferences.filechooser.XdfFileChooserPreferences
import data.preferences.logheaderdefinition.LogHeaderPreference
import data.preferences.xdf.XdfFilePreferences
import data.profile.ProfileManager
import ui.navigation.ME7TunerApp
import ui.theme.ME7TunerTheme
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Locale

fun main() {
    Locale.setDefault(Locale.ENGLISH)
    System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")

    // Initialize data layer flows
    XdfParser.init()
    BinParser.init()
    LogHeaderPreference.loadHeaders()

    application {
        val binFile by BinFilePreferences.file.collectAsState()
        val xdfFile by XdfFilePreferences.file.collectAsState()

        val title = remember(binFile, xdfFile) {
            "ME7 Tuner - ${binFile.name} | XDF File - ${xdfFile.name}"
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = title,
            state = rememberWindowState(width = 1480.dp, height = 1080.dp)
        ) {
            MenuBar {
                Menu("File") {
                    Item("Open Bin...") {
                        val file = openFileDialog(window, "Open Bin File", "bin", BinFileChooserPreferences.lastDirectory)
                        if (file != null) {
                            BinFilePreferences.setFile(file)
                            BinFileChooserPreferences.lastDirectory = file.parent
                        }
                    }
                }
                Menu("XDF") {
                    Item("Select XDF...") {
                        val file = openFileDialog(window, "Select XDF File", "xdf", XdfFileChooserPreferences.lastDirectory)
                        if (file != null) {
                            MapPreferenceManager.clear()
                            XdfFilePreferences.setFile(file)
                            XdfFileChooserPreferences.lastDirectory = file.parent
                        }
                    }
                }
                Menu("Profiles") {
                    Item("Load Profile...") {
                        val file = openFileDialog(window, "Load Profile", "me7profile.json", "")
                        if (file != null) {
                            runCatching {
                                val profile = ProfileManager.loadFromFile(file)
                                ProfileManager.applyProfile(profile)
                                ProfileManager.addUserProfile(profile)
                            }
                        }
                    }
                    Item("Save Profile...") {
                        javax.swing.SwingUtilities.invokeLater {
                            val name = javax.swing.JOptionPane.showInputDialog(
                                window,
                                "Profile name:",
                                "Save Profile",
                                javax.swing.JOptionPane.PLAIN_MESSAGE
                            )
                            if (name != null && name.isNotBlank()) {
                                val dialog = FileDialog(window, "Save Profile", FileDialog.SAVE)
                                dialog.file = "${name.replace(Regex("[^a-zA-Z0-9_ -]"), "")}.me7profile.json"
                                dialog.isVisible = true
                                val dir = dialog.directory
                                val fileName = dialog.file
                                if (dir != null && fileName != null) {
                                    runCatching {
                                        val profile = ProfileManager.exportCurrentProfile(name)
                                        ProfileManager.saveToFile(profile, File(dir, fileName))
                                    }
                                }
                            }
                        }
                    }
                }
                Menu("Preferences") {
                    Item("Reset Preferences") {
                        MapPreferenceManager.clear()
                        LogHeaderPreference.loadHeaders()
                        XdfFilePreferences.clear()
                        BinFilePreferences.clear()
                        XdfFileChooserPreferences.clear()
                        BinFileChooserPreferences.clear()
                    }
                }
            }

            ME7TunerTheme {
                ME7TunerApp()
            }
        }
    }
}

private fun openFileDialog(parent: Frame, title: String, extension: String, initialDir: String): File? {
    val dialog = FileDialog(parent, title, FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".$extension", ignoreCase = true) }
    if (initialDir.isNotEmpty()) dialog.directory = initialDir
    dialog.isVisible = true
    val dir = dialog.directory
    val file = dialog.file
    return if (dir != null && file != null) File(dir, file) else null
}
