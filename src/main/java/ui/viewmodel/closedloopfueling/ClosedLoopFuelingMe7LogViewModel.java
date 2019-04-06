package ui.viewmodel.closedloopfueling;

import io.reactivex.subjects.PublishSubject;
import parser.me7log.Me7LogParser;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ClosedLoopFuelingMe7LogViewModel {

    private Me7LogParser me7LogParser;
    private PublishSubject<Map<String, List<Double>>> publishSubject;

    private static ClosedLoopFuelingMe7LogViewModel instance;

    public static ClosedLoopFuelingMe7LogViewModel getInstance() {
        if (instance == null) {
            instance = new ClosedLoopFuelingMe7LogViewModel();
        }

        return instance;
    }

    private ClosedLoopFuelingMe7LogViewModel() {
        me7LogParser = new Me7LogParser();
        publishSubject = PublishSubject.create();
    }

    public PublishSubject<Map<String, List<Double>>> getPublishSubject() {
        return publishSubject;
    }

    public void loadDirectory(File directory) {
        if (directory.isDirectory()) {
            Map<String, List<Double>> me7LogMap = me7LogParser.parseLogDirectory(Me7LogParser.LogType.CLOSED_LOOP, directory);

            if (me7LogMap != null) {
                publishSubject.onNext(me7LogMap);
            }
        } else {
            publishSubject.onError(new Throwable("Not a directory!"));
        }
    }
}
