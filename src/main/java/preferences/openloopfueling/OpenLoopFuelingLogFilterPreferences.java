package preferences.openloopfueling;

import java.util.prefs.Preferences;

public class OpenLoopFuelingLogFilterPreferences {

    private static final String MIN_THROTTLE_ANGLE_PREFERENCE = "min_throttle_angle_preference";
    private static final String MIN_RPM_PREFERENCE = "min_rpm_preference";
    private static final String MIN_ME7_POINTS_PREFERENCE = "min_me7_points_preference";
    private static final String MIN_AFR_POINTS = "min_afr_points_preference";
    private static final String MAX_AFR = "max_afr_preference";
    private static final String FUEL_INJECTOR_SIZE = "open_loop_fuel_injector_size_preference";
    private static final String FUEL_DENSITY = "fuel_density_open_loop_preference";
    private static final String NUM_FUEL_INJECTORS = "num_fuel_injectors_open_loop_preference";

    private static final Preferences prefs = Preferences.userNodeForPackage(OpenLoopFuelingLogFilterPreferences.class);

    public static double getMinThrottleAnglePreference() {
        return Double.parseDouble(prefs.get(MIN_THROTTLE_ANGLE_PREFERENCE, String.valueOf(80)));
    }

    public static void setMinThrottleAnglePreference(double minThrottleAngle) {
        prefs.put(MIN_THROTTLE_ANGLE_PREFERENCE, String.valueOf(minThrottleAngle));
    }

    public static double getMinRpmPreference() {
        return Double.parseDouble(prefs.get(MIN_RPM_PREFERENCE, String.valueOf(2000)));
    }

    public static void setMinRpmPreference(double minRpm) {
        prefs.put(MIN_RPM_PREFERENCE, String.valueOf(minRpm));
    }

    public static int getMinMe7PointsPreference() {
        return Integer.parseInt(prefs.get(MIN_ME7_POINTS_PREFERENCE, String.valueOf(75)));
    }

    public static void setMinMe7PointsPreference(int minPoints) {
        prefs.put(MIN_ME7_POINTS_PREFERENCE, String.valueOf(minPoints));
    }

    public static int getMinAfrPointsPreference() {
        return Integer.parseInt(prefs.get(MIN_AFR_POINTS, String.valueOf(150)));
    }

    public static void setMinAfrPointsPreference(int minPoints) {
        prefs.put(MIN_AFR_POINTS, String.valueOf(minPoints));
    }

    public static double getMaxAfrPreference() {
        return Double.parseDouble(prefs.get(MAX_AFR, String.valueOf(16)));
    }

    public static void setMaxAfrPreference(double maxAfr) {
        prefs.put(MAX_AFR, String.valueOf(maxAfr));
    }

    public static double getFuelInjectorSizePreference() {
        return Double.parseDouble(prefs.get(FUEL_INJECTOR_SIZE, String.valueOf(349)));
    }

    public static void setFuelInjectorSizePreference(double fuelInjectorSize) {
        prefs.put(FUEL_INJECTOR_SIZE, String.valueOf(fuelInjectorSize));
    }

    public static double getGasolineGramsPerCubicCentimeterPreference() {
        return Double.parseDouble(prefs.get((FUEL_DENSITY), String.valueOf(0.7)));
    }

    public static void setGasolineGramsPerCubicCentimeterPreference(double fuelDensity) {
        prefs.put(FUEL_DENSITY, String.valueOf(fuelDensity));
    }

    public static double getNumFuelInjectorsPreference() {
        return Double.parseDouble(prefs.get((NUM_FUEL_INJECTORS), String.valueOf(6)));
    }

    public static void setNumFuelInjectorsPreference(double numFuelInjectors) {
        prefs.put(NUM_FUEL_INJECTORS, String.valueOf(numFuelInjectors));
    }
}
