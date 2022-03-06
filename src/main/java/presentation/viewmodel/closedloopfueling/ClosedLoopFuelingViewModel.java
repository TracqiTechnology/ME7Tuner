package presentation.viewmodel.closedloopfueling;

import data.contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import domain.math.map.Map3d;
import org.apache.commons.math3.util.Pair;
import data.parser.bin.BinParser;
import data.parser.me7log.ClosedLoopLogParser;
import data.parser.xdf.TableDefinition;
import data.preferences.mlhfm.MlhfmPreferences;
import data.writer.BinWriter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClosedLoopFuelingViewModel {

    private final BehaviorSubject<ClosedLoopMlfhmModel> behaviorSubject = BehaviorSubject.create();

    public ClosedLoopFuelingViewModel() {

        BinWriter.getInstance().register(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull TableDefinition tableDefinition) {
                if (tableDefinition.getTableName().contains("MLHFM")) {

                    ClosedLoopMlfhmModel model = behaviorSubject.getValue();
                    ClosedLoopMlfhmModel.Builder builder;
                    if (model == null) {
                        builder = new ClosedLoopMlfhmModel.Builder();
                    } else {
                        builder = new ClosedLoopMlfhmModel.Builder(model);
                    }

                    builder.logsTabEnabled(true);
                    builder.correctionsTabEnabled(false);
                    builder.selectedTabIndex(0);

                    behaviorSubject.onNext(builder.build());
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
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
                ClosedLoopMlfhmModel model = behaviorSubject.getValue();
                ClosedLoopMlfhmModel.Builder builder;
                if (model == null) {
                    builder = new ClosedLoopMlfhmModel.Builder();
                } else {
                    builder = new ClosedLoopMlfhmModel.Builder(model);
                }

                builder.logsTabEnabled(tableDefinition != null);
                builder.selectedTabIndex(0);

                behaviorSubject.onNext(builder.build());
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        MlhfmPreferences.getInstance().registerOnMapChanged(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                ClosedLoopMlfhmModel model = behaviorSubject.getValue();
                if(tableDefinitionMap3dPair.isPresent()) {
                    ClosedLoopMlfhmModel.Builder builder;
                    if (model == null) {
                        builder = new ClosedLoopMlfhmModel.Builder();
                    } else {
                        builder = new ClosedLoopMlfhmModel.Builder(model);
                    }

                    builder.logsTabEnabled(true);
                    builder.correctionsTabEnabled(false);
                    builder.selectedTabIndex(0);

                    behaviorSubject.onNext(builder.build());
                } else {
                    ClosedLoopMlfhmModel.Builder builder;
                    if (model == null) {
                        builder = new ClosedLoopMlfhmModel.Builder();
                    } else {
                        builder = new ClosedLoopMlfhmModel.Builder(model);
                    }

                    builder.logsTabEnabled(false);
                    builder.correctionsTabEnabled(false);
                    builder.selectedTabIndex(0);

                    behaviorSubject.onNext(builder.build());
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        ClosedLoopLogParser.getInstance().registerClosedLoopLogOnChangeObserver(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Map<Me7LogFileContract.Header, List<Double>> stringListMap) {
                ClosedLoopMlfhmModel model = behaviorSubject.getValue();
                ClosedLoopMlfhmModel.Builder builder;
                if (model == null) {
                    builder = new ClosedLoopMlfhmModel.Builder();
                } else {
                    builder = new ClosedLoopMlfhmModel.Builder(model);
                }

                builder.correctionsTabEnabled(!stringListMap.isEmpty());
                builder.selectedTabIndex(1);

                behaviorSubject.onNext(builder.build()); // No map found
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    public void register(Observer<ClosedLoopMlfhmModel> observer) {
        behaviorSubject.subscribe(observer);
    }

    public static class ClosedLoopMlfhmModel {
        private final int selectedTabIndex;
        private final boolean logsTabEnabled;
        private final boolean correctionsTabEnabled;

        private ClosedLoopMlfhmModel(Builder builder) {
            this.selectedTabIndex = builder.selectedTabIndex;
           this.logsTabEnabled = builder.logsTabEnabled;
           this.correctionsTabEnabled = builder.correctionsTabEnabled;
        }

        public int getSelectedTabIndex() { return selectedTabIndex; }

        public boolean isLogsTabEnabled() {
            return logsTabEnabled;
        }

        public boolean isCorrectionsTabEnabled() {
            return correctionsTabEnabled;
        }

        public static class Builder {
            private int selectedTabIndex;
            private boolean logsTabEnabled;
            private boolean correctionsTabEnabled;

            public Builder() {}

            public Builder(ClosedLoopMlfhmModel model) {
                this.selectedTabIndex = model.selectedTabIndex;
                this.logsTabEnabled = model.logsTabEnabled;
                this.correctionsTabEnabled = model.correctionsTabEnabled;
            }

            public Builder selectedTabIndex(int selectedTabIndex) {
                this.selectedTabIndex = selectedTabIndex;

                return this;
            }

            public Builder logsTabEnabled(boolean enabled) {
                this.logsTabEnabled = enabled;

                return this;
            }

            public Builder correctionsTabEnabled(boolean enabled) {
                this.correctionsTabEnabled = enabled;

                return this;
            }

            public ClosedLoopMlfhmModel build() {
                return new ClosedLoopMlfhmModel(this);
            }
        }
    }
}
