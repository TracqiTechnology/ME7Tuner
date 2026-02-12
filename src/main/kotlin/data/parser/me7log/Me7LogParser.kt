package data.parser.me7log

import data.contract.Me7LogFileContract
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileReader

class Me7LogParser {

    enum class LogType {
        OPEN_LOOP,
        CLOSED_LOOP,
        LDRPID,
        KFVPDKSD
    }

    fun interface ProgressCallback {
        fun onProgress(value: Int, max: Int)
    }

    private var timeColumnIndex = -1
    private var rpmColumnIndex = -1
    private var stftColumnIndex = -1
    private var ltftColumnIndex = -1
    private var mafVoltageIndex = -1
    private var mafGramsPerSecondIndex = -1
    private var throttlePlateAngleIndex = -1
    private var lambdaControlActiveIndex = -1
    private var requestedLambdaIndex = -1
    private var fuelInjectorOnTimeIndex = -1
    private var engineLoadIndex = -1
    private var wastegateDutyCycleIndex = -1
    private var barometricPressureIndex = -1
    private var absoluteBoostPressureActualIndex = -1
    private var selectedGearIndex = -1
    private var wideBandO2Index = -1

    fun parseLogDirectory(logType: LogType, directory: File, callback: ProgressCallback): Map<Me7LogFileContract.Header, List<Double>> {
        val map = generateMap(logType)
        val files = directory.listFiles() ?: return map
        val numFiles = files.size
        var count = 0

        for (file in files) {
            parse(file, logType, map)
            callback.onProgress(count++, numFiles)
        }

        return map
    }

    fun parseLogFile(logType: LogType, file: File): Map<Me7LogFileContract.Header, List<Double>> {
        val map = generateMap(logType)
        parse(file, logType, map)
        return map
    }

