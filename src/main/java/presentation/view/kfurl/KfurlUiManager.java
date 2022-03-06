package presentation.view.kfurl;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import domain.model.kfurl.KfurlCorrection;
import presentation.view.listener.OnTabSelectedListener;
import presentation.viewmodel.kfurl.KfurlViewModel;

import javax.swing.*;
import java.awt.*;

public class KfurlUiManager implements OnTabSelectedListener {

    private JTabbedPane tabbedPane;

    private boolean selected;

    public KfurlUiManager() {
        KfurlViewModel.getInstance().getOutputSubject().subscribe(new Observer<KfurlCorrection>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override
            public void onNext(@NonNull KfurlCorrection kfurlCorrection) {
                enableCorrectionTab(selected && kfurlCorrection.kfurl != null);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    public JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(getTabbedPane(), BorderLayout.CENTER);

        return panel;
    }

    private JTabbedPane getTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        tabbedPane.addTab("KFURL", null, new KfurlMapUiManager().getPanel(), "KFURL");
        tabbedPane.addTab("Logs", null, new KfurlLogUiManager().getPanel(), "ME7 Logging");
        tabbedPane.addTab("Corrections", null, new KfurlCorrectionUiManager().getPanel(), "Corrected KFURL");

        enableLogsTab(true);
        enableCorrectionTab(false);

        return tabbedPane;
    }

    private void enableLogsTab(boolean enabled) {
        tabbedPane.setEnabledAt(1, enabled);
    }

    private void enableCorrectionTab(boolean enabled) {
        tabbedPane.setEnabledAt(2, enabled);
    }

    @Override
    public void onTabSelected(boolean selected) {
        this.selected = selected;
    }
}
