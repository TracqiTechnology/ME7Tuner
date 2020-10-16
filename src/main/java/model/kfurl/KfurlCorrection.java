package model.kfurl;

import math.map.Map3d;

import java.util.List;

public class KfurlCorrection {

    public final Map3d kfurl;
    public final Double[][] correction;
    public final List<List<Double>> corrections;

    public KfurlCorrection(Map3d kfurl, Double[][] correction, List<List<Double>> corrections ) {
        this.kfurl = kfurl;
        this.correction = correction;
        this.corrections = corrections;
    }


}
