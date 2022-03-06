package presentation.viewmodel.openloopfueling;

import contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import model.airflow.AirflowEstimation;
import model.airflow.AirflowEstimationManager;
import org.apache.commons.math3.util.Pair;
import parser.afrLog.AfrLogParser;
import parser.bin.BinParser;
import parser.me7log.OpenLoopLogParser;
import parser.xdf.TableDefinition;
import preferences.mlhfm.MlhfmPreferences;
import preferences.openloopfueling.OpenLoopFuelingLogFilterPreferences;
import writer.BinWriter;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenLoopFuelingLogViewModel {

    private final BehaviorSubject<OpenLoopFuelingLogModel> behaviorSubject = BehaviorSubject.create();

    public OpenLoopFuelingLogViewModel() {
        OpenLoopLogParser.getInstance().register(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Map<Me7LogFileContract.Header, List<Double>> logs) {
                OpenLoopFuelingLogModel model = behaviorSubject.getValue();
                OpenLoopFuelingLogModel.Builder builder;
                if (model == null) {
                    builder = new OpenLoopFuelingLogModel.Builder();
                } else {
                    builder = new OpenLoopFuelingLogModel.Builder(model);
                }

                builder.me7Logs(logs);

                behaviorSubject.onNext(builder.build());
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        AfrLogParser.getInstance().register(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Map<String, List<Double>> logs) {
                OpenLoopFuelingLogModel model = behaviorSubject.getValue();
                OpenLoopFuelingLogModel.Builder builder;
                if (model == null) {
                    builder = new OpenLoopFuelingLogModel.Builder();
                } else {
                    builder = new OpenLoopFuelingLogModel.Builder(model);
                }

                builder.afrLogs(logs);
                behaviorSubject.onNext(generateAirflowEstimation(builder.build()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        BinParser.getInstance().registerMapListObserver(new Observer<List<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                Pair<TableDefinition, Map3d> tableDefinition = MlhfmPreferences.getInstance().getSelectedMap();
                if(tableDefinition != null) {
                    OpenLoopFuelingLogModel model = behaviorSubject.getValue();
                    OpenLoopFuelingLogModel.Builder builder;
                    if (model == null) {
                        builder = new OpenLoopFuelingLogModel.Builder();
                    } else {
                        builder = new OpenLoopFuelingLogModel.Builder(model);
                    }

                    builder.mlhfm(tableDefinition.getSecond());
                    behaviorSubject.onNext(generateAirflowEstimation(builder.build()));
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        BinWriter.getInstance().register(new Observer<TableDefinition>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull TableDefinition tableDefinition) {
                if(tableDefinition.getTableName().contains("MLHFM")) {
                    behaviorSubject.onNext(new OpenLoopFuelingLogModel.Builder().build());
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() { }
        });
    }

    public void register(Observer<OpenLoopFuelingLogModel> observer) {
        behaviorSubject.subscribe(observer);
    }

    public void loadMe7File(File file) {
        OpenLoopLogParser.getInstance().loadFile(file);
    }

    public void loadAfrFile(File file) {
        AfrLogParser.getInstance().loadFile(file);
    }

    private OpenLoopFuelingLogModel generateAirflowEstimation(OpenLoopFuelingLogModel model) {
        if (model.me7Logs != null && model.afrLogs != null) {
            AirflowEstimationManager airflowEstimationManager = new AirflowEstimationManager(OpenLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference(), OpenLoopFuelingLogFilterPreferences.getMinMe7PointsPreference(), OpenLoopFuelingLogFilterPreferences.getMinMe7PointsPreference(), OpenLoopFuelingLogFilterPreferences.getMinAfrPointsPreference(), OpenLoopFuelingLogFilterPreferences.getMaxAfrPreference(), OpenLoopFuelingLogFilterPreferences.getFuelInjectorSizePreference(), OpenLoopFuelingLogFilterPreferences.getNumFuelInjectorsPreference(), OpenLoopFuelingLogFilterPreferences.getGasolineGramsPerCubicCentimeterPreference());
            airflowEstimationManager.estimate(model.me7Logs, model.afrLogs);
            AirflowEstimation airflowEstimation = airflowEstimationManager.getAirflowEstimation();

            if (airflowEstimation != null) {
                OpenLoopFuelingLogModel.Builder builder = new OpenLoopFuelingLogModel.Builder(model);
                builder.airflowEstimation(airflowEstimation);

                return builder.build();
            }
        }

        return model;
    }

    public static class OpenLoopFuelingLogModel {
        private final Map<Me7LogFileContract.Header, List<Double>> me7Logs;
        private final Map<String, List<Double>> afrLogs;
        private final Map3d mlhfm;
        private final AirflowEstimation airflowEstimation;

        public OpenLoopFuelingLogModel(Builder builder) {
            this.me7Logs = builder.me7Logs;
            this.afrLogs = builder.afrLogs;
            this.mlhfm = builder.mlhfm;
            this.airflowEstimation = builder.airflowEstimation;
        }

        @Nullable
        public Map<Me7LogFileContract.Header, List<Double>> getMe7Logs() {
            return me7Logs;
        }

        public Map<String, List<Double>> getAfrLogs() {
            return afrLogs;
        }

        @Nullable
        public Map3d getMlhfm() {
            return mlhfm;
        }

        public AirflowEstimation getAirflowEstimation() {
            return airflowEstimation;
        }

        public static class Builder {
            private Map<Me7LogFileContract.Header, List<Double>> me7Logs;
            private Map<String, List<Double>> afrLogs;
            private Map3d mlhfm;
            private AirflowEstimation airflowEstimation;

            public Builder() {
            }

            Builder(OpenLoopFuelingLogModel model) {
                this.me7Logs = model.me7Logs;
                this.afrLogs = model.afrLogs;
                this.mlhfm = model.mlhfm;
                this.airflowEstimation = model.airflowEstimation;
            }

            public Builder me7Logs(Map<Me7LogFileContract.Header, List<Double>> me7Logs) {
                this.me7Logs = me7Logs;

                return this;
            }

            public Builder afrLogs(Map<String, List<Double>> afrLogs) {
                this.afrLogs = afrLogs;

                return this;
            }

            public Builder mlhfm(Map3d mlhfm) {
                this.mlhfm = mlhfm;

                return this;
            }

            public Builder airflowEstimation(AirflowEstimation airflowEstimation) {
                this.airflowEstimation = airflowEstimation;

                return this;
            }

            public OpenLoopFuelingLogModel build() {
                return new OpenLoopFuelingLogModel(this);
            }
        }
    }
}
