package domain.model.mlhfm

import domain.math.map.Map3d
import kotlin.math.abs
import kotlin.test.*

/**
 * Unit tests for [MlhfmFitter] — polynomial curve fitting for corrected MLHFM.
 *
 * The fitter takes a corrected (potentially noisy/non-monotonic) MLHFM and produces
 * a smooth polynomial fit. This is the last step before writing to the ECU, so it must:
 *   1. Preserve the general correction direction
 *   2. Produce a smooth, usable curve
 *   3. Not introduce negative values (polynomial overshoot)
 *   4. Handle edge cases gracefully
 */
class MlhfmFitterTest {

    private fun buildMlhfm(voltages: Array<Double>, kgH: Array<Double>): Map3d {
        val zAxis = Array(kgH.size) { i -> arrayOf(kgH[i]) }
        return Map3d(emptyArray(), voltages, zAxis)
    }

    @Test
    fun `fit of monotonic input remains monotonic`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0)
        val kgH = arrayOf(10.0, 30.0, 60.0, 100.0, 150.0, 210.0, 280.0, 360.0, 450.0, 550.0)
        val mlhfm = buildMlhfm(voltages, kgH)

        val fitted = MlhfmFitter.fitMlhfm(mlhfm, 6)

        assertEquals(voltages.size, fitted.zAxis.size, "Fitted size must match input")

        for (i in 1 until fitted.zAxis.size) {
            assertTrue(fitted.zAxis[i][0] >= fitted.zAxis[i - 1][0] - 0.5,
                "Fitted should remain monotonic: index ${i-1}=${fitted.zAxis[i-1][0]} > index $i=${fitted.zAxis[i][0]}")
        }
    }

    @Test
    fun `fit of slightly noisy input produces smooth output`() {
        // Simulate a corrected MLHFM with some noise
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0)
        val kgH = arrayOf(10.0, 32.0, 58.0, 105.0, 145.0, 215.0, 275.0, 365.0, 445.0, 555.0)
        val mlhfm = buildMlhfm(voltages, kgH)

        val fitted = MlhfmFitter.fitMlhfm(mlhfm, 6)

        // Fitted values should be close to input (noise is small)
        for (i in fitted.zAxis.indices) {
            val orig = kgH[i]
            val fit = fitted.zAxis[i][0]
            val deviation = abs(fit - orig)
            assertTrue(deviation < 30.0,
                "Fitted[$i] = $fit deviates $deviation from input $orig — fit too aggressive")
        }
    }

    @Test
    fun `fit does not produce negative values`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0)
        val kgH = arrayOf(5.0, 15.0, 35.0, 70.0, 120.0, 180.0, 250.0, 330.0, 420.0, 520.0)
        val mlhfm = buildMlhfm(voltages, kgH)

        val fitted = MlhfmFitter.fitMlhfm(mlhfm, 6)

        // The fitter uses maxOf(0.0, ...) so no values should be negative
        for (i in fitted.zAxis.indices) {
            assertTrue(fitted.zAxis[i][0] >= 0.0,
                "Fitted[$i] = ${fitted.zAxis[i][0]} is negative — polynomial overshoot not clamped")
        }
    }

    @Test
    fun `fit preserves voltage axis unchanged`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0)
        val kgH = arrayOf(10.0, 30.0, 60.0, 100.0, 150.0, 210.0, 280.0, 360.0, 450.0, 550.0)
        val mlhfm = buildMlhfm(voltages, kgH)

        val fitted = MlhfmFitter.fitMlhfm(mlhfm, 6)

        assertContentEquals(voltages, fitted.yAxis,
            "Fitter must not modify voltage axis")
    }

    @Test
    fun `fit output size matches input size`() {
        val voltages = arrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val kgH = arrayOf(20.0, 80.0, 180.0, 320.0, 500.0)
        val mlhfm = buildMlhfm(voltages, kgH)

        val fitted = MlhfmFitter.fitMlhfm(mlhfm, 4)

        assertEquals(voltages.size, fitted.zAxis.size,
            "Fitted output must have same number of points as input")
    }

    @Test
    fun `lower degree fit is smoother but may deviate more`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0)
        val kgH = arrayOf(10.0, 35.0, 55.0, 110.0, 140.0, 220.0, 270.0, 370.0, 440.0, 560.0)
        val mlhfm = buildMlhfm(voltages, kgH)

        val fitDeg3 = MlhfmFitter.fitMlhfm(mlhfm, 3)
        val fitDeg6 = MlhfmFitter.fitMlhfm(mlhfm, 6)

        // Higher degree should fit more closely
        var totalDevDeg3 = 0.0
        var totalDevDeg6 = 0.0
        for (i in kgH.indices) {
            totalDevDeg3 += abs(fitDeg3.zAxis[i][0] - kgH[i])
            totalDevDeg6 += abs(fitDeg6.zAxis[i][0] - kgH[i])
        }

        // Degree 6 should have less total deviation than degree 3
        assertTrue(totalDevDeg6 <= totalDevDeg3 + 10.0,
            "Degree 6 (total dev=$totalDevDeg6) should fit at least as well as degree 3 ($totalDevDeg3)")
    }

    @Test
    fun `fit of all-equal values produces flat output`() {
        val voltages = arrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val kgH = arrayOf(100.0, 100.0, 100.0, 100.0, 100.0)
        val mlhfm = buildMlhfm(voltages, kgH)

        val fitted = MlhfmFitter.fitMlhfm(mlhfm, 3)

        for (i in fitted.zAxis.indices) {
            assertTrue(abs(fitted.zAxis[i][0] - 100.0) < 1.0,
                "Fit of flat input should stay flat: fitted[$i] = ${fitted.zAxis[i][0]}")
        }
    }

    @Test
    fun `fit does not produce NaN or Infinity`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0)
        val kgH = arrayOf(10.0, 30.0, 60.0, 100.0, 150.0, 210.0, 280.0, 360.0, 450.0, 550.0)
        val mlhfm = buildMlhfm(voltages, kgH)

        val fitted = MlhfmFitter.fitMlhfm(mlhfm, 6)

        for (i in fitted.zAxis.indices) {
            assertFalse(fitted.zAxis[i][0].isNaN(), "Fitted[$i] is NaN")
            assertFalse(fitted.zAxis[i][0].isInfinite(), "Fitted[$i] is Infinite")
        }
    }
}
