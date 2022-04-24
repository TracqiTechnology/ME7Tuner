package presentation.viewmodel.openloopfueling;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import data.parser.afrLog.AfrLogParser;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenLoopFuelingAfrLogViewModel {

    private final PublishSubject<Map<String, List<Double>>> publishSubject = PublishSubject.create();

    public OpenLoopFuelingAfrLogViewModel() {
        AfrLogParser.getInstance().register(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Map<String, List<Double>> logs) {
                publishSubject.onNext(logs);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public PublishSubject<Map<String, List<Double>>> getPublishSubject() {
        return publishSubject;
    }

    public void loadFile(File file) {
       AfrLogParser.getInstance().load(file);
    }
}
