package ui.view;

import ui.view.closedloopfueling.kfkhfm.ClosedLoopKfkhfmUiManager;
import ui.view.closedloopfueling.mlhfm.ClosedLoopMlhfmUiManager;
import ui.view.kfmiop.KfmiopUiManager;
import ui.view.kfmirl.KfmirlUiManager;
import ui.view.kfurl.KfurlUiManager;
import ui.view.kfmsnwdk.KfmsnwdkUiManager;
import ui.view.kfzw.KfzwUiManager;
import ui.view.kfzwop.KfzwopUiManager;
import ui.view.krkte.KrkteUiManager;
import ui.view.ldrpid.LdrpidUiManager;
import ui.view.listener.OnTabSelectedListener;
import ui.view.openloopfueling.OpenLoopFuelingUiManager;
import ui.view.wdkugdn.WdkugdnUiManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;

public class MainManager {
    private final List<OnTabSelectedListener> tabSelectedListeners = new ArrayList<>();

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
        tabbedPane.addTab("KRKTE", null, new JScrollPane(krkteUiManager.getPanel()), "KRKTE Calculator");

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

        KfmirlUiManager kfmirlUiManager = new KfmirlUiManager();
        tabSelectedListeners.add(kfmirlUiManager);
        tabbedPane.addTab("KFMIRL", null, new JScrollPane(kfmirlUiManager.getPanel()), "KFMIRL Calculator");

        KfmiopUiManager kfmiopUiManager = new KfmiopUiManager();
        tabSelectedListeners.add(kfmiopUiManager);
        tabbedPane.addTab("KFMIOP", null, new JScrollPane(kfmiopUiManager.getPanel()), "KFMIOP Calculator");

        KfzwopUiManager kfzwopUiManager = new KfzwopUiManager();
        tabSelectedListeners.add(kfzwopUiManager);
        tabbedPane.addTab("KFZWOP", null, new JScrollPane(kfzwopUiManager.getPanel()), "KFZWOP Calculator");

        KfzwUiManager kfzwUiManager = new KfzwUiManager();
        tabSelectedListeners.add(kfzwUiManager);
        tabbedPane.addTab("KFZW", null,  new JScrollPane(kfzwUiManager.getPanel()), "KFZW Calculator");

        KfurlUiManager kfurlUiManager = new KfurlUiManager();
        tabSelectedListeners.add(kfurlUiManager);
        tabbedPane.addTab("KFURL", null, new JScrollPane(kfurlUiManager.getPanel()), "KFURL");

        WdkugdnUiManager wdkugdnUiManager = new WdkugdnUiManager();
        tabSelectedListeners.add(wdkugdnUiManager);
        tabbedPane.addTab("WDKUGDN", null, wdkugdnUiManager.getPanel(), "KFURL");

        KfmsnwdkUiManager kfmsnwdkUiManager = new KfmsnwdkUiManager();
        tabSelectedListeners.add(kfmsnwdkUiManager);
        tabbedPane.addTab("KFWDKMSN", null, new JScrollPane(kfmsnwdkUiManager.getPanel()), "KFWDKMSN");

        LdrpidUiManager ldrpidUiManager = new LdrpidUiManager();
        tabbedPane.addTab("LDRPID", null, new JScrollPane(ldrpidUiManager.getPanel()), "LDRPID");

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int selectedIndex = tabbedPane.getSelectedIndex();
                for (int i = 0; i < tabSelectedListeners.size(); i++) {
                    tabSelectedListeners.get(i).onTabSelected(selectedIndex == i);
                }
            }
        });

        return tabbedPane;
    }


}
