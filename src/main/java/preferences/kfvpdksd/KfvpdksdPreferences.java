package preferences.kfvpdksd;

import preferences.MapPreference;

import java.util.prefs.Preferences;

public class KfvpdksdPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "kfvpdksd_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfvpdksd_description_preference";
    private static final String UNIT_PREFERENCE = "kfvpdksd_unit_preference";

    private static final String MAX_WASTEGATE_CRACKING_PRESSURE = "max_wastegate_cracking_pressure_preference";

    private static KfvpdksdPreferences instance;

    private final Preferences prefs = Preferences.userNodeForPackage(KfvpdksdPreferences.class);

    public static KfvpdksdPreferences getInstance() {
        if(instance == null) {
            instance = new KfvpdksdPreferences();
        }

        return instance;
    }

    private KfvpdksdPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }

    public double getMaxWasteGateCrackingPressurePreference() {
        return Double.parseDouble(prefs.get(MAX_WASTEGATE_CRACKING_PRESSURE, String.valueOf(8.82)));
    }

    public void setMaxWasteGateCrackingPressurePreference(double max) {
        prefs.put(MAX_WASTEGATE_CRACKING_PRESSURE, String.valueOf(max));
    }
}

