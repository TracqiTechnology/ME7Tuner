package ui.viewmodel.kfzw;

import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import model.kfzw.KfzwManager;

public class KfzwViewModel {

    private PublishSubject<Map3d> kfzwMapPublishSubject;

    public KfzwViewModel() {
        kfzwMapPublishSubject = PublishSubject.create();
    }

    public PublishSubject<Map3d> getKfzwMapPublishSubject() {
        return kfzwMapPublishSubject;
    }

    public void calculateKfzw(Map3d kfzw, Double[] newXAxis) {

        Map3d newKfzw = new Map3d();
        newKfzw.xAxis = newXAxis;
        newKfzw.yAxis = kfzw.yAxis;
        newKfzw.zAxis = KfzwManager.generateKfzw(kfzw.xAxis, kfzw.zAxis, newXAxis);

        kfzwMapPublishSubject.onNext(newKfzw);
    }
}
