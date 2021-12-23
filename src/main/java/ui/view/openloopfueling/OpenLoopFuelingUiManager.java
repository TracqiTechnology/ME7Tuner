package ui.view.openloopfueling;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map2d;
import ui.view.listener.OnTabSelectedListener;
import ui.view.mlhfm.MlhfmView;
import ui.viewmodel.mlmhfm.MlhfmViewModel;
import ui.viewmodel.openloopfueling.OpenLoopFuelingAfrLogViewModel;
import ui.viewmodel.openloopfueling.OpenLoopFuelingMe7LogViewModel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class OpenLoopFuelingUiManager implements OnTabSelectedListener {

    private JTabbedPane tabbedPane;
    private boolean selected;
    private boolean me7LogsLoaded;
    private boolean afrLogsLoaded;

    public OpenLoopFuelingUiManager() {
        this.selected = false;
        this.me7LogsLoaded = false;
        this.afrLogsLoaded = false;

//        MlhfmViewModel.getInstance().getMlhfmPublishSubject().subscribe(new Observer<Map2d>() {
//            @Override
//            public void onNext(Map2d mlhfmMap) {
//                enableLogsTab(selected && mlhfmMap != null);
//            }
//
//            @Override
//            public void onSubscribe(Disposable disposable) {
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//            }
//
//            @Override
//            public void onComplete() {
//            }
//        });

        OpenLoopFuelingAfrLogViewModel.getInstance().getPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onNext(Map<String, List<Double>> afrLogMap) {
                afrLogsLoaded = afrLogMap != null;
                enableCorrectionTab(me7LogsLoaded && afrLogsLoaded && selected && afrLogMap != null);
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        OpenLoopFuelingMe7LogViewModel.getInstance().getPublishSubject().subscribe(new Observer<Map<String, java.util.List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> me7LogMap) {
                me7LogsLoaded = me7LogMap != null;
                enableCorrectionTab(me7LogsLoaded && afrLogsLoaded && selected && me7LogMap != null);
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
        tabbedPane.addTab("MLHFM", null, new MlhfmView().getMlhfmPanel(), "Voltage to Kg/Hr");
        tabbedPane.addTab("ME7 Logs", null, new OpenLoopFuelingMe7LogUiManager().getMe7LogPanel(), "ME7 Logging");
        tabbedPane.addTab("Correction", null, new OpenLoopFuelingCorrectionUiManager().getCorrectionPanel(), "Corrected MLHFM");
        tabbedPane.addTab("Help", null, new OpenLoopMlhfmHelpManager().getPanel(), "");

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
