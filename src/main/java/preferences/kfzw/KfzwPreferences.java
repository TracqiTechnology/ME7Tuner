package preferences.kfzw;

import preferences.MapPreference;

public class KfzwPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "kfzw_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfzw_description_preference";
    private static final String UNIT_PREFERENCE = "kfzw_unit_preference";

    private static KfzwPreferences instance;

    public static KfzwPreferences getInstance() {
        if(instance == null) {
            instance = new KfzwPreferences();
        }

        return instance;
    }

    private KfzwPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }
}
