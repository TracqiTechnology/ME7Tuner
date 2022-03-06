package presentation.viewmodel.closedloopfueling;

import data.contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import domain.math.map.Map3d;
import domain.model.closedloopfueling.ClosedLoopFuelingCorrection;
import domain.model.closedloopfueling.ClosedLoopFuelingCorrectionManager;
import org.apache.commons.math3.util.Pair;
import data.parser.xdf.TableDefinition;
import data.preferences.closedloopfueling.ClosedLoopFuelingLogPreferences;
import data.parser.me7log.ClosedLoopLogParser;
import data.preferences.mlhfm.MlhfmPreferences;

import java.util.List;
import java.util.Map;

public class ClosedLoopFuelingCorrectionViewModel {

    private final PublishSubject<ClosedLoopFuelingCorrection> publishSubject = PublishSubject.create();

    public ClosedLoopFuelingCorrectionViewModel() {
        ClosedLoopLogParser.getInstance().registerClosedLoopLogOnChangeObserver(new Observer<Map<Me7LogFileContract.Header, List<Double>>>() {
            @Override
            public void onNext(@NonNull Map<Me7LogFileContract.Header, List<Double>> me7LogMap) {
                Pair<TableDefinition, Map3d> mlhfmDefinition = MlhfmPreferences.getInstance().getSelectedMap();
                if (mlhfmDefinition != null) {
                    Map3d mlhfm = mlhfmDefinition.getSecond();
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

        ClosedLoopFuelingCorrectionManager closedLoopFuelingCorrectionManager = new ClosedLoopFuelingCorrectionManager(ClosedLoopFuelingLogPreferences.getMinThrottleAnglePreference(), ClosedLoopFuelingLogPreferences.getMinRpmPreference(), ClosedLoopFuelingLogPreferences.getMaxVoltageDtPreference());
        closedLoopFuelingCorrectionManager.correct(me7LogMap, mlhfmMap);
        ClosedLoopFuelingCorrection closedLoopFuelingCorrection = closedLoopFuelingCorrectionManager.getClosedLoopMlhfmCorrection();

        if (closedLoopFuelingCorrection != null) {
            publishSubject.onNext(closedLoopFuelingCorrection);
        }

    }
}
