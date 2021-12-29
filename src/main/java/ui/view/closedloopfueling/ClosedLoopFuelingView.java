package ui.view.closedloopfueling;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import ui.view.listener.OnTabSelectedListener;
import ui.view.mlhfm.MlhfmView;
import ui.viewmodel.closedloopfueling.ClosedLoopFuelingViewModel;

import javax.swing.*;
import java.awt.*;

public class ClosedLoopFuelingView implements OnTabSelectedListener {

    private JTabbedPane tabbedPane;

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
        tabbedPane.addTab("ME7 Logs", null, new ClosedLoopFuelingLogView().getMe7LogPanel(), "Closed Loop ME7 Logs");
        tabbedPane.addTab("Correction", null, new ClosedLoopFuelingCorrectionView().getCorrectionPanel(), "Corrected MLHFM");
        tabbedPane.addTab("Help", null, new ClosedLoopFuelingHelpView().getPanel(),"");

        enableLogsTab(false);
        enableCorrectionTab(false);

        initViewModel();

        return tabbedPane;
    }

    private void enableLogsTab(boolean enabled) {
        tabbedPane.setEnabledAt(1, enabled);
    }

    private void enableCorrectionTab(boolean enabled) {
        tabbedPane.setEnabledAt(2, enabled);
    }

    private void setSelectedTab(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    private void initViewModel() {
        ClosedLoopFuelingViewModel viewModel = new ClosedLoopFuelingViewModel();
        viewModel.register(new Observer<ClosedLoopFuelingViewModel.ClosedLoopMlfhmModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull ClosedLoopFuelingViewModel.ClosedLoopMlfhmModel closedLoopMlfhmModel) {
                enableLogsTab(closedLoopMlfhmModel.isLogsTabEnabled());
                enableCorrectionTab(closedLoopMlfhmModel.isCorrectionsTabEnabled());
                setSelectedTab(closedLoopMlfhmModel.getSelectedTabIndex());
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    @Override
    public void onTabSelected(boolean selected) {}
}
