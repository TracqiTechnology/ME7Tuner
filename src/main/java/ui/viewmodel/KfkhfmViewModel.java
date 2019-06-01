package ui.viewmodel;

import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;

public class KfkhfmViewModel {

    private PublishSubject<Map3d> kfkhfmPublishSubject;

    private static KfkhfmViewModel instance;

    public static KfkhfmViewModel getInstance() {
        if (instance == null) {
            instance = new KfkhfmViewModel();
        }

        return instance;
    }

    private KfkhfmViewModel() {
        kfkhfmPublishSubject = PublishSubject.create();
    }

    public PublishSubject<Map3d> getKfkhfmPublishSubject() {
        return kfkhfmPublishSubject;
    }

    public void setKfkhfm(Map3d kfkhfm) {
        if (kfkhfm != null) {
            kfkhfmPublishSubject.onNext(kfkhfm);
        }
    }
}
