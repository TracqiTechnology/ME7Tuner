package ui.view;

import ui.view.closedloopfueling.ClosedLoopFuelingUiManager;
import ui.view.kfmiop.KfmiopUiManager;
import ui.view.kfmirl.KfmirlUiManager;
import ui.view.kfzwop.KfzwopUiManager;
import ui.view.krkte.KrkteUiManager;
import ui.view.openloopfueling.OpenLoopFuelingUiManager;

import javax.swing.*;

public class MainManager {
    public void start() {
        JFrame frame = new JFrame();
        frame.setTitle("MAF Scaler");
        frame.setSize(1480, 800);
        frame.add(getTabbedPane());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private JTabbedPane getTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Closed Loop Fueling", null, new ClosedLoopFuelingUiManager().getClosedLoopPanel(), "Closed Loop Fueling Compensation");
        tabbedPane.addTab("Open Loop Fueling", null, new OpenLoopFuelingUiManager().getOpenLoopPanel(), "Open Loop Fueling Compensation");
        tabbedPane.addTab("KRKTE", null, new KrkteUiManager().getKrkteCalculationPanel(), "KRKTE Calculator");
        tabbedPane.addTab("KFMIRL", null, new KfmirlUiManager().getKfmirlCalculationPanel(), "KFMIRL Calculator");
        tabbedPane.addTab("KFMIOP", null, new KfmiopUiManager().getKfmiopCalculationPanel(), "KFMIOP Calculator");
        tabbedPane.addTab("KFZWOP", null, new KfzwopUiManager().getKfzwopCalculationPanel(), "KFZWOP Calculator");

        return tabbedPane;
    }


}
