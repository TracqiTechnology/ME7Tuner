package ui.viewmodel;

import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;

public class KfkhfmViewModel {

    private BehaviorSubject<Map3d> kfkhfmBehaviorSubject;

    private static KfkhfmViewModel instance;

    public static KfkhfmViewModel getInstance() {
        if (instance == null) {
            instance = new KfkhfmViewModel();
        }

        return instance;
    }

    private KfkhfmViewModel() {
        kfkhfmBehaviorSubject = BehaviorSubject.create();
    }

    public BehaviorSubject<Map3d> getKfkhfmBehaviorSubject() {
        return kfkhfmBehaviorSubject;
    }

    public void setKfkhfm(Map3d kfkhfm) {
        if (kfkhfm != null) {
            kfkhfmBehaviorSubject.onNext(kfkhfm);
        }
    }
}
