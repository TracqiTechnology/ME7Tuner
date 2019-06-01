package ui.view.fkfhfm;

import math.map.Map3d;
import model.kfkhfm.Kfkhfm;
import ui.map.map.MapTable;

import javax.swing.*;
import java.awt.*;

public class KfkhfmUiManager {

    private MapTable kfkhfm;

    public JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        panel.add(getMapPanel(), c);

        return panel;
    }

    public void setMap(Map3d map) {
        kfkhfm.setMap(map);
    }

    private JPanel getMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        kfkhfm = MapTable.getMapTable(Kfkhfm.getStockYAxis(), Kfkhfm.getStockXAxis(), Kfkhfm.getStockMap());

        c.weightx = 1;
        c.gridx = 0;

        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("KFKHFM"),c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.top = 16;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollPane = kfkhfm.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(930, 295));

        panel.add(scrollPane ,c);

        return panel;
    }
}
