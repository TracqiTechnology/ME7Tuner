package ui.view.krkte;

import model.krkte.KrkteCalculator;
import preferences.primaryfueling.PrimaryFuelingPreferences;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;


public class KrkteUiManager {

    private KrkteConstantsPanel krkteConstantsPanel;
    private JFormattedTextField outputTextField;
    private DecimalFormat decimalFormat;

    public JPanel getPanel() {
        decimalFormat = new DecimalFormat("#.####");

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        krkteConstantsPanel = new KrkteConstantsPanel(new KrkteConstantsPanel.OnValueChangedListener() {
            @Override
            public void onValueChanged(KrkteConstantsPanel.FieldTitle fieldTitle) {
                double krkte = calculateKrkte();
                outputTextField.setText(decimalFormat.format(krkte));
            }
        });

        panel.add(krkteConstantsPanel, c);

        c.gridx = 0;
        c.gridy = 1;

        panel.add( getResultsPanel(), c);

        double krkte = calculateKrkte();
        outputTextField.setText(decimalFormat.format(krkte));

        return panel;
    }

    private JPanel getResultsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        JLabel label = new JLabel("KRKTE: ");

        c.gridx = 0;
        c.gridy = 0;

        panel.add(label, c);

        NumberFormatter decimalFormatter = new NumberFormatter(decimalFormat);
        decimalFormatter.setOverwriteMode(true);
        decimalFormatter.setAllowsInvalid(true);
        decimalFormatter.setMinimum(0d);

        outputTextField = new JFormattedTextField(decimalFormat);
        outputTextField.setEditable(false);
        outputTextField.setColumns(6);

        c.gridx = 1;
        c.gridy = 0;

        panel.add(outputTextField, c);

        c.gridx = 2;

        JLabel krkteLabel = new JLabel("ms/%");
        krkteLabel.setFont(krkteLabel.getFont().deriveFont(krkteLabel.getFont().getStyle() & ~Font.BOLD));
        panel.add(krkteLabel, c);

        return  panel;
    }

    private double calculateKrkte() {
        return KrkteCalculator.calculateKrkte(PrimaryFuelingPreferences.getAirDensityGramsPerCubicDecimeterPreference(),
                PrimaryFuelingPreferences.getEngineDisplacementCubicDecimeterPreference()/(double)PrimaryFuelingPreferences.getNumEngineCylindersPreference(),
                PrimaryFuelingPreferences.getFuelInjectorSizePreference(),
                PrimaryFuelingPreferences.getGasolineGramsPerCubicCentimeterPreference(),
                PrimaryFuelingPreferences.getStoichiometricAirFuelRatioPreference());
    }
}
