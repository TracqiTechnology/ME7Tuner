package domain.model.ldrpid;

import data.contract.Me7LogFileContract;
import domain.math.Index;
import domain.math.map.Map3d;
import domain.math.LinearInterpolation;

import java.util.*;

public class LdrpidCalculator {

    public static class LdrpidResult {
        public final Map3d nonLinearOutput;
        public final Map3d linearOutput;
        public final Map3d kfldrl;
        public final Map3d kfldimx;

        public LdrpidResult(Map3d nonLinearOutput, Map3d linearOutput, Map3d kfldrl, Map3d kfldimx) {
            this.nonLinearOutput = nonLinearOutput;
            this.linearOutput = linearOutput;
            this.kfldrl = kfldrl;
            this.kfldimx = kfldimx;
        }
    }

    public static Map3d calculateNonLinearTable(Map<Me7LogFileContract.Header, List<Double>> values, Map3d kfldrlMap) {
        List<Double> throttlePlateAngles = values.get(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER);
        List<Double> rpms = values.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER);
        List<Double> dutyCycles = values.get(Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER);
        List<Double> barometricPressures = values.get(Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER);
        List<Double> absoluteBoostPressures = values.get(Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER);

        Double[][] nonLinearTable = new Double[kfldrlMap.yAxis.length][kfldrlMap.xAxis.length];

        for(Double[] array:nonLinearTable) {
            Arrays.fill(array, 0.0);
        }

        double[][] pressure = new double[kfldrlMap.yAxis.length][kfldrlMap.xAxis.length];
        double[][] count = new double[kfldrlMap.yAxis.length][kfldrlMap.xAxis.length];

        for(int i = 0; i < throttlePlateAngles.size(); i++) {
            double throttlePlateAngle = throttlePlateAngles.get(i);
            if(throttlePlateAngle >= 80) {
                double rpm = rpms.get(i);
                double dutyCycle = dutyCycles.get(i);
                double barometricPressure = barometricPressures.get(i);
                double absoluteBoostPressure = absoluteBoostPressures.get(i);
                double relativeBoostPressure = absoluteBoostPressure - barometricPressure;

                int rpmIndex = Index.getInsertIndex(Arrays.asList(kfldrlMap.yAxis), rpm);
                int dutyCycleIndex = Index.getInsertIndex(Arrays.asList(kfldrlMap.xAxis), dutyCycle);

                if(relativeBoostPressure > 0) {
                    pressure[rpmIndex][dutyCycleIndex] += relativeBoostPressure;
                    count[rpmIndex][dutyCycleIndex] += 1;
                }

                for(int j = 0; j < nonLinearTable.length; j++) {
                    for(int k = 0; k < nonLinearTable[j].length; k++) {
                        if(count[j][k] != 0) {
                            nonLinearTable[j][k] = (pressure[j][k] / count[j][k]) * 0.0145038;
                        } else {
                            nonLinearTable[j][k] = pressure[j][k] * 0.0145038;
                        }
                    }
                }
            }
        }

        for(Double[] array: nonLinearTable) {
            Arrays.sort(array);
            for(int i = 0; i < array.length - 1; i++) {
                if(array[i] == 0) {
                    array[i] = 0.1;
                }

                if(array[i] >= array[i + 1]) {
                    if(i > 0) {
                        double theta = array[i]/array[i-1];
                        array[i + 1] = array[i]*(1+(theta-1)/2);
                    } else {
                        array[i + 1] = array[i]*1.1;
                    }

                    if(Double.isNaN(array[i + 1]) || array[i + 1] == 0) {
                        array[i + 1] = array[i] + 0.1;
                    }
                }
            }
        }

