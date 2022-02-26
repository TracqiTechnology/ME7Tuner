package preferences.kfldimx;

import preferences.MapPreference;

public class KfldimxPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "kfldimx_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfldimx_description_preference";
    private static final String UNIT_PREFERENCE = "kfldimx_unit_preference";

    private static KfldimxPreferences instance;

    public static KfldimxPreferences getInstance() {
        if(instance == null) {
            instance = new KfldimxPreferences();
        }

        return instance;
    }

    private KfldimxPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }
}

