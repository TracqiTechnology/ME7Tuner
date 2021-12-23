package ui.viewmodel.closedloopfueling.kfkhfm;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import model.closedloopfueling.kfkhfm.ClosedLoopKfkhfmCorrection;
import model.closedloopfueling.kfkhfm.ClosedLoopKfkhfmCorrectionManager;
import preferences.closedloopfueling.ClosedLoopFuelingLogFilterPreferences;
import ui.viewmodel.kfkhfm.KfkhfmViewModel;
import parser.me7log.ClosedLoopLogParser;

import java.util.List;
import java.util.Map;

public class ClosedLoopKfkhfmCorrectionViewModel {

    private static ClosedLoopKfkhfmCorrectionViewModel instance;

    private Map3d kfkhfm;
    private Map<String, List<Double>> me7LogMap;

    private final PublishSubject<ClosedLoopKfkhfmCorrection> publishSubject;

    public static ClosedLoopKfkhfmCorrectionViewModel getInstance() {
        if (instance == null) {
            instance = new ClosedLoopKfkhfmCorrectionViewModel();
        }

        return instance;
    }

    private ClosedLoopKfkhfmCorrectionViewModel() {
        publishSubject = PublishSubject.create();
        ClosedLoopLogParser.getInstance().registerClosedLoopLogOnChangeObserver(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(@NonNull Map<String, List<Double>> me7LogMap) {
                ClosedLoopKfkhfmCorrectionViewModel.this.me7LogMap = me7LogMap;
                generateCorrection();
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() { }
        });

        KfkhfmViewModel kfkhfmViewModel = KfkhfmViewModel.getInstance();
        kfkhfmViewModel.getKfkhfmBehaviorSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(@NonNull Map3d kfkhfm) {
                ClosedLoopKfkhfmCorrectionViewModel.this.kfkhfm = kfkhfm;
                generateCorrection();
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
