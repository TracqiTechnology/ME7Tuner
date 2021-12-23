package parser.xdf;

import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class AxisDefinition {

    private final String id;
    private final int address;
    private final int indexCount;
    private final int sizeBits;

    private final int rowCount;
    private final int columnCount;

    private final String unit;
    private final String equation;
    private final String varId;

    private final List<Pair<Integer, Float>> axisValues = new ArrayList<>();

    public AxisDefinition(String id, int address, int indexCount, int sizeBits, int rowCount, int columnCount, String unit, String equation, String varId, List<Pair<Integer, Float>> axisValues) {
        this.id = id;
        this.address = address;
        this.indexCount = indexCount;
        this.sizeBits = sizeBits;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.unit = unit;
        this.equation = equation;
        this.varId = varId;
        this.axisValues.addAll(axisValues);
    }

    public String getId() {
        return id;
    }

    public int getAddress() {
        return address;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public int getSizeBits() {
        return sizeBits;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() { return columnCount; }

    public String getUnit() {
        return unit;
    }

    public String getEquation() {
        return equation;
    }

    public String getVarId() {
        return varId;
    }

    public List<Pair<Integer, Float>> getAxisValues() {
        return axisValues;
    }

    public boolean is2D() {
        if(rowCount != 0 && columnCount != 0) {
            return true;
        }

        return false;
    }

}
