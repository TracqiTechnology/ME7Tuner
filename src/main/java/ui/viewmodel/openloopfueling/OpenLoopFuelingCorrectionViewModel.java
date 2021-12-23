package ui.viewmodel.openloopfueling;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map2d;
import math.map.Map3d;
import model.openloopfueling.correction.OpenLoopMlhfmCorrection;
import model.openloopfueling.correction.OpenLoopMlhfmCorrectionManager;
import preferences.openloopfueling.OpenLoopFuelingLogFilterPreferences;

import java.util.List;
import java.util.Map;

public class OpenLoopFuelingCorrectionViewModel {

    private static OpenLoopFuelingCorrectionViewModel instance;

    private Map3d mlhfmMap;
    private Map<String, List<Double>> me7LogMap;
    private Map<String, List<Double>> afrLogMap;

    private PublishSubject<OpenLoopMlhfmCorrection> publishSubject;

    public static OpenLoopFuelingCorrectionViewModel getInstance() {
        if (instance == null) {
            instance = new OpenLoopFuelingCorrectionViewModel();
        }

        return instance;
    }

    private OpenLoopFuelingCorrectionViewModel() {
        publishSubject = PublishSubject.create();

        OpenLoopFuelingMe7LogViewModel.getInstance().getPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> me7LogMap) {
                OpenLoopFuelingCorrectionViewModel.this.me7LogMap = me7LogMap;
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

        OpenLoopFuelingAfrLogViewModel.getInstance().getPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> afrLogMap) {
                OpenLoopFuelingCorrectionViewModel.this.afrLogMap = afrLogMap;
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

//        MlhfmViewModel mlhfmViewModel = MlhfmViewModel.getInstance();
//        mlhfmViewModel.getMlhfmPublishSubject().subscribe(new Observer<Map2d>() {
//            @Override
//            public void onNext(Map2d mlhfmMap) {
//                OpenLoopFuelingCorrectionViewModel.this.mlhfmMap = mlhfmMap;
//                generateCorrection();
//            }
//
//            @Override
//            public void onSubscribe(Disposable disposable) {
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//            }
//
//            @Override
//            public void onComplete() {
//            }
//        });
    }

    private void generateCorrection() {
        if (me7LogMap != null && afrLogMap != null && mlhfmMap != null) {
            OpenLoopMlhfmCorrectionManager openLoopMlhfmCorrectionManager = new OpenLoopMlhfmCorrectionManager(OpenLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference(), OpenLoopFuelingLogFilterPreferences.getMinRpmPreference(), OpenLoopFuelingLogFilterPreferences.getMinMe7PointsPreference(), OpenLoopFuelingLogFilterPreferences.getMinAfrPointsPreference(), OpenLoopFuelingLogFilterPreferences.getMaxAfrPreference());
            openLoopMlhfmCorrectionManager.correct(me7LogMap, afrLogMap, mlhfmMap);
            OpenLoopMlhfmCorrection openLoopMlhfmCorrection = openLoopMlhfmCorrectionManager.getOpenLoopCorrection();

            if (openLoopMlhfmCorrection != null) {
                publishSubject.onNext(openLoopMlhfmCorrection);
            }
        }
    }

    public PublishSubject<OpenLoopMlhfmCorrection> getPublishSubject() {
        return publishSubject;
    }
}
