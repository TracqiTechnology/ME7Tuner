package ui.viewmodel.kfzwop;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import model.kfzwop.Kfzwop;
import parser.bin.BinParser;
import parser.xdf.TableDefinition;
import preferences.kfzwop.KfzwopPreferences;

import java.util.Arrays;
import java.util.List;

public class KfzwopViewModel {

    private final BehaviorSubject<KfzwopModel> subject = BehaviorSubject.create();

    public KfzwopViewModel() {
        BinParser.getInstance().registerMapListObserver(new Observer<List<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                Pair<TableDefinition, Map3d> kfzwopTable = KfzwopPreferences.getSelectedMap();
                if (kfzwopTable != null) {
                    subject.onNext(new KfzwopModel(kfzwopTable, kfzwopTable.snd));
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {
            }
        });
    }

    public void register(Observer<KfzwopModel> observer) {
        subject.subscribe(observer);
    }

    public void cacluateKfzwop(Map3d kfzwop, Double[] newXAxis) {
        Map3d newKfzwop = new Map3d();
        newKfzwop.xAxis = newXAxis;
        newKfzwop.yAxis = kfzwop.yAxis;
        newKfzwop.zAxis = Kfzwop.generateKfzwop(kfzwop.xAxis, kfzwop.zAxis, newXAxis);
        subject.onNext(new KfzwopModel(KfzwopPreferences.getSelectedMap(), newKfzwop));
    }

    public static class KfzwopModel {
        private final Pair<TableDefinition, Map3d> kfzwop;
        private final Map3d outputKfzwop;

        public KfzwopModel(@Nullable Pair<TableDefinition, Map3d> kfzwop, Map3d outputKfzwop) {
            this.kfzwop = kfzwop;
            this.outputKfzwop = outputKfzwop;
        }

        @Nullable
        public Pair<TableDefinition, Map3d> getKfzwop() { return kfzwop; }

        @Nullable
        public Map3d getOutputKfzwop() {
            return outputKfzwop;
        }
    }
}
