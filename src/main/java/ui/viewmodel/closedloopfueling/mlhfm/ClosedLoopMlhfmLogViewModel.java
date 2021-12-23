package ui.viewmodel.closedloopfueling.mlhfm;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import parser.me7log.ClosedLoopLogParser;
import parser.xdf.TableDefinition;
import preferences.mlhfm.MlhfmMapPreferences;

import java.util.List;
import java.util.Map;

public class ClosedLoopMlhfmLogViewModel {

    private final PublishSubject<ClosedLoopMlhfmLogModel> behaviorSubject = PublishSubject.create();

    public ClosedLoopMlhfmLogViewModel() {

        ClosedLoopLogParser.getInstance().registerClosedLoopLogOnChangeObserver(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(@NonNull Map<String, List<Double>> me7LogMap) {
                System.out.println("Logs parsed");

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
    }

    public void registerMLHFMOnChange(Observer<ClosedLoopMlhfmLogModel> observer) {
        behaviorSubject.subscribe(observer);
    }

    public static class ClosedLoopMlhfmLogModel {
        private final Map<String, List<Double>> me7LogMap;
        private final Map3d mlhfm;

        public ClosedLoopMlhfmLogModel(Map<String, List<Double>> me7LogMap, Map3d mlhfm) {
            this.me7LogMap = me7LogMap;
            this.mlhfm = mlhfm;
        }

        public Map<String, List<Double>> getMe7LogMap() {
            return me7LogMap;
        }

        public Map3d getMlhfm() {
            return mlhfm;
        }
    }
}
