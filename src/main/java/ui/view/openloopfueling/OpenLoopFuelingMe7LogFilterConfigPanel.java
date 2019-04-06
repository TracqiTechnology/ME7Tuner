package ui.view.openloopfueling;

import preferences.openloopfueling.OpenLoopFuelingLogFilterPreferences;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class OpenLoopFuelingMe7LogFilterConfigPanel extends JPanel {
    private static final Insets WEST_INSETS = new Insets(5, 0, 5, 5);
    private static final Insets EAST_INSETS = new Insets(5, 5, 5, 0);

    enum FieldTitle {
        MIN_THROTTLE_ANGLE("Minimum Throttle Angle"), MIN_RPM("Minimum RPM"), MIN_ME7_POINTS("Minimum ME7 Points"), MIN_AFR_POINTS("Minimum AFR Points"), MAX_AFR("Maximum AFR");
        private String title;

        FieldTitle(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private Map<FieldTitle, JTextField> fieldMap = new HashMap<FieldTitle, JTextField>();

    OpenLoopFuelingMe7LogFilterConfigPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Configure Open Loop Filter"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        GridBagConstraints gbc;

        NumberFormat integerFormat = NumberFormat.getIntegerInstance();
        integerFormat.setGroupingUsed(false);

        NumberFormatter integerFormatter = new NumberFormatter(integerFormat);
        integerFormatter.setValueClass(Integer.class);
        integerFormatter.setAllowsInvalid(false);
        integerFormatter.setMinimum(0);

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        NumberFormatter decimalFormatter = new NumberFormatter(decimalFormat);
        decimalFormatter.setOverwriteMode(true);
        decimalFormatter.setAllowsInvalid(true);
        decimalFormatter.setMinimum(0d);

        for (int i = 0; i < FieldTitle.values().length; i++) {
            FieldTitle fieldTitle = FieldTitle.values()[i];
            gbc = createGbc(0, i);
            add(new JLabel(fieldTitle.getTitle() + ":", JLabel.LEFT), gbc);
            gbc = createGbc(1, i);

            JFormattedTextField textField = null;

            switch (fieldTitle) {
                case MIN_THROTTLE_ANGLE:
                    textField = new JFormattedTextField(integerFormatter);
                    textField.setValue(OpenLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference());
                    break;
                case MIN_RPM:
                    textField = new JFormattedTextField(integerFormatter);
                    textField.setValue(OpenLoopFuelingLogFilterPreferences.getMinRpmPreference());
                    break;
                case MIN_ME7_POINTS:
                    textField = new JFormattedTextField(decimalFormatter);
                    textField.setValue(OpenLoopFuelingLogFilterPreferences.getMinMe7PointsPreference());
                    break;
                case MIN_AFR_POINTS:
                    textField = new JFormattedTextField(integerFormatter);
                    textField.setValue(OpenLoopFuelingLogFilterPreferences.getMinAfrPointsPreference());
                    break;
                case MAX_AFR:
                    textField = new JFormattedTextField(decimalFormat);
                    textField.setValue(OpenLoopFuelingLogFilterPreferences.getMaxAfrPreference());
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
