package model.wdkugdn;

import math.map.Map3d;

import java.util.List;

public class WdkugdnCorrection {
    public final Map3d wdkudgn;
    public final Double[][] correction;
    public final List<List<Double>> corrections;

    public WdkugdnCorrection(Map3d wdkudgn, Double[][] correction, List<List<Double>> corrections) {
        this.wdkudgn = wdkudgn;
        this.correction = correction;
        this.corrections = corrections;
    }


}
