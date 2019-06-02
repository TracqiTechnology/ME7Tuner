package derivative;

import contract.Me7LogFileContract;
import contract.MlhfmFileContract;
import math.map.Map3d;

import java.util.*;

public class Derivative {

    public static Map<Double, List<Double>> getDtMap(Map<String, List<Double>> me7Logs, Map<String, List<Double>> mlhfm) { ;
        Map<Double, List<Double>> rawVoltageDt = new HashMap<>();

        for (Double voltage : mlhfm.get(MlhfmFileContract.MAF_VOLTAGE_HEADER)) {
            rawVoltageDt.put(voltage, new ArrayList<>());
        }

        List<Double> me7Voltages = me7Logs.get(Me7LogFileContract.MAF_VOLTAGE_HEADER);
        List<Double> me7Timestamps = me7Logs.get(Me7LogFileContract.TIME_COLUMN_HEADER);
        List<Double> me7voltageDt = getDt(me7Voltages, me7Timestamps);

        for (int i = 0; i < me7Voltages.size(); i++) {
            double me7Voltage = me7Voltages.get(i);
            int mlhfmVoltageIndex = Math.abs(Collections.binarySearch(mlhfm.get(MlhfmFileContract.MAF_VOLTAGE_HEADER), me7Voltage));
            double mlhfmVoltageKey = mlhfm.get(MlhfmFileContract.MAF_VOLTAGE_HEADER).get(mlhfmVoltageIndex);
            rawVoltageDt.get(mlhfmVoltageKey).add(me7voltageDt.get(i));
        }

        return rawVoltageDt;
    }

    public static Map<Double, List<Double>> getDtMap(Map<String, List<Double>> me7Logs, Map3d kfkhfm) { ;
        Map<Double, List<Double>> rawVoltageDt = new HashMap<>();

        for (Double load : kfkhfm.xAxis) {
            rawVoltageDt.put(load, new ArrayList<>());
        }

        List<Double> me7Loads = me7Logs.get(Me7LogFileContract.ENGINE_LOAD_HEADER);
        List<Double> me7Voltages = me7Logs.get(Me7LogFileContract.MAF_VOLTAGE_HEADER);
        List<Double> me7Timestamps = me7Logs.get(Me7LogFileContract.TIME_COLUMN_HEADER);
        List<Double> me7voltageDt = getDt(me7Voltages, me7Timestamps);

        for (int i = 0; i < me7Loads.size(); i++) {
            double me7Load = me7Loads.get(i);
            int kfkhfmLoadIndex = Math.abs(Arrays.binarySearch(kfkhfm.xAxis, me7Load));

            if(kfkhfmLoadIndex >= kfkhfm.xAxis.length) {
                kfkhfmLoadIndex = kfkhfm.xAxis.length - 1;
            }

            double kfkhfmLoadKey = kfkhfm.xAxis[kfkhfmLoadIndex];
            rawVoltageDt.get(kfkhfmLoadKey).add(me7voltageDt.get(i));
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

        result.add(result.get(result.size() -1));

        return result;
    }
}
