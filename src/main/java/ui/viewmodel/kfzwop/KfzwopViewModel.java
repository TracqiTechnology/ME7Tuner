package ui.viewmodel.kfzwop;

import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import model.kfzwop.KfzwopManager;

public class KfzwopViewModel {

    private PublishSubject<Map3d> kfzwopMapPublishSubject;

    public KfzwopViewModel() {
        kfzwopMapPublishSubject = PublishSubject.create();
    }

    public PublishSubject<Map3d> getKfzwopMapPublishSubject() {
        return kfzwopMapPublishSubject;
    }

    public void cacluateKfzwop(Map3d kfzwop, Double[] newXAxis) {

        Map3d newKfzwop = new Map3d();
        newKfzwop.xAxis = newXAxis;
        newKfzwop.yAxis = kfzwop.yAxis;
        newKfzwop.data = KfzwopManager.generateKfzwop(kfzwop.xAxis, kfzwop.data, newXAxis);

        kfzwopMapPublishSubject.onNext(newKfzwop);
    }
}
