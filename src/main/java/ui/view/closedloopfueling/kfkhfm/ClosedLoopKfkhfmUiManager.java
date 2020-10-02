package ui.view.closedloopfueling.kfkhfm;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import ui.view.kfkhfm.KfkhfmUiManager;
import ui.view.listener.OnTabSelectedListener;
import ui.viewmodel.closedloopfueling.ClosedLoopFuelingMe7LogViewModel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ClosedLoopKfkhfmUiManager implements OnTabSelectedListener {

    private JTabbedPane tabbedPane;
    private boolean selected;

    public ClosedLoopKfkhfmUiManager() {
        ClosedLoopFuelingMe7LogViewModel.getInstance().getPublishSubject().subscribe(new Observer<Map<String, java.util.List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> me7LogMap) {
                enableCorrectionTab(selected && me7LogMap != null);
            }

            @Override
            public void onSubscribe(Disposable disposable) {
            }

            @Override
            public void onError(Throwable throwable) {
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
        tabbedPane.addTab("KFKHFM", null, new KfkhfmUiManager("KFKHFM (Input)",true).getPanel(), "Correction map for MAF");
        tabbedPane.addTab("ME7 Logs", null, new ClosedLoopKfkhfmMe7LogUiManager().getMe7LogPanel(), "ME7 Logging");
        tabbedPane.addTab("Correction", null, new ClosedLoopKfkhfmCorrectionUiManager().getCorrectionPanel(), "Corrected MLHFM");
        tabbedPane.addTab("Help", null, new ClosedLoopKfkhfmHelpManager().getPanel(), "");

        enableCorrectionTab(false);

        return tabbedPane;
    }

    private void enableCorrectionTab(boolean enabled) {
        tabbedPane.setEnabledAt(2, enabled);
    }

    @Override
    public void onTabSelected(boolean selected) {
        this.selected = selected;
    }
}