        return new Map3d(kfldrlMap.xAxis, kfldrlMap.yAxis, nonLinearTable);
    }

    public static Map3d calculateLinearTable(Double[][] nonLinearTable, Map3d kfldrlMap) {

        Double[][] linearTable = new Double[nonLinearTable.length][nonLinearTable[0].length];

        for(int i = 0; i < nonLinearTable[0].length; i++) {
            double min = nonLinearTable[0][i];
            double max = nonLinearTable[nonLinearTable.length - 1][i];
            double step = (max - min)/ (nonLinearTable.length - 1);

            for(int j = 0; j < linearTable.length; j++) {
                linearTable[j][i] = min + step * j;
            }
        }

        return new Map3d(kfldrlMap.xAxis, kfldrlMap.yAxis, linearTable);
    }

    public static Map3d calculateKfldrl(Double[][] nonLinearTable, Double[][] linearTable, Map3d kfldrlMap) {
        Double[][] kfldrl = new Double[nonLinearTable.length][nonLinearTable[0].length];

        for(int i = 0; i < nonLinearTable.length; i++) {
            for(int j = 0; j < nonLinearTable[i].length; j++) {
                Double[] x = nonLinearTable[i];
                Double[] y = kfldrlMap.xAxis;
                Double[] xi = new Double[]{linearTable[i][j]};

                kfldrl[i][j] = LinearInterpolation.interpolate(x, y, xi)[0];

                if(Double.isNaN(kfldrl[i][j])) {
                    kfldrl[i][j] = 0d;
                }
            }
        }

        return new Map3d(kfldrlMap.xAxis, kfldrlMap.yAxis, kfldrl);
    }

    public static Map3d calculateKfldimx(Double[][] nonLinearTable, Double[][] linearTable, Map3d kfldrlMap, Map3d kfldimxMap) {
        Double[] linearBoostMax = new Double[linearTable[0].length];

        for(int i = 0; i < linearBoostMax.length; i++) {

            List<Double> linearBoost = new ArrayList<>();
            for(int j = 0; j < linearTable.length; j++) {
                linearBoost.add(linearTable[j][i]*68.9476);
            }

            linearBoostMax[i] = Collections.max(linearBoost);
        }

        Double[] kfldimxXAxis = new Double[kfldimxMap.xAxis.length];

        double min = (Math.ceil(linearBoostMax[0]/100.0))*100;
        double max = (Math.ceil(linearBoostMax[linearBoostMax.length - 1]/100.0))*100;
        double interval = (max - min)/(kfldimxXAxis.length - 1);

        for(int i = 0; i < kfldimxXAxis.length; i++) {
            kfldimxXAxis[i] = min + (interval * i);
        }

        Double[][] kfldimx = new Double[nonLinearTable.length][kfldimxXAxis.length];

        for(int i = 0; i < kfldimx.length; i++) {
            for (int j = 0; j < kfldimx[i].length; j++) {
                Double[] x = linearBoostMax;
                Double[] y = kfldrlMap.xAxis;
                Double[] xi = new Double[]{kfldimxXAxis[j]};

                kfldimx[i][j] = LinearInterpolation.interpolate(x, y, xi)[0];
            }
        }

        return new Map3d(kfldimxXAxis, kfldimxMap.yAxis, kfldimx);
    }

    public static LdrpidResult caclulateLdrpid(Map<Me7LogFileContract.Header, List<Double>> values, Map3d kfldrlMap, Map3d kfldimxMap) {

        Map3d nonLinearTable = calculateNonLinearTable(values, kfldrlMap);
        Map3d linearTable = calculateLinearTable(nonLinearTable.zAxis, kfldrlMap);
        Map3d kfldrl = calculateKfldrl(nonLinearTable.zAxis, linearTable.zAxis, kfldrlMap);
        Map3d kfldimxMap3d = calculateKfldimx(nonLinearTable.zAxis, linearTable.zAxis, kfldrlMap, kfldimxMap);

        return new LdrpidResult(nonLinearTable, linearTable, kfldrl, kfldimxMap3d);
    }

    private static int getMaxIndex(double number, Double[] array){
        if(number <= array[0]) {
            return -1;
        } else if(number >= array[array.length -1]) {
            return array.length;
        } else {
            for (int i = 0; i < array.length - 1; i++) {
                if(number >= array[i] && number < array[i + 1]) {
                    return i + 1;
                }
            }
        }

        return -1;
    }

    private static void reverse(Double[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            double temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }




}
