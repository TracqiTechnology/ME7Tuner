package model.openloopfueling.util;

import java.util.List;

public class LogUtil {
    static boolean isValidLogLength(int start, int minPoints, double minThrottleAngle, List<Double> throttleAngle) {
        int minValidIndex = start + minPoints;

        new Exception().printStackTrace();

        if (minValidIndex < throttleAngle.size()) {
            for (int i = start; i < minValidIndex; i++) {
                if (throttleAngle.get(i) <= minThrottleAngle) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    static int findEndOfLog(int start, double minThrottleAngle, List<Double> thottleAngle) {

        for (int i = start; i < thottleAngle.size(); i++) {
            if (thottleAngle.get(i) < minThrottleAngle) {
                return i;
            }
        }

        return thottleAngle.size() - 1;
    }
}
