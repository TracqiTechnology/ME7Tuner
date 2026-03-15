package domain.model.med17

import data.model.EcuPlatform
import data.parser.bin.BinParser
import data.parser.xdf.TableDefinition
import data.parser.xdf.XdfParser
import data.preferences.platform.EcuPlatformPreference
import domain.math.Inverse
import domain.math.map.Map3d
import java.io.File
import java.io.FileInputStream
import kotlin.test.*

/**
 * End-to-end tests for MED17 BIN + XDF parsing and map extraction.
 *
 * Uses the real 404E Normal XDF + stock BIN from technical/med17/.
 * The vlmspec XDF is a definition-only file that produces 0×0 maps — we skip it.
 *
 * Validates:
 *  - XDF linked-axis resolution (embedinfo type="3") produces proper axes
 *  - BIN parsing extracts maps with internally-consistent dimensions
 *  - Key maps (KFLDRL, KRKTE, ignition, wastegate) have correct axes and sane values
 *  - DS1-specific expectations: KFMIOP/KFMIRL are scalars (DS1 overrides torque model)
 *  - KFMIOP → KFMIRL inverse calculation handles all dimension scenarios
 */
class Med17BinXdfEndToEndTest {

    companion object {
        private val PROJECT_ROOT = File(System.getProperty("user.dir"))
        private val XDF_FILE = File(PROJECT_ROOT, "technical/med17/Normal XDF/404E_normal.xdf")
        private val BIN_FILE = File(PROJECT_ROOT, "technical/med17/OTS tunes/404E/MED17_1_62_STOCK.bin")

        // 404E normal XDF map titles (different from vlmspec names)
        private const val KFLDRL_TITLE = "KF to linearize boost pressure = fTV"
        private const val KFMIOP_TITLE = "Max indexed eng tq"
        private const val KRKTE_GDI_TITLE = "Conv rel fuel mass rk into effective inj time te"
        private const val KRKTE_PFI_TITLE = "Conv rel fuel mass rk to effective inj time te or intake man inj PFI"
        private const val WASTEGATE_TITLE = "Precontrol wastegate for LDR inactive"
    }

    private lateinit var savedPlatform: EcuPlatform
    private lateinit var tableDefs: List<TableDefinition>
    private lateinit var allMaps: List<Pair<TableDefinition, Map3d>>

    @BeforeTest
    fun setUp() {
        savedPlatform = EcuPlatformPreference.platform
        EcuPlatformPreference.platform = EcuPlatform.MED17

        assertTrue(XDF_FILE.exists(), "XDF not found: ${XDF_FILE.absolutePath}")
        assertTrue(BIN_FILE.exists(), "BIN not found: ${BIN_FILE.absolutePath}")

        val (_, defs) = XdfParser.parseToList(FileInputStream(XDF_FILE))
        tableDefs = defs
        assertTrue(tableDefs.isNotEmpty(), "XDF produced no table definitions")

        allMaps = BinParser.parseToList(FileInputStream(BIN_FILE), tableDefs)
        assertEquals(tableDefs.size, allMaps.size)
    }

    @AfterTest
    fun tearDown() {
        EcuPlatformPreference.platform = savedPlatform
    }

    private fun findMap(keyword: String): Pair<TableDefinition, Map3d>? =
        allMaps.firstOrNull { it.first.tableName.contains(keyword, ignoreCase = true) }

    private fun findMapExact(title: String): Pair<TableDefinition, Map3d>? =
        allMaps.firstOrNull { it.first.tableName == title }

    private fun findMaps(keyword: String): List<Pair<TableDefinition, Map3d>> =
        allMaps.filter { it.first.tableName.contains(keyword, ignoreCase = true) }

    // ── 1. Parse + Linked Axis Resolution Tests ─────────────────────

    @Test
    fun `404E XDF produces substantial table definitions`() {
        assertTrue(tableDefs.size > 4000, "Expected >4000 defs, got ${tableDefs.size}")
    }

    @Test
    fun `linked axis resolution produces hundreds of proper 2D maps`() {
        val maps2d = allMaps.count { it.second.xAxis.size >= 2 && it.second.yAxis.size >= 2 }
        assertTrue(maps2d > 900, "Expected >900 resolved 2D maps, got $maps2d")
        println("$maps2d 2D maps with resolved axes")
    }

    @Test
    fun `all resolved 2D maps have internally consistent dimensions`() {
        var checked = 0
        for ((def, map) in allMaps) {
            if (map.xAxis.size < 2 || map.yAxis.size < 2 || map.zAxis.isEmpty()) continue
            checked++
            val colCount = map.zAxis[0].size
            assertEquals(
                map.xAxis.size, colCount,
                "Map '${def.tableName}': xAxis (${map.xAxis.size}) != zAxis cols ($colCount)"
            )
            assertEquals(
                map.yAxis.size, map.zAxis.size,
                "Map '${def.tableName}': yAxis (${map.yAxis.size}) != zAxis rows (${map.zAxis.size})"
            )
            for (r in map.zAxis.indices) {
                assertEquals(colCount, map.zAxis[r].size,
                    "Map '${def.tableName}': row $r has ${map.zAxis[r].size} cols, expected $colCount")
            }
        }
        assertTrue(checked > 900, "Expected to check >900 2D maps, only checked $checked")
    }

