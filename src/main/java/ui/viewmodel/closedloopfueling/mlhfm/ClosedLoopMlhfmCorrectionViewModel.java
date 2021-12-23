package ui.viewmodel.closedloopfueling.mlhfm;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map2d;
import math.map.Map3d;
import model.closedloopfueling.mlfhm.ClosedLoopMlhfmCorrection;
import model.closedloopfueling.mlfhm.ClosedLoopMlhfmCorrectionManager;
import parser.xdf.TableDefinition;
import preferences.closedloopfueling.ClosedLoopFuelingLogFilterPreferences;
import parser.me7log.ClosedLoopLogParser;
import preferences.mlhfm.MlhfmMapPreferences;
import ui.viewmodel.mlmhfm.MlhfmViewModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClosedLoopMlhfmCorrectionViewModel {

    private final PublishSubject<ClosedLoopMlhfmCorrection> publishSubject = PublishSubject.create();

    public ClosedLoopMlhfmCorrectionViewModel() {
        ClosedLoopLogParser.getInstance().registerClosedLoopLogOnChangeObserver(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(@NonNull Map<String, List<Double>> me7LogMap) {
                Pair<TableDefinition, Map3d> mlhfmDefinition = MlhfmMapPreferences.getSelectedMlhfmTableDefinition();
                if (mlhfmDefinition != null) {
                    Map3d mlhfm = mlhfmDefinition.snd;
                    if (mlhfm != null) {
                        generateCorrection(me7LogMap, mlhfm);
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
    }

    public void registerMLHFMOnChange(Observer<ClosedLoopMlhfmCorrection> observer) {
        publishSubject.subscribe(observer);
    }

    private void generateCorrection(Map<String, List<Double>> me7LogMap, Map3d mlhfmMap) {

        ClosedLoopMlhfmCorrectionManager closedLoopMlhfmCorrectionManager = new ClosedLoopMlhfmCorrectionManager(ClosedLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference(), ClosedLoopFuelingLogFilterPreferences.getMinRpmPreference(), ClosedLoopFuelingLogFilterPreferences.getMaxVoltageDtPreference());
        closedLoopMlhfmCorrectionManager.correct(me7LogMap, mlhfmMap);
        ClosedLoopMlhfmCorrection closedLoopMlhfmCorrection = closedLoopMlhfmCorrectionManager.getClosedLoopMlhfmCorrection();

        if (closedLoopMlhfmCorrection != null) {
            publishSubject.onNext(closedLoopMlhfmCorrection);
        }

    }
}
