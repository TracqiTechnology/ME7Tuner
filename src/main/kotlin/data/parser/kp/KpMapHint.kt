package data.parser.kp

/**
 * A name/address hint extracted from a WinOLS KP file.
 *
 * The KP format is a proprietary binary container. We extract only what can be
 * reliably read without the private format spec: the map name and, where present,
 * the raw ECU binary address embedded in the name string as "(AR HEXADDR)".
 *
 * See documentation/me7-kp-format.md for the full format analysis.
 */
data class KpMapHint(
    /** Map identifier, e.g. "KFPBRK", "MLHFM", "KRKTE" */
    val name: String,
    /** Human-readable description, e.g. "Correction factor for combustion chamber pressure" */
    val description: String,
    /** Raw ECU binary address from "(AR HEXADDR)" annotation, or -1 if not present */
    val arAddress: Int
) {
    /** True when we have a binary address that can be matched against XDF definitions */
    val hasAddress: Boolean get() = arAddress >= 0
}

