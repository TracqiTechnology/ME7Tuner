package domain.model.plsol

import kotlin.test.*

/**
 * Tests for [Plsol] with MED17-typical parameters.
 *
 * MED17 RS3 2.5T logs show fupsrls_w ≈ 0.091 (measured at runtime),
 * which is lower than the ME7-typical kfurl = 0.1037.  A lower kfurl
 * means lower volumetric-efficiency conversion, so the ECU must command
 * higher manifold pressure for the same target load.
 */
class PlsolMed17Test {

    // ── MED17 constants ──────────────────────────────────────────────
    private val kfurlMed17 = 0.091          // fupsrls_w from RS3 2.5T logs
    private val kfurlMe7   = 0.1037         // traditional ME7 value
    private val pu         = 1013.0         // sea-level baro (hPa)
    private val tans       = 25.0           // intake air temp (°C)
    private val tmot       = 90.0           // coolant temp (°C, fully warm)

    // ── 1. Idle ──────────────────────────────────────────────────────

    @Test
    fun `MED17 kfurl 0_091 at idle produces vacuum`() {
        val pssol = Plsol.plsol(
            pu = pu, ps = 400.0, tans = tans, tmot = tmot,
            kfurl = kfurlMed17, rlsol = 20.0
        )
        assertTrue(pssol > 0.0, "Pressure should be positive")
        assertTrue(pssol < pu, "At idle load, pressure ($pssol) should be below baro ($pu)")
        assertTrue(pssol in 100.0..700.0,
            "Idle pressure ($pssol) should be in a reasonable vacuum range")
    }

    // ── 2. Part throttle ────────────────────────────────────────────

    @Test
    fun `MED17 kfurl 0_091 at part throttle is below baro but above idle`() {
        val pssolIdle = Plsol.plsol(pu, 400.0, tans, tmot, kfurlMed17, 20.0)
        val pssolPart = Plsol.plsol(pu, 700.0, tans, tmot, kfurlMed17, 60.0)

        assertTrue(pssolPart < pu,
            "At part throttle, pressure ($pssolPart) should still be below baro ($pu)")
        assertTrue(pssolPart > pssolIdle,
            "Part-throttle pressure ($pssolPart) should exceed idle ($pssolIdle)")
    }

    // ── 3. Full boost ───────────────────────────────────────────────

    @Test
    fun `MED17 kfurl 0_091 at full boost exceeds baro`() {
        val pssol = Plsol.plsol(
            pu = pu, ps = 2000.0, tans = tans, tmot = tmot,
            kfurl = kfurlMed17, rlsol = 180.0
        )
        assertTrue(pssol > pu,
            "At 180% load, pressure ($pssol) should exceed baro ($pu)")
        assertTrue(pssol in 1500.0..2500.0,
            "RS3 full-boost pressure ($pssol) should be in 1500–2500 hPa")
    }

    // ── 4. Monotonicity ─────────────────────────────────────────────

    @Test
    fun `MED17 pressure increases monotonically with load`() {
        val loads = listOf(20.0, 60.0, 100.0, 140.0, 180.0)
        var prevPs = pu
        val pressures = loads.map { rl ->
            val p = Plsol.plsol(pu, prevPs, tans, tmot, kfurlMed17, rl)
            prevPs = p
            p
        }

        for (i in 1 until pressures.size) {
            assertTrue(pressures[i] > pressures[i - 1],
                "Pressure at load ${loads[i]} (${pressures[i]}) " +
                "should exceed load ${loads[i - 1]} (${pressures[i - 1]})")
        }
    }

    // ── 5. Constructor with explicit load range ─────────────────────