    private fun parse(file: File, logType: LogType, map: Map<Me7LogFileContract.Header, MutableList<Double>>) {
        resetIndices()
        try {
            FileReader(file).use { reader ->
                var headersFound = false
                val records = CSVFormat.RFC4180.parse(reader)
                val iterator = records.iterator()

                while (iterator.hasNext()) {
                    val record = iterator.next()
                    if (record.size() > 0) {
                        val string = record.get(0)
                        if (string.contains("Log started at:")) {
                            val split = string.split(" ")
                            val timestamp = split[5]
                            val timeParts = timestamp.split(":")
                            val minuteSeconds = timeParts[1].toDouble() * 60
                            val secondsSeconds = timeParts[2].toDouble()
                            val startTime = minuteSeconds + secondsSeconds
                            map[Me7LogFileContract.Header.START_TIME_HEADER]!!.add(startTime)
                        }
                    }

                    for (i in 0 until record.size()) {
                        val trimmed = record.get(i).trim()
                        when {
                            Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER.header == trimmed -> timeColumnIndex = i
                            Me7LogFileContract.Header.RPM_COLUMN_HEADER.header == trimmed -> rpmColumnIndex = i
                            Me7LogFileContract.Header.STFT_COLUMN_HEADER.header == trimmed -> stftColumnIndex = i
                            Me7LogFileContract.Header.LTFT_COLUMN_HEADER.header == trimmed -> ltftColumnIndex = i
                            Me7LogFileContract.Header.MAF_VOLTAGE_HEADER.header == trimmed -> mafVoltageIndex = i
                            Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER.header == trimmed -> mafGramsPerSecondIndex = i
                            Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER.header == trimmed -> throttlePlateAngleIndex = i
                            Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER.header == trimmed -> lambdaControlActiveIndex = i
                            Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER.header == trimmed -> requestedLambdaIndex = i
                            Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER.header == trimmed -> fuelInjectorOnTimeIndex = i
                            Me7LogFileContract.Header.ENGINE_LOAD_HEADER.header == trimmed -> engineLoadIndex = i
                            Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER.header == trimmed -> absoluteBoostPressureActualIndex = i
                            Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER.header == trimmed -> barometricPressureIndex = i
                            Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER.header == trimmed -> wastegateDutyCycleIndex = i
                            Me7LogFileContract.Header.SELECTED_GEAR_HEADER.header == trimmed -> selectedGearIndex = i
                            Me7LogFileContract.Header.WIDE_BAND_O2_HEADER.header == trimmed -> wideBandO2Index = i
                        }

                        headersFound = headersFound(logType)
                        if (headersFound) break
                    }

                    if (headersFound) break
                }

                if (headersFound) {
                    while (iterator.hasNext()) {
                        val record = iterator.next()
                        try {
                            when (logType) {
                                LogType.CLOSED_LOOP, LogType.OPEN_LOOP -> {
                                    val time = record.get(timeColumnIndex).toDouble()
                                    val rpm = record.get(rpmColumnIndex).toDouble()
                                    val stft = record.get(stftColumnIndex).toDouble()
                                    val ltft = record.get(ltftColumnIndex).toDouble()
                                    val mafVoltage = record.get(mafVoltageIndex).toDouble()
                                    val throttlePlateAngle = record.get(throttlePlateAngleIndex).toDouble()
                                    val lambdaControlActive = record.get(lambdaControlActiveIndex).toDouble()
                                    val engineLoad = record.get(engineLoadIndex).toDouble()

                                    if (logType == LogType.OPEN_LOOP) {
                                        val mafGsec = record.get(mafGramsPerSecondIndex).toDouble()
                                        val requestedLambda = record.get(requestedLambdaIndex).toDouble()
                                        val fuelInjectorOnTime = record.get(fuelInjectorOnTimeIndex).toDouble()

                                        map[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER]!!.add(mafGsec)
                                        map[Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER]!!.add(requestedLambda)
                                        map[Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER]!!.add(fuelInjectorOnTime)

                                        if (wideBandO2Index != -1) {
                                            val wideBandO2 = record.get(wideBandO2Index).toDouble()
                                            map[Me7LogFileContract.Header.WIDE_BAND_O2_HEADER]!!.add(wideBandO2)
                                        }
                                    }

                                    map[Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER]!!.add(time)
                                    map[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!.add(rpm)
                                    map[Me7LogFileContract.Header.STFT_COLUMN_HEADER]!!.add(stft)
                                    map[Me7LogFileContract.Header.LTFT_COLUMN_HEADER]!!.add(ltft)
                                    map[Me7LogFileContract.Header.MAF_VOLTAGE_HEADER]!!.add(mafVoltage)
                                    map[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!.add(throttlePlateAngle)
                                    map[Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER]!!.add(lambdaControlActive)
                                    map[Me7LogFileContract.Header.ENGINE_LOAD_HEADER]!!.add(engineLoad)
                                }
                                LogType.LDRPID -> {
                                    map[Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER]!!.add(record.get(timeColumnIndex).toDouble())
                                    map[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!.add(record.get(rpmColumnIndex).toDouble())
                                    map[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!.add(record.get(throttlePlateAngleIndex).toDouble())
                                    map[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER]!!.add(record.get(barometricPressureIndex).toDouble())
                                    map[Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER]!!.add(record.get(wastegateDutyCycleIndex).toDouble())
                                    map[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!.add(record.get(absoluteBoostPressureActualIndex).toDouble())
                                    map[Me7LogFileContract.Header.SELECTED_GEAR_HEADER]!!.add(record.get(selectedGearIndex).toDouble())
                                }
                                LogType.KFVPDKSD -> {
                                    map[Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER]!!.add(record.get(timeColumnIndex).toDouble())
                                    map[Me7LogFileContract.Header.RPM_COLUMN_HEADER]!!.add(record.get(rpmColumnIndex).toDouble())
                                    map[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]!!.add(record.get(throttlePlateAngleIndex).toDouble())
                                    map[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER]!!.add(record.get(barometricPressureIndex).toDouble())
                                    map[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]!!.add(record.get(absoluteBoostPressureActualIndex).toDouble())
                                }
                            }
                        } catch (_: NumberFormatException) {
                        } catch (_: ArrayIndexOutOfBoundsException) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Validate data is square
        var size = -1
        for ((key, value) in map) {
            if (key != Me7LogFileContract.Header.START_TIME_HEADER && size == -1) {
                size = value.size
            }
            if (key != Me7LogFileContract.Header.START_TIME_HEADER && value.size != size) {
                // Wideband is optional
                if (key != Me7LogFileContract.Header.WIDE_BAND_O2_HEADER) {
                    throw RuntimeException("Data is not square! Got: ${value.size} Expected: $size")
                }
            }
        }
    }

    private fun resetIndices() {
        timeColumnIndex = -1
        rpmColumnIndex = -1
        stftColumnIndex = -1
        ltftColumnIndex = -1
        mafVoltageIndex = -1
        mafGramsPerSecondIndex = -1
        throttlePlateAngleIndex = -1
        lambdaControlActiveIndex = -1
        requestedLambdaIndex = -1
        fuelInjectorOnTimeIndex = -1
        engineLoadIndex = -1
        wastegateDutyCycleIndex = -1
        barometricPressureIndex = -1
        absoluteBoostPressureActualIndex = -1
        selectedGearIndex = -1
        wideBandO2Index = -1
    }

    private fun headersFound(logType: LogType): Boolean = when (logType) {
        LogType.OPEN_LOOP -> timeColumnIndex != -1 && rpmColumnIndex != -1 && stftColumnIndex != -1 && ltftColumnIndex != -1 && mafVoltageIndex != -1 && mafGramsPerSecondIndex != -1 && throttlePlateAngleIndex != -1 && lambdaControlActiveIndex != -1 && requestedLambdaIndex != -1 && fuelInjectorOnTimeIndex != -1
        LogType.CLOSED_LOOP -> timeColumnIndex != -1 && rpmColumnIndex != -1 && stftColumnIndex != -1 && ltftColumnIndex != -1 && mafVoltageIndex != -1 && throttlePlateAngleIndex != -1 && lambdaControlActiveIndex != -1 && engineLoadIndex != -1
        LogType.LDRPID -> timeColumnIndex != -1 && rpmColumnIndex != -1 && throttlePlateAngleIndex != -1 && wastegateDutyCycleIndex != -1 && barometricPressureIndex != -1 && absoluteBoostPressureActualIndex != -1 && selectedGearIndex != -1
        LogType.KFVPDKSD -> timeColumnIndex != -1 && rpmColumnIndex != -1 && throttlePlateAngleIndex != -1 && barometricPressureIndex != -1 && absoluteBoostPressureActualIndex != -1
    }

    private fun generateMap(logType: LogType): Map<Me7LogFileContract.Header, MutableList<Double>> {
        val map = mutableMapOf<Me7LogFileContract.Header, MutableList<Double>>()
        map[Me7LogFileContract.Header.START_TIME_HEADER] = mutableListOf()

        when (logType) {
            LogType.CLOSED_LOOP, LogType.OPEN_LOOP -> {
                map[Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.RPM_COLUMN_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.STFT_COLUMN_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.LTFT_COLUMN_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.MAF_VOLTAGE_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.ENGINE_LOAD_HEADER] = mutableListOf()

                if (logType == LogType.OPEN_LOOP) {
                    map[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER] = mutableListOf()
                    map[Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER] = mutableListOf()
                    map[Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER] = mutableListOf()
                    map[Me7LogFileContract.Header.WIDE_BAND_O2_HEADER] = mutableListOf()
                }
            }
            LogType.LDRPID -> {
                map[Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.RPM_COLUMN_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.SELECTED_GEAR_HEADER] = mutableListOf()
            }
            LogType.KFVPDKSD -> {
                map[Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.RPM_COLUMN_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER] = mutableListOf()
                map[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER] = mutableListOf()
            }
        }

        return map
    }
}
