package ui.viewmodel.kfmirl;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.Inverse;
import math.map.Map3d;
import parser.bin.BinParser;
import parser.xdf.TableDefinition;
import preferences.kfmiop.KfmiopPreferences;
import preferences.kfmirl.KfmirlPreferences;

import java.util.Arrays;
import java.util.List;

public class KfmirlViewModel {
    private final BehaviorSubject<KfmirlModel> behaviorSubject = BehaviorSubject.create();

    public KfmirlViewModel() {
        BinParser.getInstance().registerMapListObserver(new Observer<List<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                Pair<TableDefinition, Map3d> kfmiopTableDefinition = KfmiopPreferences.getSelectedMap();
                if (kfmiopTableDefinition != null) {
                    calculateKfmirl(kfmiopTableDefinition.snd);
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {
            }
        });
    }

    public void calculateKfmirl(Map3d kfmiop) {
        Pair<TableDefinition, Map3d> kfmiopTableDefinition = KfmiopPreferences.getSelectedMap();
        Pair<TableDefinition, Map3d> kfmirlTableDefinition = KfmirlPreferences.getSelectedMap();
        if (kfmiopTableDefinition != null && kfmirlTableDefinition != null && kfmiop != null) {

            Map3d map3d = Inverse.calculateInverse(kfmiop, kfmirlTableDefinition.snd);

            // Don't change the first column
            for (int i = 0; i < map3d.zAxis.length; i++) {
                map3d.zAxis[i][0] = kfmirlTableDefinition.snd.zAxis[i][0];
            }

            behaviorSubject.onNext(new KfmirlModel(kfmiopTableDefinition, kfmirlTableDefinition, map3d)); // Found the map
        } else {
            behaviorSubject.onNext(new KfmirlModel(kfmiopTableDefinition, kfmirlTableDefinition, null)); // No map found
        }
    }

    public void register(Observer<KfmirlModel> observer) {
        behaviorSubject.subscribe(observer);
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
        public Pair<TableDefinition, Map3d> getKfmirl() { return kfmirl; }

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
