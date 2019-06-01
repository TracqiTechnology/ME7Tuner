package ui.view.closedloopfueling.mlhfm;

import ui.view.closedloopfueling.ClosedLoopMe7LogUiManager;
import ui.view.fkfhfm.KfkhfmUiManager;
import ui.view.mlhfm.MlhfmUiManager;

import javax.swing.*;
import java.awt.*;

public class ClosedLoopMlhfmUiManager {

    public JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(getTabbedPane(), BorderLayout.CENTER);

        return panel;
    }

    private JTabbedPane getTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        tabbedPane.addTab("MLHFM", null, new MlhfmUiManager().getMlhfmPanel(), "Voltage to Kg/Hr");
        tabbedPane.addTab("ME7 Logs", null, new ClosedLoopMe7LogUiManager().getMe7LogPanel(), "ME7 Logging");
        tabbedPane.addTab("Correction", null, new ClosedLoopMlhfmCorrectionUiManager().getCorrectionPanel(), "Corrected MLHFM");

        return tabbedPane;
    }
}
