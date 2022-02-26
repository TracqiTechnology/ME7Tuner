package preferences;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.Nullable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import parser.bin.BinParser;
import parser.xdf.TableDefinition;

import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class MapPreference {
    private final String tableTitlePreference;
    private final String tableDescriptionPreference;
    private final String tableUnitPreference;
    private final Preferences prefs = Preferences.userNodeForPackage(MapPreference.class);
    private final PublishSubject<Optional<Pair<TableDefinition, Map3d>>> publishSubject = PublishSubject.create();

    public MapPreference(String tableTitlePreference, String tableDescriptionPreference, String tableUnitPreference) {
        this.tableTitlePreference = tableTitlePreference;
        this.tableDescriptionPreference = tableDescriptionPreference;
        this.tableUnitPreference = tableUnitPreference;
        MapPreferenceManager.add(this);
    }

    public void registerOnMapChanged(Observer<Optional<Pair<TableDefinition, Map3d>>> observer) {
        publishSubject.subscribe(observer);
    }

    public void clear() {
        try {
            prefs.clear();
            publishSubject.onNext(Optional.empty());
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public Pair<TableDefinition, Map3d> getSelectedMap() {
        List<Pair<TableDefinition, Map3d>> mapList = BinParser.getInstance().getMapList();

        String mapTitle = getTableTitlePreference();
        String mapDescription = getTableDescriptionPreference();
        String mapUnit = getTableUnitPreference();

        if (mapTitle.isEmpty() && mapDescription.isEmpty()) {
            return null;
        } else {
            for (Pair<TableDefinition, Map3d> map : mapList) {
                if (mapTitle.equals(map.fst.getTableName()) && mapDescription.equals(map.fst.getTableDescription()) && mapUnit.equals(map.fst.getZAxis().getUnit())) {
                    return map;
                }
            }
        }

        return null;
    }

    public void setSelectedMap(@Nullable TableDefinition tableDefinition) {
        if (tableDefinition != null) {
            setTableTitlePreference(tableDefinition.getTableName());
            setTableDescriptionPreference(tableDefinition.getTableDescription());
            setTableUnitPreference(tableDefinition.getZAxis().getUnit());
        } else {
            setTableTitlePreference("");
            setTableDescriptionPreference("");
            setTableUnitPreference("");
        }

        publishSubject.onNext(Optional.ofNullable(getSelectedMap()));
    }

    private String getTableTitlePreference() {
        return prefs.get(tableTitlePreference, "");
    }

    private String getTableDescriptionPreference() {
        return prefs.get(tableDescriptionPreference, "");
    }

    private String getTableUnitPreference() {
        return prefs.get(tableUnitPreference, "");
    }

    private void setTableTitlePreference(String tableTitlePreference) {
        prefs.put(this.tableTitlePreference, tableTitlePreference);
    }

    private void setTableDescriptionPreference(String tableDescriptionPreference) {
        prefs.put(this.tableDescriptionPreference, tableDescriptionPreference);
    }

    private void setTableUnitPreference(String tableUnitPreference) {
        prefs.put(this.tableUnitPreference, tableUnitPreference);
    }
}
