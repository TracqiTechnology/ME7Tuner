package parser.me7log;

import contract.Me7LogFileContract;
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
        CLOSED_LOOP
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

    public Map<String, List<Double>> parseLogDirectory(LogType logType, File directory) {
        Map<String, List<Double>> map = generateMap(logType);

        for (File file : directory.listFiles()) {
           parse(file, logType, map);
        }

        return map;
    }

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
                    map.get(Me7LogFileContract.TIME_COLUMN_HEADER).add(Double.parseDouble(record.get(timeColumnIndex)));
                    map.get(Me7LogFileContract.RPM_COLUMN_HEADER).add(Double.parseDouble(record.get(rpmColumnIndex)));
                    map.get(Me7LogFileContract.STFT_COLUMN_HEADER).add(Double.parseDouble(record.get(stftColumnIndex)));
                    map.get(Me7LogFileContract.LTFT_COLUMN_HEADER).add(Double.parseDouble(record.get(ltftColumnIndex)));
                    map.get(Me7LogFileContract.MAF_VOLTAGE_HEADER).add(Double.parseDouble(record.get(mafVoltageIndex)));
                    map.get(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER).add(Double.parseDouble(record.get(throttlePlateAngleIndex)));
                    map.get(Me7LogFileContract.LAMBDA_CONTROL_ACTIVE_HEADER).add(Double.parseDouble(record.get(lambdaControlActiveIndex)));
                    map.get(Me7LogFileContract.FUEL_INJECTOR_ON_TIME_HEADER).add(Double.parseDouble(record.get(fuelInjectorOnTimeIndex)));

                    if(logType == LogType.OPEN_LOOP) {
                        map.get(Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER).add(Double.parseDouble(record.get(mafGramsPerSecondIndex)));
                        map.get(Me7LogFileContract.REQUESTED_LAMBDA_HEADER).add(Double.parseDouble(record.get(requestedLambdaIndex)));
                    }
                }
            }
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
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
    }

    private boolean headersFound(LogType logType) {
        if(logType == LogType.OPEN_LOOP) {
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && stftColumnIndex != -1 && ltftColumnIndex != -1 && mafVoltageIndex != -1 && mafGramsPerSecondIndex != -1 && throttlePlateAngleIndex != -1 && lambdaControlActiveIndex != -1 && requestedLambdaIndex != -1 && fuelInjectorOnTimeIndex != -1;
        } else {
            return timeColumnIndex != -1 && rpmColumnIndex != -1 && stftColumnIndex != -1 && ltftColumnIndex != -1 && mafVoltageIndex != -1 && throttlePlateAngleIndex != -1 && lambdaControlActiveIndex != -1 && fuelInjectorOnTimeIndex != -1;
        }
    }

    private Map<String, List<Double>> generateMap(LogType logType) {
        Map<String, List<Double>> map = new HashMap<>();
        map.put(Me7LogFileContract.TIME_COLUMN_HEADER, new ArrayList<>());
        map.put(Me7LogFileContract.RPM_COLUMN_HEADER, new ArrayList<>());
        map.put(Me7LogFileContract.STFT_COLUMN_HEADER, new ArrayList<>());
        map.put(Me7LogFileContract.LTFT_COLUMN_HEADER, new ArrayList<>());
        map.put(Me7LogFileContract.MAF_VOLTAGE_HEADER, new ArrayList<>());
        map.put(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER, new ArrayList<>());
        map.put(Me7LogFileContract.LAMBDA_CONTROL_ACTIVE_HEADER, new ArrayList<>());
        map.put(Me7LogFileContract.FUEL_INJECTOR_ON_TIME_HEADER, new ArrayList<>());

        if(logType == LogType.OPEN_LOOP) {
            map.put(Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER, new ArrayList<>());
            map.put(Me7LogFileContract.REQUESTED_LAMBDA_HEADER, new ArrayList<>());
        }

        return map;
    }
}
