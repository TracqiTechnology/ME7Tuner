package data.parser.csv

import data.preferences.csv.WinOlsCsvFilePreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * Parses a WinOLS CSV export file (File → Export → Map data as CSV in WinOLS)
 * and exposes the resulting [WinOlsCsvMapDefinition] list as a [StateFlow].
 *
 * ## How it complements KP hint mode
 * The binary `.kp` file format is proprietary — we can only reliably extract
 * map names and binary addresses (name/address hints). The WinOLS CSV export
 * contains the **full** map metadata: dimensions, element size, axis addresses,
 * scaling factors, physical units, and cross-references to other KP variants.
 *
 * By loading the CSV alongside the KP file the user gets:
 *  - Map picker pre-selection (from KP address hint, unchanged)
 *  - **Richer hint badge** showing dimensions, units, and scaling
 *  - Optional future feature: bridge CSV → XDF-compatible definition for
 *    maps not in the XDF file
 *
 * ## CSV format
 * The CSV must have been exported from WinOLS with the standard column layout
 * (see [WinOlsCsvMapDefinition] for the column-to-field mapping). The first
 * line is a header row that is detected and skipped automatically.
 *
 * See `documentation/me7-kp-format.md` for the full format analysis.
 */
object WinOlsCsvParser {

    // Regex: "(AR 1E3B0)" — captures 4-6 hex-digit address annotations in the ID column
    private val AR_ADDR_RE = Regex("""\(AR\s+([0-9A-Fa-f]{4,6})\)""")
    // Regex: "(RS4 10330)" etc — other variant address annotations
    private val VARIANT_ADDR_RE = Regex("""\((\w+)\s+([0-9A-Fa-f]{4,6})\)""")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _definitions = MutableStateFlow<List<WinOlsCsvMapDefinition>>(emptyList())
    val definitions: StateFlow<List<WinOlsCsvMapDefinition>> = _definitions.asStateFlow()

