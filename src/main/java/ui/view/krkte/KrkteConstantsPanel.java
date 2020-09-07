package ui.view.krkte;

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

class KrkteConstantsPanel extends JPanel {
    private static final Insets WEST_INSETS = new Insets(5, 0, 5, 5);
    private static final Insets EAST_INSETS = new Insets(5, 5, 5, 0);

    enum FieldTitle {
        AIR_DENSITY("Air Density (0C and 1013hPa)", "g/dm^3", false), ENGINE_DISPLACEMENT("Engine Displacement", "dm^3", true), NUM_ENGINE_CYLINDERS("Number of Cylinders", "", true),
        ENGINE_DISPLACEMENT_PER_CYLINDER("Cylinder Displacement", "dm^3", false), GASOLINE_GRAMS_PER_CUBIC_CENTIMETER("Gasoline Grams per Cubic Centimeter", "g/cc^3", true),
        STOICHIOMETRIC_AIR_FUEL_RATIO("Stoichiometric A/F Ratio", "", true), FUEL_INJECTOR_CC_MIN("Fuel Injector cc/min", "cc/min", true);

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

    KrkteConstantsPanel(OnValueChangedListener listener) {
        this.listener = listener;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Configure KRKTE Constants"),
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
                case AIR_DENSITY:
                    addAirDensity(fieldTitle, gbc, decimalFormatter);
                    break;
                case ENGINE_DISPLACEMENT:
                    addEngineDisplacment(fieldTitle, gbc, decimalFormatter);
                    break;
                case NUM_ENGINE_CYLINDERS:
                    addEngineCylinders(fieldTitle, gbc, integerFormatter);
                    break;
                case ENGINE_DISPLACEMENT_PER_CYLINDER:
                    addCylinderDisplacment(fieldTitle, gbc, decimalFormatter);
                    break;
                case GASOLINE_GRAMS_PER_CUBIC_CENTIMETER:
                    addGasolineGramsPerCubicCentimeter(fieldTitle, gbc, decimalFormatter);
                    break;
                case STOICHIOMETRIC_AIR_FUEL_RATIO:
                    addStoichiometricAirFuelRatio(fieldTitle, gbc, decimalFormatter);
                    break;
                case FUEL_INJECTOR_CC_MIN:
                    addFuelInjectorCubicCentimeters(fieldTitle, gbc, integerFormatter);
                    break;
            }
        }
    }

    private void addAirDensity(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(PrimaryFuelingPreferences.getAirDensityGramsPerCubicDecimeterPreference());

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
    }

    private void addEngineDisplacment(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(PrimaryFuelingPreferences.getEngineDisplacementCubicDecimeterPreference());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setEngineDisplacementCubicDecimeterPreference(Double.parseDouble(textField.getText()));
                    fieldMap.get(FieldTitle.ENGINE_DISPLACEMENT_PER_CYLINDER).setValue(PrimaryFuelingPreferences.getEngineDisplacementCubicDecimeterPreference() / (double) PrimaryFuelingPreferences.getNumEngineCylindersPreference());
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setEngineDisplacementCubicDecimeterPreference(Double.parseDouble(textField.getText()));
                    fieldMap.get(FieldTitle.ENGINE_DISPLACEMENT_PER_CYLINDER).setValue(PrimaryFuelingPreferences.getEngineDisplacementCubicDecimeterPreference() / (double) PrimaryFuelingPreferences.getNumEngineCylindersPreference());
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }
        });

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
    }

    private void addEngineCylinders(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter integerFormat) {
        final JFormattedTextField textField = new JFormattedTextField(integerFormat);
        textField.setValue(PrimaryFuelingPreferences.getNumEngineCylindersPreference());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setNumEngineCylindersPreference(Integer.parseInt(textField.getText()));
                    fieldMap.get(FieldTitle.ENGINE_DISPLACEMENT_PER_CYLINDER).setValue(PrimaryFuelingPreferences.getEngineDisplacementCubicDecimeterPreference() / (double) PrimaryFuelingPreferences.getNumEngineCylindersPreference());
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setNumEngineCylindersPreference(Integer.parseInt(textField.getText()));
                    fieldMap.get(FieldTitle.ENGINE_DISPLACEMENT_PER_CYLINDER).setValue(PrimaryFuelingPreferences.getEngineDisplacementCubicDecimeterPreference() / (double) PrimaryFuelingPreferences.getNumEngineCylindersPreference());
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }
        });

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
    }

    private void addCylinderDisplacment(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(PrimaryFuelingPreferences.getEngineDisplacementCubicDecimeterPreference() / (double) PrimaryFuelingPreferences.getNumEngineCylindersPreference());

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
    }

    private void addGasolineGramsPerCubicCentimeter(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(PrimaryFuelingPreferences.getGasolineGramsPerCubicCentimeterPreference());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setGasolineGramsPerCubicCentimeterPreference(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setGasolineGramsPerCubicCentimeterPreference(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }
        });

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
    }

    private void addStoichiometricAirFuelRatio(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter decimalFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(decimalFormatter);
        textField.setValue(PrimaryFuelingPreferences.getStoichiometricAirFuelRatioPreference());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setStoichiometricAirFuelRatioPreference(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setStoichiometricAirFuelRatioPreference(Double.parseDouble(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }
        });

        textField.setEditable(fieldTitle.isEditable());
        textField.setColumns(5);
        add(textField, gbc);

        fieldMap.put(fieldTitle, textField);
    }

    private void addFuelInjectorCubicCentimeters(FieldTitle fieldTitle, GridBagConstraints gbc, NumberFormatter integerFormatter) {
        final JFormattedTextField textField = new JFormattedTextField(integerFormatter);
        textField.setValue(PrimaryFuelingPreferences.getFuelInjectorSizePreference());

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {}

            public void removeUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setFuelInjectorSizePreference(Integer.parseInt(textField.getText()));
                    listener.onValueChanged(fieldTitle);
                } catch (NumberFormatException exception) {};
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    PrimaryFuelingPreferences.setFuelInjectorSizePreference(Integer.parseInt(textField.getText()));
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
