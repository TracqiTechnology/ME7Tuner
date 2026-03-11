package data.parser.med17log

import data.contract.Med17LogFileContract
import data.contract.Med17LogFileContract.Header as H
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileReader

/**
 * Parser for ScorpionEFI / DynoScorpion MED17 CSV log files.
 *
 * ScorpionEFI CSV format:
 *   Line 1: Metadata — `DS1 firmware:...,WUAZZZFFXJ...,MED17_1_62,...`
 *   Line 2: Headers — `Time(s),Description(signal_name) (unit),...`
 *   Line 3+: Numeric data rows
 *
 * Signal names are extracted from the parenthetical group in each header column.
 * For example, `Eng spd(nmot_w) (1/min)` → signal name = `nmot_w`.
 *
 * This parser produces `Map<Med17LogFileContract.Header, List<Double>>` which is
 * then adapted by [Med17LogAdapter] to feed into the platform-generic optimizer
 * and LDRPID calculators.
 */
class Med17LogParser {

    enum class LogType {
        LDRPID,
        OPTIMIZER
    }

    fun interface ProgressCallback {
        fun onProgress(value: Int, max: Int)
    }

    // Column indices resolved from header line
    private var columnIndices = mutableMapOf<Med17LogFileContract.Header, Int>()

    fun parseLogDirectory(
        logType: LogType,
        directory: File,
        callback: ProgressCallback
    ): Map<Med17LogFileContract.Header, List<Double>> {
        val map = generateMap(logType)
        val files = directory.listFiles()?.filter {
            it.isFile && it.name.endsWith(".csv", ignoreCase = true)
        } ?: return map
        val numFiles = files.size
        var count = 0

        for (file in files) {
            parse(file, logType, map)
            callback.onProgress(++count, numFiles)
        }

        return map
    }

    fun parseLogFile(
        logType: LogType,
        file: File
    ): Map<Med17LogFileContract.Header, List<Double>> {
        val map = generateMap(logType)
        parse(file, logType, map)
        return map
    }