    /** Re-parse whenever the stored CSV file preference changes. */
    fun init() {
        scope.launch {
            WinOlsCsvFilePreferences.file.collect { file ->
                _definitions.value = if (file.exists() && file.extension.equals("csv", ignoreCase = true)) {
                    runCatching { parseFile(file) }.getOrElse { emptyList() }
                } else {
                    emptyList()
                }
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Parse [file] synchronously and return the extracted definitions. */
    fun parseFile(file: File): List<WinOlsCsvMapDefinition> {
        val lines = file.readLines(Charsets.ISO_8859_1)
        if (lines.isEmpty()) return emptyList()

        // Detect (and skip) the header row — it starts with "ID","Address"
        val dataLines = if (lines.first().startsWith("\"ID\"") || lines.first().startsWith("ID,")) {
            lines.drop(1)
        } else {
            lines
        }

        // Collect cross-reference column headers if present
        // Header format: ..., "4Z7907551R.kp", "8D0907551F.kp", "8D0907551G.kp"
        val crossRefHeaders: List<String> = if (lines.first().startsWith("\"ID\"")) {
            parseCsvRow(lines.first())
                .drop(18) // first 18 fixed columns (0..17)
                .map { it.trim('"').trim() }
                .filter { it.isNotBlank() }
        } else emptyList()

        return dataLines
            .mapNotNull { line -> runCatching { parseLine(line, crossRefHeaders) }.getOrNull() }
            .filter { it.isNamedMap }
            .distinctBy { it.rawId + it.address }
            .sortedBy { it.id }
    }

    /** Look up a [WinOlsCsvMapDefinition] by map name (case-insensitive). */
    fun findByName(name: String): WinOlsCsvMapDefinition? =
        _definitions.value.firstOrNull { it.id.equals(name, ignoreCase = true) }

    /** Look up by binary address. */
    fun findByAddress(address: Int): WinOlsCsvMapDefinition? =
        _definitions.value.firstOrNull { it.address == address }

    // ── Internal parsing ──────────────────────────────────────────────────

    private fun parseLine(line: String, crossRefHeaders: List<String>): WinOlsCsvMapDefinition? {
        val cols = parseCsvRow(line)
        if (cols.size < 14) return null

        val rawId      = cols.getOrElse(0) { "-" }.trim('"').trim()
        val addrStr    = cols.getOrElse(1) { "" }.trim('"').trim()
        val name       = cols.getOrElse(2) { "" }.trim('"').trim()
        val sizeStr    = cols.getOrElse(3) { "1x1" }.trim('"').trim()
        val orgStr     = cols.getOrElse(4) { "8 Bit" }.trim('"').trim()
        val desc       = cols.getOrElse(5) { "" }.trim('"').trim()
        val units      = cols.getOrElse(6) { "" }.trim('"').trim().let { if (it == "-") "" else it }
        val xAddrStr   = cols.getOrElse(7) { "-" }.trim('"').trim()
        val yAddrStr   = cols.getOrElse(8) { "-" }.trim('"').trim()
        val xUnits     = cols.getOrElse(9) { "" }.trim('"').trim().let { if (it == "-" || it == "?") "" else it }
        val yUnits     = cols.getOrElse(10) { "" }.trim('"').trim().let { if (it == "-" || it == "?") "" else it }
        val scaleStr   = cols.getOrElse(11) { "1.0" }.trim('"').trim()
        val xScaleStr  = cols.getOrElse(12) { "1.0" }.trim('"').trim()
        val yScaleStr  = cols.getOrElse(13) { "1.0" }.trim('"').trim()
        val valMinStr  = cols.getOrElse(14) { "0.0" }.trim('"').trim()
        val valMaxStr  = cols.getOrElse(15) { "0.0" }.trim('"').trim()

        // Parse map ID — strip address annotations to get the clean name
        val id = rawId.replace(AR_ADDR_RE, "").replace(VARIANT_ADDR_RE, "").trim()
            .trimEnd('(').trim()
            .ifBlank { "-" }

        // Binary address
        val address = parseHex(addrStr)

        // AR address from ID annotation (may differ from address column for some ECU variants)
        // We prefer the explicit address column when present
        val arFromId = AR_ADDR_RE.find(rawId)?.groupValues?.get(1)?.toIntOrNull(16) ?: -1

        // Dimensions: "10x10" → columns=10, rows=10  |  "4x1" → columns=4, rows=1
        val (cols2, rows2) = parseDimensions(sizeStr)

        // Organization: "8 Bit", "16 Bit (LoHi)", "16 Bit (HiLo)", "32 Bit (LoHi)"
        val (sizeBits, lsbFirst) = parseOrganization(orgStr)

        // Axis addresses
        val xAddress = parseHex(xAddrStr)
        val yAddress = parseHex(yAddrStr)

        // Scaling
        val scale  = scaleStr.toDoubleOrNull() ?: 1.0
        val xScale = xScaleStr.toDoubleOrNull() ?: 1.0
        val yScale = yScaleStr.toDoubleOrNull() ?: 1.0
        val valueMin = valMinStr.toDoubleOrNull() ?: 0.0
        val valueMax = valMaxStr.toDoubleOrNull() ?: 0.0

        // Cross-reference values for other KP variants
        val crossRefs: Map<String, String> = if (crossRefHeaders.isNotEmpty()) {
            crossRefHeaders.zip(cols.drop(18).map { it.trim('"').trim() })
                .filter { (_, v) -> v.isNotBlank() }
                .toMap()
        } else emptyMap()

        return WinOlsCsvMapDefinition(
            id = id,
            rawId = rawId,
            address = if (address > 0) address else arFromId,
            name = name,
            columns = cols2,
            rows = rows2,
            sizeBits = sizeBits,
            lsbFirst = lsbFirst,
            description = desc,
            units = units,
            xAddress = xAddress,
            yAddress = yAddress,
            xUnits = xUnits,
            yUnits = yUnits,
            scale = scale,
            xScale = xScale,
            yScale = yScale,
            valueMin = valueMin,
            valueMax = valueMax,
            crossRefs = crossRefs
        )
    }

    // ── CSV tokeniser ─────────────────────────────────────────────────────
    // WinOLS CSV uses comma-separated, double-quote-enclosed fields. Values
    // themselves may contain commas or newlines (multi-line raw data), so we
    // need a proper state-machine tokeniser rather than a simple split.

    fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuote = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuote -> inQuote = true
                c == '"' && inQuote && i + 1 < line.length && line[i + 1] == '"' -> {
                    // Escaped double-quote inside quoted field
                    sb.append('"')
                    i++ // skip the second quote
                }
                c == '"' && inQuote -> inQuote = false
                c == ',' && !inQuote -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun parseHex(s: String): Int {
        if (s.isBlank() || s == "-" || s == "?") return -1
        return runCatching {
            val clean = s.trim().lowercase().removePrefix("0x")
            clean.toInt(16)
        }.getOrElse { -1 }
    }

    private fun parseDimensions(s: String): Pair<Int, Int> {
        val parts = s.lowercase().split("x")
        if (parts.size != 2) return Pair(1, 1)
        val c = parts[0].trim().toIntOrNull() ?: 1
        val r = parts[1].trim().toIntOrNull() ?: 1
        return Pair(c, r)
    }

    private fun parseOrganization(s: String): Pair<Int, Boolean> {
        val lower = s.lowercase()
        val bits = when {
            lower.contains("32") -> 32
            lower.contains("16") -> 16
            else -> 8
        }
        // "LoHi" = little-endian (LSB first); "HiLo" = big-endian
        val lsb = !lower.contains("hilo")
        return Pair(bits, lsb)
    }
}

