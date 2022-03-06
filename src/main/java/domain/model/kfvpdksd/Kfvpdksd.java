package domain.model.kfvpdksd;

import data.contract.Me7LogFileContract;

import java.util.*;

public class Kfvpdksd {

    private final Double[][] kfvpdksd;

    public Kfvpdksd(Double[][] kfvpdksd) {
        this.kfvpdksd = kfvpdksd;
    }

    public Double[][] getKfvpdksd() {
        return kfvpdksd;
    }

    public static Double[] parsePressure(Map<Me7LogFileContract.Header, List<Double>> log, Double[] rpmAxis) {
        List<List<Double>> boostValues = new ArrayList<>();

        for (int i = 0; i < rpmAxis.length; i++) {
            boostValues.add(new ArrayList<>());
        }

        List<Double> timestamps = log.get(Me7LogFileContract.Header.TIME_COLUMN_HEADER);
        List<Double> throttleAngle = log.get(Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER);
        List<Double> rpm = log.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER);
        List<Double> barometricPressure = log.get(Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER);
        List<Double> absolutePressure = log.get(Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER);

        for (int i = 0; i < timestamps.size(); i++) {
            if (throttleAngle.get(i) > 80) {

                int index = Arrays.binarySearch(rpmAxis, rpm.get(i));

                if (index < 0) {
                    index = Math.abs(index + 1);
                }

                index = Math.min(index, rpmAxis.length - 1);

                boostValues.get(index).add(absolutePressure.get(i) - barometricPressure.get(i));
            }
        }

        Double[] maxPressure = new Double[rpmAxis.length];

        for (int i = 0; i < rpmAxis.length; i++) {
            Collections.sort(boostValues.get(i));
            Collections.reverse(boostValues.get(i));

            int numElements = boostValues.get(i).size() > 0 ? (int) Math.max(1, boostValues.get(i).size() * 0.05) : 0;

            double sum = 0;
            for (int j = 0; j < numElements; j++) {
                sum += boostValues.get(i).get(j);
            }

            if (numElements > 0) {
                maxPressure[i] = sum / numElements;
            } else {
                maxPressure[i] = 0.0;
            }
        }

        return maxPressure;
    }

    public static Kfvpdksd generate(Double[] maxPressure, Double[] rpmAxis, Double[] pressureRatioAxis) {
        Double[] pressureRatio = new Double[maxPressure.length];

        for (int i = 0; i < pressureRatio.length; i++) {
            pressureRatio[i] = ((maxPressure[i]) + 1000) / 1000;

            if (Double.isNaN(pressureRatio[i])) {
                pressureRatio[i] = 0.0;
            }
        }

        Double[][] kfvpdksd = new Double[rpmAxis.length][pressureRatioAxis.length];

        for (int rpmIndex = 0; rpmIndex < kfvpdksd.length; rpmIndex++) {
            double maxPressureRatio = pressureRatio[rpmIndex];
            for (int prIndex = 0; prIndex < kfvpdksd[rpmIndex].length; prIndex++) {
                if(prIndex == 0) {
                    kfvpdksd[rpmIndex][prIndex] = 0.965;
                    continue;
                }

                if (pressureRatioAxis[prIndex] > maxPressureRatio) {
                    kfvpdksd[rpmIndex][prIndex] = 1.016;
                } else {
                    kfvpdksd[rpmIndex][prIndex] = 0.965;
                }
            }
        }

        return new Kfvpdksd(kfvpdksd);
    }
}
