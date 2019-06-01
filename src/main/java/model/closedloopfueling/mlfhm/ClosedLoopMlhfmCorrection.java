package model.closedloopfueling.mlfhm;

import java.util.List;
import java.util.Map;

public class ClosedLoopMlhfmCorrection {
    public final Map<String, List<Double>> inputMlhfm;
    public final Map<String, List<Double>> correctedMlhfm;
    public final Map<Double, List<Double>> filteredVoltageDt;
    public final Map<Double, List<Double>> correctionsAfrMap;
    public final Map<Double, Double> meanAfrMap;
    public final Map<Double, double[]> modeAfrMap;
    public final Map<Double, Double> correctedAfrMap;

    ClosedLoopMlhfmCorrection(Map<String, List<Double>> inputMlhfm, Map<String, List<Double>> correctedMlhfm, Map<Double, List<Double>> filteredVoltageDt, Map<Double, List<Double>> correctionsAfrMap, Map<Double, Double> meanAfrMap, Map<Double, double[]> modeAfrMap, Map<Double, Double> correctedAfrMap) {
        this.inputMlhfm = inputMlhfm;
        this.correctedMlhfm = correctedMlhfm;
        this.filteredVoltageDt = filteredVoltageDt;
        this.correctionsAfrMap = correctionsAfrMap;
        this.meanAfrMap = meanAfrMap;
        this.modeAfrMap = modeAfrMap;
        this.correctedAfrMap = correctedAfrMap;
    }
}
