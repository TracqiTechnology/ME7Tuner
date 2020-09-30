package parser.afrLog;

import contract.AfrLogFileContract;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AfrLogParser {

    private int timeColumnIndex = -1;
    private int rpmColumnIndex = -1;
    private int tpsColumnIndex = -1;
    private int afrColumnIndex = -1;
    private int boostColumnIndex = -1;

    private double lastPsi = 0;

    public Map<String, List<Double>> parse(File file) {

        Map<String, List<Double>> map = new HashMap<>();
        map.put(AfrLogFileContract.START_TIME, new ArrayList<>());
        map.put(AfrLogFileContract.TIMESTAMP, new ArrayList<>());
        map.put(AfrLogFileContract.RPM_HEADER, new ArrayList<>());
        map.put(AfrLogFileContract.AFR_HEADER, new ArrayList<>());
        map.put(AfrLogFileContract.TPS_HEADER, new ArrayList<>());
        map.put(AfrLogFileContract.BOOST_HEADER, new ArrayList<>());

        try {
            boolean headersFound = false;
            Reader in = new FileReader(file);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);

            for (CSVRecord record : records) {
                for (int i = 0; i < record.size(); i++) {
                    switch (record.get(i).trim()) {
                        case AfrLogFileContract.TIMESTAMP:
                            timeColumnIndex = i;
                            break;
                        case AfrLogFileContract.TPS_HEADER:
                            tpsColumnIndex = i;
                            break;
                        case AfrLogFileContract.RPM_HEADER:
                            rpmColumnIndex = i;
                            break;
                        case AfrLogFileContract.AFR_HEADER:
                            afrColumnIndex = i;
                            break;
                        case AfrLogFileContract.BOOST_HEADER:
                            boostColumnIndex = i;
                            break;
                    }

                    if (headersFound = headersFound()) {
                        break;
                    }
                }

                if (headersFound) {
                    break;
                }
            }


            if (headersFound) {
                for (CSVRecord record : records) {

                    String[] split = record.get(timeColumnIndex).split(":");
                    double minuteSeconds = Double.parseDouble(split[1]) * 60;
                    double seconds = Double.parseDouble(split[2]);
                    double timestamp = (minuteSeconds + seconds);

                    map.get(AfrLogFileContract.TIMESTAMP).add(timestamp);
                    map.get(AfrLogFileContract.TPS_HEADER).add(Double.parseDouble(record.get(tpsColumnIndex)));
                    map.get(AfrLogFileContract.RPM_HEADER).add(Double.parseDouble(record.get(rpmColumnIndex)));
                    map.get(AfrLogFileContract.AFR_HEADER).add(Double.parseDouble(record.get(afrColumnIndex)));

                    double psi = Double.parseDouble(record.get(boostColumnIndex));

                    if(psi > 50) {
                        psi = lastPsi;
                    } else {
                        lastPsi = psi;
                    }

                    double mbar;

                    // zeit reports psi for positive pressure and inHg for negative pressure
                    if(psi >= 0) {
                        mbar = psi*68.9476;
                    } else {
                        mbar = psi*33.8639;
                    }

                    map.get(AfrLogFileContract.BOOST_HEADER).add(mbar);
                }
            }

            double startTime = map.get(AfrLogFileContract.TIMESTAMP).get(0);
            map.get(AfrLogFileContract.START_TIME).add(startTime);

            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean headersFound() {
        return tpsColumnIndex != -1 && rpmColumnIndex != -1 && afrColumnIndex != -1 && timeColumnIndex != -1 && boostColumnIndex != -1;
    }
}
