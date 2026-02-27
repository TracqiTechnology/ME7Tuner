package data.parser.kp

import data.preferences.kp.KpFilePreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Parses WinOLS KP files and exposes a list of [KpMapHint] objects for use
 * as XDF map-selection hints in the Configuration screen.
 *
 * ## Format overview
 * A KP file is a proprietary EVC/WinOLS binary container:
 *   - Bytes 0..N : WinOLS binary header (magic: `WinOLS File\0`)
 *   - Embedded ZIP archive containing a single entry named `intern`
 *   - `intern` : flat binary record database; each record is variable-length
 *     with the total length stored as uint32-LE in the first 4 bytes
 *
 * ## What we extract
 * From each record we extract reliably:
 *   1. Description string  — null-terminated at a fixed offset (16) within the record
 *   2. Name string         — found by scanning for the pattern `[A-Z][A-Z0-9_]+ (AR HEXADDR)`
 *   3. AR address          — the 5-6 hex-digit address from the name annotation
 *
 * Fields we do NOT attempt to extract (undocumented binary layout):
 * axis dimensions, element sizes, scaling factors, axis breakpoints.
 * For full binary parsing the user must also provide an XDF file.
 *
 * See `documentation/me7-kp-format.md` for the full reverse-engineering notes.
 */
object KpHintParser {

    // Regex: captures "MAPNAME (AR 1E3B0)" or "MAPNAME (AR 1E3B0, RS4 ...)"
    private val NAME_WITH_ADDR_RE = Regex("""([A-Z][A-Z0-9_]{1,24}) \(AR ([0-9A-F]{4,6})""")
    // Regex: plain name without address — at least 2 uppercase chars, ends at null
    private val PLAIN_NAME_RE = Regex("""^([A-Z][A-Z0-9_]{1,24})$""")

    // ZIP magic embedded somewhere inside the KP file
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _hints = MutableStateFlow<List<KpMapHint>>(emptyList())
    val hints: StateFlow<List<KpMapHint>> = _hints.asStateFlow()

