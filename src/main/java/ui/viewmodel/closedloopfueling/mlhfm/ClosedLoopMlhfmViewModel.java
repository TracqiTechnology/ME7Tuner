package ui.viewmodel.closedloopfueling.mlhfm;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import parser.bin.BinParser;
import parser.me7log.ClosedLoopLogParser;
import parser.xdf.TableDefinition;
import preferences.mlhfm.MlhfmMapPreferences;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClosedLoopMlhfmViewModel {

    private final BehaviorSubject<ClosedLoopMlfhmModel> behaviorSubject = BehaviorSubject.create();

    public ClosedLoopMlhfmViewModel() {
        BinParser.getInstance().registerMapListObserver(new Observer<List<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                Pair<TableDefinition, Map3d> tableDefinition = MlhfmMapPreferences.getSelectedMlhfmTableDefinition();
                ClosedLoopMlfhmModel model = behaviorSubject.getValue();
                ClosedLoopMlfhmModel.Builder builder;
                if(model == null) {
                    builder = new ClosedLoopMlfhmModel.Builder();
                } else {
                    builder = new ClosedLoopMlfhmModel.Builder(model);
                }

                builder.logsTabEnabled(tableDefinition != null);

                behaviorSubject.onNext(builder.build()); // No map found
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        ClosedLoopLogParser.getInstance().registerClosedLoopLogOnChangeObserver(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Map<String, List<Double>> stringListMap) {
                ClosedLoopMlfhmModel model = behaviorSubject.getValue();
                ClosedLoopMlfhmModel.Builder builder;
                if(model == null) {
                    builder = new ClosedLoopMlfhmModel.Builder();
                } else {
                    builder = new ClosedLoopMlfhmModel.Builder(model);
                }

                builder.correctionsTabEnabled(!stringListMap.isEmpty());

                behaviorSubject.onNext(builder.build()); // No map found
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public void registerMLHFMOnChange(Observer<ClosedLoopMlfhmModel> observer) {
        behaviorSubject.subscribe(observer);
    }

    public static class ClosedLoopMlfhmModel {
        private final boolean logsTabEnabled;
        private final boolean correctionsTabEnabled;

        private ClosedLoopMlfhmModel(Builder builder) {
           this.logsTabEnabled = builder.logsTabEnabled;
           this.correctionsTabEnabled = builder.correctionsTabEnabled;
        }

        public boolean isLogsTabEnabled() {
            return logsTabEnabled;
        }

        public boolean isCorrectionsTabEnabled() {
            return correctionsTabEnabled;
        }

        public static class Builder {
            private boolean logsTabEnabled;
            private boolean correctionsTabEnabled;

            public Builder() {}

            public Builder(ClosedLoopMlfhmModel model) {
                this.logsTabEnabled = model.logsTabEnabled;
                this.correctionsTabEnabled = model.correctionsTabEnabled;
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
