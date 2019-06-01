package ui.viewmodel.closedloopfueling.kfkhfm;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import model.closedloopfueling.kfkhfm.ClosedLoopKfkhfmCorrection;
import model.closedloopfueling.kfkhfm.ClosedLoopKfkhfmCorrectionManager;
import model.closedloopfueling.mlfhm.ClosedLoopMlhfmCorrection;
import model.closedloopfueling.mlfhm.ClosedLoopMlhfmCorrectionManager;
import preferences.closedloopfueling.ClosedLoopFuelingLogFilterPreferences;
import ui.viewmodel.MlhfmViewModel;
import ui.viewmodel.closedloopfueling.ClosedLoopFuelingMe7LogViewModel;

import java.util.List;
import java.util.Map;

public class ClosedLoopKfkhfmCorrectionViewModel {

    private static ClosedLoopKfkhfmCorrectionViewModel instance;

    private Map3d kfkhfm;
    private Map<String, List<Double>> me7LogMap;

    private PublishSubject<ClosedLoopKfkhfmCorrection> publishSubject;

    public static ClosedLoopKfkhfmCorrectionViewModel getInstance() {
        if (instance == null) {
            instance = new ClosedLoopKfkhfmCorrectionViewModel();
        }

        return instance;
    }

    private ClosedLoopKfkhfmCorrectionViewModel() {
        publishSubject = PublishSubject.create();

        ClosedLoopFuelingMe7LogViewModel closedLoopViewModel = ClosedLoopFuelingMe7LogViewModel.getInstance();
        closedLoopViewModel.getPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> me7LogMap) {
                ClosedLoopKfkhfmCorrectionViewModel.this.me7LogMap = me7LogMap;
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
        mlhfmViewModel.getMlhfmPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> mlhfmMap) {
                ClosedLoopKfkhfmCorrectionViewModel.this.mlhfmMap = mlhfmMap;
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
        if (me7LogMap != null && kfkhfm != null) {
            ClosedLoopKfkhfmCorrectionManager closedLoopKfkhfmCorrectionManager = new ClosedLoopKfkhfmCorrectionManager(ClosedLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference(), ClosedLoopFuelingLogFilterPreferences.getMinRpmPreference(), ClosedLoopFuelingLogFilterPreferences.getMaxVoltageDtPreference());
            ClosedLoopKfkhfmCorrection closedLoopKfkhfmCorrection = closedLoopKfkhfmCorrectionManager.correct(me7LogMap, kfkhfm);

            if (closedLoopKfkhfmCorrection != null) {
                publishSubject.onNext(closedLoopKfkhfmCorrection);
            }
        }
    }

    public PublishSubject<ClosedLoopKfkhfmCorrection> getPublishSubject() {
        return publishSubject;
    }
}
