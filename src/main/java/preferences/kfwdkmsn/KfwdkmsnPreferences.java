package preferences.kfwdkmsn;

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

public class KfwdkmsnPreferences {
    private static final String TITLE_PREFERENCE = "kfmsnwdk_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "kfmsnwdk_description_preference";

    private static final Preferences prefs = Preferences.userNodeForPackage(KfwdkmsnPreferences.class);
    private static final PublishSubject<Optional<Pair<TableDefinition, Map3d>>> publishSubject = PublishSubject.create();

    @Nullable
    public static Pair<TableDefinition, Map3d> getSelectedMap() {
        List<Pair<TableDefinition, Map3d>> mapList = BinParser.getInstance().getMapList();

        String mapTitle = KfwdkmsnPreferences.getTitlePreference();
        String mapDescription = KfwdkmsnPreferences.getDescriptionPreference();

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

    public static void setSelectedMap(@Nullable TableDefinition tableDefinition) {
        if(tableDefinition != null) {
            setTitlePreference(tableDefinition.getTableName());
            setDescriptionPreference(tableDefinition.getTableDescription());
        } else {
            setTitlePreference("");
            setDescriptionPreference("");
        }

        publishSubject.onNext(Optional.ofNullable(getSelectedMap()));
    }

    public static void register(Observer<Optional<Pair<TableDefinition, Map3d>>> observer) {
        publishSubject.subscribe(observer);
    }

    private static String getTitlePreference() {
        return prefs.get(TITLE_PREFERENCE, "");
    }

    private static String getDescriptionPreference() {
        return prefs.get(DESCRIPTION_PREFERENCE, "");
    }

    private static void setTitlePreference(String title) {
        prefs.put(TITLE_PREFERENCE, title);
    }

    private static void setDescriptionPreference(String description) {
        prefs.put(DESCRIPTION_PREFERENCE, description);
    }
}
