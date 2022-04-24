package data.parser.me7log;

import data.contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenLoopLogParser {

    private final PublishSubject<Map<Me7LogFileContract.Header, List<Double>>> publishSubject;

    private static OpenLoopLogParser instance;

    public static OpenLoopLogParser getInstance() {
        if (instance == null) {
            synchronized (OpenLoopLogParser.class) {
                if (instance == null) {
                    instance = new OpenLoopLogParser();
                }
            }
        }

        return instance;
    }

    private OpenLoopLogParser() {
        publishSubject = PublishSubject.create();
    }

    public void register(Observer<Map<Me7LogFileContract.Header, List<Double>>> observer) {
        SwingUtilities.invokeLater(() -> publishSubject.subscribe(observer));
    }

    public void loadFile(File file) {
        Me7LogParser me7LogParser = new Me7LogParser();
        Single.fromCallable(() -> me7LogParser.parseLogFile(Me7LogParser.LogType.OPEN_LOOP, file)).subscribeOn(Schedulers.io()).subscribe(new SingleObserver<Map<Me7LogFileContract.Header, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onSuccess(@NonNull Map<Me7LogFileContract.Header, List<Double>> logMap) {
                SwingUtilities.invokeLater(() -> publishSubject.onNext(logMap));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }
}
