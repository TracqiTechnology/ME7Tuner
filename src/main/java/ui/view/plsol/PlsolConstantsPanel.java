package ui.view.plsol;

import preferences.plsol.PlsolPreferences;
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

class PlsolConstantsPanel extends JPanel {
    private static final Insets WEST_INSETS = new Insets(5, 0, 5, 0);
    private static final Insets EAST_INSETS = new Insets(5, 5, 5, 0);

    enum FieldTitle {
        BAROMETRIC_PRESSURE("Barometric Pressure", "mbar", true), INTAKE_AIR_TEMPERATURE("Intake Air Temperature", "C°", true), KFURL("KFURL", "%", true);

        private final String title;
        private final String units;
        private final boolean editable;

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

    private final Map<FieldTitle, JFormattedTextField> fieldMap = new HashMap<>();
    private final OnValueChangedListener listener;

    public interface OnValueChangedListener {
        void onValueChanged(FieldTitle fieldTitle);
    }

    PlsolConstantsPanel(OnValueChangedListener listener) {
        this.listener = listener;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Configure PLSOL Constants"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

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
            GridBagConstraints gbc = createGbc(i + i, 0);
            add(new JLabel(fieldTitle.getTitle() + ":", JLabel.LEFT), gbc);
            gbc = createGbc(i + i + 1, 1);
            add(new JLabel(fieldTitle.getUnits(), JLabel.LEFT), gbc);
            gbc = createGbc(i + i, 1);

            switch (fieldTitle) {
                case BAROMETRIC_PRESSURE:
                    addBarometricPressure(fieldTitle, gbc, decimalFormatter);
                    break;
                case INTAKE_AIR_TEMPERATURE:
                    addIntakeAirTemperature(fieldTitle, gbc, decimalFormatter);
                    break;
                case KFURL:
                    addKfurl(fieldTitle, gbc, decimalFormatter);
                    break;
            }
        }
    }

    private void addBarometricPressure(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(PlsolPreferences.getBarometricPressure());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    PlsolPreferences.setBarometricPressure(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    PlsolPreferences.setBarometricPressure(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }
        });

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
    }

    private void addIntakeAirTemperature(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(PlsolPreferences.getIntakeAirTemperature());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    PlsolPreferences.setIntakeAirTemperature(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    PlsolPreferences.setIntakeAirTemperature(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }
        });

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
    }

    private void addKfurl(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(PlsolPreferences.getKfurl());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    PlsolPreferences.setKfurl(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    PlsolPreferences.setKfurl(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
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
