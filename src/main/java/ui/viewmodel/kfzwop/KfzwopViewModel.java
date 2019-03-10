package ui.viewmodel.kfzwop;

import io.reactivex.subjects.PublishSubject;
import math.FindMax;
import math.Inverse;
import math.RescaleAxis;
import math.map.Map;
import model.kfmiop.Kfmiop;
import model.kfzwop.Kfzwop;
import model.kfzwop.KfzwopManager;
import ui.map.axis.MapAxis;

public class KfzwopViewModel {

    private PublishSubject<Map> kfzwopMapPublishSubject;

    public KfzwopViewModel() {
        kfzwopMapPublishSubject = PublishSubject.create();
    }

    public PublishSubject<Map> getKfzwopMapPublishSubject() {
        return kfzwopMapPublishSubject;
    }

    public void cacluateKfzwop(Map kfzwop, Double[] newXAxis) {

        Map newKfzwop = new Map();
        newKfzwop.xAxis = newXAxis;
        newKfzwop.yAxis = kfzwop.yAxis;
        newKfzwop.data = KfzwopManager.generateKfzwop(kfzwop.xAxis, kfzwop.data, newXAxis);

        kfzwopMapPublishSubject.onNext(newKfzwop);
    }
}
