package ui.viewmodel.kfvpdksd;

import com.sun.tools.javac.util.Pair;
import contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.RescaleAxis;
import math.map.Map3d;
import model.kfvpdksd.Kfvpdksd;
import parser.bin.BinParser;
import parser.me7log.KfvpdksdLogParser;
import parser.me7log.Me7LogParser;
import parser.xdf.TableDefinition;
import preferences.kfvpdksd.KfvpdksdPreferences;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KfvpdksdViewModel {

    private final BehaviorSubject<KfvpdksdModel> subject = BehaviorSubject.create();



    public KfvpdksdViewModel() {
        BinParser.getInstance().registerMapListObserver(new Observer<List<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                Pair<TableDefinition, Map3d> kfvpdksdTable =  KfvpdksdPreferences.getSelectedMap();
                if (kfvpdksdTable != null) {
                    subject.onNext(new  KfvpdksdModel(kfvpdksdTable, null,null));
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {
            }
        });

        KfvpdksdLogParser.getInstance().registerLogOnChangeObserver(new Observer<Map<Me7LogFileContract.Header, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Map<Me7LogFileContract.Header, List<Double>> log) {
                Pair<TableDefinition, Map3d> kfvpdksdTable =  KfvpdksdPreferences.getSelectedMap();
                if (kfvpdksdTable != null) {
                    cacluateKfvpdksd(Kfvpdksd.parsePressure(log, kfvpdksdTable.snd.yAxis));
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {}
        });
    }

    public void register(Observer<KfvpdksdModel> observer) {
        subject.subscribe(observer);
    }

    public void loadLogs(File file, Me7LogParser.ProgressCallback progressCallback) {
        KfvpdksdLogParser.getInstance().loadDirectory(file, progressCallback);
    }

    public void cacluateKfvpdksd(Double[] maxPressure) {
        Pair<TableDefinition, Map3d> kfvpdksdTable = KfvpdksdPreferences.getSelectedMap();
        if(kfvpdksdTable != null) {
            Double[] rescaledPressureRatio = RescaleAxis.rescaleAxis(kfvpdksdTable.snd.xAxis, (1000+getMax(maxPressure))/1000d);
            kfvpdksdTable.snd.xAxis = rescaledPressureRatio;
            Kfvpdksd kfvpdksd = Kfvpdksd.generate(maxPressure, KfvpdksdPreferences.getSelectedMap().snd.yAxis, rescaledPressureRatio);
            SwingUtilities.invokeLater(() -> subject.onNext(new KfvpdksdModel(kfvpdksdTable, kfvpdksd, maxPressure)));
        }
    }


    private double getMax(Double[] values) {
        return Collections.max(Arrays.asList(values.clone()));
    }

    public static class KfvpdksdModel {
        private final Pair<TableDefinition, Map3d> kfvpdksdTable;
        private final Kfvpdksd kfvpdksd;
        private final Double[] pressure;

        public KfvpdksdModel(@Nullable Pair<TableDefinition, Map3d> kfvpdksdTable, @Nullable Kfvpdksd kfvpdksd, @Nullable Double[] pressure) {
            this.kfvpdksdTable = kfvpdksdTable;
            this.kfvpdksd = kfvpdksd;
            this.pressure = pressure;
        }

        @Nullable
        public Pair<TableDefinition, Map3d> getKfvpdksdTable() { return kfvpdksdTable; }

        @Nullable
        public Kfvpdksd getKfvpdksd() {
            return kfvpdksd;
        }

        @Nullable
        public Double[] getPressure() {
            return pressure;
        }
    }
}
