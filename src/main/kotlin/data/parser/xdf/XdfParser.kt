package data.parser.xdf

import data.preferences.xdf.XdfFilePreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jdom2.*
import org.jdom2.input.SAXBuilder
import java.io.*

object XdfParser {
    // ── Element / attribute name constants ────────────────────────────────
    private const val XDF_HEADER_TAG          = "XDFHEADER"
    private const val XDF_CONSTANT_TAG        = "XDFCONSTANT"
    private const val XDF_TABLE_TAG           = "XDFTABLE"
    private const val XDF_AXIS_TAG            = "XDFAXIS"
    private const val XDF_LABEL_TAG           = "LABEL"
    private const val XDF_EMBEDDED_TAG        = "EMBEDDEDDATA"
    private const val XDF_MATH_TAG            = "MATH"
    private const val XDF_VAR_TAG             = "VAR"
    private const val XDF_TABLE_TITLE_TAG     = "title"
    private const val XDF_TABLE_DESCRIPTION_TAG = "description"
    private const val XDF_ID_TAG              = "id"
    private const val XDF_INDEX_TAG           = "index"
    private const val XDF_VALUE_TAG           = "value"
    private const val XDF_INDEX_COUNT_TAG     = "indexcount"
    private const val XDF_UNITS_TAG           = "units"
    private const val XDF_DECIMAL_PL_TAG      = "decimalpl"
    private const val XDF_MIN_TAG             = "min"
    private const val XDF_MAX_TAG             = "max"
    private const val XDF_EMBED_INFO_TAG      = "embedinfo"
    private const val XDF_CATEGORY_TAG        = "CATEGORY"
    private const val XDF_CATEGORY_MEM_TAG    = "CATEGORYMEM"
    private const val XDF_BASEOFFSET_TAG      = "BASEOFFSET"
    private const val XDF_DEFAULTS_TAG        = "DEFAULTS"
    private const val XDF_DEFTITLE_TAG        = "deftitle"
    private const val XDF_AUTHOR_TAG          = "author"

    // EMBEDDEDDATA attributes
    private const val XDF_TYPE_FLAG           = "mmedtypeflags"
    private const val XDF_ADDRESS_TAG         = "mmedaddress"
    private const val XDF_SIZE_BITS_TAG       = "mmedelementsizebits"
    private const val XDF_ROW_COUNT_TAG       = "mmedrowcount"
    private const val XDF_COLUMN_COUNT_TAG    = "mmedcolcount"
    private const val XDF_MAJOR_STRIDE_TAG    = "mmedmajorstridebits"
    private const val XDF_MINOR_STRIDE_TAG    = "mmedminorstridebits"
    private const val XDF_EQUATION_TAG        = "equation"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _tableDefinitions = MutableStateFlow<List<TableDefinition>>(emptyList())
    val tableDefinitions: StateFlow<List<TableDefinition>> = _tableDefinitions.asStateFlow()

    /** Exposes the parsed header so consumers can use baseOffset / category names. */
    private val _xdfHeader = MutableStateFlow(XdfHeader())
    val xdfHeader: StateFlow<XdfHeader> = _xdfHeader.asStateFlow()

