package model.ldrpid;

import contract.Me7LogFileContract;
import math.CurveFitter;
import math.Index;
import math.LinearInterpolation;
import math.map.Map3d;
import model.kfldimx.Kfldimx;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    public static Map3d calculateNonLinearTable(Map<String, List<Double>> values) {
        List<Double> throttlePlateAngles = values.get(Me7LogFileContract.THROTTLE_PLATE_ANGLE_HEADER);
        List<Double> rpms = values.get(Me7LogFileContract.RPM_COLUMN_HEADER);
        List<Double> dutyCycles = values.get(Me7LogFileContract.WASTEGATE_DUTY_CYCLE_HEADER);
        List<Double> barometricPressures = values.get(Me7LogFileContract.BAROMETRIC_PRESSURE_HEADER);
        List<Double> absoluteBoostPressures = values.get(Me7LogFileContract.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER);
        List<Double> selectedGears = values.get(Me7LogFileContract.SELECTED_GEAR_HEADER);

        double[][] sums = new double[Kfldrl.getStockYAxis().length][Kfldrl.getStockXAxis().length];
        double[][] counts = new double[Kfldrl.getStockYAxis().length][Kfldrl.getStockXAxis().length];

        Double[][] nonLinearTable = new Double[Kfldrl.getStockYAxis().length][Kfldrl.getStockXAxis().length];

        for(int i = 0; i < throttlePlateAngles.size(); i++) {
            double throttlePlateAngle = throttlePlateAngles.get(i);
            double selectedGear = selectedGears.get(i);
            if(throttlePlateAngle >= 80 && selectedGear == 3) {
                double rpm = rpms.get(i);
                double dutyCycle = dutyCycles.get(i);
                double barometricPressure = barometricPressures.get(i);
                double absoluteBoostPressure = absoluteBoostPressures.get(i);
                double relativeBoostPressure = absoluteBoostPressure - barometricPressure;

                int rpmIndex = Index.getInsertIndex(Arrays.asList(Kfldrl.getStockYAxis()), rpm);
                int dutyCycleIndex = Index.getInsertIndex(Arrays.asList(Kfldrl.getStockXAxis()), dutyCycle);

                sums[rpmIndex][dutyCycleIndex] += relativeBoostPressure;
                counts[rpmIndex][dutyCycleIndex] += 1;

                for(int j = 0; j < nonLinearTable.length; j++) {
                    for(int k = 0; k < nonLinearTable[j].length; k++) {
                        double sum = sums[j][k];
                        double count = Math.max(counts[j][k], 1);

                        nonLinearTable[j][k] = (sum/count)*0.0145038;
                    }
                }
            }
        }

        for(Double[] array: nonLinearTable) {
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

        return new Map3d(Kfldrl.getStockXAxis(), Kfldrl.getStockYAxis(), nonLinearTable);
    }

    public static Map3d calculateLinearTable(Double[][] nonLinearTable) {
        Double[][] linearTable = new Double[nonLinearTable.length][nonLinearTable[0].length];

        for(int i = 0; i < linearTable.length; i++) {
            List<Double> x = Arrays.asList(Kfldrl.getStockXAxis());
            List<Double> y = Arrays.asList(nonLinearTable[i]);

            double[] coeff = CurveFitter.fitCurve(x, y, 1);
            linearTable[i] = CurveFitter.buildCurve(coeff, x).toArray(new Double[0]);
        }

        // The polynomial is monotonically decreasing... reverse it
        for(Double[] array: linearTable) {
            if(array[0] > array[array.length -1]) {
                reverse(array);
            }
        }

        // No negative numbers
        for(Double[] array: linearTable) {
            for(int i = 0; i < array.length; i++) {
                array[i]=Math.max(array[i],0);
            }
        }

        return new Map3d(Kfldrl.getStockXAxis(), Kfldrl.getStockYAxis(), linearTable);
    }

    public static Map3d calculateKfldrl(Double[][] nonLinearTable, Double[][] linearTable) {
        Double[][] kfldrl = new Double[nonLinearTable.length][nonLinearTable[0].length];

        for(int i = 0; i < nonLinearTable.length; i++) {
            for(int j = 0; j < nonLinearTable[i].length; j++) {
                Double[] x = nonLinearTable[i];
                Double[] y = Kfldrl.getStockXAxis();
                Double[] xi = new Double[]{linearTable[i][j]};

                kfldrl[i][j] = LinearInterpolation.interpolate(x, y, xi)[0];

                if(Double.isNaN(kfldrl[i][j])) {
                    kfldrl[i][j] = 0d;
                }
            }
        }

        return new Map3d(Kfldrl.getStockXAxis(), Kfldrl.getStockYAxis(), kfldrl);
    }

    public static Map3d calculateKfldimx(Double[][] nonLinearTable, Double[][] linearTable) {
        Double[] linearBoostAverage = new Double[linearTable[0].length];

        for(int i = 0; i < linearBoostAverage.length; i++) {
            double sum = 0;
            double count = 0;
            for(int j = 11; j < linearTable.length; j++) {
                sum += linearTable[j][i]*68.9476;
                count++;
            }

            linearBoostAverage[i] = (double) Math.round(sum / count);
        }

        Double[] kfldimxXAxis = new Double[Kfldimx.getStockXAxis().length];

        double min = (Math.ceil(linearBoostAverage[0]/100.0))*100;
        double max = (Math.ceil(linearBoostAverage[linearBoostAverage.length - 1]/100.0))*100;
        double interval = (max - min)/kfldimxXAxis.length;

        for(int i = 0; i < kfldimxXAxis.length; i++) {
            kfldimxXAxis[i] = min + (interval * i);
        }

        Double[][] kfldimx = new Double[nonLinearTable.length][kfldimxXAxis.length];

        for(int i = 0; i < kfldimx.length; i++) {
            for (int j = 0; j < kfldimx[i].length; j++) {
                Double[] x = linearBoostAverage;
                Double[] y = Kfldrl.getStockXAxis();
                Double[] xi = new Double[]{kfldimxXAxis[j]};

                kfldimx[i][j] = LinearInterpolation.interpolate(x, y, xi)[0];
            }
        }

        return new Map3d(kfldimxXAxis, Kfldimx.getStockYAxis(), kfldimx);
    }

    public static LdrpidResult caclulateLdrpid(Map<String, List<Double>> values) {

        Map3d nonLinearTable = calculateNonLinearTable(values);
        Map3d linearTable = calculateLinearTable(nonLinearTable.data);
        Map3d kfldrl = calculateKfldrl(nonLinearTable.data, linearTable.data);
        Map3d kfldimxMap3d = calculateKfldimx(nonLinearTable.data, linearTable.data);

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
