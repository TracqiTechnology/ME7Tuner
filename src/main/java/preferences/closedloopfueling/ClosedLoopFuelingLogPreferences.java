package preferences.closedloopfueling;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.prefs.Preferences;

public class ClosedLoopFuelingLogPreferences {

    private static final String MIN_THROTTLE_ANGLE_PREFERENCE = "min_throttle_angle_preference";
    private static final String MIN_RPM_PREFERENCE = "min_rpm_preference";
    private static final String MAX_VOLTAGE_DT_PREFERENCE = "max_voltage_dt_preference";

    private static final String LAST_USED_DIRECTORY_KEY = "closed_loop_last_used_directory_key";

    private static final Preferences prefs = Preferences.userNodeForPackage(ClosedLoopFuelingLogPreferences.class);

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

    public static File getDirectory() {
        return new File(prefs.get(LAST_USED_DIRECTORY_KEY, FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath()));
    }

    public static void setDirectory(File file) {
        prefs.put(LAST_USED_DIRECTORY_KEY, file.getAbsolutePath());
    }
}
