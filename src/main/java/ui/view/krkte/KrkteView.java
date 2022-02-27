package ui.view.krkte;

import com.sun.tools.javac.util.Pair;
import math.map.Map3d;
import model.krkte.KrkteCalculator;
import parser.xdf.TableDefinition;
import preferences.bin.BinFilePreferences;
import preferences.krkte.KrktePreferences;
import preferences.primaryfueling.PrimaryFuelingPreferences;
import ui.view.listener.OnTabSelectedListener;
import writer.BinWriter;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;

public class KrkteView implements OnTabSelectedListener {

    private JFormattedTextField outputTextField;
    private DecimalFormat decimalFormat;
    private JPanel panel;

    public JPanel getPanel() {
        decimalFormat = new DecimalFormat("#.####");

        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        KrkteConstantsPanel krkteConstantsPanel = new KrkteConstantsPanel(fieldTitle -> {
            double krkte = calculateKrkte();
            outputTextField.setText(decimalFormat.format(krkte));
        });

        panel.add(krkteConstantsPanel, c);

        c.gridx = 0;
        c.gridy = 1;

        panel.add(getResultsPanel(), c);

        double krkte = calculateKrkte();
        outputTextField.setText(decimalFormat.format(krkte));

        c.gridy = 2;
        panel.add(getWriteFileButton(), c);

        c.gridy = 3;
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

        jep.addHyperlinkListener(hle -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(hle.getURL().toURI());
                } catch (Exception ex) {
                    ex.printStackTrace();
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

    private JButton getWriteFileButton() {
        JButton button = new JButton("Write KRKTE");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KRKTE to the binary?",
                    "Write KRKTE",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                Pair<TableDefinition, Map3d> krkteTable = KrktePreferences.getInstance().getSelectedMap();

                if(krkteTable != null) {
                    TableDefinition krkteTableDefinition = krkteTable.fst;

                    if (krkteTableDefinition != null) {
                        try {
                            Map3d krkte = new Map3d();
                            krkte.zAxis = new Double[1][1];
                            krkte.zAxis[0][0] = Double.parseDouble(outputTextField.getText());
                            BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), krkteTableDefinition, krkte);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
            }
        });

        return button;
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
