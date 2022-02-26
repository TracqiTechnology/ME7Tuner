package preferences.kfzwop;

import preferences.MapPreference;

public class KfzwopPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "kfzwop_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfzwop_description_preference";
    private static final String UNIT_PREFERENCE = "kfzwop_unit_preference";

    private static KfzwopPreferences instance;

    public static KfzwopPreferences getInstance() {
        if(instance == null) {
            instance = new KfzwopPreferences();
        }

        return instance;
    }

    private KfzwopPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }
}