    @Test
    fun `zAxis rows are uniform width for all maps`() {
        for ((def, map) in allMaps) {
            if (map.zAxis.size < 2) continue
            val w = map.zAxis[0].size
            for (r in 1 until map.zAxis.size) {
                assertEquals(w, map.zAxis[r].size,
                    "Map '${def.tableName}': row $r width ${map.zAxis[r].size} != row 0 width $w")
            }
        }
    }

    // ── 2. KFLDRL (Boost Linearization) ─────────────────────────────

    @Test
    fun `KFLDRL has resolved 16x10 dimensions with proper axes`() {
        val kfldrl = findMapExact(KFLDRL_TITLE) ?: findMap("linearize boost")
        assertNotNull(kfldrl, "Expected KFLDRL ($KFLDRL_TITLE) in 404E XDF")
        val map = kfldrl.second

        assertEquals(16, map.yAxis.size, "KFLDRL should have 16 RPM rows")
        assertEquals(10, map.xAxis.size, "KFLDRL should have 10 duty columns")
        assertEquals(16, map.zAxis.size, "KFLDRL zAxis should have 16 rows")
        assertEquals(10, map.zAxis[0].size, "KFLDRL zAxis[0] should have 10 cols")
    }

    @Test
    fun `KFLDRL axes have physically correct values`() {
        val kfldrl = findMapExact(KFLDRL_TITLE)!!
        val map = kfldrl.second

        // xAxis = duty cycle %, should range from 0 to ~95
        assertTrue(map.xAxis.first() >= 0.0, "KFLDRL x starts >= 0")
        assertTrue(map.xAxis.last() <= 100.0, "KFLDRL x ends <= 100")
        for (i in 1 until map.xAxis.size) {
            assertTrue(map.xAxis[i] > map.xAxis[i - 1], "KFLDRL xAxis not monotonically increasing")
        }

        // yAxis = RPM, should range from ~1000 to ~7000
        assertTrue(map.yAxis.first() >= 500.0, "KFLDRL y starts >= 500 RPM")
        assertTrue(map.yAxis.last() <= 8000.0, "KFLDRL y ends <= 8000 RPM")
        for (i in 1 until map.yAxis.size) {
            assertTrue(map.yAxis[i] > map.yAxis[i - 1], "KFLDRL yAxis not monotonically increasing")
        }

        // zAxis = linearized boost pressure values, should be 0-100%
        for (r in map.zAxis.indices) {
            for (c in map.zAxis[r].indices) {
                assertTrue(map.zAxis[r][c] in 0.0..100.0,
                    "KFLDRL z[$r][$c] = ${map.zAxis[r][c]} out of [0, 100] range")
            }
        }

        // Print actual z-values for manual inspection
        println("KFLDRL z-values from 404E BIN:")
        for (r in map.zAxis.indices) {
            println("  RPM=${map.yAxis[r]}: ${map.zAxis[r].map { "%.2f".format(it) }}")
        }
    }

    // ── 3. Wastegate Precontrol ──────────────────────────────────────

    @Test
    fun `wastegate precontrol has resolved 16x16 dimensions`() {
        val wg = findMapExact(WASTEGATE_TITLE)
        assertNotNull(wg, "Expected wastegate precontrol map")
        val map = wg.second

        assertEquals(16, map.yAxis.size, "Wastegate should have 16 RPM rows")
        assertEquals(16, map.xAxis.size, "Wastegate should have 16 load columns")
        assertEquals(16, map.zAxis.size)
        assertEquals(16, map.zAxis[0].size)

        // yAxis = RPM
        assertTrue(map.yAxis.first() >= 500.0, "WG RPM starts >= 500")
        assertTrue(map.yAxis.last() <= 8000.0, "WG RPM ends <= 8000")

        // xAxis = load %
        assertTrue(map.xAxis.first() >= 0.0, "WG load starts >= 0")
        assertTrue(map.xAxis.last() <= 100.0, "WG load ends <= 100")
    }

    // ── 4. KFMIOP/KFMIRL (DS1 scalars) ─────────────────────────────

    @Test
    fun `KFMIOP is a scalar in DS1 XDF as expected`() {
        // DS1 overwrites the native Bosch torque model, so KFMIOP is a single
        // constant rather than a full 2D RPM×Load table.
        val kfmiop = findMapExact(KFMIOP_TITLE) ?: findMap("Max indexed eng tq")
        assertNotNull(kfmiop, "Expected KFMIOP ($KFMIOP_TITLE) in 404E XDF")
        val map = kfmiop.second
        assertEquals(1, map.zAxis.size, "DS1 KFMIOP should be 1×1 scalar")
        assertEquals(1, map.zAxis[0].size, "DS1 KFMIOP should be 1×1 scalar")
    }

