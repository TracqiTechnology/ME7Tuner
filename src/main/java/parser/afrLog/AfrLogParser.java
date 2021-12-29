package parser.afrLog;

import contract.AfrLogFileContract;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import parser.me7log.Me7LogParser;
import parser.me7log.OpenLoopLogParser;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AfrLogParser {

    private static AfrLogParser instance;

    private final PublishSubject<Map<String, List<Double>>> publishSubject = PublishSubject.create();

    private AfrLogParser() {}

    public static AfrLogParser getInstance() {
        if (instance == null) {
            synchronized (AfrLogParser.class) {
                if (instance == null) {
                    instance = new AfrLogParser();
                }
            }
        }

        return instance;
    }

    public void register(Observer<Map<String, List<Double>>> observer){
        SwingUtilities.invokeLater(() -> publishSubject.subscribe(observer));
    }

    public void loadFile(File file) {
        Single.fromCallable(() -> parse(file)).subscribeOn(Schedulers.io()).subscribe(new SingleObserver<Map<String, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onSuccess(@NonNull Map<String, List<Double>> logMap) {
                SwingUtilities.invokeLater(() -> publishSubject.onNext(logMap));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}
        });
    }

    private Map<String, List<Double>> parse(File file) {

        int timeColumnIndex = -1;
        int rpmColumnIndex = -1;
        int tpsColumnIndex = -1;
        int afrColumnIndex = -1;
        int boostColumnIndex = -1;

        double lastPsi = 0;

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

                    if (headersFound = headersFound(timeColumnIndex, rpmColumnIndex, tpsColumnIndex, afrColumnIndex, boostColumnIndex)) {
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

        return new HashMap<>();
    }

    private boolean headersFound(int timeColumnIndex, int rpmColumnIndex, int tpsColumnIndex, int afrColumnIndex, int boostColumnIndex) {
        return tpsColumnIndex != -1 && rpmColumnIndex != -1 && afrColumnIndex != -1 && timeColumnIndex != -1 && boostColumnIndex != -1;
    }
}
