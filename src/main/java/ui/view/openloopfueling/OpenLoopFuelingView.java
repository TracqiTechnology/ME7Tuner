package ui.view.openloopfueling;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import ui.view.listener.OnTabSelectedListener;
import ui.view.mlhfm.MlhfmView;
import ui.viewmodel.openloopfueling.OpenLoopFuelingViewModel;

import javax.swing.*;
import java.awt.*;

public class OpenLoopFuelingView implements OnTabSelectedListener {

    private JTabbedPane tabbedPane;
    private boolean selected;
    private OpenLoopFuelingViewModel.OpenLoopMlfhmModel openLoopMlfhmModel;

    public OpenLoopFuelingView() {
        this.selected = false;
    }

    public JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(getTabbedPane(), BorderLayout.CENTER);

        initViewModel();

        return panel;
    }

    @Override
    public void onTabSelected(boolean selected) {
        this.selected = selected;
        updateModel(openLoopMlfhmModel);
    }

    private void initViewModel() {
        OpenLoopFuelingViewModel viewModel = new OpenLoopFuelingViewModel();
        viewModel.registerMLHFMOnChange(new Observer<OpenLoopFuelingViewModel.OpenLoopMlfhmModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull OpenLoopFuelingViewModel.OpenLoopMlfhmModel openLoopMlfhmModel) {
                updateModel(openLoopMlfhmModel);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void updateModel(OpenLoopFuelingViewModel.OpenLoopMlfhmModel openLoopMlfhmModel) {
        this.openLoopMlfhmModel = openLoopMlfhmModel;

        if(openLoopMlfhmModel != null && selected) {
            enableLogsTab(openLoopMlfhmModel.isLogsTabEnabled());
            enableCorrectionTab(openLoopMlfhmModel.isCorrectionsTabEnabled());
            setSelectedTab(openLoopMlfhmModel.getSelectedTabIndex());
        } else {
            enableLogsTab(false);
            enableCorrectionTab(false);
            setSelectedTab(0);
        }
    }

    private JTabbedPane getTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        tabbedPane.addTab("MLHFM", null, new MlhfmView().getMlhfmPanel(), "Voltage to Kg/Hr");
        tabbedPane.addTab("ME7 Logs", null, new OpenLoopFuelingLogView().getMe7LogPanel(), "ME7 Logging");
        tabbedPane.addTab("Correction", null, new OpenLoopFuelingCorrectionView().getCorrectionPanel(), "Corrected MLHFM");
        tabbedPane.addTab("Help", null, new OpenLoopMlhfmHelpView().getPanel(), "");

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

    private void setSelectedTab(int index) {
        tabbedPane.setSelectedIndex(index);
    }

}
