package presentation.viewmodel.kfmirl;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import domain.math.Inverse;
import domain.math.map.Map3d;
import org.apache.commons.math3.util.Pair;
import data.parser.bin.BinParser;
import data.parser.xdf.TableDefinition;
import data.preferences.kfmiop.KfmiopPreferences;
import data.preferences.kfmirl.KfmirlPreferences;

import java.util.List;
import java.util.Optional;

public class KfmirlViewModel {
    private final BehaviorSubject<KfmirlModel> behaviorSubject = BehaviorSubject.create();

    public KfmirlViewModel() {
        BinParser.getInstance().registerMapListObserver(new Observer<List<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                updateModel();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        KfmirlPreferences.getInstance().registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                updateModel();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    public void calculateKfmirl(Map3d kfmiop) {
        Pair<TableDefinition, Map3d> kfmiopTableDefinition = KfmiopPreferences.getInstance().getSelectedMap();
        Pair<TableDefinition, Map3d> kfmirlTableDefinition = KfmirlPreferences.getInstance().getSelectedMap();
        if (kfmiopTableDefinition != null && kfmirlTableDefinition != null && kfmiop != null) {

            Map3d map3d = Inverse.calculateInverse(kfmiop, kfmirlTableDefinition.getSecond());

            // Don't change the first column
            for (int i = 0; i < map3d.zAxis.length; i++) {
                map3d.zAxis[i][0] = kfmirlTableDefinition.getSecond().zAxis[i][0];
            }

            behaviorSubject.onNext(new KfmirlModel(kfmiopTableDefinition, kfmirlTableDefinition, map3d)); // Found the map
        } else {
            behaviorSubject.onNext(new KfmirlModel(kfmiopTableDefinition, kfmirlTableDefinition, null)); // No map found
        }
    }

    public void register(Observer<KfmirlModel> observer) {
        behaviorSubject.subscribe(observer);
    }

    private void updateModel() {
        Pair<TableDefinition, Map3d> kfmiopTableDefinition = KfmiopPreferences.getInstance().getSelectedMap();
        Pair<TableDefinition, Map3d> kfmirlTableDefinition = KfmirlPreferences.getInstance().getSelectedMap();
        if (kfmiopTableDefinition != null && kfmirlTableDefinition != null) {
            calculateKfmirl(kfmiopTableDefinition.getSecond());
        } else if (kfmiopTableDefinition == null) {
            behaviorSubject.onNext(new KfmirlModel(null, kfmirlTableDefinition, null));
        } else {
            behaviorSubject.onNext(new KfmirlModel(kfmiopTableDefinition, null, null));
        }
    }

    public static class KfmirlModel {
        private final Pair<TableDefinition, Map3d> kfmiop;
        private final Pair<TableDefinition, Map3d> kfmirl;
        private final Map3d outputKfmirl;

        public KfmirlModel(@Nullable Pair<TableDefinition, Map3d> kfmiop, @Nullable Pair<TableDefinition, Map3d> kfmirl, Map3d outputKfmirl) {
            this.kfmiop = kfmiop;
            this.kfmirl = kfmirl;
            this.outputKfmirl = outputKfmirl;
        }

        @Nullable
        public Pair<TableDefinition, Map3d> getKfmirl() {
            return kfmirl;
        }

        @Nullable
        public Pair<TableDefinition, Map3d> getKfmiop() {
            return kfmiop;
        }

        @Nullable
        public Map3d getOutputKfmirl() {
            return outputKfmirl;
        }
    }
}
