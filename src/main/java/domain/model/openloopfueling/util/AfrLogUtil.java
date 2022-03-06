package domain.model.openloopfueling.util;

import data.contract.AfrLogFileContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AfrLogUtil {
    public static List<Map<String, List<Double>>> findAfrLogs(Map<String, List<Double>> afrLog, double minThrottleAngle, double minRpm, double maxAfr, int minPointsAfr) {
        ArrayList<Map<String, List<Double>>> logList = new ArrayList<>();

        List<Double> throttleAngle = afrLog.get(AfrLogFileContract.TPS_HEADER);
        List<Double> rpm = afrLog.get(AfrLogFileContract.RPM_HEADER);
        List<Double> afr = afrLog.get(AfrLogFileContract.AFR_HEADER);

        for (int i = 0; i < throttleAngle.size(); i++) {

            if (throttleAngle.get(i) >= minThrottleAngle && rpm.get(i) >= minRpm && afr.get(i) < maxAfr) {
                if (LogUtil.isValidLogLength(i, minPointsAfr, minThrottleAngle, throttleAngle)) {
                    int endOfLog = LogUtil.findEndOfLog(i, minThrottleAngle, throttleAngle);
                    logList.add(getAfrLog(i, endOfLog, afrLog));
                    i = endOfLog + 1;
                }
            }
        }

        return logList;
    }

    private static Map<String, List<Double>> getAfrLog(int start, int end, Map<String, List<Double>> afrLog) {
        List<Double> rpm = afrLog.get(AfrLogFileContract.RPM_HEADER);
        List<Double> afr = afrLog.get(AfrLogFileContract.AFR_HEADER);
        List<Double> tps = afrLog.get(AfrLogFileContract.TPS_HEADER);

        Map<String, List<Double>> log = new HashMap<>();
        log.put(AfrLogFileContract.RPM_HEADER, rpm.subList(start, end));
        log.put(AfrLogFileContract.AFR_HEADER, afr.subList(start, end));
        log.put(AfrLogFileContract.TPS_HEADER, tps.subList(start, end));

        return log;
    }
}
