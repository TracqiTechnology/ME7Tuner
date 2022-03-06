package presentation.view.kfmiop;

import preferences.kfmiop.KfmiopPreferences;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

class DesiredMaximumMapPressurePanel extends JPanel {
    private static final Insets WEST_INSETS = new Insets(5, 0, 5, 5);
    private static final Insets EAST_INSETS = new Insets(5, 5, 5, 0);

    enum FieldTitle {
        MAP_SENSOR_MAX("MAP Sensor Maximum",  "", "mbar (absolute)", true), BOOST_PRESSURE_MAX("Boost Pressure Maximum",  "", "mbar (absolute)", true);

        private final String title;
        private final String units;
        private final String hint;
        private final boolean editable;

        FieldTitle(String title, String hint, String units, boolean editable) {
            this.title = title;
            this.hint = hint;
            this.units = units;
            this.editable = editable;
        }

        public String getTitle() {
            return title;
        }

        public String getHint() {
            return hint;
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

    DesiredMaximumMapPressurePanel(OnValueChangedListener listener) {
        this.listener = listener;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Desired Maximum Boost"),
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
                case MAP_SENSOR_MAX:
                    addMaxDesiredMap(fieldTitle, gbc, integerFormatter);
                    break;
                case BOOST_PRESSURE_MAX:
                    addMaxDesiredBoost(fieldTitle, gbc, integerFormatter);
                    break;
            }
        }
    }

    private void addMaxDesiredMap(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(KfmiopPreferences.getInstance().getMaxMapPressurePreference());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    KfmiopPreferences.getInstance().setMaxMapPressurePreference(Integer.parseInt(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {}
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    KfmiopPreferences.getInstance().setMaxMapPressurePreference(Integer.parseInt(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {}
            }
        });

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
    }

    private void addMaxDesiredBoost(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(KfmiopPreferences.getInstance().getMaxBoostPressurePreference());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    KfmiopPreferences.getInstance().setMaxBoostPressurePreference(Integer.parseInt(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {}
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    KfmiopPreferences.getInstance().setMaxBoostPressurePreference(Integer.parseInt(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {}
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
