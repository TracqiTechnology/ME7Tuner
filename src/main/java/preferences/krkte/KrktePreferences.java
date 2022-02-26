package preferences.krkte;

import preferences.MapPreference;

public class KrktePreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "krkte_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "krkte_description_preference";
    private static final String UNIT_PREFERENCE = "krkte_unit_preference";

    private static KrktePreferences instance;

    public static KrktePreferences getInstance() {
        if(instance == null) {
            instance = new KrktePreferences();
        }

        return instance;
    }

    private KrktePreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }
}
