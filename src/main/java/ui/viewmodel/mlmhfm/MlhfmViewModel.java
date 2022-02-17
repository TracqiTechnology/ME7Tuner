package ui.viewmodel.mlmhfm;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import parser.bin.BinParser;
import parser.xdf.TableDefinition;
import preferences.mlhfm.MlhfmPreferences;

import java.util.List;
import java.util.Optional;

public class MlhfmViewModel {
    private final BehaviorSubject<MlfhmModel> mlhfmPublishSubject = BehaviorSubject.create();

    public MlhfmViewModel() {
        BinParser.getInstance().registerMapListObserver(new Observer<List<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                Pair<TableDefinition, Map3d> tableDefinition = MlhfmPreferences.getSelectedMap();
                if(tableDefinition != null) {
                    mlhfmPublishSubject.onNext(new MlfhmModel(tableDefinition.fst, tableDefinition.snd)); // Found the map
                } else {
                    mlhfmPublishSubject.onNext(new MlfhmModel(null, null)); // No map found
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        MlhfmPreferences.registerOnSelectedMlhfmChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> selectedTableDefinitionPair) {
                // Found the map
                selectedTableDefinitionPair.ifPresent(tableDefinitionPair -> mlhfmPublishSubject.onNext(new MlfhmModel(tableDefinitionPair.fst, tableDefinitionPair.snd)));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public void register(Observer<MlfhmModel> observer) {
        mlhfmPublishSubject.subscribe(observer);
    }

    public static class MlfhmModel {
        private final TableDefinition tableDefinition;
        private final Map3d map3d;
        private final boolean mapSelected;

        public MlfhmModel(@Nullable TableDefinition tableDefinition, @Nullable Map3d map3d) {
            this.tableDefinition = tableDefinition;
            this.map3d = map3d;
            this.mapSelected = tableDefinition != null && map3d != null;
        }

        @Nullable
        public TableDefinition getTableDefinition() { return tableDefinition; }

        @Nullable
        public Map3d getMap3d() {
            return map3d;
        }

        public boolean isMapSelected() {
            return mapSelected;
        }
    }

}
