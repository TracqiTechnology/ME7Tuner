package domain.model.optimizer

import domain.model.plsol.Plsol
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KfurlSolverTest {

    @Test
    fun `solveFromActuals returns default when inputs are empty`() {
        assertEquals(0.106, KfurlSolver.solveFromActuals(emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun `solveFromActuals recovers known KFURL from synthetic data`() {
        val knownKfurl = 0.120
        val baro = 1013.0

        // Generate synthetic WOT data: for various loads, compute pressure via Plsol
        val loads = (100..250 step 10).map { it.toDouble() }
        val pressures = loads.map { load ->
            Plsol.plsol(baro, 1500.0, 20.0, 96.0, knownKfurl, load)
        }
        val baros = List(loads.size) { baro }

        val solved = KfurlSolver.solveFromActuals(loads, pressures, baros)

        assertEquals(knownKfurl, solved, 0.002, "Solved KFURL should be within 0.002 of known value")
    }

    @Test
    fun `solveFromActuals returns value in search range`() {
        val baro = 1013.0
        val loads = listOf(120.0, 150.0, 180.0, 200.0)
        val pressures = listOf(1200.0, 1500.0, 1800.0, 2000.0)
        val baros = List(loads.size) { baro }

        val solved = KfurlSolver.solveFromActuals(loads, pressures, baros)

        assertTrue(solved in 0.050..0.200, "Solved KFURL ($solved) should be in default search range")
    }

    @Test
    fun `solveFromActuals handles mismatched list sizes`() {
        val solved = KfurlSolver.solveFromActuals(
            listOf(100.0, 150.0, 200.0),
            listOf(1200.0, 1500.0),
            listOf(1013.0)
        )
        assertTrue(solved in 0.050..0.200)
    }
}
