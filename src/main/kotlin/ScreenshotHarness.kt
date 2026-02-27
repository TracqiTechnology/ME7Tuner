@file:OptIn(ExperimentalComposeUiApi::class)

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.parser.afrlog.AfrLogParser
import data.parser.bin.BinParser
import data.parser.me7log.*
import data.parser.xdf.XdfParser
import data.preferences.MapPreference
import data.preferences.bin.BinFilePreferences
import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfmirl.KfmirlPreferences
import data.preferences.kfpbrk.KfpbrkPreferences
import data.preferences.kfpbrknw.KfpbrknwPreferences
import data.preferences.kfvpdksd.KfvpdksdPreferences
import data.preferences.kfzw.KfzwPreferences
import data.preferences.kfzwop.KfzwopPreferences
import data.preferences.krkte.KrktePreferences
import data.preferences.logheaderdefinition.LogHeaderPreference
import data.preferences.mlhfm.MlhfmPreferences
import data.preferences.wdkugdn.WdkugdnPreferences
import data.preferences.xdf.XdfFilePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import ui.navigation.CalibrationTab
import ui.navigation.ME7TunerApp
import ui.navigation.NavigationState
import ui.navigation.RailDestination
import ui.theme.ME7TunerTheme
import java.io.File
import java.util.Locale

fun main() {
    Locale.setDefault(Locale.ENGLISH)

    // Initialize data layer
    XdfParser.init()
    BinParser.init()
    LogHeaderPreference.loadHeaders()

    // Load example files
    XdfFilePreferences.setFile(File("example/8D0907551M-20170411-16bit-kfzw.xdf"))
    BinFilePreferences.setFile(File("example/8D0907551M-0002 (16Bit KFZW).bin"))

    // Wait for BIN parsing to complete
    println("Waiting for BIN parsing...")
    runBlocking {
        BinParser.mapList.first { it.isNotEmpty() }
    }
    println("BIN parsed: ${BinParser.mapList.value.size} maps loaded")

    // Auto-select maps
    autoSelectMap(KrktePreferences, "KRKTE")
    autoSelectMap(MlhfmPreferences, "MLHFM")
    autoSelectMap(KfmiopPreferences, "KFMIOP")
    autoSelectMap(KfmirlPreferences, "KFMIRL")
    autoSelectMap(KfzwopPreferences, "KFZWOP")
    autoSelectMap(KfzwPreferences, "KFZW")
    autoSelectMap(KfvpdksdPreferences, "KFVPDKSD")
    autoSelectMap(WdkugdnPreferences, "WDKUGDN")
    autoSelectMap(KfldrlPreferences, "KFLDRL")
    autoSelectMap(KfldimxPreferences, "KFLDIMX")
    autoSelectMap(KfpbrkPreferences, "KFPBRK")
    autoSelectMap(KfpbrknwPreferences, "KFPBRKNW")

    val outputDir = File("documentation/images")
    outputDir.mkdirs()

    // --- Brand banner ---
    @Suppress("DEPRECATION")
    captureScreen("banner.png", width = 420, height = 100) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource("pistons_0.png"),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryContainer)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TracQi",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    Text(
                        text = "ME7Tuner",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // --- Static screens (no log data needed) ---

    // Hero shot / KRKTE
    captureScreen("me7Tuner.png") {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.FUELING)
        ME7TunerApp(navState)
    }

    captureScreen("krkte.png") {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.FUELING)
        ME7TunerApp(navState)
    }

    // Configuration
    captureScreen("configuration.png") {
        val navState = NavigationState()
        navState.navigateTo(RailDestination.CONFIGURATION)
        ME7TunerApp(navState)
    }

    // PLSOL
    captureScreen("plsol.png") {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.PLSOL)
        ME7TunerApp(navState)
    }

    // KFMIOP
    captureScreen("kfmiop.png") {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.KFMIOP)
        ME7TunerApp(navState)
    }

    // KFMIRL
    captureScreen("kfmirl.png") {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.KFMIRL)
        ME7TunerApp(navState)
    }

    // KFZWOP
    captureScreen("kfzwop.png") {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.KFZWOP)
        ME7TunerApp(navState)
    }

    // KFZW
    captureScreen("kfzw.png") {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.KFZW)
        ME7TunerApp(navState)
    }

    // WDKUGDN
    captureScreen("wdkugdn.png") {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.WDKUGDN)
        ME7TunerApp(navState)
    }

    // LDRPID (no log data — renders in ready state)
    captureScreen("ldrpid.png") {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.LDRPID)
        ME7TunerApp(navState)
    }

    // --- Log-dependent screens ---

    // Closed Loop: load logs, then capture
    captureScreenWithLogData(
        filename = "closed_loop_mlhfm.png",
        loadData = { ClosedLoopLogParser.loadDirectory(File("example/")) }
    ) {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.CLOSED_LOOP)
        ME7TunerApp(navState)
    }

    // Open Loop: load ME7Logger + Zeitronix logs
    captureScreenWithLogData(
        filename = "open_loop_mlhfm.png",
        loadData = {
            OpenLoopLogParser.loadFile(File("example/me7logger.csv"))
            AfrLogParser.load(File("example/zeitronix.csv"))
        }
    ) {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.OPEN_LOOP)
        ME7TunerApp(navState)
    }

    // KFVPDKSD: load logs
    captureScreenWithLogData(
        filename = "kfvpdksd.png",
        loadData = {
            KfvpdksdLogParser.loadDirectory(File("example/")) { _, _ -> }
        }
    ) {
        val navState = NavigationState()
        navState.navigateToCalibration(CalibrationTab.KFVPDKSD)
        ME7TunerApp(navState)
    }

    println("All screenshots generated in documentation/images/")
}

