package domain.model.krkte

import kotlin.test.*

/**
 * Tests for [KrkteCalculator] — injector constant calculation from first principles.
 */
class KrkteCalculatorTest {

    @Test
    fun `calculateKrkte with RS3 2_5T parameters produces reasonable value`() {
        // RS3 2.5T profile: 5 cylinders, 2.48L → 0.496L per cylinder = 0.496 dm³
        // Port injectors: 220 cc/min @ 4.0 bar → flow rate in cc/min
        // Gasoline: 0.755 g/cm³, Stoich: 14.7
        val krkte = KrkteCalculator.calculateKrkte(
            airDensityGramsPerDecimetersCubed = 1.293,     // Standard air density
            cylinderDisplacementDecimetersCubed = 0.496,   // 496 cc = 0.496 dm³
            fuelInjectorSizeCubicCentimeters = 220.0,      // 220 cc/min PFI
            gasolineGramsPerCubicCentimeter = 0.755,
            stoichiometricAirFuelRatio = 14.7
        )

        // KRKTE is ms/% — typical values for port injection are 0.1–0.5 ms/%
        assertTrue(krkte > 0.0, "KRKTE should be positive")
        assertTrue(krkte in 0.01..2.0, "KRKTE $krkte should be in reasonable range 0.01–2.0 ms/%")
    }

    @Test
    fun `calculateKrkte larger injector produces smaller constant`() {
        // Larger injector flows more fuel per ms → needs less time per load % → smaller KRKTE
        val smallInjector = KrkteCalculator.calculateKrkte(
            airDensityGramsPerDecimetersCubed = 1.293,
            cylinderDisplacementDecimetersCubed = 0.496,
            fuelInjectorSizeCubicCentimeters = 220.0,
            gasolineGramsPerCubicCentimeter = 0.755,
            stoichiometricAirFuelRatio = 14.7
        )

        val largeInjector = KrkteCalculator.calculateKrkte(
            airDensityGramsPerDecimetersCubed = 1.293,
            cylinderDisplacementDecimetersCubed = 0.496,
            fuelInjectorSizeCubicCentimeters = 440.0,
            gasolineGramsPerCubicCentimeter = 0.755,
            stoichiometricAirFuelRatio = 14.7
        )

        assertTrue(largeInjector < smallInjector,
            "Larger injector ($largeInjector) should produce smaller KRKTE than smaller injector ($smallInjector)")
    }

    @Test
    fun `calculateKrkte larger displacement produces larger constant`() {
        // Larger cylinder needs more fuel for the same load % → larger KRKTE
        val small = KrkteCalculator.calculateKrkte(
            airDensityGramsPerDecimetersCubed = 1.293,
            cylinderDisplacementDecimetersCubed = 0.400,
            fuelInjectorSizeCubicCentimeters = 220.0,
            gasolineGramsPerCubicCentimeter = 0.755,
            stoichiometricAirFuelRatio = 14.7
        )

        val large = KrkteCalculator.calculateKrkte(
            airDensityGramsPerDecimetersCubed = 1.293,
            cylinderDisplacementDecimetersCubed = 0.600,
            fuelInjectorSizeCubicCentimeters = 220.0,
            gasolineGramsPerCubicCentimeter = 0.755,
            stoichiometricAirFuelRatio = 14.7
        )

        assertTrue(large > small,
            "Larger displacement ($large) should produce larger KRKTE than smaller ($small)")
    }

    @Test
    fun `calculateKrkte is deterministic`() {
        val result1 = KrkteCalculator.calculateKrkte(1.293, 0.496, 220.0, 0.755, 14.7)
        val result2 = KrkteCalculator.calculateKrkte(1.293, 0.496, 220.0, 0.755, 14.7)
        assertEquals(result1, result2, 0.0, "Same inputs should produce identical output")
    }
}
