package domain.model.krkte

object KrkteCalculator {
    private const val MILLIS_PER_MINUTE = 1.6667e-5

    fun calculateKrkte(
        airDensityGramsPerDecimetersCubed: Double,
        cylinderDisplacementDecimetersCubed: Double,
        fuelInjectorSizeCubicCentimeters: Double,
        gasolineGramsPerCubicCentimeter: Double,
        stoichiometricAirFuelRatio: Double
    ): Double {
        return (airDensityGramsPerDecimetersCubed * cylinderDisplacementDecimetersCubed) /
                (100 * MILLIS_PER_MINUTE * stoichiometricAirFuelRatio * fuelInjectorSizeCubicCentimeters * gasolineGramsPerCubicCentimeter)
    }
}
