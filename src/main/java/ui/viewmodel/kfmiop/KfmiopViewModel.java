package ui.viewmodel.kfmiop;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import model.kfmiop.Kfmiop;
import model.rlsol.Rlsol;
import org.apache.commons.math3.util.Pair;
import parser.xdf.TableDefinition;
import preferences.kfmiop.KfmiopPreferences;

import java.util.Optional;

public class KfmiopViewModel {

    private final BehaviorSubject<KfmiopModel> behaviorSubject = BehaviorSubject.create();

    public KfmiopViewModel() {
        behaviorSubject.onNext(new KfmiopModel.Builder().build());

        onTableSelected(getSelectedKfmiopTableDefinition());

        KfmiopPreferences.getInstance().registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> selectedTableDefinitionPair) {
                onTableSelected(selectedTableDefinitionPair.get());
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    public void register(Observer<KfmiopModel> observer) {
        behaviorSubject.subscribe(observer);
    }

    public void calculateKfmiop() {
        double maxMapSensorLoad = Rlsol.rlsol(1030, KfmiopPreferences.getInstance().getMaxMapPressurePreference(), 0, 96, 0.106, KfmiopPreferences.getInstance().getMaxMapPressurePreference());
        double maxBoostPressureLoad = Rlsol.rlsol(1030, KfmiopPreferences.getInstance().getMaxBoostPressurePreference(), 0, 96, 0.106, KfmiopPreferences.getInstance().getMaxBoostPressurePreference());

        Pair<TableDefinition, Map3d> tableDefinition = getSelectedKfmiopTableDefinition();

        if (tableDefinition != null) {
            cacluateKfmiop(getSelectedKfmiopTableDefinition().getSecond(), maxMapSensorLoad, maxBoostPressureLoad);
        }
    }

    private void onTableSelected(@Nullable Pair<TableDefinition, Map3d> selectedTable) {
        // Found the map
        KfmiopModel.Builder builder = new KfmiopModel.Builder(behaviorSubject.getValue());
        if (selectedTable != null) {
            builder.tableDefinition(selectedTable.getFirst()).inputKfmiop(selectedTable.getSecond());
            behaviorSubject.onNext(builder.build());
            calculateKfmiop();
        } else {
            builder.tableDefinition(null).inputKfmiop(null).outputKfmiop(null).inputBoost(null).outputBoost(null).maxMapSensorPressure(0).maxBoostPressure(0);
            behaviorSubject.onNext(builder.build());
        }
    }

    private Pair<TableDefinition, Map3d> getSelectedKfmiopTableDefinition() {
        return KfmiopPreferences.getInstance().getSelectedMap();
    }

    private void cacluateKfmiop(Map3d baseKfmiop, double maxMapSensorLoad, double maxBoostPressureLoad) {
        KfmiopModel.Builder builder = new KfmiopModel.Builder(behaviorSubject.getValue());
        Kfmiop kfmiop = Kfmiop.calculateKfmiop(baseKfmiop, maxMapSensorLoad, maxBoostPressureLoad);
        builder.outputKfmiop(kfmiop.getOutputKfmiop()).inputBoost(kfmiop.getInputBoost()).outputBoost(kfmiop.getOutputBoost()).maxMapSensorPressure(kfmiop.getMaxMapSensorPressure()).maxBoostPressure(kfmiop.getMaxBoostPressure());
        behaviorSubject.onNext(builder.build());
    }

    public static class KfmiopModel {
        private final TableDefinition tableDefinition;
        private final Map3d inputKfmiop;
        private final Map3d outputKfmiop;
        private final Map3d inputBoost;
        private final Map3d outputBoost;
        private final double maxMapSensorPressure;
        private final double maxBoostPressure;

        private KfmiopModel(Builder builder) {
            this.tableDefinition = builder.tableDefinition;
            this.inputKfmiop = builder.inputKfmiop;
            this.outputKfmiop = builder.outputKfmiop;
            this.inputBoost = builder.inputBoost;
            this.outputBoost = builder.outputBoost;
            this.maxMapSensorPressure = builder.maxMapSensorPressure;
            this.maxBoostPressure = builder.maxBoostPressure;
        }

        @Nullable
        public TableDefinition getTableDefinition() {
            return tableDefinition;
        }

        @Nullable
        public Map3d getInputKfmiop() {
            return inputKfmiop;
        }

        @Nullable
        public Map3d getOutputKfmiop() {
            return outputKfmiop;
        }

        @Nullable
        public Map3d getInputBoost() {
            return inputBoost;
        }

        @Nullable
        public Map3d getOutputBoost() {
            return outputBoost;
        }

        public double getMaxMapSensorPressure() {
            return maxMapSensorPressure;
        }

        public double getMaxBoostPressure() {
            return maxBoostPressure;
        }

        public static class Builder {
            private TableDefinition tableDefinition;
            private Map3d inputKfmiop;
            private Map3d outputKfmiop;
            private Map3d inputBoost;
            private Map3d outputBoost;
            private double maxMapSensorPressure;
            private double maxBoostPressure;

            private Builder() {
            }

            private Builder(KfmiopModel model) {
                this.tableDefinition = model.tableDefinition;
                this.inputKfmiop = model.inputKfmiop;
                this.outputKfmiop = model.outputKfmiop;
                this.inputBoost = model.inputBoost;
                this.outputBoost = model.outputBoost;
                this.maxBoostPressure = model.maxBoostPressure;
                this.maxMapSensorPressure = model.maxMapSensorPressure;
            }

            public Builder tableDefinition(TableDefinition tableDefinition) {
                this.tableDefinition = tableDefinition;

                return this;
            }

            public Builder inputKfmiop(Map3d map) {
                this.inputKfmiop = map;

                return this;
            }

            public Builder outputKfmiop(Map3d map) {
                this.outputKfmiop = map;

                return this;
            }

            public Builder inputBoost(Map3d map) {
                this.inputBoost = map;

                return this;
            }

            public Builder outputBoost(Map3d map) {
                this.outputBoost = map;

                return this;
            }

            public Builder maxMapSensorPressure(double pressure) {
                this.maxMapSensorPressure = pressure;

                return this;
            }

            public Builder maxBoostPressure(double pressure) {
                this.maxBoostPressure = pressure;

                return this;
            }

            public KfmiopModel build() {
                return new KfmiopModel(this);
            }
        }
    }
}
