package derivative;

import contract.Me7LogFileContract;
import contract.MlhfmFileContract;
import math.map.Map3d;

import java.util.*;

public class Derivative {

    public static Map<Double, List<Double>> getDtMap2d(Map<String, List<Double>> me7Logs, Map<String, List<Double>> mlhfm) { ;
        Map<Double, List<Double>> rawVoltageDt = new HashMap<>();

        for (Double voltage : mlhfm.get(MlhfmFileContract.MAF_VOLTAGE_HEADER)) {
            rawVoltageDt.put(voltage, new ArrayList<>());
        }

        List<Double> me7Voltages = me7Logs.get(Me7LogFileContract.MAF_VOLTAGE_HEADER);
        List<Double> me7Timestamps = me7Logs.get(Me7LogFileContract.TIME_COLUMN_HEADER);
        List<Double> me7voltageDt = getDt(me7Voltages, me7Timestamps);

        for (int i = 0; i < me7voltageDt.size(); i++) {
            double me7Voltage = me7Voltages.get(i + 1);
            int mlhfmVoltageIndex = Collections.binarySearch(mlhfm.get(MlhfmFileContract.MAF_VOLTAGE_HEADER), me7Voltage);

            if(mlhfmVoltageIndex < 0) {
                mlhfmVoltageIndex = Math.abs(mlhfmVoltageIndex + 1);
            }

            double mlhfmVoltageKey = mlhfm.get(MlhfmFileContract.MAF_VOLTAGE_HEADER).get(mlhfmVoltageIndex);
            rawVoltageDt.get(mlhfmVoltageKey).add(me7voltageDt.get(i));
        }

        return rawVoltageDt;
    }

    public static Map<Double, List<Double>> getDtMap2d(Map<String, List<Double>> me7Logs, Map3d kfkhfm) { ;
        Map<Double, List<Double>> rawVoltageDt = new HashMap<>();

        for (Double load : kfkhfm.xAxis) {
            rawVoltageDt.put(load, new ArrayList<>());
        }

        List<Double> me7Loads = me7Logs.get(Me7LogFileContract.ENGINE_LOAD_HEADER);
        List<Double> me7Voltages = me7Logs.get(Me7LogFileContract.MAF_VOLTAGE_HEADER);
        List<Double> me7Timestamps = me7Logs.get(Me7LogFileContract.TIME_COLUMN_HEADER);
        List<Double> me7voltageDt = getDt(me7Voltages, me7Timestamps);

        for (int i = 0; i < me7voltageDt.size(); i++) {
            double me7Load = me7Loads.get(i + 1);
            int kfkhfmLoadIndex = Arrays.binarySearch(kfkhfm.xAxis, me7Load);

            if(kfkhfmLoadIndex < 0) {
                kfkhfmLoadIndex = Math.abs(kfkhfmLoadIndex + 1);
            }

            if(kfkhfmLoadIndex >= kfkhfm.xAxis.length) {
                kfkhfmLoadIndex = kfkhfm.xAxis.length - 1;
            }

            double kfkhfmLoadKey = kfkhfm.xAxis[kfkhfmLoadIndex];
            rawVoltageDt.get(kfkhfmLoadKey).add(me7voltageDt.get(i));
        }

        return rawVoltageDt;
    }

    public static List<List<List<Double>>> getDtMap3d(Map<String, List<Double>> me7Logs, Map3d kfkhfm) { ;
        List<List<List<Double>>> rawVoltageDt = new ArrayList<>();

        for (int i = 0; i < kfkhfm.xAxis.length; i++) {
            rawVoltageDt.add(new ArrayList<>());

            for (int j = 0; j < kfkhfm.yAxis.length; j++) {
                rawVoltageDt.get(i).add(new ArrayList<>());
            }
        }

        List<Double> me7Loads = me7Logs.get(Me7LogFileContract.ENGINE_LOAD_HEADER);
        List<Double> me7Rpms = me7Logs.get(Me7LogFileContract.RPM_COLUMN_HEADER);
        List<Double> me7Voltages = me7Logs.get(Me7LogFileContract.MAF_VOLTAGE_HEADER);
        List<Double> me7Timestamps = me7Logs.get(Me7LogFileContract.TIME_COLUMN_HEADER);
        List<Double> me7voltageDt = getDt(me7Voltages, me7Timestamps);

        for (int i = 0; i < me7voltageDt.size(); i++) {
            double me7Load = me7Loads.get(i + 1);
            double rpmValue = me7Rpms.get(i + 1);
            int kfkhfmLoadIndex = Arrays.binarySearch(kfkhfm.xAxis, me7Load);
            if(kfkhfmLoadIndex < 0) {
                kfkhfmLoadIndex = Math.abs(kfkhfmLoadIndex + 1);
            }

            if(kfkhfmLoadIndex >= kfkhfm.xAxis.length) {
                kfkhfmLoadIndex = kfkhfm.xAxis.length - 1;
            }

            int kfkhfmRpmIndex = Arrays.binarySearch(kfkhfm.yAxis, rpmValue);
            if (kfkhfmRpmIndex < 0) {
                kfkhfmRpmIndex = Math.abs(kfkhfmRpmIndex + 1);
            }

            if(kfkhfmRpmIndex >= kfkhfm.yAxis.length) {
                kfkhfmRpmIndex = kfkhfm.yAxis.length - 1;
            }

            rawVoltageDt.get(kfkhfmLoadIndex).get(kfkhfmRpmIndex).add(me7voltageDt.get(i));
        }

        return rawVoltageDt;
    }

    public static ArrayList<Double> getDt(List<Double> voltages, List<Double> timestamps) {

        ArrayList<Double> result = new ArrayList<>();

        for (int i = 0; i < voltages.size() - 1; i++) {
            double v1 = voltages.get(i);
            double v2 = voltages.get(i+1);
            double t1 = timestamps.get(i);
            double t2 = timestamps.get(i+1);

            double dt = Math.abs((v2-v1)/(t2-t1));

            result.add(dt);
        }

        return result;
    }
}
