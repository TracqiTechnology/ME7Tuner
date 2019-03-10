package preferences.openloopfueling;

import java.util.prefs.Preferences;

public class OpenLoopFuelingLogFilterPreferences {

    private static final String MIN_THROTTLE_ANGLE_PREFERENCE = "min_throttle_angle_preference";
    private static final String MIN_RPM_PREFERENCE = "min_rpm_preference";
    private static final String MIN_ME7_POINTS_PREFERENCE = "min_me7_points_preference";
    private static final String MIN_AFR_POINTS = "min_afr_points_preference";
    private static final String MAX_AFR = "max_afr_preference";

    private static Preferences prefs = Preferences.userNodeForPackage(OpenLoopFuelingLogFilterPreferences.class);

    public static double getMinThrottleAnglePreference() {
        return Double.valueOf(prefs.get(MIN_THROTTLE_ANGLE_PREFERENCE, String.valueOf(80)));
    }

    public static void setMinThrottleAnglePreference(double minThrottleAngle) {
        prefs.put(MIN_THROTTLE_ANGLE_PREFERENCE, String.valueOf(minThrottleAngle));
    }

    public static double getMinRpmPreference() {
        return Double.valueOf(prefs.get(MIN_RPM_PREFERENCE, String.valueOf(2000)));
    }

    public static void setMinRpmPreference(double minRpm) {
        prefs.put(MIN_RPM_PREFERENCE, String.valueOf(minRpm));
    }

    public static int getMinMe7PointsPreference() {
        return Integer.valueOf(prefs.get(MIN_ME7_POINTS_PREFERENCE, String.valueOf(75)));
    }

    public static void setMinMe7PointsPreference(int minPoints) {
        prefs.put(MIN_ME7_POINTS_PREFERENCE, String.valueOf(minPoints));
    }

    public static int getMinAfrPointsPreference() {
        return Integer.valueOf(prefs.get(MIN_AFR_POINTS, String.valueOf(150)));
    }

    public static void setMinAfrPointsPreference(int minPoints) {
        prefs.put(MIN_AFR_POINTS, String.valueOf(minPoints));
    }

    public static double getMaxAfrPreference() {
        return Double.valueOf(prefs.get(MAX_AFR, String.valueOf(16)));
    }

    public static void setMaxAfrPreference(double maxAfr) {
        prefs.put(MAX_AFR, String.valueOf(maxAfr));
    }
}
