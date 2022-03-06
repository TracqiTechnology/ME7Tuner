package presentation.viewmodel.kfurl;

import contract.AfrLogFileContract;
import contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import model.kfurl.KfurlCalculator;
import model.kfurl.KfurlCorrection;
import parser.afrLog.AfrLogParser;
import parser.me7log.Me7LogParser;

import java.io.File;
import java.util.List;
import java.util.Map;

public class KfurlViewModel {
    private final BehaviorSubject<Map3d> inputSubject;
    private final BehaviorSubject<KfurlCorrection> outputSubject;
    private final BehaviorSubject<Map<Me7LogFileContract.Header, List<Double>>> me7LogsSubject;
    private final BehaviorSubject<Map<String, List<Double>>> zeitLogsSubject;

    private static KfurlViewModel instance;

    private Map<Me7LogFileContract.Header, List<Double>> me7logs;

    public static KfurlViewModel getInstance() {
        if (instance == null) {
            instance = new KfurlViewModel();
        }

        return instance;
    }

    private KfurlViewModel() {
        inputSubject = BehaviorSubject.create();
        outputSubject = BehaviorSubject.create();
        me7LogsSubject = BehaviorSubject.create();
        zeitLogsSubject = BehaviorSubject.create();

        AfrLogParser.getInstance().register(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Map<String, List<Double>> logs) {
                processZeitLogs(logs);
                zeitLogsSubject.onNext(logs);

                if(me7logs != null) {
                    outputSubject.onNext(KfurlCalculator.calculateKfurl(inputSubject.getValue(), me7logs, logs));
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public BehaviorSubject<Map3d> getInputSubject() {
        return inputSubject;
    }

    public BehaviorSubject<KfurlCorrection> getOutputSubject() {
        return outputSubject;
    }

    public BehaviorSubject<Map<Me7LogFileContract.Header, List<Double>>> getMe7LogsSubject() {
        return me7LogsSubject;
    }

    public BehaviorSubject<Map<String, List<Double>>> getZeitLogsSubject() {
        return zeitLogsSubject;
    }

    public void setMap(Map3d map) {
        if (map != null) {
            inputSubject.onNext(map);
        }
    }

    public void loadMe7File(File file) {
        Me7LogParser me7LogParser = new Me7LogParser();
        me7logs = me7LogParser.parseLogFile(Me7LogParser.LogType.KFURL, file);

        me7LogsSubject.onNext(me7logs);
    }

    public void loadAfrFile(File file) {
        AfrLogParser.getInstance().loadFile(file);
    }

    private void processZeitLogs(Map<String, List<Double>> zeitLogs) {
        if(me7logs == null) {
            return;
        }

        double me7StartTime = me7logs.get(Me7LogFileContract.Header.START_TIME_HEADER.getHeader()).get(0);
        double zeitStartTime = zeitLogs.get(AfrLogFileContract.START_TIME).get(0);
        double timeOffset = (zeitStartTime - me7StartTime) - zeitStartTime;

        List<Double> timestamps = zeitLogs.get(AfrLogFileContract.TIMESTAMP);
        for(int i = 0; i < timestamps.size(); i++) {
            timestamps.set(i, timestamps.get(i) + timeOffset);
        }

        double barometricPressureMbar = me7logs.get(Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER.getHeader()).get(0);
        List<Double> boostPressures = zeitLogs.get(AfrLogFileContract.BOOST_HEADER);
        for(int i = 0; i < boostPressures .size(); i++) {
            boostPressures .set(i, boostPressures .get(i) + barometricPressureMbar);
        }
    }


}
