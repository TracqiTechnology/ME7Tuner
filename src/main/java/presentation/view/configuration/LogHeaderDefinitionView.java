package presentation.view.configuration;

import data.contract.Me7LogFileContract;
import data.preferences.logheaderdefinition.LogHeaderPreference;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class LogHeaderDefinitionView {
    private static final Insets WEST_INSETS = new Insets(5, 0, 5, 5);
    private static final Insets EAST_INSETS = new Insets(5, 5, 5, 0);

    private final JPanel panel = new JPanel();

    public JPanel getPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;

        panel.add(this.panel, constraints);

        return panel;
    }

    LogHeaderDefinitionView() {
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Log Headers"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        GridBagConstraints gbc;

        for (int i = 0; i < Me7LogFileContract.Header.values().length; i++) {
            Me7LogFileContract.Header header = Me7LogFileContract.Header.values()[i];
            gbc = createGbc(0, i);
            panel.add(new JLabel(header.getTitle() + ":", JLabel.LEFT), gbc);
            gbc = createGbc(1, i);
            
            addField(gbc, new LogHeaderPreference(header, header.getHeader()));
        }
    }

    private void addField(GridBagConstraints gbc, LogHeaderPreference preference) {
        final JFormattedTextField textField = new JFormattedTextField();
        textField.setValue(preference.getHeader());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                preference.setHeader(textField.getText());
            }

            public void insertUpdate(DocumentEvent e) {
                preference.setHeader(textField.getText());
            }
        });

        textField.setColumns(10);
        panel.add(textField, gbc);
    }

    private GridBagConstraints createGbc(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
        gbc.fill = (x == 0) ? GridBagConstraints.BOTH
                : GridBagConstraints.HORIZONTAL;

        gbc.insets = (x == 0) ? WEST_INSETS : EAST_INSETS;
        gbc.weightx = (x == 0) ? 0.1 : 1.0;
        gbc.weighty = 1.0;
        return gbc;
    }
}
