package math.map;

import java.util.Arrays;

public class Map3d {
    public Double[] xAxis;
    public Double[] yAxis;
    public Double[][] data;

    public Map3d() {
        super();
    }

    public Map3d(Double[] xAxis, Double[] yAxis, Double[][] data) {
        this.xAxis = Arrays.copyOf(xAxis, xAxis.length);
        this.yAxis = Arrays.copyOf(yAxis, yAxis.length);

        this.data = new Double[data.length][];

        for(int i = 0; i < data.length; i++) {
            this.data[i] = Arrays.copyOf(data[i], data[i].length);
        }
    }

    public Map3d(Map3d map3d) {
        this.xAxis = Arrays.copyOf(map3d.xAxis, map3d.xAxis.length);
        this.yAxis = Arrays.copyOf(map3d.yAxis, map3d.yAxis.length);

        this.data = new Double[map3d.data.length][];

        for(int i = 0; i < data.length; i++) {
            data[i] = Arrays.copyOf(map3d.data[i], map3d.data[i].length);
        }
    }

    public static Map3d transpose(Map3d map3d) {
        Double[] xAxis = map3d.yAxis;
        Double[] yAxis = map3d.xAxis;
        Double[][] data = transposeMatrix(map3d.data);

        return new Map3d(xAxis, yAxis, data);
    }

    private static Double[][] transposeMatrix(Double[][] matrix){
        int m = matrix.length;
        int n = matrix[0].length;

        Double[][] transposedMatrix = new Double[n][m];

        for(int x = 0; x < n; x++) {
            for(int y = 0; y < m; y++) {
                transposedMatrix[x][y] = matrix[y][x];
            }
        }

        return transposedMatrix;
    }
}
