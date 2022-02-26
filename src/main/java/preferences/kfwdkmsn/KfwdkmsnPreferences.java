package preferences.kfwdkmsn;

import preferences.MapPreference;

public class KfwdkmsnPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "kfmsnwdk_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfmsnwdk_description_preference";
    private static final String UNIT_PREFERENCE = "kfmsnwdk_unit_preference";

    private static KfwdkmsnPreferences instance;

    public static KfwdkmsnPreferences getInstance() {
        if(instance == null) {
            instance = new KfwdkmsnPreferences();
        }

        return instance;
    }

    private KfwdkmsnPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }
}
