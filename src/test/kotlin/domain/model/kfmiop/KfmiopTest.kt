package domain.model.kfmiop

import data.model.EcuPlatform
import data.parser.bin.BinParser
import data.parser.xdf.TableDefinition
import data.parser.xdf.XdfParser
import data.preferences.platform.EcuPlatformPreference
import domain.math.map.Map3d
import java.io.File
import java.io.FileInputStream
import kotlin.test.*

/**
 * Tests for [Kfmiop.calculateKfmiop] — verifies correct rescaling output,
 * especially when the x-axis starts at 0 (MED17 load axes).
 */
class KfmiopTest {

    companion object {
        private val PROJECT_ROOT = File(System.getProperty("user.dir"))

        // 404A fixtures (x-axis starts at 0 — the bug trigger)
        private val XDF_404A = File(PROJECT_ROOT, "example/med17/404A/404A_normal.xdf")
        private val BIN_404A = File(PROJECT_ROOT, "example/med17/404A/MED17_1_62_STOCK.bin")

        // 404E fixtures (regression guard)
        private val XDF_404E = File(PROJECT_ROOT, "example/med17/404E/404E_normal.xdf")
        private val BIN_404E = File(PROJECT_ROOT, "example/med17/404E/MED17_1_62_STOCK.bin")

        private const val KFMIOP_TITLE = "Opt eng tq"
    }

    private lateinit var savedPlatform: EcuPlatform

    @BeforeTest
    fun setUp() {
        savedPlatform = EcuPlatformPreference.platform
        EcuPlatformPreference.platform = EcuPlatform.MED17
    }

    @AfterTest
    fun tearDown() {
        EcuPlatformPreference.platform = savedPlatform
    }

    private fun loadMap(xdfFile: File, binFile: File, title: String): Map3d? {
        assertTrue(xdfFile.exists(), "XDF not found: ${xdfFile.absolutePath}")
        assertTrue(binFile.exists(), "BIN not found: ${binFile.absolutePath}")
        val (_, defs) = XdfParser.parseToList(FileInputStream(xdfFile))
        val allMaps = BinParser.parseToList(FileInputStream(binFile), defs)
        return allMaps.firstOrNull { it.first.tableName == title }?.second
    }

    private fun assertNoNanOrInf(map: Map3d, label: String) {
        for (x in map.xAxis) {
            assertFalse(x.isNaN(), "$label xAxis contains NaN")
            assertFalse(x.isInfinite(), "$label xAxis contains Inf")
        }
        for (y in map.yAxis) {
            assertFalse(y.isNaN(), "$label yAxis contains NaN")
            assertFalse(y.isInfinite(), "$label yAxis contains Inf")
        }
        for (i in map.zAxis.indices) {
            for (j in map.zAxis[i].indices) {
                assertFalse(map.zAxis[i][j].isNaN(), "$label zAxis[$i][$j] is NaN")
                assertFalse(map.zAxis[i][j].isInfinite(), "$label zAxis[$i][$j] is Inf")
            }
        }
    }

    // ── 1a. Real fixture test: 404A (reproduces the exact bug) ──────

    @Test
    fun `404A KFMIOP rescaled output contains no NaN or Inf`() {
        val baseMap = loadMap(XDF_404A, BIN_404A, KFMIOP_TITLE)
        assertNotNull(baseMap, "404A '$KFMIOP_TITLE' map not found")
        assertTrue(baseMap.xAxis.isNotEmpty(), "xAxis should not be empty")

        // MED17 load axes start at 0 — this was the bug trigger
        assertEquals(0.0, baseMap.xAxis[0], "404A KFMIOP xAxis[0] should be 0")

        val result = Kfmiop.calculateKfmiop(baseMap, 3500.0, 2800.0)
        assertNotNull(result, "calculateKfmiop should not return null")

        assertNoNanOrInf(result.outputKfmiop, "outputKfmiop")
        assertNoNanOrInf(result.inputBoost, "inputBoost")
        assertNoNanOrInf(result.outputBoost, "outputBoost")

        // All output torque % values should be non-negative
        for (i in result.outputKfmiop.zAxis.indices) {
            for (j in result.outputKfmiop.zAxis[i].indices) {
                assertTrue(
                    result.outputKfmiop.zAxis[i][j] >= 0.0,
                    "outputKfmiop[$i][$j] should be non-negative, got ${result.outputKfmiop.zAxis[i][j]}"
                )
            }
        }
    }

    // ── 1b. Synthetic unit test: zero-start x-axis edge case ────────

    @Test
    fun `calculateKfmiop with xAxis starting at zero produces no NaN`() {
        // Small 2×3 map with xAxis[0] = 0
        val xAxis = arrayOf(0.0, 50.0, 100.0)
        val yAxis = arrayOf(1000.0, 2000.0)
        val zAxis = arrayOf(
            arrayOf(10.0, 50.0, 100.0),
            arrayOf(15.0, 60.0, 100.0)
        )
        val baseMap = Map3d(xAxis, yAxis, zAxis)

        val result = Kfmiop.calculateKfmiop(baseMap, 200.0, 150.0)
        assertNotNull(result)

        assertNoNanOrInf(result.outputKfmiop, "synthetic outputKfmiop")
        assertNoNanOrInf(result.inputBoost, "synthetic inputBoost")
        assertNoNanOrInf(result.outputBoost, "synthetic outputBoost")
    }

    // ── 1c. 404E regression guard ───────────────────────────────────

    @Test
    fun `404E KFMIOP rescaled output contains no NaN or Inf`() {
        val baseMap = loadMap(XDF_404E, BIN_404E, KFMIOP_TITLE)
        assertNotNull(baseMap, "404E '$KFMIOP_TITLE' map not found")

        val result = Kfmiop.calculateKfmiop(baseMap, 3500.0, 2800.0)
        assertNotNull(result, "calculateKfmiop should not return null")

        assertNoNanOrInf(result.outputKfmiop, "outputKfmiop")
        assertNoNanOrInf(result.inputBoost, "inputBoost")
        assertNoNanOrInf(result.outputBoost, "outputBoost")
    }
}
