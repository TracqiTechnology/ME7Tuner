package data.preferences.kfvpdksd;

import data.preferences.MapPreference;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.prefs.Preferences;

public class KfvpdksdPreferences extends MapPreference {
    private static final String TITLE_PREFERENCE = "kfvpdksd_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfvpdksd_description_preference";
    private static final String UNIT_PREFERENCE = "kfvpdksd_unit_preference";

    private static final String MAX_WASTEGATE_CRACKING_PRESSURE = "max_wastegate_cracking_pressure_preference";

    private static final String LAST_USED_DIRECTORY_KEY = "kfvpdksd_last_used_directory_key";

    private static KfvpdksdPreferences instance;

    private final Preferences prefs = Preferences.userNodeForPackage(KfvpdksdPreferences.class);

    public static KfvpdksdPreferences getInstance() {
        if(instance == null) {
            instance = new KfvpdksdPreferences();
        }

        return instance;
    }

    private KfvpdksdPreferences() {
        super(TITLE_PREFERENCE, DESCRIPTION_PREFERENCE, UNIT_PREFERENCE);
    }

    public double getMaxWasteGateCrackingPressurePreference() {
        return Double.parseDouble(prefs.get(MAX_WASTEGATE_CRACKING_PRESSURE, String.valueOf(8.82)));
    }

    public void setMaxWasteGateCrackingPressurePreference(double max) {
        prefs.put(MAX_WASTEGATE_CRACKING_PRESSURE, String.valueOf(max));
    }

    public File getDirectory() {
        return new File(prefs.get(LAST_USED_DIRECTORY_KEY, FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath()));
    }

    public void setDirectory(File file) {
        prefs.put(LAST_USED_DIRECTORY_KEY, file.getAbsolutePath());
    }
}

