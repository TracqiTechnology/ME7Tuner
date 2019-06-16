package ui.view;

import ui.view.closedloopfueling.kfkhfm.ClosedLoopKfkhfmUiManager;
import ui.view.closedloopfueling.mlhfm.ClosedLoopMlhfmUiManager;
import ui.view.kfmiop.KfmiopUiManager;
import ui.view.kfmirl.KfmirlUiManager;
import ui.view.kfzw.KfzwUiManager;
import ui.view.kfzwop.KfzwopUiManager;
import ui.view.krkte.KrkteUiManager;
import ui.view.openloopfueling.OpenLoopFuelingUiManager;

import javax.swing.*;

public class MainManager {
    public void start() {
        JFrame frame = new JFrame();
        frame.setTitle("MAF Scaler");
        frame.setSize(1580, 800);
        frame.add(getTabbedPane());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private JTabbedPane getTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Closed Loop MLHFM", null, new ClosedLoopMlhfmUiManager().getPanel(), "Closed Loop MLFHM Compensation");
        tabbedPane.addTab("Closed Loop KFKHFM", null, new ClosedLoopKfkhfmUiManager().getPanel(), "Closed Loop KFKHFM Compensation");
        tabbedPane.addTab("Open Loop Fueling", null, new OpenLoopFuelingUiManager().getPanel(), "Open Loop Fueling Compensation");
        tabbedPane.addTab("KRKTE", null, new KrkteUiManager().getPanel(), "KRKTE Calculator");
        tabbedPane.addTab("KFMIRL", null, new KfmirlUiManager().getPanel(), "KFMIRL Calculator");
        tabbedPane.addTab("KFMIOP", null, new KfmiopUiManager().getPanel(), "KFMIOP Calculator");
        tabbedPane.addTab("KFZWOP", null, new KfzwopUiManager().getPanel(), "KFZWOP Calculator");
        tabbedPane.addTab("KFZW", null, new KfzwUiManager().getPanel(), "KFZW Calculator");

        return tabbedPane;
    }


}
