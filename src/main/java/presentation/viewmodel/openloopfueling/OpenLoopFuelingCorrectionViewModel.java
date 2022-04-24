package presentation.viewmodel.openloopfueling;

import data.contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import domain.math.map.Map3d;
import domain.model.openloopfueling.correction.OpenLoopMlhfmCorrection;
import domain.model.openloopfueling.correction.OpenLoopMlhfmCorrectionManager;
import org.apache.commons.math3.util.Pair;
import data.parser.afrLog.AfrLogParser;
import data.parser.bin.BinParser;
import data.parser.me7log.OpenLoopLogParser;
import data.parser.xdf.TableDefinition;
import data.preferences.mlhfm.MlhfmPreferences;
import data.preferences.openloopfueling.OpenLoopFuelingLogFilterPreferences;

import java.util.List;
import java.util.Map;

public class OpenLoopFuelingCorrectionViewModel {

    private Map3d mlhfmMap;
    private Map<Me7LogFileContract.Header, List<Double>> me7LogMap;
    private Map<String, List<Double>> afrLogMap;

    private final BehaviorSubject<OpenLoopMlhfmCorrection> publishSubject = BehaviorSubject.create();

    public OpenLoopFuelingCorrectionViewModel() {
        OpenLoopLogParser.getInstance().register(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Map<Me7LogFileContract.Header, List<Double>> logs) {
                OpenLoopFuelingCorrectionViewModel.this.me7LogMap = logs;
                generateCorrection();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
            }
        });

        AfrLogParser.getInstance().register(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Map<String, List<Double>> logs) {
                OpenLoopFuelingCorrectionViewModel.this.afrLogMap = logs;
                generateCorrection();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
            }
        });

        BinParser.getInstance().registerMapListObserver(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                Pair<TableDefinition, Map3d> tableDefinition = MlhfmPreferences.getInstance().getSelectedMap();
                if (tableDefinition != null) {
                    OpenLoopFuelingCorrectionViewModel.this.mlhfmMap = tableDefinition.getSecond();
                    generateCorrection();
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
            }
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
