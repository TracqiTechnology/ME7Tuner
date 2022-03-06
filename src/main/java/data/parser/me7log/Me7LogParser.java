package data.parser.me7log;

import data.contract.Me7LogFileContract;
import io.reactivex.annotations.NonNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Me7LogParser {

    public enum LogType {
        OPEN_LOOP,
        CLOSED_LOOP,
        LDRPID,
        KFVPDKSD
    }

    private int timeColumnIndex = -1;
    private int rpmColumnIndex = -1;
    private int stftColumnIndex = -1;
    private int ltftColumnIndex = -1;
    private int mafVoltageIndex = -1;
    private int mafGramsPerSecondIndex = -1;
    private int throttlePlateAngleIndex = -1;
    private int lambdaControlActiveIndex = -1;
    private int requestedLambdaIndex = -1;
    private int fuelInjectorOnTimeIndex = -1;
    private int engineLoadIndex = -1;
    private int wastegateDutyCycleIndex = -1;
    private int barometricPressureIndex = -1;
    private int absoluteBoostPressureActualIndex = -1;
    private int selectedGearIndex = -1;

    public interface ProgressCallback {
        void onProgress(int value, int max);
    }

    public Map<Me7LogFileContract.Header, List<Double>> parseLogDirectory(LogType logType, File directory, ProgressCallback callback) {
        Map<Me7LogFileContract.Header, List<Double>> map = generateMap(logType);
        File[] files = directory.listFiles();
        int numFiles = files.length;
        int count = 0;

        for (File file : files) {
            parse(file, logType, map);
            callback.onProgress(count++, numFiles);
        }

        return map;
    }

    @NonNull
    public Map<Me7LogFileContract.Header, List<Double>> parseLogFile(LogType logType, File file) {
        Map<Me7LogFileContract.Header, List<Double>> map = generateMap(logType);

        parse(file, logType, map);

        return map;
    }

    private void parse(File file, LogType logType, Map<Me7LogFileContract.Header, List<Double>> map) {
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

                        map.get(Me7LogFileContract.Header.START_TIME_HEADER).add(startTime);
                    }
                }

                for (int i = 0; i < record.size(); i++) {
                    if (Me7LogFileContract.Header.TIME_COLUMN_HEADER.getHeader().equals(record.get(i).trim())) {
                        timeColumnIndex = i;
                    } else if (Me7LogFileContract.Header.RPM_COLUMN_HEADER.getHeader().equals(record.get(i).trim())) {
                        rpmColumnIndex = i;
                    } else if (Me7LogFileContract.Header.STFT_COLUMN_HEADER.getHeader().equals(record.get(i).trim())) {
                        stftColumnIndex = i;
                    } else if (Me7LogFileContract.Header.LTFT_COLUMN_HEADER.getHeader().equals(record.get(i).trim())) {
                        ltftColumnIndex = i;
                    } else if (Me7LogFileContract.Header.MAF_VOLTAGE_HEADER.getHeader().equals(record.get(i).trim())) {
                        mafVoltageIndex = i;
                    } else if (Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER.getHeader().equals(record.get(i).trim())) {
                        mafGramsPerSecondIndex = i;
                    } else if (Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER.getHeader().equals(record.get(i).trim())) {
                        throttlePlateAngleIndex = i;
                    } else if (Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER.getHeader().equals(record.get(i).trim())) {
                        lambdaControlActiveIndex = i;
                    } else if (Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER.getHeader().equals(record.get(i).trim())) {
                        requestedLambdaIndex = i;
                    } else if (Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER.getHeader().equals(record.get(i).trim())) {
                        fuelInjectorOnTimeIndex = i;
                    } else if (Me7LogFileContract.Header.ENGINE_LOAD_HEADER.getHeader().equals(record.get(i).trim())) {
                        engineLoadIndex = i;
                    } else if (Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER.getHeader().equals(record.get(i).trim())) {
                        absoluteBoostPressureActualIndex = i;
                    } else if (Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER.getHeader().equals(record.get(i).trim())) {
                        barometricPressureIndex = i;
                    } else if (Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER.getHeader().equals(record.get(i).trim())) {
                        wastegateDutyCycleIndex = i;
                    } else if (Me7LogFileContract.Header.SELECTED_GEAR_HEADER.getHeader().equals(record.get(i).trim())) {
                        selectedGearIndex = i;
                    }

                    headersFound = headersFound(logType);

                    if (headersFound) {
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

                                map.get(Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER).add(mafGsec);
                                map.get(Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER).add(requestedLambda);
                                map.get(Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER).add(fuelInjectorOnTime);
                            }

                            map.get(Me7LogFileContract.Header.TIME_COLUMN_HEADER).add(time);
                            map.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER).add(rpm);
                            map.get(Me7LogFileContract.Header.STFT_COLUMN_HEADER).add(stft);
                            map.get(Me7LogFileContract.Header.LTFT_COLUMN_HEADER).add(ltft);
                            map.get(Me7LogFileContract.Header.MAF_VOLTAGE_HEADER).add(mafVoltage);
                            map.get(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER).add(throttlePlateAngle);
                            map.get(Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER).add(lambdaControlActive);
                            map.get(Me7LogFileContract.Header.ENGINE_LOAD_HEADER).add(engineLoad);
                        } else if (logType == LogType.LDRPID) {
                            double time = Double.parseDouble(record.get(timeColumnIndex));
                            double rpm = Double.parseDouble(record.get(rpmColumnIndex));
                            double throttlePlateAngle = Double.parseDouble(record.get(throttlePlateAngleIndex));
                            double barometricPressure = Double.parseDouble(record.get(barometricPressureIndex));
                            double wastegateDutyCycle = Double.parseDouble(record.get(wastegateDutyCycleIndex));
                            double absoluteBoostPressure = Double.parseDouble(record.get(absoluteBoostPressureActualIndex));
                            double selectedGear = Double.parseDouble(record.get(selectedGearIndex));

                            map.get(Me7LogFileContract.Header.TIME_COLUMN_HEADER).add(time);
                            map.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER).add(rpm);
                            map.get(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER).add(throttlePlateAngle);
                            map.get(Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER).add(barometricPressure);
                            map.get(Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER).add(wastegateDutyCycle);
                            map.get(Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER).add(absoluteBoostPressure);
                            map.get(Me7LogFileContract.Header.SELECTED_GEAR_HEADER).add(selectedGear);
                        } else if (logType == LogType.KFVPDKSD) {
                            double time = Double.parseDouble(record.get(timeColumnIndex));
                            double rpm = Double.parseDouble(record.get(rpmColumnIndex));
                            double throttlePlateAngle = Double.parseDouble(record.get(throttlePlateAngleIndex));
                            double barometricPressure = Double.parseDouble(record.get(barometricPressureIndex));
                            double absoluteBoostPressure = Double.parseDouble(record.get(absoluteBoostPressureActualIndex));

                            map.get(Me7LogFileContract.Header.TIME_COLUMN_HEADER).add(time);
                            map.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER).add(rpm);
                            map.get(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER).add(throttlePlateAngle);
                            map.get(Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER).add(barometricPressure);
                            map.get(Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER).add(absoluteBoostPressure);
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
        for (Me7LogFileContract.Header key : map.keySet()) {
            if (!key.equals(Me7LogFileContract.Header.START_TIME_HEADER) && size == -1) {
                size = map.get(key).size();
            }

            if (!key.equals(Me7LogFileContract.Header.START_TIME_HEADER) && map.get(key).size() != size) {
                throw new RuntimeException("Data is not square! Got: " + map.get(key).size() + " Expected: " + size);
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
        selectedGearIndex = -1;
    }

    private boolean headersFound(LogType logType) {
        if (logType == LogType.OPEN_LOOP) {
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && stftColumnIndex != -1 && ltftColumnIndex != -1 && mafVoltageIndex != -1 && mafGramsPerSecondIndex != -1 && throttlePlateAngleIndex != -1 && lambdaControlActiveIndex != -1 && requestedLambdaIndex != -1 && fuelInjectorOnTimeIndex != -1;
        } else if (logType == LogType.CLOSED_LOOP) {
            // nmot, fr_w, fra_2, uhfm_w, wdkba, B_lr, rl_w
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && stftColumnIndex != -1 && ltftColumnIndex != -1 && mafVoltageIndex != -1 && throttlePlateAngleIndex != -1 && lambdaControlActiveIndex != -1 && engineLoadIndex != -1;
        } else if (logType == LogType.LDRPID) {
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && throttlePlateAngleIndex != -1 && wastegateDutyCycleIndex != -1 && barometricPressureIndex != -1 && absoluteBoostPressureActualIndex != -1 && selectedGearIndex != -1;
        } else if (logType == LogType.KFVPDKSD) {
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && throttlePlateAngleIndex != -1 && barometricPressureIndex != -1 && absoluteBoostPressureActualIndex != -1;
        }

        return false;
    }

    private Map<Me7LogFileContract.Header, List<Double>> generateMap(LogType logType) {
        Map<Me7LogFileContract.Header, List<Double>> map = new HashMap<>();

        map.put(Me7LogFileContract.Header.START_TIME_HEADER, new ArrayList<>());

        if (logType == LogType.CLOSED_LOOP || logType == LogType.OPEN_LOOP) {
            map.put(Me7LogFileContract.Header.TIME_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.RPM_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.STFT_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.LTFT_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.MAF_VOLTAGE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.ENGINE_LOAD_HEADER, new ArrayList<>());

            if (logType == LogType.OPEN_LOOP) {
                map.put(Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER, new ArrayList<>());
                map.put(Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER, new ArrayList<>());
                map.put(Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER, new ArrayList<>());
            }
        } else if (logType == LogType.LDRPID) {
            map.put(Me7LogFileContract.Header.TIME_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.RPM_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.SELECTED_GEAR_HEADER, new ArrayList<>());
        } else if (logType == LogType.KFVPDKSD) {
            map.put(Me7LogFileContract.Header.TIME_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.RPM_COLUMN_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER, new ArrayList<>());
        }

        return map;
    }
}
