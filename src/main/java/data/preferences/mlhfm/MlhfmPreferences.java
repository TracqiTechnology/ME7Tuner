package data.preferences.mlhfm;

import data.preferences.MapPreference;

public class MlhfmPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "mlfhm_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "mlfhm_description_preference";
    private static final String UNIT_PREFERENCE = "mlfhm_unit_preference";

    private static MlhfmPreferences instance;

    public static MlhfmPreferences getInstance() {
        if(instance == null) {
            instance = new MlhfmPreferences();
        }

        return instance;
    }

    private MlhfmPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }
}

