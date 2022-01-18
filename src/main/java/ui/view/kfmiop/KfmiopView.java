package ui.view.kfmiop;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfmirl.Kfmirl;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import parser.xdf.TableDefinition;
import preferences.bin.BinFilePreferences;
import preferences.kfmiop.KfmiopPreferences;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.view.color.ColorMapGreenYellowRed;
import ui.view.listener.OnTabSelectedListener;
import ui.view.map.MapPickerDialog;
import ui.viewmodel.kfmiop.KfmiopViewModel;
import writer.BinWriter;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

public class KfmiopView implements OnTabSelectedListener {
    private final MapTable inputKfmiop = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);
    private final MapTable outputKfmiop = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);

    private final MapTable inputBoost = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);
    private final MapTable outputBoost = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);

    private final MapAxis kfmiopXAxis =  MapAxis.getMapAxis(new Double[1][0]);

    private JLabel fileLabel;
    private Chart chart3d;
    private JPanel panel;
    private CalculatedMaximumMapPressurePanel calculatedMaximumMapPressurePanel;
    private final KfmiopViewModel viewModel = new KfmiopViewModel();

    public JPanel getPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.insets.right = 20;

        panel.add(getInputPanel(), c);

        c.gridx = 1;
        c.gridy = 0;
        c.insets.top = -55;

        panel.add(getOutputPanel(), c);

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

        panel.add(getDefinitionButton(), constraints);

        constraints.gridy = 3;
        constraints.insets.top = 0;

        fileLabel = new JLabel("No Map Selected");
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
        constraints.insets.top = 0;

        JScrollPane kfzwXAxisScrollPane = kfmiopXAxis.getScrollPane();
        kfzwXAxisScrollPane.setPreferredSize(new Dimension(615, 20));
        panel.add(kfzwXAxisScrollPane, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addTab("Torque", null, getOutputKfmiopMapPanel(), "Optimal Torque Map");
        tabbedPane.addTab("Boost", null, getOutputBoostMapPanel(), "Optimal Boost Map");

        panel.add(tabbedPane, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets.top = 16;
        panel.add(getFileButton(), constraints);

        return panel;
    }

    private void initViewModel() {

        viewModel.register(new Observer<KfmiopViewModel.KfmiopModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull KfmiopViewModel.KfmiopModel kfmiopModel) {

                if(kfmiopModel.getTableDefinition() != null) {
                    fileLabel.setText(kfmiopModel.getTableDefinition().getTableName());
                }

                calculatedMaximumMapPressurePanel.setFieldText(CalculatedMaximumMapPressurePanel.FieldTitle.MAP_SENSOR_MAX, (int) kfmiopModel.getMaxMapSensorPressure());
                calculatedMaximumMapPressurePanel.setFieldText(CalculatedMaximumMapPressurePanel.FieldTitle.BOOST_PRESSURE_MAX, (int) kfmiopModel.getMaxBoostPressure());

                Map3d inputKfmiop = kfmiopModel.getInputKfmiop();

                if(inputKfmiop != null) {
                    KfmiopView.this.inputKfmiop.setColumnHeaders(inputKfmiop.xAxis);
                    KfmiopView.this.inputKfmiop.setRowHeaders(inputKfmiop.yAxis);
                    KfmiopView.this.inputKfmiop.setTableData(inputKfmiop.zAxis);
                }

                Map3d outputKfmiop = kfmiopModel.getOutputKfmiop();

                if(outputKfmiop != null) {
                    KfmiopView.this.outputKfmiop.setColumnHeaders(outputKfmiop.xAxis);
                    KfmiopView.this.outputKfmiop.setRowHeaders(outputKfmiop.yAxis);
                    KfmiopView.this.outputKfmiop.setTableData(outputKfmiop.zAxis);

                    Double[][] xAxis = new Double[1][];
                    xAxis[0] = outputKfmiop.xAxis;
                    kfmiopXAxis.setTableData(xAxis);
                }

                Map3d inputBoost = kfmiopModel.getInputBoost();

                if(inputBoost != null) {
                    KfmiopView.this.inputBoost.setColumnHeaders(inputBoost.xAxis);
                    KfmiopView.this.inputBoost.setRowHeaders(inputBoost.yAxis);
                    KfmiopView.this.inputBoost.setTableData(inputBoost.zAxis);
                }

                Map3d outputBoost = kfmiopModel.getOutputBoost();

                if(outputBoost != null) {
                    KfmiopView.this.outputBoost.setColumnHeaders(outputBoost.xAxis);
                    KfmiopView.this.outputBoost.setRowHeaders(outputBoost.yAxis);
                    KfmiopView.this.outputBoost.setTableData(outputBoost.zAxis);
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }


    private JPanel getInputKfmioplMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 52;

        panel.add(getHeader("KFMIOP (Input)", e -> showChart3d()),c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.left = 0;
        c.insets.top = 8;

        JScrollPane scrollPane = inputKfmiop.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(655, 275));

        panel.add(scrollPane ,c);

        return panel;
    }

    private JPanel getInputBoostMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 52;

        panel.add(getHeader("Boost (Input)", e -> showChart3d()),c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.left = 0;
        c.insets.top = 8;

        JScrollPane scrollPane = inputBoost.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(655, 275));

        panel.add(scrollPane ,c);

        return panel;
    }

    private JPanel getOutputKfmiopMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 52;

        panel.add(getHeader("KFMIOP (Output)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showChart3d();
            }
        }),c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.left = 0;
        c.insets.top = 8;

        JScrollPane scrollPane =  outputKfmiop.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(655, 275));

        panel.add(scrollPane ,c);

        return panel;
    }

    private JPanel getOutputBoostMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 52;

        panel.add(getHeader("Boost (Output)", e -> showChart3d()),c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.left = 0;
        c.insets.top = 8;

        JScrollPane scrollPane = outputBoost.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(655, 275));

        panel.add(scrollPane ,c);

        return panel;
    }

    private JPanel getHeader(String title, ActionListener chartActionListener) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;

        JLabel label = new JLabel(title);
        panel.add(label,c);

        c.gridx = 1;

        java.net.URL imgURL = getClass().getResource("/insert_chart.png");
        ImageIcon icon = new ImageIcon(imgURL, "");
        JButton button = new JButton(icon);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.addActionListener(chartActionListener);
        panel.add(button, c);

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
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfmiopPreferences.getSelectedMap().fst, outputKfmiop.getMap3d());
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

    private void showChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getChart3d());
        jd.setVisible(true);
    }

    private JPanel getChart3d() {
        initChart3d();
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;

        panel.add((Component)chart3d.getCanvas(), c);

        return panel;
    }

    private void initChart3d() {
        // Create a chart and add scatterAfr
        chart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        chart3d.getAxeLayout().setMainColor(Color.BLACK);
        chart3d.getView().setBackgroundColor(Color.WHITE);
        chart3d.getAxeLayout().setXAxeLabel("Torque Request (mifa)");
        chart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        chart3d.getAxeLayout().setZAxeLabel("Specified Load (rlsol)");

        Double[][] data = inputKfmiop.getData();

        Double[] xAxis = Kfmirl.getStockKfmirlXAxis();
        Double[] yAxis = Kfmirl.getStockKfmirlYAxis();

        ArrayList<org.jzy3d.plot3d.primitives.Polygon> polygons = new ArrayList<>();
        for(int i = 0; i < xAxis.length -1; i++){
            for(int j = 0; j < yAxis.length -1; j++){
                org.jzy3d.plot3d.primitives.Polygon polygon = new org.jzy3d.plot3d.primitives.Polygon();
                polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(xAxis[i], yAxis[j], data[j][i])));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i], yAxis[j + 1], data[j + 1][i]) ));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i + 1], yAxis[j + 1], data[j+1][i+1]) ));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i + 1], yAxis[j], data[j][i+1])));
                polygons.add(polygon);
            }
        }

        // Create the object to represent the function over the given range.
        final org.jzy3d.plot3d.primitives.Shape surface = new org.jzy3d.plot3d.primitives.Shape(polygons);
        surface.setColorMapper(new ColorMapper(new ColorMapGreenYellowRed(), surface.getBounds().getZmin(), surface.getBounds().getZmax()));
        surface.setFaceDisplayed(true);
        surface.setWireframeColor(Color.BLACK);
        surface.setWireframeDisplayed(true);

        chart3d.getScene().add(surface, true);
    }

    private JButton getDefinitionButton() {
        JButton button = new JButton("Set KFMIOP Definition");

        button.addActionListener(e -> {
            Pair<TableDefinition, Map3d> tableDefinition = KfmiopPreferences.getSelectedMap();

            if(tableDefinition != null) {
                MapPickerDialog.showDialog(panel, panel, "Select KFMIOP", "Map Selection", tableDefinition.fst, KfmiopPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select KFMIOP", "Map Selection", null, KfmiopPreferences::setSelectedMap);
            }
        });

        return button;
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
