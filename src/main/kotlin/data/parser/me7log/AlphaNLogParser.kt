package data.parser.me7log

import data.contract.Me7LogFileContract
import org.apache.commons.csv.CSVFormat
import java.io.File
import java.io.FileReader

/**
 * Lightweight parser for Alpha-N diagnostic logs.
 *
 * Parses CSV logs that contain both mshfm_w (MAF airflow) and msdk_w (throttle
 * model airflow) for alpha-n accuracy analysis. Unlike the main Me7LogParser,
 * this only requires nmot + mshfm_w + msdk_w as mandatory columns; throttle angle,
 * pressure and baro are optional context.
 */
object AlphaNLogParser {

    /**
     * Parse a single CSV log file for alpha-n diagnostic data.
     *
     * @return Map of headers to value lists, or empty map if required columns missing.
     */
    fun parseLogFile(file: File): Map<Me7LogFileContract.Header, List<Double>> {
        val map = mutableMapOf<Me7LogFileContract.Header, MutableList<Double>>()
        map[Me7LogFileContract.Header.RPM_COLUMN_HEADER] = mutableListOf()
        map[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER] = mutableListOf()
        map[Me7LogFileContract.Header.THROTTLE_MODEL_AIRFLOW_HEADER] = mutableListOf()
        map[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER] = mutableListOf()
        map[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER] = mutableListOf()
        map[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER] = mutableListOf()
        map[Me7LogFileContract.Header.ENGINE_LOAD_HEADER] = mutableListOf()

        try {
            FileReader(file).use { reader ->
                val records = CSVFormat.RFC4180.parse(reader)
                val iterator = records.iterator()

                // Find column indices
                var rpmIdx = -1
                var mshfmIdx = -1
                var msdkIdx = -1
                var wdkbaIdx = -1
                var pvdksIdx = -1
                var pusIdx = -1
                var rlIdx = -1
                var headersFound = false

                while (iterator.hasNext() && !headersFound) {
                    val record = iterator.next()
                    for (i in 0 until record.size()) {
                        val trimmed = record.get(i).trim()
                        when (trimmed) {
                            Me7LogFileContract.Header.RPM_COLUMN_HEADER.header -> rpmIdx = i
                            Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER.header -> mshfmIdx = i
                            Me7LogFileContract.Header.THROTTLE_MODEL_AIRFLOW_HEADER.header -> msdkIdx = i
                            Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER.header -> wdkbaIdx = i
                            Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER.header -> pvdksIdx = i
                            Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER.header -> pusIdx = i
                            Me7LogFileContract.Header.ENGINE_LOAD_HEADER.header -> rlIdx = i
                        }
                    }
                    // Minimum: RPM + mshfm + msdk
                    headersFound = rpmIdx != -1 && mshfmIdx != -1 && msdkIdx != -1
                }

                if (!headersFound) return emptyMap()

                while (iterator.hasNext()) {
                    val record = iterator.next()
                    try {
                        val rpm = record.get(rpmIdx).toDouble()
                        val mshfm = record.get(mshfmIdx).toDouble()
                        val msdk = record.get(msdkIdx).toDouble()

                        map[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!.add(rpm)
                        map[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER]!!.add(mshfm)
                        map[Me7LogFileContract.Header.THROTTLE_MODEL_AIRFLOW_HEADER]!!.add(msdk)

                        // Optional columns
                        if (wdkbaIdx != -1) {
                            try { map[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!.add(record.get(wdkbaIdx).toDouble()) }
                            catch (_: Exception) { map[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!.add(0.0) }
                        }
                        if (pvdksIdx != -1) {
                            try { map[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!.add(record.get(pvdksIdx).toDouble()) }
                            catch (_: Exception) { map[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!.add(0.0) }
                        }
                        if (pusIdx != -1) {
                            try { map[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER]!!.add(record.get(pusIdx).toDouble()) }
                            catch (_: Exception) { map[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER]!!.add(0.0) }
                        }
                        if (rlIdx != -1) {
                            try { map[Me7LogFileContract.Header.ENGINE_LOAD_HEADER]!!.add(record.get(rlIdx).toDouble()) }
                            catch (_: Exception) { map[Me7LogFileContract.Header.ENGINE_LOAD_HEADER]!!.add(0.0) }
                        }
                    } catch (_: NumberFormatException) {
                    } catch (_: ArrayIndexOutOfBoundsException) {
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Remove optional columns that have no data
        val rpmSize = map[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!.size
        map.entries.removeAll { (key, value) ->
            key != Me7LogFileContract.Header.RPM_COLUMN_HEADER &&
            key != Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER &&
            key != Me7LogFileContract.Header.THROTTLE_MODEL_AIRFLOW_HEADER &&
            value.size != rpmSize
        }

        return map
    }

    /**
     * Parse all CSV files in a directory for alpha-n diagnostic data.
     *
     * @return Merged map with all samples aggregated.
     */
    fun parseLogDirectory(directory: File): Map<Me7LogFileContract.Header, List<Double>> {
        val files = directory.listFiles()?.filter { it.extension.equals("csv", ignoreCase = true) }
            ?: return emptyMap()

        val merged = mutableMapOf<Me7LogFileContract.Header, MutableList<Double>>()

        for (file in files) {
            val fileData = parseLogFile(file)
            for ((header, values) in fileData) {
                merged.getOrPut(header) { mutableListOf() }.addAll(values)
            }
        }

        return merged
    }
}

