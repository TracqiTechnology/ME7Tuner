package math.map;

import java.util.Arrays;

public class Map2d {
    public Double[] axis;
    public Double[] data;


    public Map2d() {
        super();
    }

    public Map2d(Double[] axis, Double[] data) {
        this.axis = axis;
        this.data = data;
    }

    public Map2d(Map2d map2d) {
        this.axis = Arrays.copyOf(map2d.axis, map2d.axis.length);
        this.data = Arrays.copyOf(map2d.data, map2d.data.length);
    }
}
