package presentation.view.kfmiop;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import domain.math.map.Map3d;
import data.preferences.bin.BinFilePreferences;
import data.preferences.kfmiop.KfmiopPreferences;
import presentation.map.axis.MapAxis;
import presentation.map.map.MapTable;
import presentation.view.listener.OnTabSelectedListener;
import presentation.viewmodel.kfmiop.KfmiopViewModel;
import data.writer.BinWriter;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;

public class KfmiopView implements OnTabSelectedListener {
    private final MapTable inputKfmiop = MapTable.getMapTable(new Double[16], new Double[11], new Double[16][11]);
    private final MapTable outputKfmiop = MapTable.getMapTable(new Double[16], new Double[11], new Double[16][11]);

    private final MapTable inputBoost = MapTable.getMapTable(new Double[16], new Double[11], new Double[16][11]);
    private final MapTable outputBoost = MapTable.getMapTable(new Double[16], new Double[11], new Double[16][11]);

    private final MapAxis kfmiopXAxis =  MapAxis.getMapAxis(new Double[1][11]);

    private JLabel fileLabel;
    private JPanel panel;
    private CalculatedMaximumMapPressurePanel calculatedMaximumMapPressurePanel;
    private final KfmiopViewModel viewModel = new KfmiopViewModel();

    public JPanel getPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.right = 16;
        constraints.insets.top = 80;

        panel.add(getInputPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets.top = 0;

        panel.add(getOutputPanel(), constraints);

        initViewModel();

        return panel;
    }

    public JPanel getInputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        calculatedMaximumMapPressurePanel = getCalculatedMaxPressurePanel();

        panel.add(calculatedMaximumMapPressurePanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addTab("Torque", null, getInputKfmioplMapPanel(), "Optimal Torque Map");
        tabbedPane.addTab("Boost", null, getInputBoostMapPanel(), "Optimal Boost Map");

        panel.add(tabbedPane, constraints);

        constraints.gridy = 2;

        fileLabel = new JLabel("No Definition Selected");
        panel.add(fileLabel, constraints);

        return panel;
    }

    public JPanel getOutputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getDesiredMaxMapPressurePanel(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(new JLabel("KFMIOP X-Axis (Output)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(kfmiopXAxis.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addTab("Torque", null, getOutputKfmiopMapPanel(), "Optimal Torque Map");
        tabbedPane.addTab("Boost", null, getOutputBoostMapPanel(), "Optimal Boost Map");

        panel.add(tabbedPane, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets.top = 0;
        panel.add(getFileButton(), constraints);

        return panel;
    }

    private void initViewModel() {

        viewModel.register(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull KfmiopViewModel.KfmiopModel kfmiopModel) {

                if (kfmiopModel.getTableDefinition() != null) {
                    fileLabel.setText(kfmiopModel.getTableDefinition().getTableName());
                }

                calculatedMaximumMapPressurePanel.setFieldText(CalculatedMaximumMapPressurePanel.FieldTitle.MAP_SENSOR_MAX, (int) kfmiopModel.getMaxMapSensorPressure());
                calculatedMaximumMapPressurePanel.setFieldText(CalculatedMaximumMapPressurePanel.FieldTitle.BOOST_PRESSURE_MAX, (int) kfmiopModel.getMaxBoostPressure());

                Map3d inputKfmiop = kfmiopModel.getInputKfmiop();

                if (inputKfmiop != null) {
                    KfmiopView.this.inputKfmiop.setColumnHeaders(inputKfmiop.xAxis);
                    KfmiopView.this.inputKfmiop.setRowHeaders(inputKfmiop.yAxis);
                    KfmiopView.this.inputKfmiop.setTableData(inputKfmiop.zAxis);
                }

                Map3d outputKfmiop = kfmiopModel.getOutputKfmiop();

                if (outputKfmiop != null) {
                    KfmiopView.this.outputKfmiop.setColumnHeaders(outputKfmiop.xAxis);
                    KfmiopView.this.outputKfmiop.setRowHeaders(outputKfmiop.yAxis);
                    KfmiopView.this.outputKfmiop.setTableData(outputKfmiop.zAxis);

                    Double[][] xAxis = new Double[1][];
                    xAxis[0] = outputKfmiop.xAxis;
                    kfmiopXAxis.setTableData(xAxis);
                }

                Map3d inputBoost = kfmiopModel.getInputBoost();

                if (inputBoost != null) {
                    KfmiopView.this.inputBoost.setColumnHeaders(inputBoost.xAxis);
                    KfmiopView.this.inputBoost.setRowHeaders(inputBoost.yAxis);
                    KfmiopView.this.inputBoost.setTableData(inputBoost.zAxis);
                }

                Map3d outputBoost = kfmiopModel.getOutputBoost();

                if (outputBoost != null) {
                    KfmiopView.this.outputBoost.setColumnHeaders(outputBoost.xAxis);
                    KfmiopView.this.outputBoost.setRowHeaders(outputBoost.yAxis);
                    KfmiopView.this.outputBoost.setTableData(outputBoost.zAxis);
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }


    private JPanel getInputKfmioplMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("KFMIOP (Input)"),constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(inputKfmiop.getScrollPane() ,constraints);

        return panel;
    }

    private JPanel getInputBoostMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("Boost (Input)"),constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(inputBoost.getScrollPane(),constraints);

        return panel;
    }

    private JPanel getOutputKfmiopMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("KFMIOP (Output)"),constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(outputKfmiop.getScrollPane() ,constraints);

        return panel;
    }

    private JPanel getOutputBoostMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("Boost (Output)"),constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(outputBoost.getScrollPane() ,constraints);

        return panel;
    }

    private JPanel getHeader(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;

        JLabel label = new JLabel(title);
        panel.add(label,c);

        return panel;
    }

    private JPanel getDesiredMaxMapPressurePanel() {
        return new DesiredMaximumMapPressurePanel(fieldTitle -> viewModel.calculateKfmiop());
    }

    private CalculatedMaximumMapPressurePanel getCalculatedMaxPressurePanel() {
        return new CalculatedMaximumMapPressurePanel();
    }

    private JButton getFileButton() {
        JButton button = new JButton("Write KFMIOP");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFMIOP to the binary?",
                    "Write KFMIOP",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfmiopPreferences.getInstance().getSelectedMap().getFirst(), outputKfmiop.getMap3d());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        return button;
    }

    private JEditorPane getHelpPanel() {
        JEditorPane jep = new JEditorPane();
        jep.setContentType("text/html");//set content as html
        jep.setText("<a href='https://github.com/KalebKE/ME7Tuner#kfmirl-torque-request-to-loadfill-request'>Load Request KFMIRL User Guide</a>.");
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

    @Override
    public void onTabSelected(boolean selected) {

    }
}
