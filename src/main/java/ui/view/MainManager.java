package ui.view;

import ui.view.closedloopfueling.kfkhfm.ClosedLoopKfkhfmUiManager;
import ui.view.closedloopfueling.mlhfm.ClosedLoopMlhfmUiManager;
import ui.view.kfmiop.KfmiopUiManager;
import ui.view.kfmirl.KfmirlUiManager;
import ui.view.kfzw.KfzwUiManager;
import ui.view.kfzwop.KfzwopUiManager;
import ui.view.krkte.KrkteUiManager;
import ui.view.ldrpid.LdrpidUiManager;
import ui.view.listener.OnTabSelectedListener;
import ui.view.openloopfueling.OpenLoopFuelingUiManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;

public class MainManager {
    private List<OnTabSelectedListener> tabSelectedListeners = new ArrayList<>();

    public void start() {
        JFrame frame = new JFrame();
        frame.setTitle("ME7 Tuner");
        frame.setSize(1480, 800);
        frame.add(getTabbedPane());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private JTabbedPane getTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();

        KrkteUiManager krkteUiManager = new KrkteUiManager();
        tabSelectedListeners.add(krkteUiManager);
        tabbedPane.addTab("KRKTE (Primary Fueling)", null, krkteUiManager.getPanel(), "KRKTE Calculator");

        ClosedLoopMlhfmUiManager closedLoopMlhfmUiManager = new ClosedLoopMlhfmUiManager();
        // first tab is selected
        closedLoopMlhfmUiManager.onTabSelected(true);
        tabSelectedListeners.add(closedLoopMlhfmUiManager);
        tabbedPane.addTab("Closed Loop MLHFM", null, closedLoopMlhfmUiManager.getPanel(), "Closed Loop MLFHM Compensation");

        ClosedLoopKfkhfmUiManager closedLoopKfkhfmUiManager = new ClosedLoopKfkhfmUiManager();
        tabSelectedListeners.add(closedLoopKfkhfmUiManager);
        tabbedPane.addTab("Closed Loop KFKHFM", null, closedLoopKfkhfmUiManager.getPanel(), "Closed Loop KFKHFM Compensation");

        OpenLoopFuelingUiManager openLoopFuelingUiManager = new OpenLoopFuelingUiManager();
        tabSelectedListeners.add(openLoopFuelingUiManager);
        tabbedPane.addTab("Open Loop Fueling", null, openLoopFuelingUiManager.getPanel(), "Open Loop Fueling Compensation");

        tabbedPane.addTab("KFMIRL", null, new KfmirlUiManager().getPanel(), "KFMIRL Calculator");
        tabbedPane.addTab("KFMIOP", null, new KfmiopUiManager().getPanel(), "KFMIOP Calculator");
        tabbedPane.addTab("KFZWOP", null, new KfzwopUiManager().getPanel(), "KFZWOP Calculator");
        tabbedPane.addTab("KFZW", null, new KfzwUiManager().getPanel(), "KFZW Calculator");
        tabbedPane.addTab("LDRPID", null, new LdrpidUiManager().getPanel(), "LDRPID");

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int selectedIndex = tabbedPane.getSelectedIndex();
                for(int i = 0; i < tabSelectedListeners.size(); i++) {
                    tabSelectedListeners.get(i).onTabSelected(selectedIndex == i);
                }
            }
        });

        return tabbedPane;
    }


}
