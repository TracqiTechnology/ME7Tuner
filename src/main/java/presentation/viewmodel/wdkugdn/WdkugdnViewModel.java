package presentation.viewmodel.wdkugdn;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import domain.math.map.Map3d;
import domain.model.wdkugdn.Wdkugdn;
import org.apache.commons.math3.util.Pair;
import data.parser.xdf.TableDefinition;
import data.preferences.kfwdkmsn.KfwdkmsnPreferences;
import data.preferences.wdkugdn.WdkugdnPreferences;

import javax.swing.*;
import java.util.Optional;

public class WdkugdnViewModel {

    private final Subject<WdkugnModel> subject = PublishSubject.create();

    public WdkugdnViewModel() {

        WdkugdnPreferences.getInstance().registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
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

        KfwdkmsnPreferences.getInstance().registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
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

        calculateWdkugdn();
    }

    public void calculateWdkugdn() {
        SwingUtilities.invokeLater(() -> {
            Pair<TableDefinition, Map3d> wdkugdn = WdkugdnPreferences.getInstance().getSelectedMap();
            Pair<TableDefinition, Map3d> kfwdkmsn = KfwdkmsnPreferences.getInstance().getSelectedMap();
            if (wdkugdn != null && kfwdkmsn != null) {
                subject.onNext(new WdkugnModel(Wdkugdn.calculateWdkugdn(wdkugdn.getSecond(), kfwdkmsn.getSecond(), WdkugdnPreferences.getInstance().getEngineDisplacementPreference()), wdkugdn.getFirst().getTableName(), kfwdkmsn.getFirst().getTableName()));
            } else if(wdkugdn != null) {
                subject.onNext(new WdkugnModel(null, wdkugdn.getFirst().getTableName(), null));
            }else if(kfwdkmsn != null) {
                subject.onNext(new WdkugnModel(null, null, kfwdkmsn.getFirst().getTableName()));
            }
        });
    }

    public void registerOnChange(Observer<WdkugnModel> observer) {
        subject.subscribe(observer);
    }

    public static class WdkugnModel {
        private final String wdkudgnDefinitionTitle;
        private final String kfwdkmsnDefinitionTitle;
        private final Map3d wdkudgn;

        private WdkugnModel(@Nullable Map3d wdkudgn, @Nullable String wdkudgnDefinitionTitle, @Nullable String kfwdkmsnDefinitionTitle) {
            this.wdkudgn = wdkudgn;
            this.wdkudgnDefinitionTitle = wdkudgnDefinitionTitle;
            this.kfwdkmsnDefinitionTitle = kfwdkmsnDefinitionTitle;
        }

        @Nullable
        public Map3d getWdkugdn() {
            return wdkudgn;
        }

        @Nullable
        public String getWdkudgnDefinitionTitle() {
            return wdkudgnDefinitionTitle;
        }

        @Nullable
        public String getKfwdkmsnDefinitionTitle() {
            return kfwdkmsnDefinitionTitle;
        }
    }
}
