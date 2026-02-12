package data.preferences.xdf;

import io.reactivex.Observer;
import io.reactivex.subjects.BehaviorSubject;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class XdfFilePreferences {

    private static final String FILE_PATH_KEY = "file_path_key";
    private static final Preferences PREFERENCES = Preferences.userRoot().node(XdfFilePreferences.class.getName());
    private static volatile XdfFilePreferences instance;

    private final BehaviorSubject<File> behaviorSubject = BehaviorSubject.create();

    private XdfFilePreferences() {
        behaviorSubject.onNext(getFile());
    }

    public static void clear() {
        try {
            PREFERENCES.clear();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public static XdfFilePreferences getInstance() {
        if (instance == null) {
            synchronized (XdfFilePreferences.class) {
                if (instance == null) {
                    instance = new XdfFilePreferences();
                }
            }
        }

        return instance;
    }

    public void registerObserver(Observer<File> observer) {
        behaviorSubject.subscribe(observer);
    }

    public File getFile() {
        return new File(PREFERENCES.get(FILE_PATH_KEY, ""));
    }

    public void setFile(File file) {
        PREFERENCES.put(FILE_PATH_KEY, file.getAbsolutePath());
        behaviorSubject.onNext(file);
    }
}
