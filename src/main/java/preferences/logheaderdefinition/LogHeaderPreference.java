package preferences.logheaderdefinition;


import contract.Me7LogFileContract;
import io.reactivex.annotations.NonNull;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class LogHeaderPreference {

    private static final Preferences prefs = Preferences.userNodeForPackage(LogHeaderPreference.class);
    private final Me7LogFileContract.Header header;
    private final String defaultValue;

    public LogHeaderPreference(Me7LogFileContract.Header header, String defaultValue) {
        this.header = header;
        this.defaultValue = defaultValue;
        this.header.setHeader(getHeader());
    }

    public static void clear() {
        try {
            prefs.clear();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    public String getHeader() {
        return prefs.get(header.name(), defaultValue);
    }

    public void setHeader(@NonNull String headerLabel) {
        prefs.put(header.name(), headerLabel);
        header.setHeader(headerLabel);
    }
}
