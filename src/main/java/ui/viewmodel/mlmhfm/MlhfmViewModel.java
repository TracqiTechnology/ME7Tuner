package ui.viewmodel.mlmhfm;

import io.reactivex.subjects.PublishSubject;
import math.map.Map2d;
import parser.mlhfm.MlhfmParser;

import java.io.File;

public class MlhfmViewModel {

    private MlhfmParser mlhfmParser;
    private PublishSubject<Map2d> mlhfmPublishSubject;
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

    public PublishSubject<Map2d> getMlhfmPublishSubject() {
        return mlhfmPublishSubject;
    }

    public PublishSubject<File> getFilePublishSubject() {
        return filePublishSubject;
    }

    public void loadFile(File file) {

        Map2d mlhfmMap = mlhfmParser.parse(file);

        if (mlhfmMap != null) {
            mlhfmPublishSubject.onNext(mlhfmMap);
            filePublishSubject.onNext(file);
        }
    }
}
