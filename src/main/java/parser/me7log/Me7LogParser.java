package parser.me7log;

import com.oracle.tools.packager.Log;
import contract.Me7LogFileContract;
import io.reactivex.annotations.NonNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class Me7LogParser {

    public enum LogType {
        OPEN_LOOP,
        CLOSED_LOOP,
        LDRPID,
        KFURL,
        WDKUGDN
    }

    private int timeColumnIndex = -1;
    private int rpmColumnIndex = -1;
    private int stftColumnIndex = -1;
    private int ltftColumnIndex = -1;
    private int mafVoltageIndex = -1;
    private int mafGramsPerSecondIndex = -1;
    private int mafAtThrottlePlateIndex = -1;
    private int throttlePlateAngleIndex = -1;
    private int lambdaControlActiveIndex = -1;
    private int requestedLambdaIndex = -1;
    private int fuelInjectorOnTimeIndex = -1;
    private int engineLoadIndex = -1;
    private int wastegateDutyCycleIndex = -1;
    private int barometricPressureIndex = -1;
    private int absoluteBoostPressureActualIndex = -1;
    private int absoluteBoostPressureModeledIndex = -1;
    private int selectedGearIndex = -1;

    public Map<String, List<Double>> parseLogDirectory(LogType logType, File directory) {
        Map<String, List<Double>> map = generateMap(logType);

        for (File file : directory.listFiles()) {
            parse(file, logType, map);
        }

        return map;
    }

    @NonNull
    public Map<String, List<Double>> parseLogFile(LogType logType, File file) {
        Map<String, List<Double>> map = generateMap(logType);

        parse(file, logType, map);

        return map;
    }

    private void parse(File file, LogType logType, Map<String, List<Double>> map) {
        resetIndices();
        try {
            boolean headersFound = false;
            Reader in = new FileReader(file);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            for (CSVRecord record : records) {
                if (record.size() > 0) {
                    String string = record.get(0);
                    if (string.contains("Log started at:")) {
                        String[] split = string.split(" ");
                        String timestamp = split[5];
                        split = timestamp.split(":");
                        double minuteSeconds = Double.parseDouble(split[1]) * 60;
                        double secondsSeconds = Double.parseDouble(split[2]);

                        double startTime = (minuteSeconds + secondsSeconds);

                        map.get(Me7LogFileContract.START_TIME).add(startTime);
                    }
                }

                for (int i = 0; i < record.size(); i++) {
                    switch (record.get(i).trim()) {
                        case Me7LogFileContract.TIME_COLUMN_HEADER:
                            timeColumnIndex = i;
                            break;
                        case Me7LogFileContract.RPM_COLUMN_HEADER:
                            rpmColumnIndex = i;
                            break;
                        case Me7LogFileContract.STFT_COLUMN_HEADER:
                            stftColumnIndex = i;
                            break;
                        case Me7LogFileContract.LTFT_COLUMN_HEADER:
                            ltftColumnIndex = i;
                            break;
                        case Me7LogFileContract.MAF_VOLTAGE_HEADER:
                            mafVoltageIndex = i;
                            break;
                        case Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER:
                            mafGramsPerSecondIndex = i;
                            break;
                        case Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER:
                            throttlePlateAngleIndex = i;
                            break;
                        case Me7LogFileContract.LAMBDA_CONTROL_ACTIVE_HEADER:
                            lambdaControlActiveIndex = i;
                            break;
                        case Me7LogFileContract.REQUESTED_LAMBDA_HEADER:
                            requestedLambdaIndex = i;
                            break;
                        case Me7LogFileContract.FUEL_INJECTOR_ON_TIME_HEADER:
                            fuelInjectorOnTimeIndex = i;
                            break;
                        case Me7LogFileContract.ENGINE_LOAD_HEADER:
                            engineLoadIndex = i;
                            break;
                        case Me7LogFileContract.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER:
                            absoluteBoostPressureActualIndex = i;
                            break;
                        case Me7LogFileContract.ABSOLUTE_BOOST_PRESSURE_MODELED_HEADER:
                            absoluteBoostPressureModeledIndex = i;
                            break;
                        case Me7LogFileContract.BAROMETRIC_PRESSURE_HEADER:
                            barometricPressureIndex = i;
                            break;
                        case Me7LogFileContract.WASTEGATE_DUTY_CYCLE_HEADER:
                            wastegateDutyCycleIndex = i;
                            break;
                        case Me7LogFileContract.SELECTED_GEAR_HEADER:
                            selectedGearIndex = i;
                            break;
                        case Me7LogFileContract.MAF_AT_THROTTLE_PLATE:
                            mafAtThrottlePlateIndex = i;
                            break;
                    }

                    if (headersFound = headersFound(logType)) {
                        break;
                    }
                }

                if (headersFound) {
                    break;
                }
            }

            if (headersFound) {
                for (CSVRecord record : records) {
                    try {
                        if (logType == LogType.CLOSED_LOOP || logType == LogType.OPEN_LOOP) {
                            double time = Double.parseDouble(record.get(timeColumnIndex));
                            double rpm = Double.parseDouble(record.get(rpmColumnIndex));
                            double stft = Double.parseDouble(record.get(stftColumnIndex));
                            double ltft = Double.parseDouble(record.get(ltftColumnIndex));
                            double mafVoltage = Double.parseDouble(record.get(mafVoltageIndex));
                            double throttlePlateAngle = Double.parseDouble(record.get(throttlePlateAngleIndex));
                            double lambdaControlActive = Double.parseDouble(record.get(lambdaControlActiveIndex));
                            double engineLoad = Double.parseDouble(record.get(engineLoadIndex));

                            if (logType == LogType.OPEN_LOOP) {
                                double mafGsec = Double.parseDouble(record.get(mafGramsPerSecondIndex));
                                double requestedLambda = Double.parseDouble(record.get(requestedLambdaIndex));
                                double fuelInjectorOnTime = Double.parseDouble(record.get(fuelInjectorOnTimeIndex));

                                map.get(Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER).add(mafGsec);
                                map.get(Me7LogFileContract.REQUESTED_LAMBDA_HEADER).add(requestedLambda);
                                map.get(Me7LogFileContract.FUEL_INJECTOR_ON_TIME_HEADER).add(fuelInjectorOnTime);
                            }

                            map.get(Me7LogFileContract.TIME_COLUMN_HEADER).add(time);
                            map.get(Me7LogFileContract.RPM_COLUMN_HEADER).add(rpm);
                            map.get(Me7LogFileContract.STFT_COLUMN_HEADER).add(stft);
                            map.get(Me7LogFileContract.LTFT_COLUMN_HEADER).add(ltft);
                            map.get(Me7LogFileContract.MAF_VOLTAGE_HEADER).add(mafVoltage);
                            map.get(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER).add(throttlePlateAngle);
                            map.get(Me7LogFileContract.LAMBDA_CONTROL_ACTIVE_HEADER).add(lambdaControlActive);
                            map.get(Me7LogFileContract.ENGINE_LOAD_HEADER).add(engineLoad);
                        } else if (logType == LogType.LDRPID) {
                            double time = Double.parseDouble(record.get(timeColumnIndex));
                            double rpm = Double.parseDouble(record.get(rpmColumnIndex));
                            double throttlePlateAngle = Double.parseDouble(record.get(throttlePlateAngleIndex));
                            double barometricPressure = Double.parseDouble(record.get(barometricPressureIndex));
                            double wastegateDutyCycle = Double.parseDouble(record.get(wastegateDutyCycleIndex));
                            double absoluteBoostPressure = Double.parseDouble(record.get(absoluteBoostPressureActualIndex));
                            double selectedGear = Double.parseDouble(record.get(selectedGearIndex));

                            map.get(Me7LogFileContract.TIME_COLUMN_HEADER).add(time);
                            map.get(Me7LogFileContract.RPM_COLUMN_HEADER).add(rpm);
                            map.get(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER).add(throttlePlateAngle);
                            map.get(Me7LogFileContract.BAROMETRIC_PRESSURE_HEADER).add(barometricPressure);
                            map.get(Me7LogFileContract.WASTEGATE_DUTY_CYCLE_HEADER).add(wastegateDutyCycle);
                            map.get(Me7LogFileContract.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER).add(absoluteBoostPressure);
                            map.get(Me7LogFileContract.SELECTED_GEAR_HEADER).add(selectedGear);
                        } else if(logType == LogType.KFURL) {
                            double time = Double.parseDouble(record.get(timeColumnIndex));
                            double rpm = Double.parseDouble(record.get(rpmColumnIndex));
                            double barometricPressure = Double.parseDouble(record.get(barometricPressureIndex));
                            double absoluteModeledBoostPressure = Double.parseDouble(record.get(absoluteBoostPressureModeledIndex));

                            map.get(Me7LogFileContract.TIME_COLUMN_HEADER).add(time);
                            map.get(Me7LogFileContract.RPM_COLUMN_HEADER).add(rpm);
                            map.get(Me7LogFileContract.BAROMETRIC_PRESSURE_HEADER).add(barometricPressure);
                            map.get(Me7LogFileContract.ABSOLUTE_BOOST_PRESSURE_MODELED_HEADER).add(absoluteModeledBoostPressure);
                        } else if(logType == LogType.WDKUGDN) {
                            double time = Double.parseDouble(record.get(timeColumnIndex));
                            double rpm = Double.parseDouble(record.get(rpmColumnIndex));
                            double throttlePlateAngle = Double.parseDouble(record.get(throttlePlateAngleIndex));
                            double mafGsec = Double.parseDouble(record.get(mafGramsPerSecondIndex));
                            double mafAtThrottlePlateGsec = Double.parseDouble(record.get(mafAtThrottlePlateIndex));

                            map.get(Me7LogFileContract.TIME_COLUMN_HEADER).add(time);
                            map.get(Me7LogFileContract.RPM_COLUMN_HEADER).add(rpm);
                            map.get(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER).add(throttlePlateAngle);
                            map.get(Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER).add(mafGsec);
                            map.get((Me7LogFileContract.MAF_AT_THROTTLE_PLATE)).add(mafAtThrottlePlateGsec);
                        }
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        int size = -1;
        for (String key : map.keySet()) {
            if (!key.equals(Me7LogFileContract.START_TIME) && size == -1) {
                size = map.get(key).size();
            }

            if (!key.equals(Me7LogFileContract.START_TIME) && map.get(key).size() != size) {
                throw new RuntimeException("Data is not square! Got: " + map.get(key).size() +" Expected: " + size);
            }
        }
    }

    private void resetIndices() {
        timeColumnIndex = -1;
        rpmColumnIndex = -1;
        stftColumnIndex = -1;
        ltftColumnIndex = -1;
        mafVoltageIndex = -1;
        mafGramsPerSecondIndex = -1;
        throttlePlateAngleIndex = -1;
        lambdaControlActiveIndex = -1;
        requestedLambdaIndex = -1;
        fuelInjectorOnTimeIndex = -1;
        engineLoadIndex = -1;
        wastegateDutyCycleIndex = -1;
        barometricPressureIndex = -1;
        absoluteBoostPressureActualIndex = -1;
        absoluteBoostPressureModeledIndex = -1;
        selectedGearIndex = -1;
        mafAtThrottlePlateIndex = -1;
    }

    private boolean headersFound(LogType logType) {
        if (logType == LogType.OPEN_LOOP) {
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && stftColumnIndex != -1 && ltftColumnIndex != -1 && mafVoltageIndex != -1 && mafGramsPerSecondIndex != -1 && throttlePlateAngleIndex != -1 && lambdaControlActiveIndex != -1 && requestedLambdaIndex != -1 && fuelInjectorOnTimeIndex != -1;
        } else if (logType == LogType.CLOSED_LOOP) {
            // nmot, fr_w, fra_2, uhfm_w, wdkba, B_lr, rl_w
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && stftColumnIndex != -1 && ltftColumnIndex != -1 && mafVoltageIndex != -1 && throttlePlateAngleIndex != -1 && lambdaControlActiveIndex != -1 && engineLoadIndex != -1;
        } else if (logType == LogType.LDRPID) {
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && throttlePlateAngleIndex != -1 && wastegateDutyCycleIndex != -1 && barometricPressureIndex != -1 && absoluteBoostPressureActualIndex != -1 && selectedGearIndex != -1;
        } else if(logType == LogType.KFURL) {
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && barometricPressureIndex != -1 && absoluteBoostPressureModeledIndex != -1;
        } else if(logType == LogType.WDKUGDN) {
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && mafGramsPerSecondIndex != -1 && mafAtThrottlePlateIndex != -1 && throttlePlateAngleIndex != -1;
        }

        return false;
    }

    private Map<String, List<Double>> generateMap(LogType logType) {
        Map<String, List<Double>> map = new HashMap<>();

        map.put(Me7LogFileContract.START_TIME, new ArrayList<>());

        if (logType == LogType.CLOSED_LOOP || logType == LogType.OPEN_LOOP) {
            map.put(Me7LogFileContract.TIME_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.RPM_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.STFT_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.LTFT_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.MAF_VOLTAGE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.LAMBDA_CONTROL_ACTIVE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.ENGINE_LOAD_HEADER, new ArrayList<>());

            if (logType == LogType.OPEN_LOOP) {
                map.put(Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER, new ArrayList<>());
                map.put(Me7LogFileContract.REQUESTED_LAMBDA_HEADER, new ArrayList<>());
                map.put(Me7LogFileContract.FUEL_INJECTOR_ON_TIME_HEADER, new ArrayList<>());
            }
        } else if (logType == LogType.LDRPID) {
            map.put(Me7LogFileContract.TIME_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.RPM_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.BAROMETRIC_PRESSURE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.WASTEGATE_DUTY_CYCLE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.SELECTED_GEAR_HEADER, new ArrayList<>());
        } else if(logType == LogType.KFURL) {
            map.put(Me7LogFileContract.TIME_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.RPM_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.BAROMETRIC_PRESSURE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.ABSOLUTE_BOOST_PRESSURE_MODELED_HEADER, new ArrayList<>());
        } else if(logType == LogType.WDKUGDN) {
            map.put(Me7LogFileContract.TIME_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.RPM_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.MAF_AT_THROTTLE_PLATE, new ArrayList<>());
        }

        return map;
    }
}
