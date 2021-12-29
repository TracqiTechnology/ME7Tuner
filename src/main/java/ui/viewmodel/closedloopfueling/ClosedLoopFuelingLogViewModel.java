package ui.viewmodel.closedloopfueling;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import parser.me7log.ClosedLoopLogParser;
import parser.xdf.TableDefinition;
import preferences.mlhfm.MlhfmMapPreferences;
import writer.BinWriter;

import java.util.List;
import java.util.Map;

public class ClosedLoopFuelingLogViewModel {

    private final PublishSubject<ClosedLoopMlhfmLogModel> behaviorSubject = PublishSubject.create();

    public ClosedLoopFuelingLogViewModel() {
        ClosedLoopLogParser.getInstance().registerClosedLoopLogOnChangeObserver(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(@NonNull Map<String, List<Double>> me7LogMap) {
                Pair<TableDefinition, Map3d> mlhfmDefinition = MlhfmMapPreferences.getSelectedMlhfmTableDefinition();

                if(mlhfmDefinition != null) {
                    Map3d mlhfm = mlhfmDefinition.snd;
                    if (mlhfm != null) {
                        behaviorSubject.onNext(new ClosedLoopMlhfmLogModel(me7LogMap, mlhfm));
                    }
                }
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        BinWriter.getInstance().register(new Observer<TableDefinition>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull TableDefinition tableDefinition) {
                if(tableDefinition.getTableName().contains("MLHFM")) {
                    behaviorSubject.onNext(new ClosedLoopMlhfmLogModel(null, null));
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() { }
        });
    }

    public void registerMLHFMOnChange(Observer<ClosedLoopMlhfmLogModel> observer) {
        behaviorSubject.subscribe(observer);
    }

    public static class ClosedLoopMlhfmLogModel {
        private final Map<String, List<Double>> me7LogMap;
        private final Map3d mlhfm;

        public ClosedLoopMlhfmLogModel(@Nullable Map<String, List<Double>> me7LogMap, @Nullable Map3d mlhfm) {
            this.me7LogMap = me7LogMap;
            this.mlhfm = mlhfm;
        }

        @Nullable
        public Map<String, List<Double>> getMe7LogMap() {
            return me7LogMap;
        }

        @Nullable
        public Map3d getMlhfm() {
            return mlhfm;
        }
    }
}
