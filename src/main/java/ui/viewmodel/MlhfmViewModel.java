package ui.viewmodel;

import io.reactivex.subjects.PublishSubject;
import parser.mlhfm.MlhfmParser;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MlhfmViewModel {

    private MlhfmParser mlhfmParser;
    private PublishSubject<Map<String, List<Double>>> mlhfmPublishSubject;
    private PublishSubject<File> filePublishSubject;

    private static MlhfmViewModel instance;

    public static MlhfmViewModel getInstance() {
        if (instance == null) {
            instance = new MlhfmViewModel();
        }

        return instance;
    }

    private MlhfmViewModel() {
        mlhfmParser = new MlhfmParser();
        mlhfmPublishSubject = PublishSubject.create();
        filePublishSubject = PublishSubject.create();
    }

    public PublishSubject<Map<String, List<Double>>> getMlhfmPublishSubject() {
        return mlhfmPublishSubject;
    }

    public PublishSubject<File> getFilePublishSubject() {
        return filePublishSubject;
    }

    public void loadFile(File file) {

        Map<String, List<Double>> mlhfmMap = mlhfmParser.parse(file);

        if (mlhfmMap != null) {
            mlhfmPublishSubject.onNext(mlhfmMap);
            filePublishSubject.onNext(file);
        }
    }
}