private fun autoSelectMap(preference: MapPreference, keyword: String) {
    val match = BinParser.mapList.value.firstOrNull {
        it.first.tableName.contains(keyword, ignoreCase = true)
    }
    if (match != null) {
        preference.setSelectedMap(match.first)
        println("  Selected map: ${match.first.tableName}")
    } else {
        println("  WARNING: No map found for keyword '$keyword'")
    }
}

private fun captureScreen(
    filename: String,
    width: Int = 1480,
    height: Int = 1080,
    content: @Composable () -> Unit
) {
    print("Capturing $filename...")
    val scene = ImageComposeScene(width, height, density = Density(1f)) {
        ME7TunerTheme { content() }
    }
    // Render multiple frames to settle LaunchedEffects and collectAsState
    repeat(20) { scene.render(it * 16_000_000L) }
    val image = scene.render(320_000_000L)
    val data = image.encodeToData(EncodedImageFormat.PNG)
        ?: error("Failed to encode $filename to PNG")
    File("documentation/images/$filename").writeBytes(data.bytes)
    scene.close()
    println(" done")
}

private fun captureScreenWithLogData(
    filename: String,
    width: Int = 1480,
    height: Int = 1080,
    loadData: () -> Unit,
    content: @Composable () -> Unit
) {
    print("Capturing $filename (with log data)...")
    val scene = ImageComposeScene(width, height, density = Density(1f)) {
        ME7TunerTheme { content() }
    }
    // Initial render to start LaunchedEffect collectors
    repeat(5) { scene.render(it * 16_000_000L) }

    // Trigger log parsing (emits to SharedFlow)
    loadData()

    // Allow time for async parsing and state propagation
    Thread.sleep(2000)

    // Render many frames to let state propagate through compose tree
    repeat(30) { scene.render((it + 5) * 16_000_000L) }
    val image = scene.render(560_000_000L)
    val data = image.encodeToData(EncodedImageFormat.PNG)
        ?: error("Failed to encode $filename to PNG")
    File("documentation/images/$filename").writeBytes(data.bytes)
    scene.close()
    println(" done")
}
