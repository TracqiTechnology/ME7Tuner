package domain.math.map;

import java.util.Arrays;

public class Map3d {
    public Double[] xAxis;
    public Double[] yAxis;
    public Double[][] zAxis;

    public Map3d() {
        super();
        xAxis = new Double[0];
        yAxis = new Double[0];
        zAxis = new Double[0][];
    }

    public Map3d(Double[] xAxis, Double[] yAxis, Double[][] zAxis) {
        this.xAxis = Arrays.copyOf(xAxis, xAxis.length);
        this.yAxis = Arrays.copyOf(yAxis, yAxis.length);

        this.zAxis = new Double[zAxis.length][];

        for(int i = 0; i < zAxis.length; i++) {
            this.zAxis[i] = Arrays.copyOf(zAxis[i], zAxis[i].length);
        }
    }

    public Map3d(Map3d map3d) {
        this.xAxis = Arrays.copyOf(map3d.xAxis, map3d.xAxis.length);
        this.yAxis = Arrays.copyOf(map3d.yAxis, map3d.yAxis.length);

        this.zAxis = new Double[map3d.zAxis.length][];

        for(int i = 0; i < zAxis.length; i++) {
            zAxis[i] = Arrays.copyOf(map3d.zAxis[i], map3d.zAxis[i].length);
        }
    }

    public static Map3d transpose(Map3d map3d) {
        Double[] xAxis = map3d.yAxis;
        Double[] yAxis = map3d.xAxis;
        Double[][] data = transposeMatrix(map3d.zAxis);

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

    @Override
    public String toString() {

        StringBuilder zAxisString = new StringBuilder();

        for(Double[] row: zAxis) {
            zAxisString.append(Arrays.toString(row));
            zAxisString.append("\n");
        }


        return "Map3d{" +
                "xAxis=" + Arrays.toString(xAxis) +
                "\n" +
                ", yAxis=" + Arrays.toString(yAxis) +
                "\n" +
                ", zAxis=" + zAxisString.toString() +
                '}';
    }
}
