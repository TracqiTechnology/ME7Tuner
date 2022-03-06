package data.preferences;

import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.List;


public class MapPreferenceManager {
    private static final PublishSubject<Boolean> onClearSubject = PublishSubject.create();
    private static final List<MapPreference> mapPreferences = new ArrayList<>();

    public static void registerOnClear(Observer<Boolean> observer) {
        onClearSubject.subscribe(observer);
    }

    public static void add(MapPreference mapPreference) {
        mapPreferences.add(mapPreference);
    }

    public static void clear() {
        for(MapPreference mapPreference:mapPreferences) {
            mapPreference.clear();
        }

        onClearSubject.onNext(true);
    }
}
