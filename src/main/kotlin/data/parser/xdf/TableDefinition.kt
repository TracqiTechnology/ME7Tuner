package data.parser.xdf

data class TableDefinition(
    val tableName: String,
    val tableDescription: String,
    val xAxis: AxisDefinition?,
    val yAxis: AxisDefinition?,
    val zAxis: AxisDefinition
) {
    override fun toString(): String {
        return if (tableDescription.isEmpty()) tableName
        else "$tableName: $tableDescription - ${zAxis.unit}"
    }
}
