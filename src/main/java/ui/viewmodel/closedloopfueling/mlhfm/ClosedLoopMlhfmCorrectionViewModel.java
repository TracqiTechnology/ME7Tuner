package ui.viewmodel.closedloopfueling.mlhfm;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map2d;
import model.closedloopfueling.mlfhm.ClosedLoopMlhfmCorrection;
import model.closedloopfueling.mlfhm.ClosedLoopMlhfmCorrectionManager;
import preferences.closedloopfueling.ClosedLoopFuelingLogFilterPreferences;
import ui.viewmodel.mlmhfm.MlhfmViewModel;
import ui.viewmodel.closedloopfueling.ClosedLoopFuelingMe7LogViewModel;

import java.util.List;
import java.util.Map;

public class ClosedLoopMlhfmCorrectionViewModel {

    private static ClosedLoopMlhfmCorrectionViewModel instance;

    private Map2d mlhfmMap;
    private Map<String, List<Double>> me7LogMap;

    private PublishSubject<ClosedLoopMlhfmCorrection> publishSubject;

    public static ClosedLoopMlhfmCorrectionViewModel getInstance() {
        if (instance == null) {
            instance = new ClosedLoopMlhfmCorrectionViewModel();
        }

        return instance;
    }

    private ClosedLoopMlhfmCorrectionViewModel() {
        publishSubject = PublishSubject.create();

        ClosedLoopFuelingMe7LogViewModel closedLoopViewModel = ClosedLoopFuelingMe7LogViewModel.getInstance();
        closedLoopViewModel.getPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> me7LogMap) {
                ClosedLoopMlhfmCorrectionViewModel.this.me7LogMap = me7LogMap;
                generateCorrection();
            }

            @Override
            public void onSubscribe(Disposable disposable) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        MlhfmViewModel mlhfmViewModel = MlhfmViewModel.getInstance();
        mlhfmViewModel.getMlhfmPublishSubject().subscribe(new Observer<Map2d>() {
            @Override
            public void onNext(Map2d mlhfmMap) {
                ClosedLoopMlhfmCorrectionViewModel.this.mlhfmMap = mlhfmMap;
                generateCorrection();
            }

            @Override
            public void onSubscribe(Disposable disposable) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private void generateCorrection() {
        if (me7LogMap != null && mlhfmMap != null) {
            ClosedLoopMlhfmCorrectionManager closedLoopMlhfmCorrectionManager = new ClosedLoopMlhfmCorrectionManager(ClosedLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference(), ClosedLoopFuelingLogFilterPreferences.getMinRpmPreference(), ClosedLoopFuelingLogFilterPreferences.getMaxVoltageDtPreference());
            closedLoopMlhfmCorrectionManager.correct(me7LogMap, mlhfmMap);
            ClosedLoopMlhfmCorrection closedLoopMlhfmCorrection = closedLoopMlhfmCorrectionManager.getClosedLoopMlhfmCorrection();

            if (closedLoopMlhfmCorrection != null) {
                publishSubject.onNext(closedLoopMlhfmCorrection);
            }
        }
    }

    public PublishSubject<ClosedLoopMlhfmCorrection> getPublishSubject() {
        return publishSubject;
    }
}