    private fun parse(
        file: File,
        logType: LogType,
        map: Map<Med17LogFileContract.Header, MutableList<Double>>
    ) {
        columnIndices.clear()

        try {
            FileReader(file).use { reader ->
                val records = CSVFormat.RFC4180.parse(reader)
                val iterator = records.iterator()
                var headersFound = false
                var isFirstLine = true

                while (iterator.hasNext()) {
                    val record = iterator.next()

                    // Line 1: metadata line (DS1 firmware info) — skip
                    if (isFirstLine) {
                        isFirstLine = false
                        // Check if this is a ScorpionEFI metadata line
                        if (record.size() > 0) {
                            val first = record.get(0)
                            if (first.contains("firmware") || first.contains("DS1") || first.contains("MED17")) {
                                continue
                            }
                        }
                    }

                    // Try to parse as header line
                    if (!headersFound) {
                        for (i in 0 until record.size()) {
                            val raw = record.get(i).trim()
                            val signalName = extractSignalName(raw)

                            // Match against all MED17 headers by their current header value
                            for (header in Med17LogFileContract.Header.entries) {
                                if (header.header == signalName || header.header == raw) {
                                    columnIndices[header] = i
                                }
                            }

                            // Special case: Time column is just "Time(s)" → signal = "s" but we want the column
                            if (raw.startsWith("Time(") || raw == "Time") {
                                columnIndices[H.TIME_STAMP_COLUMN_HEADER] = i
                            }
                        }

                        headersFound = headersFound(logType)
                        if (headersFound) continue
                        // If this line didn't produce headers, try next line
                        continue
                    }

                    // Parse data rows
                    try {
                        when (logType) {
                            LogType.LDRPID -> parseLdrpidRow(record, map)
                            LogType.OPTIMIZER -> parseOptimizerRow(record, map)
                        }
                    } catch (_: NumberFormatException) {
                    } catch (_: ArrayIndexOutOfBoundsException) {
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Extract signal name from ScorpionEFI column header format.
     *
     * Format: `Description text(signal_name) (unit)`
     * We need the content of the LAST parenthetical group that is NOT the trailing unit.
     *
     * Examples:
     *   `Eng spd(nmot_w) (1/min)` → `nmot_w`
     *   `Load rel(rl_w) (%)` → `rl_w`
     *   `WGDC(tvldste_w) (%)` → `tvldste_w`
     *   `Tgt dist fac prop port fuel inj(InjSys_facPrtnPfiTar) (-)` → `InjSys_facPrtnPfiTar`
     *   `Time(s)` → `s` (special case handled elsewhere)
     */
    private fun extractSignalName(header: String): String {
        // Find all parenthetical groups
        val groups = Regex("\\(([^)]+)\\)").findAll(header).toList()

        return when {
            groups.size >= 2 -> {
                // Last group is unit, second-to-last is the signal name
                groups[groups.size - 2].groupValues[1]
            }
            groups.size == 1 -> {
                // Single group — could be the signal or the unit
                groups[0].groupValues[1]
            }
            else -> header.trim()
        }
    }

    private fun parseLdrpidRow(
        record: CSVRecord,
        map: Map<Med17LogFileContract.Header, MutableList<Double>>
    ) {
        val time = getDouble(record, H.TIME_STAMP_COLUMN_HEADER) ?: return
        val rpm = getDouble(record, H.RPM_COLUMN_HEADER) ?: return
        val throttle = getDouble(record, H.THROTTLE_PLATE_ANGLE_HEADER) ?: return
        val baro = getDouble(record, H.BAROMETRIC_PRESSURE_HEADER) ?: return
        val wgdc = getDouble(record, H.WASTEGATE_DUTY_CYCLE_HEADER)
            ?: getDouble(record, H.LDR_DUTY_CYCLE_HEADER) ?: return
        val absBoost = getDouble(record, H.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER) ?: return
        val gear = getDouble(record, H.SELECTED_GEAR_HEADER) ?: 0.0

        map[H.TIME_STAMP_COLUMN_HEADER]!!.add(time)
        map[H.RPM_COLUMN_HEADER]!!.add(rpm)
        map[H.THROTTLE_PLATE_ANGLE_HEADER]!!.add(throttle)
        map[H.BAROMETRIC_PRESSURE_HEADER]!!.add(baro)
        map[H.WASTEGATE_DUTY_CYCLE_HEADER]!!.add(wgdc)
        map[H.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!.add(absBoost)
        map[H.SELECTED_GEAR_HEADER]!!.add(gear)
    }

    private fun parseOptimizerRow(
        record: CSVRecord,
        map: Map<Med17LogFileContract.Header, MutableList<Double>>
    ) {
        val time = getDouble(record, H.TIME_STAMP_COLUMN_HEADER) ?: return
        val rpm = getDouble(record, H.RPM_COLUMN_HEADER) ?: return
        val throttle = getDouble(record, H.THROTTLE_PLATE_ANGLE_HEADER) ?: return
        val wgdc = getDouble(record, H.WASTEGATE_DUTY_CYCLE_HEADER)
            ?: getDouble(record, H.LDR_DUTY_CYCLE_HEADER) ?: return
        val baro = getDouble(record, H.BAROMETRIC_PRESSURE_HEADER) ?: return
        val absBoost = getDouble(record, H.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER) ?: return
        val reqPressure = getDouble(record, H.REQUESTED_PRESSURE_HEADER) ?: return
        val reqLoad = getDouble(record, H.REQUESTED_LOAD_HEADER) ?: return
        val engLoad = getDouble(record, H.ENGINE_LOAD_HEADER) ?: return

        map[H.TIME_STAMP_COLUMN_HEADER]!!.add(time)
        map[H.RPM_COLUMN_HEADER]!!.add(rpm)
        map[H.THROTTLE_PLATE_ANGLE_HEADER]!!.add(throttle)
        map[H.WASTEGATE_DUTY_CYCLE_HEADER]!!.add(wgdc)
        map[H.BAROMETRIC_PRESSURE_HEADER]!!.add(baro)
        map[H.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!.add(absBoost)
        map[H.REQUESTED_PRESSURE_HEADER]!!.add(reqPressure)
        map[H.REQUESTED_LOAD_HEADER]!!.add(reqLoad)
        map[H.ENGINE_LOAD_HEADER]!!.add(engLoad)

        // Optional: fupsrls_w (live VE factor — logged, useful for diagnostics)
        getDouble(record, H.FUPSRLS_HEADER)?.let {
            map[H.FUPSRLS_HEADER]!!.add(it)
        }
    }

    private fun getDouble(
        record: CSVRecord,
        header: Med17LogFileContract.Header
    ): Double? {
        val idx = columnIndices[header] ?: return null
        return try {
            if (idx < record.size()) record.get(idx).trim().toDoubleOrNull() else null
        } catch (_: Exception) {
            null
        }
    }

    private fun headersFound(logType: LogType): Boolean {
        val hasTime = H.TIME_STAMP_COLUMN_HEADER in columnIndices
        val hasRpm = H.RPM_COLUMN_HEADER in columnIndices
        val hasThrottle = H.THROTTLE_PLATE_ANGLE_HEADER in columnIndices
        val hasBaro = H.BAROMETRIC_PRESSURE_HEADER in columnIndices
        val hasBoost = H.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER in columnIndices
        val hasWgdc = H.WASTEGATE_DUTY_CYCLE_HEADER in columnIndices ||
                      H.LDR_DUTY_CYCLE_HEADER in columnIndices

        return when (logType) {
            LogType.LDRPID -> hasTime && hasRpm && hasThrottle && hasBaro && hasBoost && hasWgdc
            LogType.OPTIMIZER -> hasTime && hasRpm && hasThrottle && hasBaro && hasBoost &&
                    hasWgdc &&
                    H.REQUESTED_PRESSURE_HEADER in columnIndices &&
                    H.REQUESTED_LOAD_HEADER in columnIndices &&
                    H.ENGINE_LOAD_HEADER in columnIndices
        }
    }

    private fun generateMap(logType: LogType): Map<Med17LogFileContract.Header, MutableList<Double>> {
        val map = mutableMapOf<Med17LogFileContract.Header, MutableList<Double>>()

        map[H.START_TIME_HEADER] = mutableListOf()

        when (logType) {
            LogType.LDRPID -> {
                map[H.TIME_STAMP_COLUMN_HEADER] = mutableListOf()
                map[H.RPM_COLUMN_HEADER] = mutableListOf()
                map[H.THROTTLE_PLATE_ANGLE_HEADER] = mutableListOf()
                map[H.BAROMETRIC_PRESSURE_HEADER] = mutableListOf()
                map[H.WASTEGATE_DUTY_CYCLE_HEADER] = mutableListOf()
                map[H.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER] = mutableListOf()
                map[H.SELECTED_GEAR_HEADER] = mutableListOf()
            }
            LogType.OPTIMIZER -> {
                map[H.TIME_STAMP_COLUMN_HEADER] = mutableListOf()
                map[H.RPM_COLUMN_HEADER] = mutableListOf()
                map[H.THROTTLE_PLATE_ANGLE_HEADER] = mutableListOf()
                map[H.WASTEGATE_DUTY_CYCLE_HEADER] = mutableListOf()
                map[H.BAROMETRIC_PRESSURE_HEADER] = mutableListOf()
                map[H.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER] = mutableListOf()
                map[H.REQUESTED_PRESSURE_HEADER] = mutableListOf()
                map[H.REQUESTED_LOAD_HEADER] = mutableListOf()
                map[H.ENGINE_LOAD_HEADER] = mutableListOf()
                map[H.FUPSRLS_HEADER] = mutableListOf()
            }
        }

        return map
    }
}
