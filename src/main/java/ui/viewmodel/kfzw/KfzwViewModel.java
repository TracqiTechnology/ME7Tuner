package ui.viewmodel.kfzw;

import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import model.kfzw.KfzwManager;
import model.kfzwop.KfzwopManager;

public class KfzwViewModel {

    private PublishSubject<Map3d> kfzwMapPublishSubject;

    public KfzwViewModel() {
        kfzwMapPublishSubject = PublishSubject.create();
    }

    public PublishSubject<Map3d> getKfzwMapPublishSubject() {
        return kfzwMapPublishSubject;
    }

    public void cacluateKfzw(Map3d kfzw, Double[] newXAxis) {

        Map3d newKfzw = new Map3d();
        newKfzw.xAxis = newXAxis;
        newKfzw.yAxis = kfzw.yAxis;
        newKfzw.data = KfzwManager.generateKfzw(kfzw.xAxis, kfzw.data, newXAxis);

        kfzwMapPublishSubject.onNext(newKfzw);
    }
}
