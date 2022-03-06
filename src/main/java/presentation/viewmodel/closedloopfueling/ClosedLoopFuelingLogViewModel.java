package presentation.viewmodel.closedloopfueling;

import contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import org.apache.commons.math3.util.Pair;
import parser.me7log.ClosedLoopLogParser;
import parser.xdf.TableDefinition;
import preferences.mlhfm.MlhfmPreferences;
import writer.BinWriter;

import java.util.List;
import java.util.Map;

public class ClosedLoopFuelingLogViewModel {

    private final PublishSubject<ClosedLoopMlhfmLogModel> publishSubject = PublishSubject.create();

    public ClosedLoopFuelingLogViewModel() {
        ClosedLoopLogParser.getInstance().registerClosedLoopLogOnChangeObserver(new Observer<>() {
            @Override
            public void onNext(@NonNull Map<Me7LogFileContract.Header, List<Double>> me7LogMap) {
                Pair<TableDefinition, Map3d> mlhfmDefinition = MlhfmPreferences.getInstance().getSelectedMap();

                if (mlhfmDefinition != null) {
                    Map3d mlhfm = mlhfmDefinition.getValue();
                    if (mlhfm != null) {
                        publishSubject.onNext(new ClosedLoopMlhfmLogModel(me7LogMap, mlhfm));
                    }
                }
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        BinWriter.getInstance().register(new Observer<TableDefinition>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull TableDefinition tableDefinition) {
                if(tableDefinition.getTableName().contains("MLHFM")) {
                    publishSubject.onNext(new ClosedLoopMlhfmLogModel(null, null));
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() { }
        });
    }

    public void registerOnChange(Observer<ClosedLoopMlhfmLogModel> observer) {
        publishSubject.subscribe(observer);
    }

    public static class ClosedLoopMlhfmLogModel {
        private final Map<Me7LogFileContract.Header, List<Double>> me7LogMap;
        private final Map3d mlhfm;

        public ClosedLoopMlhfmLogModel(@Nullable Map<Me7LogFileContract.Header, List<Double>> me7LogMap, @Nullable Map3d mlhfm) {
            this.me7LogMap = me7LogMap;
            this.mlhfm = mlhfm;
        }

        @Nullable
        public Map<Me7LogFileContract.Header, List<Double>> getMe7LogMap() {
            return me7LogMap;
        }

        @Nullable
        public Map3d getMlhfm() {
            return mlhfm;
        }
    }
}
