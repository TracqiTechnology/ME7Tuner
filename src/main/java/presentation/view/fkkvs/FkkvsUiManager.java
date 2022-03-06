package presentation.view.fkkvs;

import domain.model.fkkvs.Fkkvs;
import presentation.map.map.MapTable;

import javax.swing.*;
import java.awt.*;

public class FkkvsUiManager {

    private MapTable fkkvs;

    public JPanel getFkkvsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        panel.add(getKfmirlMapPanel(), c);

        return panel;
    }

    private JPanel getKfmirlMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        fkkvs = MapTable.getMapTable(Fkkvs.getStockYAxis(), Fkkvs.getStockXAxis(), Fkkvs.getStockMap());

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("FKKVS"),c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.top = 16;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollPane = fkkvs.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(930, 295));

        panel.add(scrollPane ,c);

        return panel;
    }
}
