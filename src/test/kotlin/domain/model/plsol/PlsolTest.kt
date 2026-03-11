package domain.model.plsol

import kotlin.test.*

/**
 * Tests for [Plsol] — VE model forward path (pressure → load conversion).
 */
class PlsolTest {

    @Test
    fun `plsol companion method produces sane pressure at low load`() {
        // At low load (idle), pressure should be below barometric (vacuum)
        val pssol = Plsol.plsol(
            pu = 1013.0,      // Sea level baro (hPa)
            ps = 500.0,       // Previous manifold estimate
            tans = 25.0,      // Intake temp (°C)
            tmot = 90.0,      // Coolant temp (°C)
            kfurl = 0.1037,   // RS3 2.5T VE factor
            rlsol = 30.0      // Low load (%)
        )

        assertTrue(pssol > 0.0, "Pressure should be positive")
        assertTrue(pssol < 1013.0, "At low load, pressure should be below baro (vacuum)")
    }

    @Test
    fun `plsol companion method produces sane pressure at high load`() {
        // At high load (WOT boost), pressure should be above barometric
        val pssol = Plsol.plsol(
            pu = 1013.0,
            ps = 1500.0,
            tans = 25.0,
            tmot = 90.0,
            kfurl = 0.1037,
            rlsol = 150.0     // High load (boost)
        )

        assertTrue(pssol > 1013.0, "At high load (boost), pressure should exceed baro")
    }

    @Test
    fun `plsol pressure increases with load`() {
        val pressureLow = Plsol.plsol(1013.0, 500.0, 25.0, 90.0, 0.1037, 30.0)
        val pressureMid = Plsol.plsol(1013.0, 800.0, 25.0, 90.0, 0.1037, 80.0)
        val pressureHigh = Plsol.plsol(1013.0, 1500.0, 25.0, 90.0, 0.1037, 150.0)

        assertTrue(pressureMid > pressureLow,
            "Mid load pressure ($pressureMid) should exceed low load ($pressureLow)")
        assertTrue(pressureHigh > pressureMid,
            "High load pressure ($pressureHigh) should exceed mid load ($pressureMid)")
    }

    @Test
    fun `plsol constructor generates points array`() {
        val plsol = Plsol(
            pu = 1013.0,
            tans = 25.0,
            kfurl = 0.1037
        )

        assertEquals(400, plsol.points.size, "Default constructor generates 400 points")
        // Pressure should generally increase with load
        // Check overall trend: last point > first point
        assertTrue(plsol.points.last().y > plsol.points.first().y,
            "Pressure at high load should exceed pressure at low load")
    }

    @Test
    fun `plsol with explicit load range`() {
        val loads = listOf(20.0, 40.0, 60.0, 80.0, 100.0, 120.0, 150.0, 180.0)
        val plsol = Plsol(
            pu = 1013.0,
            tans = 25.0,
            kfurl = 0.1037,
            load = loads
        )

        assertEquals(loads.size, plsol.points.size, "Should have one point per load value")

        // Verify monotonically increasing
        for (i in 1 until plsol.points.size) {
            assertTrue(plsol.points[i].y > plsol.points[i - 1].y,
                "Pressure at load ${loads[i]} (${plsol.points[i].y}) should exceed at ${loads[i - 1]} (${plsol.points[i - 1].y})")
        }
    }

    @Test
    fun `plsol with MED17 typical kfurl value`() {
        // MED17 RS3 profile uses kfurl=0.1037 (static approximation of runtime fupsrls_w)
        val pssol = Plsol.plsol(
            pu = 1013.0,
            ps = 1200.0,
            tans = 25.0,
            tmot = 90.0,
            kfurl = 0.1037,
            rlsol = 100.0  // 100% load
        )

        // 100% load with residual gas correction produces above-atmospheric pressure
        assertTrue(pssol > 0.0, "Pressure should be positive")
        assertTrue(pssol in 800.0..2000.0,
            "At 100% load, pssol ($pssol) should be in reasonable range")
    }
}
