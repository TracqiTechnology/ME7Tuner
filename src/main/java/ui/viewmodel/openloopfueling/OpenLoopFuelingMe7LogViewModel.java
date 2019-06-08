package ui.viewmodel.openloopfueling;

import io.reactivex.subjects.PublishSubject;
import parser.me7log.Me7LogParser;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenLoopFuelingMe7LogViewModel {

    private Me7LogParser me7LogParser;
    private PublishSubject<Map<String, List<Double>>> publishSubject;

    private static OpenLoopFuelingMe7LogViewModel instance;

    public static OpenLoopFuelingMe7LogViewModel getInstance() {
        if (instance == null) {
            instance = new OpenLoopFuelingMe7LogViewModel();
        }

        return instance;
    }

    private OpenLoopFuelingMe7LogViewModel() {
        me7LogParser = new Me7LogParser();
        publishSubject = PublishSubject.create();
    }

    public PublishSubject<Map<String, List<Double>>> getPublishSubject() {
        return publishSubject;
    }

    public void loadFile(File file) {
        Map<String, List<Double>> me7LogMap = me7LogParser.parseLogFile(Me7LogParser.LogType.OPEN_LOOP, file);

        if (me7LogMap != null) {
            publishSubject.onNext(me7LogMap);
        }
    }
}
