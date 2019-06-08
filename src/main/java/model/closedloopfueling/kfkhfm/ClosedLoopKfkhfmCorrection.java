package model.closedloopfueling.kfkhfm;

import math.map.Map3d;

import java.util.List;
import java.util.Map;

public class ClosedLoopKfkhfmCorrection {
    public final Map3d inputKfkhfm;
    public final Map3d correctedKfkhfm;

    public final List<List<List<Double>>>filteredLoadDt;
    public final List<List<List<Double>>> correctionsAfr;
    public final List<List<List<Double>>> meanAfr;
    public final List<List<List<double[]>>> modeAfr;

    ClosedLoopKfkhfmCorrection(Map3d inputKfkhfm, Map3d correctedKfkhfm, List<List<List<Double>>>filteredLoadDt, List<List<List<Double>>> correctionsAfr, List<List<List<Double>>> meanAfr, List<List<List<double[]>>> modeAfr) {
        this.inputKfkhfm = inputKfkhfm;
        this.correctedKfkhfm = correctedKfkhfm;
        this.filteredLoadDt = filteredLoadDt;
        this.correctionsAfr = correctionsAfr;
        this.meanAfr = meanAfr;
        this.modeAfr = modeAfr;
    }
}
