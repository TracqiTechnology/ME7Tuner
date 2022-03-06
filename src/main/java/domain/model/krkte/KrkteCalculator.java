package domain.model.krkte;

public class KrkteCalculator {
    private static final double MILLIS_PER_MINUTE = 1.6667e-5;

    public static double calculateKrkte(double airDensityGramsPerDecimetersCubed, double cylinderDisplacementDecimetersCubed, double fuelInjectorSizeCubicCentimeters, double gasolineGramsPerCubicCentimter, double stoichiometricAirFuelRatio) {
        return (airDensityGramsPerDecimetersCubed*cylinderDisplacementDecimetersCubed)/(100*MILLIS_PER_MINUTE*stoichiometricAirFuelRatio*fuelInjectorSizeCubicCentimeters*gasolineGramsPerCubicCentimter);
    }
}
