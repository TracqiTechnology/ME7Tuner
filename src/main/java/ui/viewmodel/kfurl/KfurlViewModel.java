package ui.viewmodel.kfurl;

import contract.AfrLogFileContract;
import contract.Me7LogFileContract;
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
    private final BehaviorSubject<Map<String, List<Double>>> me7LogsSubject;
    private final BehaviorSubject<Map<String, List<Double>>> zeitLogsSubject;

    private static KfurlViewModel instance;

    private Map<String, List<Double>> me7logs;

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
    }

    public BehaviorSubject<Map3d> getInputSubject() {
        return inputSubject;
    }

    public BehaviorSubject<KfurlCorrection> getOutputSubject() {
        return outputSubject;
    }

    public BehaviorSubject<Map<String, List<Double>>> getMe7LogsSubject() {
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

    public void loadZeitFile(File file) {
        AfrLogParser afrLogParser = new AfrLogParser();
        Map<String, List<Double>> zeitLogs = afrLogParser.parse(file);
        processZeitLogs(zeitLogs);
        zeitLogsSubject.onNext(zeitLogs);

        outputSubject.onNext(KfurlCalculator.calculateKfurl(inputSubject.getValue(), me7logs, zeitLogs));
    }

    private void processZeitLogs(Map<String, List<Double>> zeitLogs) {
        double me7StartTime = me7logs.get(Me7LogFileContract.START_TIME).get(0);
        double zeitStartTime = zeitLogs.get(AfrLogFileContract.START_TIME).get(0);
        double timeOffset = (zeitStartTime - me7StartTime) - zeitStartTime;

        List<Double> timestamps = zeitLogs.get(AfrLogFileContract.TIMESTAMP);
        for(int i = 0; i < timestamps.size(); i++) {
            timestamps.set(i, timestamps.get(i) + timeOffset);
        }

        double barometricPressureMbar = me7logs.get(Me7LogFileContract.BAROMETRIC_PRESSURE_HEADER).get(0);
        List<Double> boostPressures = zeitLogs.get(AfrLogFileContract.BOOST_HEADER);
        for(int i = 0; i < boostPressures .size(); i++) {
            boostPressures .set(i, boostPressures .get(i) + barometricPressureMbar);
        }
    }


}
