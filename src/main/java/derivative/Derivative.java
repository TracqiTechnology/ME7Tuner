package derivative;

import contract.Me7LogFileContract;
import math.Index;
import math.map.Map2d;
import math.map.Map3d;

import java.util.*;

public class Derivative {

    public static Map<Double, List<Double>> getMlfhm(Map<String, List<Double>> me7Logs, Map3d mlhfm) {
        Map<Double, List<Double>> rawVoltageDt = new HashMap<>();

        for (Double voltage : mlhfm.yAxis) {
            rawVoltageDt.put(voltage, new ArrayList<>());
        }

        List<Double> me7Voltages = me7Logs.get(Me7LogFileContract.MAF_VOLTAGE_HEADER);
        List<Double> me7Timestamps = me7Logs.get(Me7LogFileContract.TIME_COLUMN_HEADER);
        List<Double> me7voltageDt = getDt(me7Voltages, me7Timestamps);

        for (int i = 0; i < me7voltageDt.size(); i++) {
            double me7Voltage = me7Voltages.get(i + 1);
            int mlhfmVoltageIndex = Index.getInsertIndex(Arrays.asList(mlhfm.yAxis), me7Voltage);
            double mlhfmVoltageKey = mlhfm.yAxis[mlhfmVoltageIndex];
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
            int kfkhfmLoadIndex = Index.getInsertIndex(Arrays.asList(kfkhfm.xAxis), me7Load);
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
            int kfkhfmLoadIndex = Index.getInsertIndex(Arrays.asList(kfkhfm.xAxis), me7Load);
            int kfkhfmRpmIndex = Index.getInsertIndex(Arrays.asList(kfkhfm.yAxis), rpmValue);
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
