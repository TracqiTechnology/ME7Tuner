package ui.viewmodel.closedloopfueling;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import model.closedloopfueling.ClosedLoopFuelingCorrection;
import model.closedloopfueling.ClosedLoopFuelingCorrectionManager;
import preferences.closedloopfueling.ClosedLoopFuelingLogFilterPreferences;
import ui.viewmodel.MlhfmViewModel;

import java.util.List;
import java.util.Map;

public class ClosedLoopFuelingCorrectionViewModel {

    private static ClosedLoopFuelingCorrectionViewModel instance;

    private Map<String, List<Double>> mlhfmMap;
    private Map<String, List<Double>> me7LogMap;

    private PublishSubject<ClosedLoopFuelingCorrection> publishSubject;

    public static ClosedLoopFuelingCorrectionViewModel getInstance() {
        if (instance == null) {
            instance = new ClosedLoopFuelingCorrectionViewModel();
        }

        return instance;
    }

    private ClosedLoopFuelingCorrectionViewModel() {
        publishSubject = PublishSubject.create();

        ClosedLoopFuelingMe7LogViewModel closedLoopViewModel = ClosedLoopFuelingMe7LogViewModel.getInstance();
        closedLoopViewModel.getPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> me7LogMap) {
                ClosedLoopFuelingCorrectionViewModel.this.me7LogMap = me7LogMap;
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
                ClosedLoopFuelingCorrectionViewModel.this.mlhfmMap = mlhfmMap;
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
            ClosedLoopFuelingCorrectionManager closedLoopFuelingCorrectionManager = new ClosedLoopFuelingCorrectionManager(ClosedLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference(), ClosedLoopFuelingLogFilterPreferences.getMinRpmPreference(), ClosedLoopFuelingLogFilterPreferences.getMaxVoltageDtPreference());
            closedLoopFuelingCorrectionManager.correct(me7LogMap, mlhfmMap);
            ClosedLoopFuelingCorrection closedLoopFuelingCorrection = closedLoopFuelingCorrectionManager.getClosedLoopFuelingCorrection();

            if (closedLoopFuelingCorrection != null) {
                publishSubject.onNext(closedLoopFuelingCorrection);
            }
        }
    }

    public PublishSubject<ClosedLoopFuelingCorrection> getPublishSubject() {
        return publishSubject;
    }
}
