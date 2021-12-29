package preferences.primaryfueling;

import java.util.prefs.Preferences;

public class PrimaryFuelingPreferences {

    private static final String AIR_DENSITY_GRAMS_PER_CUBIC_DECIMETER = "air_density_grams_per_cubic_decimeter_preference";
    private static final String ENGINE_DISPLACEMENT_CUBIC_DECIMETER = "engine_displacement_cubic_decimeter_preference";
    private static final String ENGINE_NUM_CYLINDERS_PREFERENCE = "engine_num_cylinders_preference";
    private static final String STOICHIOMETRIC_AIR_FUEL_RATIO_PREFERENCE = "stoichiometric_air_fuel_ratio_preference";

    private static final String GASOLINE_GRAMS_PER_CUBIC_CENTIMETER = "gasoline_grams_per_cubic_centimeter_preference";
    private static final String FUEL_INJECTOR_SIZE_PREFERENCE = "fuel_injector_size_preference";
    private static final String NUM_FUEL_INJECTOR_PREFERENCE = "num_fuel_injector_preference";

    private static final Preferences prefs = Preferences.userNodeForPackage(PrimaryFuelingPreferences.class);

    public static double getAirDensityGramsPerCubicDecimeterPreference() {
        return Double.parseDouble(prefs.get(AIR_DENSITY_GRAMS_PER_CUBIC_DECIMETER, String.valueOf(1.293)));
    }

    public static double getEngineDisplacementCubicDecimeterPreference() {
        return Double.parseDouble(prefs.get(ENGINE_DISPLACEMENT_CUBIC_DECIMETER, String.valueOf(2.7)));
    }

    public static void setEngineDisplacementCubicDecimeterPreference(double engineDisplacementCubicDecimeter) {
        prefs.put(ENGINE_DISPLACEMENT_CUBIC_DECIMETER, String.valueOf(engineDisplacementCubicDecimeter));
    }

    public static int getNumEngineCylindersPreference() {
        return Integer.parseInt(prefs.get(ENGINE_NUM_CYLINDERS_PREFERENCE , String.valueOf(6)));
    }

    public static void setNumEngineCylindersPreference(int numEngineCylinders) {
        prefs.put(ENGINE_NUM_CYLINDERS_PREFERENCE , String.valueOf(numEngineCylinders));
    }

    public static double getStoichiometricAirFuelRatioPreference() {
        return Double.parseDouble(prefs.get(STOICHIOMETRIC_AIR_FUEL_RATIO_PREFERENCE, String.valueOf(14.7)));
    }

    public static void setStoichiometricAirFuelRatioPreference(double stoichiometricAirFuelRatio) {
        prefs.put(STOICHIOMETRIC_AIR_FUEL_RATIO_PREFERENCE, String.valueOf(stoichiometricAirFuelRatio));
    }

    public static double getGasolineGramsPerCubicCentimeterPreference() {
        return Double.parseDouble(prefs.get(GASOLINE_GRAMS_PER_CUBIC_CENTIMETER, String.valueOf(0.71)));
    }

    public static void setGasolineGramsPerCubicCentimeterPreference(double gramsPerCubicCentimeter) {
        prefs.put(GASOLINE_GRAMS_PER_CUBIC_CENTIMETER, String.valueOf(gramsPerCubicCentimeter));
    }

    public static int getFuelInjectorSizePreference() {
        return Integer.parseInt(prefs.get(FUEL_INJECTOR_SIZE_PREFERENCE, String.valueOf(349)));
    }

    public static void setFuelInjectorSizePreference(int fuelInjectorSizeCcMin) {
        prefs.put(FUEL_INJECTOR_SIZE_PREFERENCE, String.valueOf(fuelInjectorSizeCcMin));
    }

    public static int getNumFuelInjectorPreference() {
        return Integer.parseInt(prefs.get(NUM_FUEL_INJECTOR_PREFERENCE, String.valueOf(6)));
    }

    public static void setNumFuelInjectorPreference(int numFuelInjectorPreference) {
        prefs.put(NUM_FUEL_INJECTOR_PREFERENCE, String.valueOf(numFuelInjectorPreference));
    }

}
