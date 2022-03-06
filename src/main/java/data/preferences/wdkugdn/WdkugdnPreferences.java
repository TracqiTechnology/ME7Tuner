package data.preferences.wdkugdn;

import data.preferences.MapPreference;

import java.util.prefs.Preferences;

public class WdkugdnPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "wdkugdn_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "wdkugdn_description_preference";
    private static final String UNIT_PREFERENCE = "wdkugdn_unit_preference";

    private static final String ENGINE_DISPLACEMENT_PREFERENCE = "engine_displacement_preference";

    private final Preferences prefs = Preferences.userNodeForPackage(WdkugdnPreferences.class);

    private static WdkugdnPreferences instance;

    public static WdkugdnPreferences getInstance() {
        if(instance == null) {
            instance = new WdkugdnPreferences();
        }

        return instance;
    }

    private WdkugdnPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }

    public double getEngineDisplacementPreference() {
        return Double.parseDouble(prefs.get(ENGINE_DISPLACEMENT_PREFERENCE, String.valueOf(2.7)));
    }

    public void setEngineDisplacementPreference(double engineDisplacementLiters) {
        prefs.put(ENGINE_DISPLACEMENT_PREFERENCE, String.valueOf(engineDisplacementLiters));
    }
}
