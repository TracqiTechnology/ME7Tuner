package model.openloopfueling.correction;

import math.map.Map3d;

import java.util.List;
import java.util.Map;

public class OpenLoopMlhfmCorrection {
    public final Map3d inputMlhfm;
    public final Map3d correctedMlhfm;
    public final Map3d fitMlhfm;
    public final Map<Double, List<Double>> correctionsAfrMap;
    public final Map<Double, Double> meanAfrMap;
    public final Map<Double, double[]> modeAfrMap;
    public final Map<Double, Double> correctedAfrMap;

    public OpenLoopMlhfmCorrection(Map3d inputMlhfm, Map3d correctedMlhfm, Map3d fitMlhfm, Map<Double, List<Double>> correctionsAfrMap, Map<Double, Double> meanAfrMap, Map<Double, double[]> modeAfrMap, Map<Double, Double> correctedAfrMap) {
        this.inputMlhfm = inputMlhfm;
        this.correctedMlhfm = correctedMlhfm;
        this.fitMlhfm = fitMlhfm;
        this.correctionsAfrMap = correctionsAfrMap;
        this.meanAfrMap = meanAfrMap;
        this.modeAfrMap = modeAfrMap;
        this.correctedAfrMap = correctedAfrMap;
    }
}