    fun init() {
        scope.launch {
            XdfFilePreferences.file.collect { file ->
                if (file.exists() && file.isFile) {
                    try {
                        BufferedInputStream(FileInputStream(file)).use { inputStream ->
                            parse(inputStream)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private fun parseInt(value: String?): Int =
        if (value == null) 0
        else runCatching { Integer.decode(value) }.getOrElse { value.toIntOrNull() ?: 0 }

    private fun parseDouble(value: String?): Double =
        value?.toDoubleOrNull() ?: Double.NaN

    private fun Attribute?.intOrDefault(default: Int = 0): Int =
        if (this == null) default else parseInt(value)

    // ─────────────────────────────────────────────────────────────────────

    private fun parse(inputStream: InputStream) {
        val (header, definitions) = parseToList(inputStream)
        _xdfHeader.value = header
        _tableDefinitions.value = definitions
    }

    /**
     * Parses an XDF input stream and returns the header and sorted table definitions
     * without touching singleton state.  Visible to tests.
     */
    internal fun parseToList(inputStream: InputStream): Pair<XdfHeader, List<TableDefinition>> {
        val definitions = mutableListOf<TableDefinition>()
        val saxBuilder = SAXBuilder()
        val document = saxBuilder.build(inputStream)
        val rootElement = document.rootElement

        val header = parseHeader(rootElement.getChild(XDF_HEADER_TAG))

        for (element in rootElement.children) {
            when (element.name) {
                XDF_TABLE_TAG -> parseTable(element, header)?.let { definitions.add(it) }
                XDF_CONSTANT_TAG -> parseConstant(element, header)?.let { definitions.add(it) }
            }
        }

        definitions.sortBy { it.toString() }
        return header to definitions
    }

    // ─────────────────────────────────────────────────────────────────────
    // Header parsing
    // ─────────────────────────────────────────────────────────────────────

    private fun parseHeader(headerEl: Element?): XdfHeader {
        if (headerEl == null) return XdfHeader()

        var deftitle = ""
        var description = ""
        var author = ""
        var baseOffsetValue = 0
        var baseOffsetSubtract = false
        var defaultSizeBits = 8
        var defaultSigDigits = 2
        var defaultLsbFirst = true
        var defaultSigned = false
        var defaultFloat = false
        val categories = mutableMapOf<Int, String>()

        for (child in headerEl.children) {
            when (child.name) {
                XDF_DEFTITLE_TAG -> deftitle = child.textTrim
                "description"   -> description = child.textTrim
                XDF_AUTHOR_TAG  -> author = child.textTrim
                XDF_BASEOFFSET_TAG -> {
                    baseOffsetValue    = parseInt(child.getAttributeValue("offset"))
                    baseOffsetSubtract = child.getAttributeValue("subtract") == "1"
                }
                XDF_DEFAULTS_TAG -> {
                    defaultSizeBits  = child.getAttributeValue("datasizeinbits")?.toIntOrNull() ?: 8
                    defaultSigDigits = child.getAttributeValue("sigdigits")?.toIntOrNull() ?: 2
                    defaultLsbFirst  = child.getAttributeValue("lsbfirst") != "0"
                    defaultSigned    = child.getAttributeValue("signed") == "1"
                    defaultFloat     = child.getAttributeValue("float") == "1"
                }
                XDF_CATEGORY_TAG -> {
                    val idx  = parseInt(child.getAttributeValue(XDF_INDEX_TAG))
                    val name = child.getAttributeValue("name") ?: ""
                    categories[idx] = name
                }
            }
        }

        return XdfHeader(
            deftitle             = deftitle,
            description          = description,
            author               = author,
            baseOffsetValue      = baseOffsetValue,
            baseOffsetSubtract   = baseOffsetSubtract,
            defaultSizeBits      = defaultSizeBits,
            defaultSigDigits     = defaultSigDigits,
            defaultLsbFirst      = defaultLsbFirst,
            defaultSigned        = defaultSigned,
            defaultFloat         = defaultFloat,
            categories           = categories
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // EMBEDDEDDATA parsing helper — returns a partial AxisDefinition that
    // will be merged with the parent XDFAXIS children.
    // ─────────────────────────────────────────────────────────────────────

    private data class EmbedData(
        val type: Int,
        val rawAddress: Int,
        val sizeBits: Int,
        val rowCount: Int,
        val columnCount: Int,
        val majorStrideBits: Int,
        val minorStrideBits: Int,
        val lsbFirst: Boolean?,   // null = inherit from DEFAULTS
        val isFloat: Boolean?,    // null = inherit from DEFAULTS
        val isColumnMajor: Boolean?
    )

    private fun parseEmbedData(el: Element, header: XdfHeader): EmbedData {
        val typeFlags     = parseInt(el.getAttributeValue(XDF_TYPE_FLAG))
        val rawAddress    = parseInt(el.getAttributeValue(XDF_ADDRESS_TAG))
        val sizeBits      = el.getAttributeValue(XDF_SIZE_BITS_TAG)?.toIntOrNull() ?: header.defaultSizeBits
        val rowCount      = el.getAttributeValue(XDF_ROW_COUNT_TAG)?.toIntOrNull() ?: 0
        val columnCount   = el.getAttributeValue(XDF_COLUMN_COUNT_TAG)?.toIntOrNull() ?: 0
        val majorStride   = el.getAttributeValue(XDF_MAJOR_STRIDE_TAG)?.toIntOrNull() ?: 0
        val minorStride   = el.getAttributeValue(XDF_MINOR_STRIDE_TAG)?.toIntOrNull() ?: 0

        // Decode mmedtypeflags bitfield
        // bit 0 = signed, bit 1 = lsb-first override, bit 2 = column-major, bit 3 = float
        val hasTypeFlags  = el.getAttribute(XDF_TYPE_FLAG) != null
        val lsbFirst      = if (hasTypeFlags) (typeFlags and 0x02) != 0 else null
        val isFloat       = if (hasTypeFlags) (typeFlags and 0x08) != 0 else null
        val isColumnMajor = if (hasTypeFlags) (typeFlags and 0x04) != 0 else null

        return EmbedData(
            type            = typeFlags,
            rawAddress      = rawAddress,
            sizeBits        = sizeBits,
            rowCount        = rowCount,
            columnCount     = columnCount,
            majorStrideBits = majorStride,
            minorStrideBits = minorStride,
            lsbFirst        = lsbFirst,
            isFloat         = isFloat,
            isColumnMajor   = isColumnMajor
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // XDFAXIS parsing
    // ─────────────────────────────────────────────────────────────────────

    private data class AxisAccumulator(
        var type: Int = 0,
        var rawAddress: Int = 0,
        var sizeBits: Int = 0,
        var rowCount: Int = 0,
        var columnCount: Int = 0,
        var majorStrideBits: Int = 0,
        var minorStrideBits: Int = 0,
        var lsbFirstOverride: Boolean? = null,
        var isFloatOverride: Boolean? = null,
        var isColumnMajorOverride: Boolean? = null,
        var indexCount: Int = 0,
        var units: String = "-",
        var equation: String = "X",
        var varId: String = "X",
        var decimalPl: Int = 2,
        var min: Double = Double.NaN,
        var max: Double = Double.NaN,
        val axisValues: MutableList<Pair<Int, Float>> = mutableListOf()
    )

    private fun parseAxisElement(axisEl: Element, header: XdfHeader): Pair<String, AxisAccumulator> {
        val axisId = axisEl.getAttributeValue(XDF_ID_TAG) ?: "z"
        val acc = AxisAccumulator()

        for (child in axisEl.children) {
            when (child.name) {
                XDF_INDEX_COUNT_TAG -> acc.indexCount  = child.textTrim.toIntOrNull() ?: 0
                XDF_UNITS_TAG       -> acc.units       = child.textTrim.ifEmpty { "-" }
                XDF_DECIMAL_PL_TAG  -> acc.decimalPl   = child.textTrim.toIntOrNull() ?: 2
                XDF_MIN_TAG         -> acc.min         = child.textTrim.toDoubleOrNull() ?: Double.NaN
                XDF_MAX_TAG         -> acc.max         = child.textTrim.toDoubleOrNull() ?: Double.NaN
                XDF_EMBEDDED_TAG    -> {
                    val ed = parseEmbedData(child, header)
                    acc.type                = ed.type
                    acc.rawAddress          = ed.rawAddress
                    acc.sizeBits            = ed.sizeBits
                    acc.rowCount            = ed.rowCount
                    acc.columnCount         = ed.columnCount
                    acc.majorStrideBits     = ed.majorStrideBits
                    acc.minorStrideBits     = ed.minorStrideBits
                    acc.lsbFirstOverride    = ed.lsbFirst
                    acc.isFloatOverride     = ed.isFloat
                    acc.isColumnMajorOverride = ed.isColumnMajor
                }
                XDF_MATH_TAG -> {
                    acc.equation = child.getAttributeValue(XDF_EQUATION_TAG) ?: "X"
                    for (varEl in child.children) {
                        if (varEl.name == XDF_VAR_TAG) {
                            acc.varId = varEl.getAttributeValue(XDF_ID_TAG) ?: "X"
                        }
                    }
                }
                XDF_LABEL_TAG -> {
                    val index = parseInt(child.getAttributeValue(XDF_INDEX_TAG))
                    val value = runCatching { child.getAttributeValue(XDF_VALUE_TAG)?.toFloat() ?: 0f }.getOrElse { 0f }
                    acc.axisValues.add(index to value)
                }
            }
        }

        return axisId to acc
    }

    private fun accToAxisDef(
        id: String,
        acc: AxisAccumulator,
        header: XdfHeader,
        tableCategories: List<Int>
    ): AxisDefinition {
        val resolvedAddress = header.resolveAddress(acc.rawAddress)
        val effectiveLsbFirst  = acc.lsbFirstOverride  ?: header.defaultLsbFirst
        val effectiveIsFloat   = acc.isFloatOverride   ?: header.defaultFloat
        val effectiveColMajor  = acc.isColumnMajorOverride ?: false

        return AxisDefinition(
            id              = id,
            type            = acc.type,
            address         = resolvedAddress,
            indexCount      = acc.indexCount,
            sizeBits        = if (acc.sizeBits == 0) header.defaultSizeBits else acc.sizeBits,
            rowCount        = acc.rowCount,
            columnCount     = acc.columnCount,
            unit            = acc.units,
            equation        = acc.equation.ifBlank { "X" },
            varId           = acc.varId.ifBlank { "X" },
            axisValues      = acc.axisValues.sortedBy { it.first },
            majorStrideBits = acc.majorStrideBits,
            minorStrideBits = acc.minorStrideBits,
            lsbFirst        = effectiveLsbFirst,
            isFloat         = effectiveIsFloat,
            isColumnMajor   = effectiveColMajor,
            decimalPl       = acc.decimalPl,
            min             = acc.min,
            max             = acc.max,
            categories      = tableCategories
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // XDFTABLE
    // ─────────────────────────────────────────────────────────────────────

    private fun parseTable(el: Element, header: XdfHeader): TableDefinition? {
        var tableName        = ""
        var tableDescription = ""
        val tableCategories  = mutableListOf<Int>()
        val axisAccumulators = mutableMapOf<String, AxisAccumulator>()

        for (child in el.children) {
            when (child.name) {
                XDF_TABLE_TITLE_TAG     -> tableName        = child.textTrim
                XDF_TABLE_DESCRIPTION_TAG -> tableDescription = child.textTrim
                XDF_CATEGORY_MEM_TAG    -> tableCategories.add(parseInt(child.getAttributeValue("category")))
                XDF_AXIS_TAG            -> {
                    val (id, acc) = parseAxisElement(child, header)
                    axisAccumulators[id] = acc
                }
            }
        }

        val zAcc = axisAccumulators["z"] ?: return null   // z is mandatory
        val xAcc = axisAccumulators["x"]
        val yAcc = axisAccumulators["y"]

        val cats = tableCategories.toList()
        val xAxisDef = xAcc?.let { accToAxisDef("x", it, header, cats) }
        val yAxisDef = yAcc?.let { accToAxisDef("y", it, header, cats) }
        val zAxisDef = accToAxisDef("z", zAcc, header, cats)

        return TableDefinition(tableName, tableDescription, xAxisDef, yAxisDef, zAxisDef)
    }

    // ─────────────────────────────────────────────────────────────────────
    // XDFCONSTANT
    // ─────────────────────────────────────────────────────────────────────

    private fun parseConstant(el: Element, header: XdfHeader): TableDefinition? {
        var tableName        = ""
        var tableDescription = ""
        val tableCategories  = mutableListOf<Int>()
        var acc              = AxisAccumulator()

        for (child in el.children) {
            when (child.name) {
                XDF_TABLE_TITLE_TAG       -> tableName        = child.textTrim
                XDF_TABLE_DESCRIPTION_TAG -> tableDescription = child.textTrim
                XDF_UNITS_TAG             -> acc.units        = child.textTrim.ifEmpty { "-" }
                XDF_DECIMAL_PL_TAG        -> acc.decimalPl    = child.textTrim.toIntOrNull() ?: 2
                XDF_CATEGORY_MEM_TAG      -> tableCategories.add(parseInt(child.getAttributeValue("category")))
                XDF_EMBEDDED_TAG          -> {
                    val ed = parseEmbedData(child, header)
                    acc.type             = ed.type
                    acc.rawAddress       = ed.rawAddress
                    acc.sizeBits         = ed.sizeBits
                    acc.majorStrideBits  = ed.majorStrideBits
                    acc.minorStrideBits  = ed.minorStrideBits
                    acc.lsbFirstOverride = ed.lsbFirst
                    acc.isFloatOverride  = ed.isFloat
                    acc.isColumnMajorOverride = ed.isColumnMajor
                }
                XDF_MATH_TAG -> {
                    acc.equation = child.getAttributeValue(XDF_EQUATION_TAG) ?: "X"
                    for (varEl in child.children) {
                        if (varEl.name == XDF_VAR_TAG) acc.varId = varEl.getAttributeValue(XDF_ID_TAG) ?: "X"
                    }
                }
            }
        }

        val cats = tableCategories.toList()
        val zAxisDef = accToAxisDef("z", acc, header, cats)
        return TableDefinition(tableName, tableDescription, null, null, zAxisDef)
    }
}
