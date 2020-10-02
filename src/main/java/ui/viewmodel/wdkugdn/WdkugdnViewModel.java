package ui.viewmodel.wdkugdn;

import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import model.wdkugdn.WdkugdnCalculator;
import model.wdkugdn.WdkugdnCorrection;
import parser.me7log.Me7LogParser;

import java.io.File;
import java.util.List;
import java.util.Map;

public class WdkugdnViewModel {
    private final BehaviorSubject<Map3d> inputSubject;
    private final BehaviorSubject<WdkugdnCorrection> outputSubject;
    private final BehaviorSubject<Map<String, List<Double>>> me7LogsSubject;

    private static WdkugdnViewModel instance;

    public static WdkugdnViewModel getInstance() {
        if (instance == null) {
            instance = new WdkugdnViewModel();
        }

        return instance;
    }

    private WdkugdnViewModel() {
        inputSubject = BehaviorSubject.create();
        outputSubject = BehaviorSubject.create();
        me7LogsSubject = BehaviorSubject.create();
    }

    public BehaviorSubject<Map3d> getInputSubject() {
        return inputSubject;
    }

    public BehaviorSubject<WdkugdnCorrection> getOutputSubject() {
        return outputSubject;
    }

    public BehaviorSubject<Map<String, List<Double>>> getMe7LogsSubject() {
        return me7LogsSubject;
    }

    public void setMap(Map3d map) {
        if (map != null) {
            inputSubject.onNext(map);
        }
    }

    public void loadMe7File(File file) {
        Me7LogParser me7LogParser = new Me7LogParser();
        Map<String, List<Double>> me7logs = me7LogParser.parseLogFile(Me7LogParser.LogType.WDKUGDN, file);
        outputSubject.onNext(WdkugdnCalculator.calculateWdkugdn(inputSubject.getValue(), me7logs));
        me7LogsSubject.onNext(me7logs);
    }
}