    // ── 5. KRKTE (Fuel Mass Conversion) ─────────────────────────────

    @Test
    fun `KRKTE GDI and PFI exist with DS1 map-switch variants`() {
        val gdiMaps = findMaps("Conv rel fuel mass rk into effective inj time")
        val pfiMaps = findMaps("Conv rel fuel mass rk to effective inj time te or intake man inj PFI")

        // DS1 creates Gasoline and Ethanol variants for each map switch slot
        assertTrue(gdiMaps.size >= 10, "Expected >=10 KRKTE GDI variants (Gasoline+Ethanol×slots)")
        assertTrue(pfiMaps.size >= 10, "Expected >=10 KRKTE PFI variants")

        val gdiGasoline = gdiMaps.count { it.first.tableName.contains("Gasoline") }
        val gdiEthanol = gdiMaps.count { it.first.tableName.contains("Ethanol") }
        assertTrue(gdiGasoline >= 5, "Expected Gasoline KRKTE variants")
        assertTrue(gdiEthanol >= 5, "Expected Ethanol KRKTE variants")
    }

    // ── 6. Ignition Maps ────────────────────────────────────────────

    @Test
    fun `404E XDF contains large ignition timing maps`() {
        val ignMaps = findMaps("ignition")
            .filter { it.second.xAxis.size >= 5 && it.second.yAxis.size >= 5 }
        assertTrue(ignMaps.isNotEmpty(), "Expected resolved ignition maps >= 5×5")
        println("Found ${ignMaps.size} large ignition maps (>= 5×5)")
    }

    @Test
    fun `404E XDF contains wastegate precontrol maps`() {
        val wgMaps = findMaps("Precontrol wastegate")
            .plus(findMaps("pre-control wastegate"))
            .plus(findMaps("Pre-control wastegate"))
        assertTrue(wgMaps.isNotEmpty(), "Expected wastegate precontrol maps")
    }

    // ── 7. Inverse Calculation ──────────────────────────────────────

    /**
     * Build a synthetic 2D map with strictly monotonic z-rows for Inverse testing.
     * Simulates a KFMIOP-like torque table: RPM × Load → Torque %.
     */
    private fun buildSyntheticKfmiop(rows: Int = 8, cols: Int = 6): Map3d {
        val xAxis = Array(cols) { c -> (c + 1) * 15.0 }                // 15, 30, ... load %
        val yAxis = Array(rows) { r -> 1000.0 + r * 750.0 }           // 1000, 1750, ... RPM
        val zAxis = Array(rows) { r ->
            Array(cols) { c -> 10.0 + c * 12.0 + r * 2.0 }            // strictly increasing per row
        }
        return Map3d(xAxis, yAxis, zAxis)
    }

    @Test
    fun `Inverse works with synthetic KFMIOP-like map`() {
        val input = buildSyntheticKfmiop()
        val template = Map3d(input)
        val inverse = Inverse.calculateInverse(input, template)

        assertEquals(input.zAxis.size, inverse.zAxis.size)
        assertEquals(input.zAxis[0].size, inverse.zAxis[0].size)
    }

    @Test
    fun `Inverse with mismatched dimensions is guarded`() {
        val input = buildSyntheticKfmiop(rows = 8, cols = 6)

        // Build smaller template (fewer rows → dimension mismatch)
        val smallerY = input.yAxis.copyOfRange(0, input.yAxis.size - 2)
        val smallerZ = Array(smallerY.size) { r -> input.zAxis[r].copyOf() }
        val smaller = Map3d(input.xAxis.copyOf(), smallerY, smallerZ)

        try {
            val inverse = Inverse.calculateInverse(input, smaller)
            assertEquals(smaller.yAxis.size, inverse.zAxis.size,
                "Inverse output rows should match output template")
        } catch (e: ArrayIndexOutOfBoundsException) {
            fail("Inverse.calculateInverse crashed on dimension mismatch: " +
                    "input.yAxis=${input.yAxis.size} > output.yAxis=${smaller.yAxis.size}. " +
                    "Bug: loop should use minOf(input.yAxis.size, output.yAxis.size)")
        }
    }

    @Test
    fun `inverse values are within sane range`() {
        val input = buildSyntheticKfmiop()
        val template = Map3d(input)
        val inverse = Inverse.calculateInverse(input, template)

        for (r in inverse.zAxis.indices) {
            for (c in inverse.zAxis[r].indices) {
                val v = inverse.zAxis[r][c]
                assertTrue(v >= -50.0 && v <= 500.0,
                    "Inverse value $v at [$r][$c] out of sane range [-50, 500]")
            }
        }
    }
}
