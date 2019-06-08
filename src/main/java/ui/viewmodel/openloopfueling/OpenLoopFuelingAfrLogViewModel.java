package ui.viewmodel.openloopfueling;

import io.reactivex.subjects.PublishSubject;
import parser.afrLog.AfrLogParser;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenLoopFuelingAfrLogViewModel {

    private AfrLogParser afrLogParser;
    private PublishSubject<Map<String, List<Double>>> publishSubject;

    private static OpenLoopFuelingAfrLogViewModel instance;

    public static OpenLoopFuelingAfrLogViewModel getInstance() {
        if (instance == null) {
            instance = new OpenLoopFuelingAfrLogViewModel();
        }

        return instance;
    }

    private OpenLoopFuelingAfrLogViewModel() {
        afrLogParser = new AfrLogParser();
        publishSubject = PublishSubject.create();
    }

    public PublishSubject<Map<String, List<Double>>> getPublishSubject() {
        return publishSubject;
    }

    public void loadFile(File file) {
        Map<String, List<Double>> afrLogMap = afrLogParser.parse(file);

        if (afrLogMap != null) {
            publishSubject.onNext(afrLogMap);
        }
    }
}
