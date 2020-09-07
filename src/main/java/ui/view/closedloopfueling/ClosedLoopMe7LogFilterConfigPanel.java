package ui.view.closedloopfueling;

import preferences.closedloopfueling.ClosedLoopFuelingLogFilterPreferences;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class ClosedLoopMe7LogFilterConfigPanel extends JPanel {
    private static final Insets WEST_INSETS = new Insets(5, 0, 5, 5);
    private static final Insets EAST_INSETS = new Insets(5, 5, 5, 0);

    public enum FieldTitle {
        MIN_THROTTLE_ANGLE("Minimum Throttle Angle", "Filter out samples with a throttle angle less than the minimum"), MIN_RPM("Minimum RPM", "Filter out samples with an RPM less than the minimum"), MAX_VOLTAGE_DT("Max dMAFv/dt", "Filter out samples with a derivative greater than the maximum. The best samples have the smallest derivative");
        private String title;
        private String hint;

        FieldTitle(String title, String hint) {
            this.title = title;
            this.hint = hint;
        }

        public String getTitle() {
            return title;
        }

        public String getHint() { return hint; }
    }

    private Map<FieldTitle, JTextField> fieldMap = new HashMap<FieldTitle, JTextField>();

    public ClosedLoopMe7LogFilterConfigPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Configure Closed Loop Filter"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        GridBagConstraints gbc;

        NumberFormat integerFormat = NumberFormat.getIntegerInstance();
        integerFormat.setGroupingUsed(false);

        NumberFormatter integerFormatter = new NumberFormatter(integerFormat);
        integerFormatter.setValueClass(Integer.class);
        integerFormatter.setAllowsInvalid(false);
        integerFormatter.setMinimum(0);
        integerFormatter.setAllowsInvalid(true);

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setGroupingUsed(false);
        NumberFormatter decimalFormatter = new NumberFormatter(decimalFormat);
        decimalFormatter.setOverwriteMode(true);
        decimalFormatter.setAllowsInvalid(true);
        decimalFormatter.setMinimum(0d);
        decimalFormatter.setAllowsInvalid(true);

        for (int i = 0; i < FieldTitle.values().length; i++) {
            FieldTitle fieldTitle = FieldTitle.values()[i];
            gbc = createGbc(0, i);
            JLabel jLabel = new JLabel(fieldTitle.getTitle() + ":", JLabel.LEFT);
            jLabel.setToolTipText(fieldTitle.getHint());
            add(jLabel, gbc);
            gbc = createGbc(1, i);

            JFormattedTextField textField = null;

            switch (fieldTitle) {
                case MIN_THROTTLE_ANGLE:
                    textField = new JFormattedTextField(integerFormatter);
                    textField.setValue(ClosedLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference());
                    break;
                case MIN_RPM:
                    textField = new JFormattedTextField(integerFormatter);
                    textField.setValue(ClosedLoopFuelingLogFilterPreferences.getMinRpmPreference());
                    break;
                case MAX_VOLTAGE_DT:
                    textField = new JFormattedTextField(decimalFormatter);
                    textField.setValue(ClosedLoopFuelingLogFilterPreferences.getMaxVoltageDtPreference());
                    break;
            }

            textField.setColumns(5);
            add(textField, gbc);

            fieldMap.put(fieldTitle, textField);
        }
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