    /** Initialise: re-parse whenever the stored KP file preference changes. */
    fun init() {
        scope.launch {
            KpFilePreferences.file.collect { file ->
                _hints.value = if (file.exists() && file.extension.equals("kp", ignoreCase = true)) {
                    runCatching { parseFile(file) }.getOrElse { emptyList() }
                } else {
                    emptyList()
                }
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Parse [file] synchronously and return the extracted hints. */
    fun parseFile(file: File): List<KpMapHint> {
        val raw = file.readBytes()
        val internBytes = extractInternBlob(raw) ?: return emptyList()
        return parseInternBlob(internBytes)
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    /** Locate the embedded ZIP archive inside the raw KP bytes, open it, and
     * return the contents of the `intern` entry.
     */
    private fun extractInternBlob(raw: ByteArray): ByteArray? {
        val zipOffset = findZipOffset(raw) ?: return null
        return try {
            val zipStream = ZipInputStream(java.io.ByteArrayInputStream(raw, zipOffset, raw.size - zipOffset))
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (entry.name == "intern") {
                    return zipStream.readBytes()
                }
                entry = zipStream.nextEntry
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /** Scan [raw] for the first occurrence of the PK local-file-header magic. */
    private fun findZipOffset(raw: ByteArray): Int? {
        val magicB = ZIP_MAGIC[1]
        val magicK = ZIP_MAGIC[2]
        val magic03 = ZIP_MAGIC[3]
        for (i in 0 until raw.size - 4) {
            if (raw[i] == 0x50.toByte() && raw[i + 1] == magicB &&
                raw[i + 2] == magicK && raw[i + 3] == magic03
            ) {
                return i
            }
        }
        return null
    }

    /**
     * Walk the flat binary record database and extract [KpMapHint] for each
     * record that has a recognisable map name.
     *
     * Record layout (byte offsets within each record):
     *   0..3   total record length (uint32-LE, inclusive of these 4 bytes)
     *   4..15  unknown header fields
     *   16..?  description string (null-terminated, Latin-1)
     *   ?..?   dimension fields (undocumented)
     *   ?..?   name string — either "NAME (AR HEXADDR[, ...])" or plain "NAME"
     *   rest   undocumented binary data (scaling, addresses, axis values)
     *
     * We scan the entire record region for the name pattern rather than trying
     * to follow the undocumented field layout.
     */
    private fun parseInternBlob(blob: ByteArray): List<KpMapHint> {
        val results = mutableListOf<KpMapHint>()
        val seen = mutableSetOf<String>()
        var pos = 0

        while (pos <= blob.size - 8) {
            val recSize = readU32(blob, pos)
            if (recSize < 50 || recSize > 20_000 || pos + recSize > blob.size) {
                pos++
                continue
            }

            val recEnd = pos + recSize
            val recBytes = blob.slice(pos until recEnd)

            // --- description string at offset 16 ---
            val desc = readCString(blob, pos + 16)

            // --- scan record region for name + optional address ---
            val regionStr = recBytes.drop(16)
                .joinToString("") { b ->
                    val i = b.toInt() and 0xFF
                    if (i in 32..126) i.toChar().toString() else if (i == 0) "\u0000" else "\u0001"
                }

            val nameMatch = NAME_WITH_ADDR_RE.find(regionStr)
            if (nameMatch != null) {
                val mapName = nameMatch.groupValues[1]
                val addr = nameMatch.groupValues[2].toInt(16)
                if (mapName !in seen) {
                    seen.add(mapName)
                    results.add(KpMapHint(mapName, desc, addr))
                }
                pos = recEnd
                continue
            }

            // Look for plain uppercase name in the second string slot
            // (name string follows description + ~28 bytes of dimension fields)
            val nameStart = 16 + desc.length + 1 + 28 // approx. after dim fields
            if (nameStart + 2 < recEnd) {
                val plainName = readCStringRaw(blob, pos + nameStart)
                if (plainName.length in 2..24 && PLAIN_NAME_RE.matches(plainName)) {
                    if (plainName !in seen) {
                        seen.add(plainName)
                        results.add(KpMapHint(plainName, desc, -1))
                    }
                }
            }

            pos = recEnd
        }

        return results.sortedBy { it.name }
    }

    // ── Binary helpers ────────────────────────────────────────────────────

    private fun readU32(blob: ByteArray, offset: Int): Int {
        if (offset + 3 >= blob.size) return 0
        return ((blob[offset + 3].toInt() and 0xFF) shl 24) or
                ((blob[offset + 2].toInt() and 0xFF) shl 16) or
                ((blob[offset + 1].toInt() and 0xFF) shl 8) or
                (blob[offset].toInt() and 0xFF)
    }

    /** Read a null-terminated ASCII/Latin-1 string; returns "" on error. */
    private fun readCString(blob: ByteArray, offset: Int): String {
        if (offset >= blob.size) return ""
        val end = blob.indexOf(0, offset).takeIf { it >= offset } ?: return ""
        return try {
            blob.decodeToString(offset, end, throwOnInvalidSequence = false)
                .filter { it.code in 32..126 }
        } catch (_: Exception) { "" }
    }

    /** Same as [readCString] but validates it looks like a map name. */
    private fun readCStringRaw(blob: ByteArray, offset: Int): String {
        if (offset >= blob.size) return ""
        val sb = StringBuilder()
        var i = offset
        while (i < blob.size && i < offset + 32) {
            val b = blob[i].toInt() and 0xFF
            if (b == 0) break
            if (b in 32..126) sb.append(b.toChar()) else break
            i++
        }
        return sb.toString()
    }

    private fun ByteArray.indexOf(value: Byte, startIndex: Int): Int {
        for (i in startIndex until size) if (this[i] == value) return i
        return -1
    }
}

