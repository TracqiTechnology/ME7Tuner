package model.closedloopfueling.kfkhfm;

import math.map.Map3d;

import java.util.List;
import java.util.Map;

public class ClosedLoopKfkhfmCorrection {
    public final Map3d inputKfkhfm;
    public final Map3d correctedKfkhfm;

    public final Map<Double, List<Double>> filteredLoadDt;
    public final Map<Double, List<Double>> correctionsAfrMap;
    public final Map<Double, Double> meanAfrMap;
    public final Map<Double, double[]> modeAfrMap;

    ClosedLoopKfkhfmCorrection(Map3d inputKfkhfm, Map3d correctedKfkhfm, Map<Double, List<Double>> filteredLoadDt, Map<Double, List<Double>> correctionsAfrMap, Map<Double, Double> meanAfrMap, Map<Double, double[]> modeAfrMap) {
        this.inputKfkhfm = inputKfkhfm;
        this.correctedKfkhfm = correctedKfkhfm;
        this.filteredLoadDt = filteredLoadDt;
        this.correctionsAfrMap = correctionsAfrMap;
        this.meanAfrMap = meanAfrMap;
        this.modeAfrMap = modeAfrMap;
    }
}
