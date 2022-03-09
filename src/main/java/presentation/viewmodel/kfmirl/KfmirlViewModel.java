package presentation.viewmodel.kfmirl;

import data.parser.bin.BinParser;
import data.parser.xdf.TableDefinition;
import data.preferences.MapPreferenceManager;
import data.preferences.kfmiop.KfmiopPreferences;
import data.preferences.kfmirl.KfmirlPreferences;
import domain.math.Inverse;
import domain.math.map.Map3d;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import org.apache.commons.math3.util.Pair;

import java.util.List;
import java.util.Optional;

public class KfmirlViewModel {
    private final Subject<KfmirlModel> subject = BehaviorSubject.create();

    public KfmirlViewModel() {
        BinParser.getInstance().registerMapListObserver(new Observer<>() {
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

        KfmirlPreferences.getInstance().registerOnMapChanged(new Observer<>() {
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

        MapPreferenceManager.registerOnClear(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Boolean aBoolean) {
                subject.onNext(new KfmirlModel(null, null, null)); // No map found
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

            subject.onNext(new KfmirlModel(kfmiopTableDefinition, kfmirlTableDefinition, map3d)); // Found the map
        } else {
            subject.onNext(new KfmirlModel(kfmiopTableDefinition, kfmirlTableDefinition, null)); // No map found
        }
    }

    public void register(Observer<KfmirlModel> observer) {
        subject.subscribe(observer);
    }

    private void updateModel() {
        Pair<TableDefinition, Map3d> kfmiopTableDefinition = KfmiopPreferences.getInstance().getSelectedMap();
        Pair<TableDefinition, Map3d> kfmirlTableDefinition = KfmirlPreferences.getInstance().getSelectedMap();
        if (kfmiopTableDefinition != null && kfmirlTableDefinition != null) {
            calculateKfmirl(kfmiopTableDefinition.getSecond());
        } else if (kfmiopTableDefinition == null) {
            subject.onNext(new KfmirlModel(null, kfmirlTableDefinition, null));
        } else {
            subject.onNext(new KfmirlModel(kfmiopTableDefinition, null, null));
        }
    }

    public record KfmirlModel(
            Pair<TableDefinition, Map3d> kfmiop,
            Pair<TableDefinition, Map3d> kfmirl,
            Map3d outputKfmirl) {

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
