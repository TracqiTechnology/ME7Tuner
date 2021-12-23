package ui.view.closedloopfueling.mlhfm;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import ui.view.listener.OnTabSelectedListener;
import ui.view.mlhfm.MlhfmView;
import ui.viewmodel.closedloopfueling.mlhfm.ClosedLoopMlhfmViewModel;

import javax.swing.*;
import java.awt.*;

public class ClosedLoopMlhfmView implements OnTabSelectedListener {

    private JTabbedPane tabbedPane;

    public ClosedLoopMlhfmView() {

        ClosedLoopMlhfmViewModel viewModel = new ClosedLoopMlhfmViewModel();
        viewModel.registerMLHFMOnChange(new Observer<ClosedLoopMlhfmViewModel.ClosedLoopMlfhmModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull ClosedLoopMlhfmViewModel.ClosedLoopMlfhmModel closedLoopMlfhmModel) {
                enableLogsTab(closedLoopMlfhmModel.isLogsTabEnabled());
                enableCorrectionTab(closedLoopMlfhmModel.isCorrectionsTabEnabled());
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
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
        tabbedPane.addTab("MLHFM", null, new MlhfmView().getMlhfmPanel(), "Base MLHFM");
        tabbedPane.addTab("ME7 Logs", null, new ClosedLoopLogView().getMe7LogPanel(), "Closed Loop ME7 Logs");
        tabbedPane.addTab("Correction", null, new ClosedLoopMlhfmCorrectionView().getCorrectionPanel(), "Corrected MLHFM");
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
    public void onTabSelected(boolean selected) {}
}
