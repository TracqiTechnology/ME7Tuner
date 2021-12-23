package preferences.closedloopfueling;

import java.util.prefs.Preferences;

public class ClosedLoopFuelingLogFilterPreferences {

    private static final String MIN_THROTTLE_ANGLE_PREFERENCE = "min_throttle_angle_preference";
    private static final String MIN_RPM_PREFERENCE = "min_rpm_preference";
    private static final String MAX_VOLTAGE_DT_PREFERENCE = "max_voltage_dt_preference";

    private static final Preferences prefs = Preferences.userNodeForPackage(ClosedLoopFuelingLogFilterPreferences.class);

    public static double getMinThrottleAnglePreference() {
        return Double.parseDouble(prefs.get(MIN_THROTTLE_ANGLE_PREFERENCE, String.valueOf(0)));
    }

    public static void setMinThrottleAnglePreference(double minThrottleAngle) {
        prefs.put(MIN_THROTTLE_ANGLE_PREFERENCE, String.valueOf(minThrottleAngle));
    }

    public static double getMinRpmPreference() {
        return Double.parseDouble(prefs.get(MIN_RPM_PREFERENCE, String.valueOf(0)));
    }

    public static void setMinRpmPreference(double minRpm) {
        prefs.put(MIN_RPM_PREFERENCE, String.valueOf(minRpm));
    }

    public static double getMaxVoltageDtPreference() {
        return Double.parseDouble(prefs.get(MAX_VOLTAGE_DT_PREFERENCE, String.valueOf(1)));
    }

    public static void setMaxVoltageDtPreference(double maxDerivative) {
        prefs.put(MAX_VOLTAGE_DT_PREFERENCE, String.valueOf(maxDerivative));
    }
}
