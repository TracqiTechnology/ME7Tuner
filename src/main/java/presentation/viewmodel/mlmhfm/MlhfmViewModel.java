package presentation.viewmodel.mlmhfm;

import data.preferences.MapPreferenceManager;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import domain.math.map.Map3d;
import org.apache.commons.math3.util.Pair;
import data.parser.bin.BinParser;
import data.parser.xdf.TableDefinition;
import data.preferences.mlhfm.MlhfmPreferences;

import java.util.List;
import java.util.Optional;


public class MlhfmViewModel {
    private final BehaviorSubject<MlfhmModel> mlhfmPublishSubject = BehaviorSubject.create();

    public MlhfmViewModel() {
        BinParser.getInstance().registerMapListObserver(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                Pair<TableDefinition, Map3d> tableDefinition = MlhfmPreferences.getInstance().getSelectedMap();
                if (tableDefinition != null) {
                    mlhfmPublishSubject.onNext(new MlfhmModel(tableDefinition.getFirst(), tableDefinition.getSecond())); // Found the map
                } else {
                    mlhfmPublishSubject.onNext(new MlfhmModel(null, null)); // No map found
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        MlhfmPreferences.getInstance().registerOnMapChanged(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> selectedTableDefinitionPair) {
                // Found the map
                selectedTableDefinitionPair.ifPresent(tableDefinitionPair -> mlhfmPublishSubject.onNext(new MlfhmModel(tableDefinitionPair.getFirst(), tableDefinitionPair.getSecond())));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        MapPreferenceManager.registerOnClear(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Boolean aBoolean) {
                mlhfmPublishSubject.onNext(new MlfhmModel(null, null));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
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
