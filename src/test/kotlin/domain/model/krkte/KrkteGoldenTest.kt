package domain.model.krkte

import kotlin.math.abs
import kotlin.test.*

/**
 * Golden-value tests for [KrkteCalculator].
 *
 * KRKTE is the primary fueling constant: ms per % engine load.
 * Formula: KRKTE = (ρ_air × V_cyl) / (100 × λ_stoich × Q_inj × ρ_fuel)
 *
 * Where:
 *   ρ_air  = air density (g/dm³)
 *   V_cyl  = cylinder displacement (dm³)
 *   Q_inj  = injector flow rate (cc/min)
 *   ρ_fuel = fuel density (g/cm³)
 *   λ_stoich = stoichiometric AFR
 *
 * These tests verify the formula against hand-calculated values.
 */
class KrkteGoldenTest {

    @Test
    fun `B5 S4 2_7T stock injectors KRKTE matches hand calculation`() {
        // B5 S4 2.7T M-box parameters:
        // 6 cylinders, 2671 cc total → 445.17 cc/cyl = 0.44517 dm³/cyl
        // Stock injectors: 309 cc/min at 3 bar
        // Air density: 1.293 g/dm³ (standard conditions)
        // Gasoline: 0.755 g/cm³
        // Stoich AFR: 14.7

        val krkte = KrkteCalculator.calculateKrkte(
            airDensityGramsPerDecimetersCubed = 1.293,
            cylinderDisplacementDecimetersCubed = 0.44517,
            fuelInjectorSizeCubicCentimeters = 309.0,
            gasolineGramsPerCubicCentimeter = 0.755,
            stoichiometricAirFuelRatio = 14.7
        )

        // Hand calculation:
        // KRKTE = (ρ_air × V_cyl) / (100 × MILLIS_PER_MINUTE × λ_stoich × Q_inj × ρ_fuel)
        // where MILLIS_PER_MINUTE = 1.6667e-5
        // KRKTE = (1.293 × 0.44517) / (100 × 1.6667e-5 × 14.7 × 309.0 × 0.755)
        val expected = (1.293 * 0.44517) / (100.0 * 1.6667e-5 * 14.7 * 309.0 * 0.755)
        assertEquals(expected, krkte, 1e-10,
            "KRKTE should match hand calculation exactly (pure formula, no rounding)")
    }

    @Test
    fun `RS4 B7 4_2L V8 injectors KRKTE matches hand calculation`() {
        // RS4 B7: 8 cylinders, 4163 cc → 520.375 cc/cyl = 0.520375 dm³
        // Injectors: 380 cc/min
        val krkte = KrkteCalculator.calculateKrkte(
            airDensityGramsPerDecimetersCubed = 1.293,
            cylinderDisplacementDecimetersCubed = 0.520375,
            fuelInjectorSizeCubicCentimeters = 380.0,
            gasolineGramsPerCubicCentimeter = 0.755,
            stoichiometricAirFuelRatio = 14.7
        )

        val expected = (1.293 * 0.520375) / (100.0 * 1.6667e-5 * 14.7 * 380.0 * 0.755)
        assertEquals(expected, krkte, 1e-10,
            "RS4 KRKTE should match hand calculation")
    }

    @Test
    fun `upgraded 630cc injectors produce proportionally smaller KRKTE`() {
        // When upgrading injectors from 309cc to 630cc, KRKTE should scale by 309/630
        val stock = KrkteCalculator.calculateKrkte(1.293, 0.44517, 309.0, 0.755, 14.7)
        val upgraded = KrkteCalculator.calculateKrkte(1.293, 0.44517, 630.0, 0.755, 14.7)

        val expectedRatio = 309.0 / 630.0
        val actualRatio = upgraded / stock

        assertTrue(abs(actualRatio - expectedRatio) < 1e-10,
            "Injector upgrade ratio should be exactly ${expectedRatio}: got $actualRatio")
    }

    @Test
    fun `E85 stoich AFR produces proportionally larger KRKTE`() {
        // E85 has lower stoich AFR (9.8 vs 14.7) → needs more fuel → larger KRKTE
        val gasoline = KrkteCalculator.calculateKrkte(1.293, 0.44517, 309.0, 0.755, 14.7)
        val e85 = KrkteCalculator.calculateKrkte(1.293, 0.44517, 309.0, 0.789, 9.8)

        assertTrue(e85 > gasoline,
            "E85 KRKTE ($e85) should be larger than gasoline ($gasoline) due to lower stoich AFR")

        // Verify the ratio makes physical sense:
        // E85 needs ~30% more fuel than gasoline at the same conditions
        val ratio = e85 / gasoline
        assertTrue(ratio > 1.0 && ratio < 3.0,
            "E85/gasoline KRKTE ratio ($ratio) should be between 1.0 and 3.0")
    }

    @Test
    fun `zero inputs are handled without crashing`() {
        // Division by zero scenarios — should produce Infinity, not crash
        val result = KrkteCalculator.calculateKrkte(1.293, 0.44517, 0.0, 0.755, 14.7)
        assertTrue(result.isInfinite() || result.isNaN(),
            "Zero injector size should produce Infinity or NaN, not a finite value: $result")
    }
}
