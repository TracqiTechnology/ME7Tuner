package data.parser.xdf

import data.preferences.xdf.XdfFilePreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jdom2.*
import org.jdom2.input.SAXBuilder
import java.io.*

object XdfParser {
    private const val XDF_CONSTANT_TAG = "XDFCONSTANT"
    private const val XDF_TABLE_TAG = "XDFTABLE"
    private const val XDF_AXIS_TAG = "XDFAXIS"
    private const val XDF_LABEL_TAG = "LABEL"
    private const val XDF_EMBEDDED_TAG = "EMBEDDEDDATA"
    private const val XDF_MATH_TAG = "MATH"
    private const val XDF_VAR_TAG = "VAR"
    private const val XDF_TABLE_TITLE_TAG = "title"
    private const val XDF_TABLE_DESCRIPTION_TAG = "description"
    private const val XDF_ID_TAG = "id"
    private const val XDF_INDEX_TAG = "index"
    private const val XDF_VALUE_TAG = "value"
    private const val XDF_INDEX_COUNT_TAG = "indexcount"
    private const val XDF_UNITS_TAG = "units"
    private const val XDF_TYPE_FLAG = "mmedtypeflags"
    private const val XDF_ADDRESS_TAG = "mmedaddress"
    private const val XDF_SIZE_BITS_TAG = "mmedelementsizebits"
    private const val XDF_ROW_COUNT_TAG = "mmedrowcount"
    private const val XDF_COLUMN_COUNT_TAG = "mmedcolcount"
    private const val XDF_EQUATION_TAG = "equation"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _tableDefinitions = MutableStateFlow<List<TableDefinition>>(emptyList())
    val tableDefinitions: StateFlow<List<TableDefinition>> = _tableDefinitions.asStateFlow()

