package ui.screens.med17

import data.model.EcuPlatform
import data.parser.bin.BinParser
import data.parser.xdf.TableDefinition
import data.parser.xdf.XdfParser
import data.preferences.bin.BinFilePreferences
import data.preferences.platform.EcuPlatformPreference
import domain.math.map.Map3d
import java.io.File
import java.io.FileInputStream
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertTrue

/**
 * Shared base for MED17 Compose UI screen tests.
 *
 * Parses the real 404E XDF + stock BIN, sets EcuPlatform to MED17,
 * and populates BinParser / XdfParser singletons so screens can render
 * with real data.
 */
abstract class Med17ScreenTestBase {

    companion object {
        private val PROJECT_ROOT = File(System.getProperty("user.dir"))
        val XDF_FILE = File(PROJECT_ROOT, "technical/med17/Normal XDF/404E_normal.xdf")
        val BIN_FILE = File(PROJECT_ROOT, "technical/med17/OTS tunes/404E/MED17_1_62_STOCK.bin")

        // 404E Normal XDF map titles
        const val KFMIOP_TITLE = "Max indexed eng tq"
        const val KFLDRL_TITLE = "KF to linearize boost pressure = fTV"
        const val KRKTE_GDI_TITLE = "Conv rel fuel mass rk into effective inj time te"
        const val KRKTE_PFI_TITLE = "Conv rel fuel mass rk to effective inj time te or intake man inj PFI"
        const val WASTEGATE_TITLE = "Precontrol wastegate for LDR inactive"
    }

    protected lateinit var savedPlatform: EcuPlatform
    protected lateinit var tableDefs: List<TableDefinition>
    protected lateinit var allMaps: List<Pair<TableDefinition, Map3d>>

    @BeforeTest
    open fun setUp() {
        savedPlatform = EcuPlatformPreference.platform
        EcuPlatformPreference.platform = EcuPlatform.MED17

        assertTrue(XDF_FILE.exists(), "XDF not found: ${XDF_FILE.absolutePath}")
        assertTrue(BIN_FILE.exists(), "BIN not found: ${BIN_FILE.absolutePath}")

        val (_, defs) = XdfParser.parseToList(FileInputStream(XDF_FILE))
        tableDefs = defs
        assertTrue(tableDefs.isNotEmpty(), "XDF produced no table definitions")

        allMaps = BinParser.parseToList(FileInputStream(BIN_FILE), tableDefs)

        // Populate singletons so screens can collect from them
        XdfParser.setTableDefinitionsForTesting(tableDefs)
        BinParser.setMapListForTesting(allMaps)

        // Set BIN file preference so screens see a "loaded" binary
        BinFilePreferences.setFile(BIN_FILE)
    }

    @AfterTest
    open fun tearDown() {
        EcuPlatformPreference.platform = savedPlatform
    }

    /** Find a map by exact table name. */
    protected fun findMap(title: String): Pair<TableDefinition, Map3d>? =
        allMaps.find { it.first.tableName == title }

    /** Find a map by partial title match (case-insensitive). */
    protected fun findMapContaining(keyword: String): Pair<TableDefinition, Map3d>? =
        allMaps.find { it.first.tableName.contains(keyword, ignoreCase = true) }

    /**
     * Find maps matching a keyword and return the largest one (by total cell count).
     * Useful for finding the "main" ignition or load table among several variants.
     */
    protected fun findLargestMapContaining(keyword: String): Pair<TableDefinition, Map3d>? =
        allMaps
            .filter { it.first.tableName.contains(keyword, ignoreCase = true) }
            .maxByOrNull { it.second.xAxis.size * it.second.yAxis.size }
}
