package data.preferences.kfmiop;

import data.preferences.MapPreference;

import java.util.prefs.Preferences;

public class KfmiopPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "kfmiop_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfmiop_description_preference";
    private static final String UNIT_PREFERENCE = "kfmiop_unit_preference";

    private static final String MAX_MAP_PRESSURE_PREFERENCE = "max_map_pressure_preference";
    private static final String MAX_BOOST_PRESSURE_PREFERENCE = "max_boost_pressure_preference";

    private static KfmiopPreferences instance;

    private final Preferences prefs = Preferences.userNodeForPackage(KfmiopPreferences.class);

    public static KfmiopPreferences getInstance() {
        if(instance == null) {
            instance = new KfmiopPreferences();
        }

        return instance;
    }

    private KfmiopPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }

    public double getMaxMapPressurePreference() {
        return Double.parseDouble(prefs.get(MAX_MAP_PRESSURE_PREFERENCE, String.valueOf(2550)));
    }

    public void setMaxMapPressurePreference(double maxDesiredMap) {
        prefs.put(MAX_MAP_PRESSURE_PREFERENCE, String.valueOf(maxDesiredMap));
    }

    public double getMaxBoostPressurePreference() {
        return Double.parseDouble(prefs.get(MAX_BOOST_PRESSURE_PREFERENCE, String.valueOf(2100)));
    }

    public void setMaxBoostPressurePreference(double maxDesiredBoost) {
        prefs.put(MAX_BOOST_PRESSURE_PREFERENCE, String.valueOf(maxDesiredBoost));
    }
}
