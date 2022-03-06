package data.preferences.kfldrl;

import data.preferences.MapPreference;

public class KfldrlPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "kfldrl_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfldrl_description_preference";
    private static final String UNIT_PREFERENCE = "kfldrl_unit_preference";

    private static KfldrlPreferences instance;

    public static KfldrlPreferences getInstance() {
        if(instance == null) {
            instance = new KfldrlPreferences();
        }

        return instance;
    }

    private KfldrlPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }
}

