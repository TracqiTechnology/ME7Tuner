package preferences.kfvpdksd;

import com.sun.tools.javac.util.Pair;
import io.reactivex.annotations.Nullable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import parser.bin.BinParser;
import parser.xdf.TableDefinition;

import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

public class KfvpdksdPreferences {

    private static final String TABLE_TITLE_PREFERENCE = "kfvpdksd_title_preference";
    private static final String TABLE_DESCRIPTION_PREFERENCE = "kfvpdksd_description_preference";
    private static final String MAX_WASTEGATE_CRACKING_PRESSURE = "max_wastegate_cracking_pressure_preference";

    private static final Preferences prefs = Preferences.userNodeForPackage(KfvpdksdPreferences.class);
    private static final PublishSubject<Optional<Pair<TableDefinition, Map3d>>> publishSubject = PublishSubject.create();

    @Nullable
    public static Pair<TableDefinition, Map3d> getSelectedMap() {
        List<Pair<TableDefinition, Map3d>> mapList = BinParser.getInstance().getMapList();

        String mapTitle = getTableTitlePreference();
        String mapDescription = getTableDescriptionPreference();

        if (mapTitle.isEmpty() && mapDescription.isEmpty()) {
            return null;
        } else {
            for (Pair<TableDefinition, Map3d> map : mapList) {
                if (mapTitle.equals(map.fst.getTableName()) && mapDescription.equals(map.fst.getTableDescription())) {
                    return map;
                }
            }
        }

        return null;
    }

    public static void setSelectedMap(@Nullable TableDefinition tableDefinition) {
        if (tableDefinition != null) {
            setTableTitlePreference(tableDefinition.getTableName());
            setTableDescriptionPreference(tableDefinition.getTableDescription());
        } else {
            setTableTitlePreference("");
            setTableDescriptionPreference("");
        }

        publishSubject.onNext(Optional.ofNullable(getSelectedMap()));
    }

    public static double getMaxWasteGateCrackingPressurePreference() {
        return Double.parseDouble(prefs.get(MAX_WASTEGATE_CRACKING_PRESSURE, String.valueOf(8.82)));
    }

    public static void setMaxWasteGateCrackingPressurePreference(double max) {
        prefs.put(MAX_WASTEGATE_CRACKING_PRESSURE, String.valueOf(max));
    }


    private static String getTableTitlePreference() {
        return prefs.get(TABLE_TITLE_PREFERENCE, "");
    }

    private static String getTableDescriptionPreference() {
        return prefs.get(TABLE_DESCRIPTION_PREFERENCE, "");
    }

    private static void setTableTitlePreference(String title) {
        prefs.put(TABLE_TITLE_PREFERENCE, title);
    }

    private static void setTableDescriptionPreference(String description) {
        prefs.put(TABLE_DESCRIPTION_PREFERENCE, description);
    }


}
