package data.preferences.plsol;

import java.util.prefs.Preferences;

public class PlsolPreferences {

    private static final String BAROMETRIC_PRESSURE = "barometric_pressure_preference_key";
    private static final String INTAKE_AIR_TEMPERATURE = "intake_air_temperature_preference_key";
    private static final String KFURL = "plsol_kfurl_preference_key";
    private static final String DISPLACEMENT = "plsol_displacement_preference_key";
    private static final String RPM = "plsol_rpm_preference_key";

    private static final Preferences prefs = Preferences.userNodeForPackage(PlsolPreferences.class);

    public static double getBarometricPressure() {
        return Double.parseDouble(prefs.get(BAROMETRIC_PRESSURE, String.valueOf(1000)));
    }

    public static void setBarometricPressure(double pressure) {
        prefs.put(BAROMETRIC_PRESSURE, String.valueOf(pressure));
    }

    public static double getIntakeAirTemperature() {
        return Double.parseDouble(prefs.get(INTAKE_AIR_TEMPERATURE, String.valueOf(0)));
    }

    public static void setIntakeAirTemperature(double temperature) {
        prefs.put(INTAKE_AIR_TEMPERATURE, String.valueOf(temperature));
    }

    public static double getKfurl() {
        return Double.parseDouble(prefs.get(KFURL, String.valueOf(0.1016)));
    }

    public static void setKfurl(double kfurl) {
        prefs.put(KFURL, String.valueOf(kfurl));
    }

    public static double getDisplacement() {
        return Double.parseDouble(prefs.get(DISPLACEMENT, String.valueOf(2.7)));
    }

    public static void setDisplacement(double displacement) {
        prefs.put(DISPLACEMENT, String.valueOf(displacement));
    }

    public static int getRpm() {
        return Integer.parseInt(prefs.get(RPM, String.valueOf(6500)));
    }

    public static void setRpm(int rpm) {
        prefs.put(RPM, String.valueOf(rpm));
    }
}
