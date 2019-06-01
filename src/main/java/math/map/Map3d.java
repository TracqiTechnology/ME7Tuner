package math.map;

import java.util.Arrays;

public class Map3d {
    public Double[] xAxis;
    public Double[] yAxis;
    public Double[][] data;

    public Map3d() {
        super();
    }

    public Map3d(Map3d map3d) {
        this.xAxis = Arrays.copyOf(map3d.xAxis, map3d.xAxis.length);
        this.yAxis = Arrays.copyOf(map3d.yAxis, map3d.yAxis.length);

        this.data = new Double[data.length][];

        for(int i = 0; i < data.length; i++) {
            data[i] = Arrays.copyOf(data[i], data[i].length);
        }
    }
}
