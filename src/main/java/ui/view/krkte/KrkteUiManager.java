package ui.view.krkte;

import model.krkte.KrkteCalculator;
import preferences.primaryfueling.PrimaryFuelingPreferences;
import ui.view.listener.OnTabSelectedListener;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;


public class KrkteUiManager implements OnTabSelectedListener {

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

        panel.add(getResultsPanel(), c);

        double krkte = calculateKrkte();
        outputTextField.setText(decimalFormat.format(krkte));

        c.gridy = 2;
        c.insets = new Insets(16, 0, 0, 0);
        panel.add(getHelpPanel(), c);

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

    private JEditorPane getHelpPanel() {
        JEditorPane jep = new JEditorPane();
        jep.setContentType("text/html");//set content as html
        jep.setText("<a href='https://github.com/KalebKE/ME7Tuner#krkte-primary-fueling'>Primary Fueling KRKTE User Guide</a>.");
        jep.setOpaque(false);

        jep.setEditable(false);//so its not editable

        jep.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(hle.getURL().toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        return jep;
    }

    private double calculateKrkte() {
        return KrkteCalculator.calculateKrkte(PrimaryFuelingPreferences.getAirDensityGramsPerCubicDecimeterPreference(),
                PrimaryFuelingPreferences.getEngineDisplacementCubicDecimeterPreference()/(double)PrimaryFuelingPreferences.getNumEngineCylindersPreference(),
                PrimaryFuelingPreferences.getFuelInjectorSizePreference(),
                PrimaryFuelingPreferences.getGasolineGramsPerCubicCentimeterPreference(),
                PrimaryFuelingPreferences.getStoichiometricAirFuelRatioPreference());
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
