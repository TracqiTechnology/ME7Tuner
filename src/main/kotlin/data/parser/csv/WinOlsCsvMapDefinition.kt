package data.parser.csv

/**
 * One map/table/scalar definition parsed from a WinOLS CSV export
 * (File → Export → Map data as CSV inside WinOLS).
 *
 * ## CSV column layout (0-based)
 * | Col | Field        | Example                            |
 * |-----|--------------|------------------------------------|
 * |  0  | id           | "KFPBRK (AR 1E3B0)"                |
 * |  1  | address      | "0x1e3b0"                          |
 * |  2  | name         | "Correction factor for ..."        |
 * |  3  | size         | "10x10"                            |
 * |  4  | organization | "8 Bit" / "16 Bit (LoHi)"         |
 * |  5  | description  | German/English description         |
 * |  6  | units        | "%/100hPa"                         |
 * |  7  | xAddress     | "0x1bb3a" / "-"                    |
 * |  8  | yAddress     | "0x1037a" / "-"                    |
 * |  9  | xUnits       | "%"                                |
 * | 10  | yUnits       | "Upm"                              |
 * | 11  | scale        | "0.023438"                         |
 * | 12  | xScale       | "0.75"                             |
 * | 13  | yScale       | "40.0"                             |
 * | 14  | valueMin     | "0.0"                              |
 * | 15  | valueMax     | "127.0"                            |
 * | 16  | valueMinHex  | "0x0"                              |
 * | 17  | valueMaxHex  | "0x7f"                             |
 * | 18+ | crossRefs    | values from other KP variants      |
 *
 * The `id` column may contain an `(AR HEXADDR)` annotation pointing to the
 * binary address in the reference ECU (8D0907551M), and optionally
 * `(RS4 HEXADDR)` for other variants.
 *
 * See [documentation/me7-kp-format.md] for the full analysis.
 */
data class WinOlsCsvMapDefinition(
    /** WinOLS map identifier, e.g. "KFPBRK". May be "-" for unnamed axis tables. */
    val id: String,
    /** Full raw ID string including any address annotations, e.g. "KFPBRK (AR 1E3B0)" */
    val rawId: String,
    /** Binary address in the ECU BIN file (0 if not present / "-") */
    val address: Int,
    /** Human-readable map name/title */
    val name: String,
    /** Number of columns in the data table */
    val columns: Int,
    /** Number of rows in the data table */
    val rows: Int,
    /** Element bit width: 8, 16, or 32 */
    val sizeBits: Int,
    /** Byte order: true = little-endian (LoHi), false = big-endian */
    val lsbFirst: Boolean,
    /** Long description (may be in German) */
    val description: String,
    /** Physical unit of the Z (value) axis, e.g. "%/100hPa" */
    val units: String,
    /** Binary address of the X axis breakpoint array, or -1 */
    val xAddress: Int,
    /** Binary address of the Y axis breakpoint array, or -1 */
    val yAddress: Int,
    /** Physical unit of the X axis */
    val xUnits: String,
    /** Physical unit of the Y axis */
    val yUnits: String,
    /** Scaling factor for Z values: physical = raw × scale */
    val scale: Double,
    /** Scaling factor for X axis breakpoints */
    val xScale: Double,
    /** Scaling factor for Y axis breakpoints */
    val yScale: Double,
    /** Physical minimum value */
    val valueMin: Double,
    /** Physical maximum value */
    val valueMax: Double,
    /** Cross-references to values in other KP variants (other ECU part numbers) */
    val crossRefs: Map<String, String> = emptyMap()
) {
    /** True when the map has a valid binary address */
    val hasAddress: Boolean get() = address > 0

    /** True when an X axis address is available */
    val hasXAxis: Boolean get() = xAddress > 0

    /** True when a Y axis address is available */
    val hasYAxis: Boolean get() = yAddress > 0

    /** True when this is a 2D table (has both X and Y axes) */
    val is2D: Boolean get() = columns > 1 && rows > 1

    /** True when this is a named map (not an unnamed axis helper) */
    val isNamedMap: Boolean get() = id != "-" && id.isNotBlank()

    /** Dimension string, e.g. "10×10" */
    val dimensionString: String get() = "${columns}×${rows}"

    /** Short summary string for display in the map picker */
    fun toPickerLabel(): String {
        val addrStr = if (hasAddress) " @ 0x${address.toString(16).uppercase()}" else ""
        val dimStr = if (is2D) " [$dimensionString]" else " [${maxOf(columns, rows)}]"
        val unitStr = if (units.isNotBlank() && units != "-") " — $units" else ""
        return "$id$addrStr$dimStr$unitStr"
    }
}

