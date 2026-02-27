package data.parser.xdf

/**
 * Describes one axis (x / y / z) within an XDF table or constant.
 *
 * Fields map directly to TunerPro XDF EMBEDDEDDATA attributes and the
 * surrounding XDFAXIS children.  Defaults are chosen so that the
 * existing behaviour (little-endian, unsigned, integer, row-major,
 * no stride) is preserved when the XDF omits an attribute.
 */
data class AxisDefinition(
    val id: String,
    /** mmedtypeflags bit 0: 1 = signed integer (or float when isFloat=true) */
    val type: Int,
    /** mmedaddress — binary file byte offset BEFORE baseOffset is applied */
    val address: Int,
    val indexCount: Int,
    /** mmedelementsizebits — 8, 16 or 32 */
    val sizeBits: Int,
    val rowCount: Int,
    val columnCount: Int,
    val unit: String,
    val equation: String,
    val varId: String,
    val axisValues: List<Pair<Int, Float>>,

    // ── Stride ─────────────────────────────────────────────────────────────
    /** mmedmajorstridebits — bits between the start of consecutive rows.
     *  0 = tightly packed.  Negative = virtual/linked axis (no data at address). */
    val majorStrideBits: Int = 0,
    /** mmedminorstridebits — bits between consecutive elements within a row.
     *  0 = tightly packed. */
    val minorStrideBits: Int = 0,

    // ── Byte order / encoding ───────────────────────────────────────────────
    /** true = little-endian (LSB first).  Sourced from mmedtypeflags bit 1
     *  (per-axis) or inherited from DEFAULTS lsbfirst. */
    val lsbFirst: Boolean = true,
    /** true = IEEE-754 single-precision float (mmedtypeflags bit 3 or DEFAULTS float=1) */
    val isFloat: Boolean = false,
    /** true = data is stored column-major instead of row-major (mmedtypeflags bit 2) */
    val isColumnMajor: Boolean = false,

    // ── Display hints ───────────────────────────────────────────────────────
    val decimalPl: Int = 2,
    val min: Double = Double.NaN,
    val max: Double = Double.NaN,

    // ── Category membership ────────────────────────────────────────────────
    /** Category indices from CATEGORYMEM elements (can be empty) */
    val categories: List<Int> = emptyList()
) {
    /** True when this axis is a virtual/linked axis — it has no data address
     *  of its own (majorStrideBits < 0). Data comes from LABELs or another table. */
    val isVirtual: Boolean get() = majorStrideBits < 0 || address == 0

    /** True if the type flag indicates this axis is signed */
    val isSigned: Boolean get() = (type and 0x01) != 0
}