    fun init() {
        scope.launch {
            XdfFilePreferences.file.collect { file ->
                if (file.exists() && file.isFile) {
                    try {
                        BufferedInputStream(FileInputStream(file)).use { inputStream ->
                            parse(inputStream)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun parse(inputStream: InputStream) {
        val definitions = mutableListOf<TableDefinition>()
        val saxBuilder = SAXBuilder()
        val document = saxBuilder.build(inputStream)
        val classElement = document.rootElement
        val elements = classElement.children

        for (element in elements) {
            if (element.name == XDF_TABLE_TAG) {
                var tableName = ""
                var tableDescription = ""
                var xType = 0; var yType = 0; var zType = 0
                var xAddress = 0; var yAddress = 0; var zAddress = 0
                var xSizeBits = 0; var ySizeBits = 0; var zSizeBits = 0
                var xRowCount = 0; var yRowCount = 0; var zRowCount = 0
                var xColumnCount = 0; var yColumnCount = 0; var zColumnCount = 0
                var xEquation = ""; var yEquation = ""; var zEquation = ""
                var xVarId = ""; var yVarId = ""; var zVarId = ""
                var xIndexCount = 0; var yIndexCount = 0; var zIndexCount = 0
                var xUnits = "-"; var yUnits = "-"; var zUnits = "-"
                val xAxisValues = mutableListOf<Pair<Int, Float>>()
                val yAxisValues = mutableListOf<Pair<Int, Float>>()
                val zAxisValues = mutableListOf<Pair<Int, Float>>()

                for (tableChild in element.children) {
                    when (tableChild.name) {
                        XDF_TABLE_TITLE_TAG -> tableName = tableChild.text
                        XDF_TABLE_DESCRIPTION_TAG -> tableDescription = tableChild.text
                        XDF_AXIS_TAG -> {
                            val axis = tableChild.getAttribute(XDF_ID_TAG).value
                            for (axisChild in tableChild.children) {
                                when (axisChild.name) {
                                    XDF_INDEX_COUNT_TAG -> {
                                        val indexCount = axisChild.value.toInt()
                                        when (axis) { "x" -> xIndexCount = indexCount; "y" -> yIndexCount = indexCount; "z" -> zIndexCount = indexCount }
                                    }
                                    XDF_UNITS_TAG -> {
                                        val indexUnit = axisChild.value
                                        when (axis) { "x" -> xUnits = indexUnit; "y" -> yUnits = indexUnit; "z" -> zUnits = indexUnit }
                                    }
                                    XDF_EMBEDDED_TAG -> {
                                        val sizeBits = axisChild.getAttribute(XDF_SIZE_BITS_TAG).intValue
                                        val type = axisChild.getAttribute(XDF_TYPE_FLAG)?.let { Integer.decode(it.value) } ?: 0
                                        val address = axisChild.getAttribute(XDF_ADDRESS_TAG)?.let { Integer.decode(it.value) } ?: 0
                                        val rowCount = axisChild.getAttribute(XDF_ROW_COUNT_TAG)?.intValue ?: 0
                                        val columnCount = axisChild.getAttribute(XDF_COLUMN_COUNT_TAG)?.intValue ?: 0
                                        when (axis) {
                                            "x" -> { xType = type; xAddress = address; xSizeBits = sizeBits; xRowCount = rowCount; xColumnCount = columnCount }
                                            "y" -> { yType = type; yAddress = address; ySizeBits = sizeBits; yRowCount = rowCount; yColumnCount = columnCount }
                                            "z" -> { zType = type; zAddress = address; zSizeBits = sizeBits; zRowCount = rowCount; zColumnCount = columnCount }
                                        }
                                    }
                                    XDF_MATH_TAG -> {
                                        val equation = axisChild.getAttribute(XDF_EQUATION_TAG).value
                                        var varId = ""
                                        for (equationChild in axisChild.children) {
                                            if (equationChild.name == XDF_VAR_TAG) varId = equationChild.getAttribute(XDF_ID_TAG).value
                                        }
                                        when (axis) { "x" -> { xEquation = equation; xVarId = varId }; "y" -> { yEquation = equation; yVarId = varId }; "z" -> { zEquation = equation; zVarId = varId } }
                                    }
                                    XDF_LABEL_TAG -> {
                                        val index = axisChild.getAttribute(XDF_INDEX_TAG).intValue
                                        val value = try { axisChild.getAttribute(XDF_VALUE_TAG).floatValue } catch (_: DataConversionException) { 0f }
                                        when (axis) { "x" -> xAxisValues.add(index to value); "y" -> yAxisValues.add(index to value); "z" -> zAxisValues.add(index to value) }
                                    }
                                }
                            }
                        }
                    }
                }

                val xAxisDef = AxisDefinition("x", xType, xAddress, xIndexCount, xSizeBits, xRowCount, xColumnCount, xUnits, xEquation, xVarId, xAxisValues)
                val yAxisDef = AxisDefinition("y", yType, yAddress, yIndexCount, ySizeBits, yRowCount, yColumnCount, yUnits, yEquation, yVarId, yAxisValues)
                val zAxisDef = AxisDefinition("z", zType, zAddress, zIndexCount, zSizeBits, zRowCount, zColumnCount, zUnits, zEquation, zVarId, zAxisValues)
                definitions.add(TableDefinition(tableName, tableDescription, xAxisDef, yAxisDef, zAxisDef))

            } else if (element.name == XDF_CONSTANT_TAG) {
                var tableName = ""
                var tableDescription = ""
                var cEquation = ""; var cVarId = ""; var cUnits = "-"
                var cType = 0; var cAddress = 0; var cSizeBits = 0

                for (tableChild in element.children) {
                    when (tableChild.name) {
                        XDF_TABLE_TITLE_TAG -> tableName = tableChild.text
                        XDF_TABLE_DESCRIPTION_TAG -> tableDescription = tableChild.text
                        XDF_UNITS_TAG -> cUnits = tableChild.value
                        XDF_EMBEDDED_TAG -> {
                            cSizeBits = tableChild.getAttribute(XDF_SIZE_BITS_TAG).intValue
                            cType = tableChild.getAttribute(XDF_TYPE_FLAG)?.let { Integer.decode(it.value) } ?: 0
                            cAddress = tableChild.getAttribute(XDF_ADDRESS_TAG)?.let { Integer.decode(it.value) } ?: 0
                        }
                        XDF_MATH_TAG -> {
                            cEquation = tableChild.getAttribute(XDF_EQUATION_TAG).value
                            for (equationChild in tableChild.children) {
                                if (equationChild.name == XDF_VAR_TAG) cVarId = equationChild.getAttribute(XDF_ID_TAG).value
                            }
                        }
                    }
                }

                val zAxisDef = AxisDefinition("z", cType, cAddress, 0, cSizeBits, 0, 0, cUnits, cEquation, cVarId, emptyList())
                definitions.add(TableDefinition(tableName, tableDescription, null, null, zAxisDef))
            }
        }

        definitions.sortBy { it.toString() }
        _tableDefinitions.value = definitions
    }
}
