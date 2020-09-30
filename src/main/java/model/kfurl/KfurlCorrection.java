package model.kfurl;

import math.map.Map3d;

public class KfurlCorrection {
    public KfurlCorrection(Map3d kfurl, Double[][] correction) {
        this.kfurl = kfurl;
        this.correction = correction;
    }

    public final Map3d kfurl;
    public final Double[][] correction;
}
