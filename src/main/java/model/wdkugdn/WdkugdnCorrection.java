package model.wdkugdn;

import math.map.Map3d;

public class WdkugdnCorrection {
    public WdkugdnCorrection(Map3d wdkudgn, Double[][] correction) {
        this.wdkudgn = wdkudgn;
        this.correction = correction;
    }

    public final Map3d wdkudgn;
    public final Double[][] correction;
}
