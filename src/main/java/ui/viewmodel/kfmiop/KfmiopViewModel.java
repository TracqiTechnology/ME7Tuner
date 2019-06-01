package ui.viewmodel.kfmiop;

import io.reactivex.subjects.PublishSubject;
import math.FindMax;
import math.Inverse;
import math.RescaleAxis;
import math.map.Map3d;
import model.kfmiop.Kfmiop;

public class KfmiopViewModel {

    private PublishSubject<Double[]> kfmiopXAxisPublishSubject;
    private PublishSubject<Map3d> kfmiopMapPublishSubject;

    public KfmiopViewModel() {
        kfmiopXAxisPublishSubject = PublishSubject.create();
        kfmiopMapPublishSubject = PublishSubject.create();
    }

    public PublishSubject<Double[]> getKfmiopXAxisPublishSubject() {
        return kfmiopXAxisPublishSubject;
    }

    public PublishSubject<Map3d> getKfmiopMapPublishSubject() {
        return kfmiopMapPublishSubject;
    }

    public void recalcuateKfmiopXAxis(Double[][] kfmirl) {
        // Find the maximum load specified in KFMIRL
        Double max = FindMax.findMax(kfmirl);
        // And rescale it against the stock KFMIOP X-Axis
        kfmiopXAxisPublishSubject.onNext(RescaleAxis.rescaleAxis(Kfmiop.getStockKfmiopXAxis(), max));
    }

    public void cacluateKfmiop(Map3d kfmirl, Map3d kfmiop) {
        kfmiopMapPublishSubject.onNext(Inverse.calculateInverse(kfmirl, kfmiop));
    }
}
