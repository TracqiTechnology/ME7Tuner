package parser.xdf;

import java.util.Objects;

public class TableDefinition {
    private final String tableName;
    private final String tableDescription;
    private final AxisDefinition xAxis;
    private final AxisDefinition yAxis;
    private final AxisDefinition zAxis;

    public TableDefinition(String tableName, String tableDescription, AxisDefinition xAxis, AxisDefinition yAxis, AxisDefinition zAxis) {
        this.tableName = tableName;
        this.tableDescription = tableDescription;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.zAxis = zAxis;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableDescription() {
        return tableDescription;
    }

    public AxisDefinition getXAxis() {
        return xAxis;
    }

    public AxisDefinition getYAxis() {
        return yAxis;
    }

    public AxisDefinition getZAxis() {
        return zAxis;
    }

    @Override
    public String toString() {
        if(tableDescription.isEmpty()) {
            return tableName;
        }

        return tableName + ": " + tableDescription + " - " + zAxis.getUnit();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableDefinition that = (TableDefinition) o;
        return tableName.equals(that.tableName) && tableDescription.equals(that.tableDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, tableDescription);
    }
}
