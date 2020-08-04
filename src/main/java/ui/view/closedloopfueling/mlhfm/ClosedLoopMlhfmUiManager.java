package ui.view.closedloopfueling.mlhfm;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map2d;
import ui.view.listener.OnTabSelectedListener;
import ui.view.mlhfm.MlhfmUiManager;
import ui.viewmodel.MlhfmViewModel;
import ui.viewmodel.closedloopfueling.ClosedLoopFuelingMe7LogViewModel;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ClosedLoopMlhfmUiManager implements OnTabSelectedListener {

    private JTabbedPane tabbedPane;

    private boolean selected;

    public ClosedLoopMlhfmUiManager() {

        MlhfmViewModel.getInstance().getMlhfmPublishSubject().subscribe(new Observer<Map2d>() {
            @Override
            public void onNext(Map2d mlhfmMap) {
                enableLogsTab(selected && mlhfmMap != null);
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
        tabbedPane.addTab("MLHFM", null, new MlhfmUiManager().getMlhfmPanel(), "Base MLHFM");
        tabbedPane.addTab("ME7 Logs", null, new ClosedLoopMlhfmMe7LogUiManager().getMe7LogPanel(), "Closed Loop ME7 Logs");
        tabbedPane.addTab("Correction", null, new ClosedLoopMlhfmCorrectionUiManager().getCorrectionPanel(), "Corrected MLHFM");
        tabbedPane.addTab("Help", null, new ClosedLoopMlhfmHelpManager().getPanel(),"");

        enableLogsTab(false);
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
