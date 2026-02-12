package data.parser.xdf

data class AxisDefinition(
    val id: String,
    val type: Int,
    val address: Int,
    val indexCount: Int,
    val sizeBits: Int,
    val rowCount: Int,
    val columnCount: Int,
    val unit: String,
    val equation: String,
    val varId: String,
    val axisValues: List<Pair<Int, Float>>
)
