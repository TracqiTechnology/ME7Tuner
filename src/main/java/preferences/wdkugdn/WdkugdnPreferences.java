package preferences.wdkugdn;

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

public class WdkugdnPreferences {
    private static final String TITLE_PREFERENCE = "wdkugdn_title_preference";
    private static final String DESCRIPTION_PREFERENCE = "wdkugdn_description_preference";
    private static final String ENGINE_DISPLACEMENT_PREFERENCE = "engine_displacement_preference";

    private static final Preferences prefs = Preferences.userNodeForPackage(WdkugdnPreferences.class);
    private static final PublishSubject<Optional<Pair<TableDefinition, Map3d>>> publishSubject = PublishSubject.create();

    @Nullable
    public static Pair<TableDefinition, Map3d> getSelectedMap() {
        List<Pair<TableDefinition, Map3d>> mapList = BinParser.getInstance().getMapList();

        String mapTitle = WdkugdnPreferences.getTitlePreference();
        String mapDescription = WdkugdnPreferences.getDescriptionPreference();

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

    public static double getEngineDisplacementPreference() {
        return Double.parseDouble(prefs.get(ENGINE_DISPLACEMENT_PREFERENCE, String.valueOf(2.7)));
    }

    public static void setEngineDisplacementPreference(double engineDisplacementLiters) {
        prefs.put(ENGINE_DISPLACEMENT_PREFERENCE, String.valueOf(engineDisplacementLiters));
    }
}
