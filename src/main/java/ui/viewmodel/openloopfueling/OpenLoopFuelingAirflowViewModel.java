package ui.viewmodel.openloopfueling;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import model.airflow.AirflowEstimation;
import model.airflow.AirflowEstimationManager;
import preferences.openloopfueling.OpenLoopFuelingLogFilterPreferences;
import preferences.primaryfueling.PrimaryFuelingPreferences;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class OpenLoopFuelingAirflowViewModel {

    private static OpenLoopFuelingAirflowViewModel instance;

    private Map<String, List<Double>> me7LogMap;
    private Map<String, List<Double>> afrLogMap;

    private PublishSubject<AirflowEstimation> publishSubject;

    public static OpenLoopFuelingAirflowViewModel getInstance() {
        if (instance == null) {
            instance = new OpenLoopFuelingAirflowViewModel();
        }

        return instance;
    }

    private OpenLoopFuelingAirflowViewModel() {
        publishSubject = PublishSubject.create();

        OpenLoopFuelingMe7LogViewModel.getInstance().getPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> me7LogMap) {
                OpenLoopFuelingAirflowViewModel.this.me7LogMap = me7LogMap;
                generateAirflowEstimation();
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
                OpenLoopFuelingAirflowViewModel.this.afrLogMap = afrLogMap;
                generateAirflowEstimation();
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

    private void generateAirflowEstimation() {
        if (me7LogMap != null && afrLogMap != null) {
            AirflowEstimationManager airflowEstimationManager = new AirflowEstimationManager(OpenLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference(), OpenLoopFuelingLogFilterPreferences.getMinRpmPreference(), OpenLoopFuelingLogFilterPreferences.getMinMe7PointsPreference(), OpenLoopFuelingLogFilterPreferences.getMinAfrPointsPreference(), OpenLoopFuelingLogFilterPreferences.getMaxAfrPreference(), PrimaryFuelingPreferences.getFuelInjectorSizePreference(), PrimaryFuelingPreferences.getNumFuelInjectorPreference(), PrimaryFuelingPreferences.getMethanolNozzleSizePreference(), PrimaryFuelingPreferences.getNumMethanolNozzlePreference(), PrimaryFuelingPreferences.getGasolineGramsPerCubicCentimeterPreference(), PrimaryFuelingPreferences.getMethanolGramsPerCubicCentimeterPreference());
            airflowEstimationManager.estimate(me7LogMap, afrLogMap);
            AirflowEstimation airflowEstimation = airflowEstimationManager.getAirflowEstimation();

            if (airflowEstimation != null) {
                publishSubject.onNext(airflowEstimation);
            }
        }
    }

    public PublishSubject<AirflowEstimation> getPublishSubject() {
        return publishSubject;
    }
}
