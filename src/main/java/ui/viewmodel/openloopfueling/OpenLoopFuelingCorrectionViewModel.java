package ui.viewmodel.openloopfueling;

import com.sun.tools.javac.util.Pair;
import contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import model.openloopfueling.correction.OpenLoopMlhfmCorrection;
import model.openloopfueling.correction.OpenLoopMlhfmCorrectionManager;
import parser.afrLog.AfrLogParser;
import parser.bin.BinParser;
import parser.me7log.OpenLoopLogParser;
import parser.xdf.TableDefinition;
import preferences.mlhfm.MlhfmPreferences;
import preferences.openloopfueling.OpenLoopFuelingLogFilterPreferences;

import java.util.List;
import java.util.Map;

public class OpenLoopFuelingCorrectionViewModel {

    private Map3d mlhfmMap;
    private Map<Me7LogFileContract.Header, List<Double>> me7LogMap;
    private Map<String, List<Double>> afrLogMap;

    private final BehaviorSubject<OpenLoopMlhfmCorrection> publishSubject = BehaviorSubject.create();

    public OpenLoopFuelingCorrectionViewModel() {
        OpenLoopLogParser.getInstance().register(new Observer<Map<Me7LogFileContract.Header, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Map<Me7LogFileContract.Header, List<Double>> logs) {
                OpenLoopFuelingCorrectionViewModel.this.me7LogMap = logs;
                generateCorrection();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        AfrLogParser.getInstance().register(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Map<String, List<Double>> logs) {
                OpenLoopFuelingCorrectionViewModel.this.afrLogMap = logs;
                generateCorrection();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        BinParser.getInstance().registerMapListObserver(new Observer<List<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                Pair<TableDefinition, Map3d> tableDefinition = MlhfmPreferences.getSelectedMap();
                OpenLoopFuelingCorrectionViewModel.this.mlhfmMap  = tableDefinition.snd;
                generateCorrection();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public void register(Observer<OpenLoopMlhfmCorrection> observer) {
        publishSubject.subscribe(observer);
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
}
