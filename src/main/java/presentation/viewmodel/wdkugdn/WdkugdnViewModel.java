package presentation.viewmodel.wdkugdn;

import data.parser.xdf.TableDefinition;
import data.preferences.MapPreferenceManager;
import data.preferences.kfwdkmsn.KfwdkmsnPreferences;
import data.preferences.wdkugdn.WdkugdnPreferences;
import domain.math.map.Map3d;
import domain.model.wdkugdn.Wdkugdn;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import org.apache.commons.math3.util.Pair;

import javax.swing.*;
import java.util.Optional;

public class WdkugdnViewModel {

    private final Subject<WdkugnModel> subject = BehaviorSubject.create();

    public WdkugdnViewModel() {

        WdkugdnPreferences.getInstance().registerOnMapChanged(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                calculateWdkugdn();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
            }
        });

        KfwdkmsnPreferences.getInstance().registerOnMapChanged(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                calculateWdkugdn();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {

            }
        });

        MapPreferenceManager.registerOnClear(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Boolean aBoolean) {
                subject.onNext(new WdkugnModel(null, null));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        calculateWdkugdn();
    }

    public void calculateWdkugdn() {
        SwingUtilities.invokeLater(() -> {
            Pair<TableDefinition, Map3d> wdkugdn = WdkugdnPreferences.getInstance().getSelectedMap();
            Pair<TableDefinition, Map3d> kfwdkmsn = KfwdkmsnPreferences.getInstance().getSelectedMap();

            if (wdkugdn != null && kfwdkmsn != null) {
                subject.onNext(new WdkugnModel(Wdkugdn.calculateWdkugdn(wdkugdn.getSecond(), kfwdkmsn.getSecond(), WdkugdnPreferences.getInstance().getEngineDisplacementPreference()), wdkugdn.getFirst().getTableName()));
            } else if(wdkugdn != null) {
                subject.onNext(new WdkugnModel(null, wdkugdn.getFirst().getTableName()));
            }else if(kfwdkmsn != null) {
                subject.onNext(new WdkugnModel(null, null));
            }
        });
    }

    public void registerOnChange(Observer<WdkugnModel> observer) {
        subject.subscribe(observer);
    }

    public record WdkugnModel(Map3d wdkudgn, String wdkudgnDefinitionTitle) {
        public WdkugnModel(@Nullable Map3d wdkudgn, @Nullable String wdkudgnDefinitionTitle) {
            this.wdkudgn = wdkudgn;
            this.wdkudgnDefinitionTitle = wdkudgnDefinitionTitle;
        }

        @Nullable
        public Map3d getWdkugdn() {
            return wdkudgn;
        }

        @Nullable
        public String getWdkudgnDefinitionTitle() {
            return wdkudgnDefinitionTitle;
        }
    }
}
