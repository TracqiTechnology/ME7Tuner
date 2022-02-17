package model.wdkugdn;
import math.LinearInterpolation;
import math.map.Map3d;
import model.load.EngineLoad;

import java.util.Arrays;

public class WdkugdnCalculator {

    public static Map3d calculateWdkugdn(Map3d wdkugdn, Map3d kfwdkmsn, double displacement) {
        Double[] xAxis = wdkugdn.xAxis;

        Map3d correctedWdkugdn = new Map3d(wdkugdn);

        for(int i = 0; i < xAxis.length; i++) {
            Double rpm = xAxis[i];
            // https://en.wikipedia.org/wiki/Choked_flow#Choking_in_change_of_cross_section_flow
            double chokedAirflow = EngineLoad.getAirflow(1, rpm, displacement)*3.6/0.528;

            System.out.println("rpm: " + rpm + " airflow: " + chokedAirflow);

            int rpmIndex = Arrays.binarySearch(kfwdkmsn.xAxis, rpm);

            if (rpmIndex < 0) {
                rpmIndex = Math.abs(rpmIndex + 1);
            }

            rpmIndex = Math.min(rpmIndex, kfwdkmsn.xAxis.length - 1);

            double[] throttleAngle = new double[kfwdkmsn.yAxis.length];

            for(int j = 0; j < throttleAngle.length; j++) {
                throttleAngle[j] = kfwdkmsn.zAxis[j][rpmIndex];
            }

            int airflowIndex = Arrays.binarySearch(kfwdkmsn.yAxis, chokedAirflow);

            if (airflowIndex < 0) {
                airflowIndex = Math.abs(airflowIndex + 1);
            }

            airflowIndex = Math.min(airflowIndex, kfwdkmsn.yAxis.length - 1);

            Double[] x = new Double[]{kfwdkmsn.yAxis[airflowIndex - 1], kfwdkmsn.yAxis[airflowIndex]};
            Double[] y = new Double[]{throttleAngle[airflowIndex - 1], throttleAngle[airflowIndex]};
            Double[] xi = new Double[]{chokedAirflow};

            Double[] chockedThrottleAngle = LinearInterpolation.interpolate(x, y, xi);

            correctedWdkugdn.zAxis[0][i] = chockedThrottleAngle[0];
        }

        return correctedWdkugdn;
    }
}
