package preferences.mlhfm;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.Nullable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import parser.bin.BinParser;
import parser.xdf.TableDefinition;

import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

public class MlhfmPreferences {
    private static final String MLHFM_TITLE_PREFERENCE = "mlfhm_title_preference";
    private static final String MLHFM_DESCRIPTION_PREFERENCE = "mlfhm_description_preference";

    private static final Preferences prefs = Preferences.userNodeForPackage(MlhfmPreferences.class);
    private static final PublishSubject<Optional<Pair<TableDefinition, Map3d>>> publishSubject = PublishSubject.create();

    @Nullable
    public static Pair<TableDefinition, Map3d> getSelectedMlhfmMap() {
        List<Pair<TableDefinition, Map3d>> mapList = BinParser.getInstance().getMapList();

        String mapTitle = MlhfmPreferences.getMlhfmTitlePreference();
        String mapDescription = MlhfmPreferences.getMlhfmDescriptionPreference();

        if(mapTitle.isEmpty() && mapDescription.isEmpty()) {
           return null;
        } else {
            for(Pair<TableDefinition, Map3d> map : mapList) {
                if(mapTitle.equals(map.fst.getTableName()) && mapDescription.equals(map.fst.getTableDescription())) {
                    return map;
                }
            }
        }

        return null;
    }

    public static void setSelectedMlhfmMap(@Nullable TableDefinition tableDefinition) {
        if(tableDefinition != null) {
            setMlhfmTitlePreference(tableDefinition.getTableName());
            setMlhfmDescriptionPreference(tableDefinition.getTableDescription());
        } else {
            setMlhfmTitlePreference("");
            setMlhfmDescriptionPreference("");
        }

        publishSubject.onNext(Optional.ofNullable(getSelectedMlhfmMap()));
    }

    public static void registerOnSelectedMlhfmChanged(Observer<Optional<Pair<TableDefinition, Map3d>>> observer) {
        publishSubject.subscribe(observer);
    }

    private static String getMlhfmTitlePreference() {
        return prefs.get(MLHFM_TITLE_PREFERENCE, "");
    }

    private static String getMlhfmDescriptionPreference() {
        return prefs.get(MLHFM_DESCRIPTION_PREFERENCE, "");
    }

    private static void setMlhfmTitlePreference(String title) {
        prefs.put(MLHFM_TITLE_PREFERENCE, title);
    }

    private static void setMlhfmDescriptionPreference(String description) {
        prefs.put(MLHFM_DESCRIPTION_PREFERENCE, description);
    }
}
