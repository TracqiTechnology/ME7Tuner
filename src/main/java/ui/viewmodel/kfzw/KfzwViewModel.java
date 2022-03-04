package ui.viewmodel.kfzw;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import model.kfzw.Kfzw;
import org.apache.commons.math3.util.Pair;
import parser.bin.BinParser;
import parser.xdf.TableDefinition;
import preferences.kfmiop.KfmiopPreferences;
import preferences.kfzw.KfzwPreferences;

import java.util.List;
import java.util.Optional;

public class KfzwViewModel {

    private final BehaviorSubject<KfzwModel> subject = BehaviorSubject.create();

    public KfzwViewModel() {
        BinParser.getInstance().registerMapListObserver(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                updateModel();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        KfzwPreferences.getInstance().registerOnMapChanged(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                updateModel();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {

            }
        });
    }

    public void register(Observer<KfzwModel> observer) {
        subject.subscribe(observer);
    }

    public void cacluateKfzw(Map3d kfzw, Double[] newXAxis) {
        Map3d newKfzw = new Map3d();
        newKfzw.xAxis = newXAxis;
        newKfzw.yAxis = kfzw.yAxis;
        newKfzw.zAxis = Kfzw.generateKfzw(kfzw.xAxis, kfzw.zAxis, newXAxis);
        subject.onNext(new KfzwModel(KfzwPreferences.getInstance().getSelectedMap(), KfmiopPreferences.getInstance().getSelectedMap().getSecond().xAxis, newKfzw));
    }

    private void updateModel() {
        Pair<TableDefinition, Map3d> kfzwTable = KfzwPreferences.getInstance().getSelectedMap();
        Pair<TableDefinition, math.map.Map3d> kfmiopTable = KfmiopPreferences.getInstance().getSelectedMap();
        if (kfzwTable != null) {
            if(kfmiopTable != null) {
                subject.onNext(new KfzwModel(kfzwTable, KfmiopPreferences.getInstance().getSelectedMap().getSecond().xAxis, kfzwTable.getSecond()));
            } else {
                subject.onNext(new KfzwModel(kfzwTable, null, kfzwTable.getSecond()));
            }
        } else {
            if(kfmiopTable != null) {
                subject.onNext(new KfzwModel(null, KfmiopPreferences.getInstance().getSelectedMap().getSecond().xAxis, null));
            } else {
                subject.onNext(new KfzwModel(null, null, null));
            }
        }
    }

    public record KfzwModel(Pair<TableDefinition, Map3d> kfzw, Double[] kfmiopXAxis, Map3d outputKfzw) {
        public KfzwModel(@Nullable Pair<TableDefinition, Map3d> kfzw, @Nullable Double[] kfmiopXAxis, @Nullable Map3d outputKfzw) {
            this.kfzw = kfzw;
            this.kfmiopXAxis = kfmiopXAxis;
            this.outputKfzw = outputKfzw;
        }

        @Nullable
        public Pair<TableDefinition, Map3d> getKfzw() {
            return kfzw;
        }

        @Nullable
        public Double[] getKfmiopXAxis() {
            return kfmiopXAxis;
        }

        @Nullable
        public Map3d getOutputKfzw() {
            return outputKfzw;
        }
    }
}
