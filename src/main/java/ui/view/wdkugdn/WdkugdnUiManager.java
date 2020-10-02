package ui.view.wdkugdn;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import model.kfurl.KfurlCorrection;
import model.wdkugdn.WdkugdnCorrection;
import ui.view.listener.OnTabSelectedListener;
import ui.viewmodel.wdkugdn.WdkugdnViewModel;

import javax.swing.*;
import java.awt.*;

public class WdkugdnUiManager implements OnTabSelectedListener {

    private JTabbedPane tabbedPane;

    private boolean selected;

    public WdkugdnUiManager() {
        WdkugdnViewModel.getInstance().getOutputSubject().subscribe(new Observer<WdkugdnCorrection>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override
            public void onNext(@NonNull WdkugdnCorrection correction) {
                enableCorrectionTab(selected && correction.wdkudgn != null);
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
        tabbedPane.addTab("WDKUDGN", null, new WdkugdnMapUiManager().getPanel(), "WDKUDGN");
        tabbedPane.addTab("Logs", null, new WdkugdnLogUiManager().getPanel(), "ME7 Logging");
        tabbedPane.addTab("Corrections", null, new WdkugdnCorrectionUiManager().getPanel(), "Corrected WDKUDGNL");

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
