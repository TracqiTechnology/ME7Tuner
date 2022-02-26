package preferences.kfmirl;

import preferences.MapPreference;

public class KfmirlPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "kfmirl_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfmirl_description_preference";
    private static final String UNIT_PREFERENCE = "kfmirl_unit_preference";

    private static KfmirlPreferences instance;

    public static KfmirlPreferences getInstance() {
        if(instance == null) {
            instance = new KfmirlPreferences();
        }

        return instance;
    }

    private KfmirlPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }
}
