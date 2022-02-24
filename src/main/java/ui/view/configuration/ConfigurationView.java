package ui.view.configuration;

import javax.swing.*;
import java.awt.*;

public class ConfigurationView {

    public JPanel getPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.NORTH;
        constraints.weighty = 1;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.right = 16;

        panel.add(new TableDefinitionView().getPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        panel.add(new LogHeaderDefinitionView().getPanel(), constraints);

        return panel;
    }
}