    @Test
    fun `MED17 constructor with explicit loads`() {
        val loads = listOf(20.0, 60.0, 100.0, 140.0, 180.0, 220.0)
        val plsol = Plsol(pu = pu, tans = tans, kfurl = kfurlMed17, load = loads)

        assertEquals(loads.size, plsol.points.size,
            "Should produce one point per supplied load")

        for (i in 1 until plsol.points.size) {
            assertTrue(plsol.points[i].y > plsol.points[i - 1].y,
                "Pressure at load ${loads[i]} (${plsol.points[i].y}) " +
                "should exceed load ${loads[i - 1]} (${plsol.points[i - 1].y})")
        }
    }

    // ── 6. ME7 vs MED17 comparison ──────────────────────────────────

    @Test
    fun `lower kfurl produces higher pressure for same load`() {
        val rlsol = 100.0
        val ps    = 1000.0

        val pressureMe7   = Plsol.plsol(pu, ps, tans, tmot, kfurlMe7,   rlsol)
        val pressureMed17 = Plsol.plsol(pu, ps, tans, tmot, kfurlMed17, rlsol)

        assertTrue(pressureMed17 > pressureMe7,
            "MED17 pressure ($pressureMed17) should exceed ME7 ($pressureMe7) " +
            "at the same load because lower kfurl → lower VE conversion")
    }

    // ── 7. Altitude (Denver, pu≈850 hPa) ───────────────────────────

    @Test
    fun `MED17 at altitude shifts the full pressure curve`() {
        val puAltitude = 850.0
        val loads = listOf(20.0, 60.0, 100.0, 140.0, 180.0)

        val curveSea = Plsol(pu = pu,         tans = tans, kfurl = kfurlMed17, load = loads)
        val curveAlt = Plsol(pu = puAltitude,  tans = tans, kfurl = kfurlMed17, load = loads)

        // At altitude, pu is lower so the absolute pressure at high boost
        // differs from sea level.  The key observable: the crossover from
        // vacuum to positive boost happens at a different load.
        // Also the full-boost endpoint should differ.
        val seaMaxP = curveSea.points.last().y
        val altMaxP = curveAlt.points.last().y

        assertNotEquals(seaMaxP, altMaxP, 1.0,
            "Full-boost pressures should differ between sea level and altitude")

        // Both curves should still be monotonically increasing
        for (curve in listOf(curveSea, curveAlt)) {
            for (i in 1 until curve.points.size) {
                assertTrue(curve.points[i].y > curve.points[i - 1].y,
                    "Pressure should increase with load")
            }
        }
    }

    // ── 8. Hot intake air ───────────────────────────────────────────

    @Test
    fun `MED17 with hot intake produces higher pressure for same load`() {
        val rlsol    = 100.0
        val ps       = 1000.0
        val tansHot  = 45.0    // heat-soaked intake

        val pressureCool = Plsol.plsol(pu, ps, tans,    tmot, kfurlMed17, rlsol)
        val pressureHot  = Plsol.plsol(pu, ps, tansHot, tmot, kfurlMed17, rlsol)

        assertTrue(pressureHot > pressureCool,
            "Hot intake pressure ($pressureHot) should exceed cool ($pressureCool) " +
            "because hot air is less dense → higher ftbr denominator → " +
            "higher pressure needed for same load")
    }

    // ── 9. Crossover point (vacuum → boost) ─────────────────────────

    @Test
    fun `MED17 crossover from vacuum to boost occurs below 100 percent load`() {
        // With residual gas correction the crossover (pssol ≈ pu) happens
        // well below 100% load — around 60–80% for kfurl=0.091.
        val loads = (10..150).map { it.toDouble() }
        var prevPs = pu
        var crossoverLoad: Double? = null

        for (rl in loads) {
            val p = Plsol.plsol(pu, prevPs, tans, tmot, kfurlMed17, rl)
            if (crossoverLoad == null && p >= pu) {
                crossoverLoad = rl
            }
            prevPs = p
        }

        assertNotNull(crossoverLoad,
            "Should find a load where pressure crosses baro")
        assertTrue(crossoverLoad in 40.0..100.0,
            "Crossover load ($crossoverLoad%) should be below 100% for kfurl=0.091")
    }
}
