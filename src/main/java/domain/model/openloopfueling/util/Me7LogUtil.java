package domain.model.openloopfueling.util;

import data.contract.Me7LogFileContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Me7LogUtil {

    public static List<Map<Me7LogFileContract.Header, List<Double>>> findMe7Logs(Map<Me7LogFileContract.Header, List<Double>> me7Log, double minThrottleAngle, double lambdaControlEnabled, double minRpm, int minPointsMe7) {
        ArrayList<Map<Me7LogFileContract.Header, List<Double>>> logList = new ArrayList<>();

        List<Double> lambdaControl = me7Log.get(Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER);
        List<Double> throttleAngle = me7Log.get(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER);
        List<Double> rpm = me7Log.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER);

        for (int i = 0; i < throttleAngle.size(); i++) {
            if (throttleAngle.get(i) >= minThrottleAngle && lambdaControl.get(i) == lambdaControlEnabled && rpm.get(i) >= minRpm) {
                if (LogUtil.isValidLogLength(i, minPointsMe7, minThrottleAngle, throttleAngle)) {
                    int endOfLog = LogUtil.findEndOfLog(i, minThrottleAngle, throttleAngle);
                    logList.add(getMe7Log(i, endOfLog, me7Log));
                    i = endOfLog + 1;
                }
            }
        }

        return logList;
    }

    private static Map<Me7LogFileContract.Header, List<Double>> getMe7Log(int start, int end, Map<Me7LogFileContract.Header, List<Double>> me7Log) {
        List<Double> voltages = me7Log.get(Me7LogFileContract.Header.MAF_VOLTAGE_HEADER);
        List<Double> requestedLambda = me7Log.get(Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER);
        List<Double> stft = me7Log.get(Me7LogFileContract.Header.STFT_COLUMN_HEADER);
        List<Double> ltft = me7Log.get(Me7LogFileContract.Header.LTFT_COLUMN_HEADER);
        List<Double> lambdaControl = me7Log.get(Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER);
        List<Double> throttleAngle = me7Log.get(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER);
        List<Double> rpm = me7Log.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER);
        List<Double> onTime = me7Log.get(Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER);
        List<Double> gsec = me7Log.get(Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER);
        List<Double> afr = me7Log.get(Me7LogFileContract.Header.WIDE_BAND_O2_HEADER);

        Map<Me7LogFileContract.Header, List<Double>> log = new HashMap<>();
        log.put(Me7LogFileContract.Header.MAF_VOLTAGE_HEADER, voltages.subList(start, end));
        log.put(Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER, requestedLambda.subList(start, end));
        log.put(Me7LogFileContract.Header.STFT_COLUMN_HEADER, stft.subList(start, end));
        log.put(Me7LogFileContract.Header.LTFT_COLUMN_HEADER, ltft.subList(start, end));
        log.put(Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER, lambdaControl.subList(start, end));
        log.put(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER, throttleAngle.subList(start, end));
        log.put(Me7LogFileContract.Header.RPM_COLUMN_HEADER, rpm.subList(start, end));
        log.put(Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER, onTime.subList(start, end));
        log.put(Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER, gsec.subList(start, end));
        // Wide band O2 is optional for ME7.5+
        if(!afr.isEmpty()) {
            log.put(Me7LogFileContract.Header.WIDE_BAND_O2_HEADER, afr.subList(start, end));
        }

        return log;
    }


}
