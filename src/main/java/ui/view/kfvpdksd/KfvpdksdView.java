package ui.view.kfvpdksd;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import parser.xdf.TableDefinition;
import preferences.bin.BinFilePreferences;
import preferences.filechooser.FileChooserPreferences;
import preferences.kfmiop.KfmiopPreferences;
import preferences.kfvpdksd.KfvpdksdPreferences;
import ui.map.map.MapTable;
import ui.view.color.ColorMapGreenYellowRed;
import ui.view.listener.OnTabSelectedListener;
import ui.view.map.MapPickerDialog;
import ui.viewmodel.kfvpdksd.KfvpdksdViewModel;
import writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class KfvpdksdView implements OnTabSelectedListener {

    private JFreeChart boostChart;

    private final MapTable kfvpdksdTable = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);
    private final MapTable boostTable = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);

    private Chart outputChart3d;

    private JPanel panel;

    private final KfvpdksdViewModel viewModel;

    private JLabel definitionFileLabel;
    private JLabel logFileLabel;

    private boolean kfvpdksdInitialized;
    private boolean pressureInitialized;

    public KfvpdksdView() {
        viewModel = new KfvpdksdViewModel();
    }

    public JPanel getPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getInputPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets.top = 50;
        constraints.insets.left = 16;

        panel.add(getOutputPanel(), constraints);

        initViewModel();

        return panel;
    }

    private void initViewModel() {
        viewModel.register(new Observer<KfvpdksdViewModel.KfvpdksdModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull KfvpdksdViewModel.KfvpdksdModel kfvpdksdModel) {
                if (!kfvpdksdInitialized) {
                    if (kfvpdksdModel.getKfvpdksdTable() != null) {
                        definitionFileLabel.setText(kfvpdksdModel.getKfvpdksdTable().fst.getTableName());

                        boostTable.setRowHeaders(new Double[]{0.0});
                        boostTable.setColumnHeaders(kfvpdksdModel.getKfvpdksdTable().snd.yAxis);
                        boostTable.setTableData(new Double[1][kfvpdksdModel.getKfvpdksdTable().snd.yAxis.length]);

                        kfvpdksdTable.setColumnHeaders(kfvpdksdModel.getKfvpdksdTable().snd.xAxis);
                        kfvpdksdTable.setRowHeaders(kfvpdksdModel.getKfvpdksdTable().snd.yAxis);
                        kfvpdksdTable.setTableData(kfvpdksdModel.getKfvpdksdTable().snd.zAxis);

                        drawPressure(kfvpdksdModel.getKfvpdksdTable().snd);

                        kfvpdksdInitialized = true;
                    }
                }

                if (!pressureInitialized) {
                    if (kfvpdksdModel.getKfvpdksd() != null && kfvpdksdModel.getPressure() != null) {

                        Map3d kfvpdks = kfvpdksdModel.getKfvpdksdTable().snd;

                        Double[][] data = new Double[1][];
                        data[0] = kfvpdksdModel.getPressure();

                        for (int i = 0; i < data[0].length; i++) {
                            data[0][i] *= 0.0145038;
                        }

                        boostTable.setTableData(data);

                        kfvpdks.zAxis = data;

                        drawPressure(kfvpdks);

                        pressureInitialized = true;
                    }
                }

                if (kfvpdksdModel.getKfvpdksd() != null) {
                    kfvpdksdTable.setTableData(kfvpdksdModel.getKfvpdksd().getKfvpdksd());
                    kfvpdksdTable.setColumnHeaders(kfvpdksdModel.getKfvpdksdTable().snd.xAxis);
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

    public void drawPressure(Map3d pressure) {
        Double[] yAxis = pressure.yAxis;
        Double[][] zAxis = pressure.zAxis;

        XYSeries boostSeries = new XYSeries("Boost (PSI)");

        for (int i = 0; i < yAxis.length; i++) {
            boostSeries.add(yAxis[i], zAxis[0][i]);
        }

        XYPlot plot = (XYPlot) boostChart.getPlot();
        ((XYSeriesCollection) plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection) plot.getDataset()).addSeries(boostSeries);
    }

    private JPanel getInputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(new JLabel("Maximum Boost (Input)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        JScrollPane xAxisScrollPane = boostTable.getScrollPane();
        xAxisScrollPane.setPreferredSize(new Dimension(710, 40));

        panel.add(xAxisScrollPane, constraints);

        initBoostChart();

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets.left = 0;
        constraints.insets.top = 8;

        ChartPanel chartPanel = new ChartPanel(boostChart);
        chartPanel.setPreferredSize(new Dimension(710, 275));

        panel.add(chartPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        final JButton logsButton = getLogFileButton();
        panel.add(logsButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;

        logFileLabel = new JLabel("No File Selected");
        panel.add(logFileLabel, constraints);

        return panel;
    }

    private JPanel getOutputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("KFVPDKSD (Output)", e -> showOutChart3d()), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        JScrollPane scrollPane = kfvpdksdTable.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(710, 215));

        panel.add(scrollPane, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets.top = 8;

        panel.add(getWriteFileButton(), constraints);

        constraints.gridy = 3;

        panel.add(getDefinitionButton(), constraints);

        constraints.gridy = 4;
        constraints.insets.top = 0;

        definitionFileLabel = new JLabel("No Map Selected");
        panel.add(definitionFileLabel, constraints);

        return panel;
    }

    private void initBoostChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        boostChart = ChartFactory.createScatterPlot(
                "Maximum Boost",
                "RPM", "Boost (PSI)", dataset);

        XYPlot plot = (XYPlot) boostChart.getPlot();
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setDomainGridlinePaint(java.awt.Color.BLACK);
        plot.setRangeGridlinePaint(java.awt.Color.BLACK);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);

        plot.getRenderer().setSeriesPaint(0, java.awt.Color.RED);
    }

    private JPanel getHeader(String title, ActionListener chartActionListener) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;

        JLabel label = new JLabel(title);
        panel.add(label, c);

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

    private void showOutChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500, 500);
        jd.setLocationRelativeTo(null);
        jd.add(getOutChart3d());
        jd.setVisible(true);
    }

    private JPanel getOutChart3d() {
        initOutputChart3d();
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

        panel.add((Component) outputChart3d.getCanvas(), c);

        return panel;
    }

    private void initOutputChart3d() {
        // Create a chart and add scatterAfr
        outputChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        outputChart3d.getAxeLayout().setMainColor(Color.BLACK);
        outputChart3d.getView().setBackgroundColor(Color.WHITE);
        outputChart3d.getAxeLayout().setXAxeLabel("Engine Load");
        outputChart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        outputChart3d.getAxeLayout().setZAxeLabel("Ignition Advance");

        NewtCameraMouseController controller = new NewtCameraMouseController(outputChart3d);

        Double[][] data = kfvpdksdTable.getData();

        Double[] xAxis = (Double[]) kfvpdksdTable.getColumnHeaders();
        Double[] yAxis = kfvpdksdTable.getRowHeaders();

        ArrayList<Polygon> polygons = new ArrayList<>();
        for (int i = 0; i < xAxis.length - 1; i++) {
            for (int j = 0; j < yAxis.length - 1; j++) {
                Polygon polygon = new Polygon();
                polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(xAxis[i], yAxis[j], data[j][i])));
                polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(xAxis[i], yAxis[j + 1], data[j + 1][i])));
                polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(xAxis[i + 1], yAxis[j + 1], data[j + 1][i + 1])));
                polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(xAxis[i + 1], yAxis[j], data[j][i + 1])));
                polygons.add(polygon);
            }
        }

        // Create the object to represent the function over the given range.
        final org.jzy3d.plot3d.primitives.Shape surface = new org.jzy3d.plot3d.primitives.Shape(polygons);
        surface.setColorMapper(new ColorMapper(new ColorMapGreenYellowRed(), surface.getBounds().getZmin(), surface.getBounds().getZmax()));
        surface.setFaceDisplayed(true);
        surface.setWireframeColor(Color.BLACK);
        surface.setWireframeDisplayed(true);

        outputChart3d.getScene().add(surface, true);
    }

    private JButton getWriteFileButton() {
        JButton button = new JButton("Write KFVPDKSD");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFVPDKSD to the binary?",
                    "Write KFVPDKSD",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfvpdksdPreferences.getSelectedMap().fst, kfvpdksdTable.getMap3d());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        return button;
    }

    private JButton getDefinitionButton() {
        JButton button = new JButton("Set KFVPDKSD Definition");

        button.addActionListener(e -> {
            Pair<TableDefinition, Map3d> tableDefinition = KfmiopPreferences.getSelectedMap();

            if (tableDefinition != null) {
                MapPickerDialog.showDialog(panel, panel, "Select KFVPDKSD", "Map Selection", tableDefinition.fst, KfvpdksdPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select KFVPDKSD", "Map Selection", null, KfvpdksdPreferences::setSelectedMap);
            }
        });

        return button;
    }

    private JButton getLogFileButton() {
        JButton button = new JButton("Load Logs");
        button.setToolTipText("Load Boost ME7 Logs");

        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setCurrentDirectory(FileChooserPreferences.getDirectory());

            int returnValue = fc.showOpenDialog(panel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File me7LogFile = fc.getSelectedFile();
                loadMe7File(me7LogFile);
                FileChooserPreferences.setDirectory(me7LogFile.getParentFile());
            }
        });

        return button;
    }

    private void loadMe7File(File file) {
        viewModel.loadLogs(file);
        logFileLabel.setText(file.getName());
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
