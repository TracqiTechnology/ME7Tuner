package ui.viewmodel.closedloopfueling;

import com.sun.tools.javac.util.Pair;
import contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import model.closedloopfueling.ClosedLoopFuelingCorrection;
import model.closedloopfueling.ClosedLoopFuelingCorrectionManager;
import parser.xdf.TableDefinition;
import preferences.closedloopfueling.ClosedLoopFuelingLogFilterPreferences;
import parser.me7log.ClosedLoopLogParser;
import preferences.mlhfm.MlhfmPreferences;

import java.util.List;
import java.util.Map;

public class ClosedLoopFuelingCorrectionViewModel {

    private final PublishSubject<ClosedLoopFuelingCorrection> publishSubject = PublishSubject.create();

    public ClosedLoopFuelingCorrectionViewModel() {
        ClosedLoopLogParser.getInstance().registerClosedLoopLogOnChangeObserver(new Observer<Map<Me7LogFileContract.Header, List<Double>>>() {
            @Override
            public void onNext(@NonNull Map<Me7LogFileContract.Header, List<Double>> me7LogMap) {
                Pair<TableDefinition, Map3d> mlhfmDefinition = MlhfmPreferences.getSelectedMap();
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

    public void registerMLHFMOnChange(Observer<ClosedLoopFuelingCorrection> observer) {
        publishSubject.subscribe(observer);
    }

    private void generateCorrection(Map<Me7LogFileContract.Header, List<Double>> me7LogMap, Map3d mlhfmMap) {

        ClosedLoopFuelingCorrectionManager closedLoopFuelingCorrectionManager = new ClosedLoopFuelingCorrectionManager(ClosedLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference(), ClosedLoopFuelingLogFilterPreferences.getMinRpmPreference(), ClosedLoopFuelingLogFilterPreferences.getMaxVoltageDtPreference());
        closedLoopFuelingCorrectionManager.correct(me7LogMap, mlhfmMap);
        ClosedLoopFuelingCorrection closedLoopFuelingCorrection = closedLoopFuelingCorrectionManager.getClosedLoopMlhfmCorrection();

        if (closedLoopFuelingCorrection != null) {
            publishSubject.onNext(closedLoopFuelingCorrection);
        }

    }
}
