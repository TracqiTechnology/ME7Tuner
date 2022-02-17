package ui.view.kfvpdksd;

import preferences.kfvpdksd.KfvpdksdPreferences;
import preferences.primaryfueling.PrimaryFuelingPreferences;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

class WastegateCrackingPressurePanel extends JPanel {
    private static final Insets WEST_INSETS = new Insets(5, 0, 5, 5);
    private static final Insets EAST_INSETS = new Insets(5, 5, 5, 0);

    enum FieldTitle {
        WASTEGATE_CRACKING_PRESSURE("Wastegate Cracking Pressure", "PSI", true);

        private String title;
        private String units;
        private boolean editable;

        FieldTitle(String title, String units, boolean editable) {
            this.title = title;
            this.units = units;
            this.editable = editable;
        }

        public String getTitle() {
            return title;
        }

        public String getUnits() {
            return units;
        }

        public boolean isEditable() {
            return editable;
        }
    }

    private Map<FieldTitle, JFormattedTextField> fieldMap = new HashMap<>();
    private OnValueChangedListener listener;

    public interface OnValueChangedListener {
        void onValueChanged(FieldTitle fieldTitle);
    }

    WastegateCrackingPressurePanel(OnValueChangedListener listener) {
        this.listener = listener;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Configure KFVPDKSD"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        GridBagConstraints gbc;

        NumberFormat integerFormat = NumberFormat.getIntegerInstance();
        integerFormat.setGroupingUsed(false);

        NumberFormatter integerFormatter = new NumberFormatter(integerFormat);
        integerFormatter.setValueClass(Integer.class);
        integerFormatter.setAllowsInvalid(false);
        integerFormatter.setMinimum(0);
        integerFormatter.setAllowsInvalid(true);

        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        decimalFormat.setGroupingUsed(false);

        NumberFormatter decimalFormatter = new NumberFormatter(decimalFormat);
        decimalFormatter.setOverwriteMode(true);
        decimalFormatter.setAllowsInvalid(true);
        decimalFormatter.setMinimum(0d);
        decimalFormatter.setAllowsInvalid(true);

        for (int i = 0; i < FieldTitle.values().length; i++) {
            FieldTitle fieldTitle = FieldTitle.values()[i];
            gbc = createGbc(0, i);
            add(new JLabel(fieldTitle.getTitle() + ":", JLabel.LEFT), gbc);
            gbc = createGbc(2, i);
            add(new JLabel(fieldTitle.getUnits(), JLabel.LEFT), gbc);
            gbc = createGbc(1, i);

            switch (fieldTitle) {
                case WASTEGATE_CRACKING_PRESSURE:
                    addWastegateCrackingPressure(fieldTitle, gbc, decimalFormatter);
                    break;
            }
        }
    }

    private void addWastegateCrackingPressure(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(KfvpdksdPreferences.getMaxWasteGateCrackingPressurePreference());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { }

            public void removeUpdate(DocumentEvent e) {
                try {
                    KfvpdksdPreferences.setMaxWasteGateCrackingPressurePreference(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {
                }
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    KfvpdksdPreferences.setMaxWasteGateCrackingPressurePreference(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {
                }
            }
        });

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
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

    public String getFieldText(FieldTitle fieldTitle) {
        return fieldMap.get(fieldTitle).getText();
    }
}
