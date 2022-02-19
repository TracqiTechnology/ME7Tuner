package parser.me7log;

import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.io.File;
import java.util.List;
import java.util.Map;

public class KfvpdksdLogParser {

    private final PublishSubject<Map<String, List<Double>>> publishSubject;

    private static KfvpdksdLogParser instance;

    public static KfvpdksdLogParser getInstance() {
        if (instance == null) {
            synchronized (KfvpdksdLogParser.class) {
                if (instance == null) {
                    instance = new KfvpdksdLogParser();
                }
            }
        }

        return instance;
    }

    private KfvpdksdLogParser() {
        publishSubject = PublishSubject.create();
    }

    public void registerLogOnChangeObserver(Observer<Map<String, List<Double>>> observer){
        publishSubject.subscribe(observer);
    }

    public void loadDirectory(File directory, Me7LogParser.ProgressCallback progressCallback) {
        if (directory.isDirectory()) {
            Me7LogParser me7LogParser = new Me7LogParser();
            Single.fromCallable(() -> me7LogParser.parseLogDirectory(Me7LogParser.LogType.KFVPDKSD, directory, progressCallback)).subscribeOn(Schedulers.io()).subscribe(new SingleObserver<Map<String, List<Double>>>() {
                @Override
                public void onSubscribe(@NonNull Disposable disposable) {}

                @Override
                public void onSuccess(@NonNull Map<String, List<Double>> logMap) {
                    publishSubject.onNext(logMap);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
        }
    }
}
