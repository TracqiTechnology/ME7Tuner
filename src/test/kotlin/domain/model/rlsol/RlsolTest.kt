package domain.model.rlsol

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Validates Rlsol.rlsol() produces physically sensible results for the
 * boost → load helper used in the DS1 scalar KFMIOP editor.
 */
class RlsolTest {

    @Test
    fun rlsolAtAtmosphericPressureGivesNearNaturallyAspiratedLoad() {
        // At 1013 mbar (atmospheric), load should be near 100% (naturally aspirated baseline)
        val load = Rlsol.rlsol(
            pu = 1013.0, ps = 1013.0,
            tans = 20.0, tmot = 90.0,
            kfurl = 0.106, plsol = 1013.0
        )
        assertTrue(load in 60.0..130.0, "At atmospheric pressure, load ≈ 75-100%, got $load")
    }

    @Test
    fun rlsolAtHighBoostGivesHighLoad() {
        // At 3500 mbar (~2.5 bar boost), load should be well above 300%
        val load = Rlsol.rlsol(
            pu = 1013.0, ps = 3500.0,
            tans = 20.0, tmot = 90.0,
            kfurl = 0.106, plsol = 3500.0
        )
        assertTrue(load > 300.0, "At 3500 mbar, load should be > 300%, got $load")
    }

    @Test
    fun rlsolIsMonotonicallyIncreasingWithPressure() {
        val pressures = listOf(1013.0, 1500.0, 2000.0, 2500.0, 3000.0, 3500.0, 4000.0, 5000.0)
        val loads = pressures.map { p ->
            Rlsol.rlsol(1013.0, p, 20.0, 90.0, 0.106, p)
        }
        for (i in 1 until loads.size) {
            assertTrue(
                loads[i] > loads[i - 1],
                "Load should increase with pressure: ${pressures[i - 1]}→${loads[i - 1]}, ${pressures[i]}→${loads[i]}"
            )
        }
    }

    @Test
    fun rlsolAt5BarGivesApprox500PercentLoad() {
        // Customer feedback: "OTS goes to 400 but we can see 450-470 with 5 bar limit"
        val load = Rlsol.rlsol(
            pu = 1013.0, ps = 5000.0,
            tans = 20.0, tmot = 90.0,
            kfurl = 0.106, plsol = 5000.0
        )
        assertTrue(load in 400.0..600.0, "At 5 bar, load should be ~450-500%, got $load")
    }
}
