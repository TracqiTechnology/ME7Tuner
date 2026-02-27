package data.parser.xdf

/**
 * Parsed contents of the XDFHEADER element.
 *
 * DEFAULTS supplies fallback values for per-table attributes that may
 * be omitted from an EMBEDDEDDATA element.  BASEOFFSET is a global
 * address adjustment applied to every mmedaddress in the file.
 */
data class XdfHeader(
    val deftitle: String = "",
    val description: String = "",
    val author: String = "",

    // ── BASEOFFSET ──────────────────────────────────────────────────────────
    /** Raw offset value from <BASEOFFSET offset="N" subtract="S" /> */
    val baseOffsetValue: Int = 0,
    /** When true, subtract the offset from addresses; when false, add it. */
    val baseOffsetSubtract: Boolean = false,

    // ── DEFAULTS ───────────────────────────────────────────────────────────
    val defaultSizeBits: Int = 8,
    val defaultSigDigits: Int = 2,
    /** 1 = little-endian (LSB first); 0 = big-endian */
    val defaultLsbFirst: Boolean = true,
    val defaultSigned: Boolean = false,
    val defaultFloat: Boolean = false,

    // ── CATEGORY map index → name ──────────────────────────────────────────
    val categories: Map<Int, String> = emptyMap()
) {
    /**
     * Resolve a raw mmedaddress from the XDF to the actual byte offset in the
     * binary file, applying BASEOFFSET.
     */
    fun resolveAddress(rawAddress: Int): Int {
        if (rawAddress == 0) return 0
        return if (baseOffsetSubtract) rawAddress - baseOffsetValue
        else rawAddress + baseOffsetValue
    }
}

