package presentation.view.kfvpdksd;

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
import preferences.bin.BinFilePreferences;
import preferences.kfvpdksd.KfvpdksdPreferences;
import presentation.map.map.MapTable;
import presentation.view.listener.OnTabSelectedListener;
import presentation.viewmodel.kfvpdksd.KfvpdksdViewModel;
import writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class KfvpdksdView implements OnTabSelectedListener {

    private JFreeChart chart;

    private final MapTable kfvpdksdTable = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);
    private final MapTable boostTable = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);

    private JPanel panel;

    private final KfvpdksdViewModel viewModel;

    private JLabel definitionFileLabel;
    private JLabel logFileLabel;

    private boolean kfvpdksdInitialized;
    private boolean pressureInitialized;

    private final JProgressBar dpb = new JProgressBar();

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
        constraints.insets.top = 20;
        constraints.insets.left = 16;

        panel.add(getOutputPanel(), constraints);

        initViewModel();

        return panel;
    }

    private void initViewModel() {
        viewModel.register(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull KfvpdksdViewModel.KfvpdksdModel kfvpdksdModel) {
                if (!kfvpdksdInitialized) {
                    if (kfvpdksdModel.getKfvpdksdTable() != null) {
                        definitionFileLabel.setText(kfvpdksdModel.getKfvpdksdTable().getFirst().getTableName());

                        boostTable.setRowHeaders(new Double[]{0.0});
                        boostTable.setColumnHeaders(kfvpdksdModel.getKfvpdksdTable().getSecond().yAxis);
                        boostTable.setTableData(new Double[1][kfvpdksdModel.getKfvpdksdTable().getSecond().yAxis.length]);

                        kfvpdksdTable.setColumnHeaders(kfvpdksdModel.getKfvpdksdTable().getSecond().xAxis);
                        kfvpdksdTable.setRowHeaders(kfvpdksdModel.getKfvpdksdTable().getSecond().yAxis);
                        kfvpdksdTable.setTableData(kfvpdksdModel.getKfvpdksdTable().getSecond().zAxis);

                        drawPressure(kfvpdksdModel.getKfvpdksdTable().getSecond());

                        kfvpdksdInitialized = true;
                    }
                }

                if (!pressureInitialized) {
                    if (kfvpdksdModel.getKfvpdksd() != null && kfvpdksdModel.getPressure() != null) {

                        Map3d kfvpdks = kfvpdksdModel.getKfvpdksdTable().getSecond();

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
                    kfvpdksdTable.setColumnHeaders(kfvpdksdModel.getKfvpdksdTable().getSecond().xAxis);
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

        XYPlot plot = (XYPlot) chart.getPlot();
        ((XYSeriesCollection) plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection) plot.getDataset()).addSeries(boostSeries);
    }

    private JPanel getInputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.top = 16;

        panel.add(new JLabel("Maximum Boost (Input)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        panel.add(boostTable.getScrollPane(), constraints);

        initBoostChart();

        constraints.gridx = 0;
        constraints.gridy = 2;

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(710, 275));

        panel.add(chartPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        final JButton logsButton = getLogFileButton();
        panel.add(logsButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;

        dpb.setIndeterminate(false);
        dpb.setVisible(false);
        panel.add(dpb, constraints);

        constraints.gridx = 0;
        constraints.gridy = 5;

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
        constraints.insets.top = 16;

        panel.add(getHeader("KFVPDKSD (Output)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        panel.add(kfvpdksdTable.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        definitionFileLabel = new JLabel("No Map Selected");
        panel.add(definitionFileLabel, constraints);

        constraints.gridy = 3;

        panel.add(getWriteFileButton(), constraints);

        return panel;
    }

    private void initBoostChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        chart = ChartFactory.createScatterPlot(
                "Maximum Boost",
                "RPM", "Boost (PSI)", dataset);

        chart.getTitle().setPaint(Color.decode("#F8F8F2"));
        chart.setBackgroundPaint(Color.decode("#383c4a"));
        chart.getLegend().setBackgroundPaint(Color.decode("#383c4a"));
        chart.getLegend().setItemPaint(Color.decode("#F8F8F2"));

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.decode("#383c4a"));
        plot.setDomainGridlinePaint(Color.decode("#F8F8F2"));
        plot.setRangeGridlinePaint(Color.decode("#F8F8F2"));
        plot.getDomainAxis().setLabelPaint(Color.decode("#F8F8F2"));
        plot.getDomainAxis().setTickLabelPaint(Color.decode("#F8F8F2"));
        plot.getRangeAxis().setLabelPaint(Color.decode("#F8F8F2"));
        plot.getRangeAxis().setTickLabelPaint(Color.decode("#F8F8F2"));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);

        plot.getRenderer().setSeriesPaint(0, Color.decode("#f57900"));
    }

    private JPanel getHeader(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;

        JLabel label = new JLabel(title);
        panel.add(label, c);

        return panel;
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
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfvpdksdPreferences.getInstance().getSelectedMap().getFirst(), kfvpdksdTable.getMap3d());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
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
            fc.setCurrentDirectory(KfvpdksdPreferences.getInstance().getDirectory());

            int returnValue = fc.showOpenDialog(panel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File me7LogFile = fc.getSelectedFile();
                loadMe7File(me7LogFile);
                KfvpdksdPreferences.getInstance().setDirectory(me7LogFile.getParentFile());
            }
        });

        return button;
    }

    private void loadMe7File(File file) {
        SwingUtilities.invokeLater(() -> {
            viewModel.loadLogs(file, (value, max) -> {
                SwingUtilities.invokeLater(() -> {
                    dpb.setMaximum(max);
                    dpb.setValue(value);
                    dpb.setVisible(value < max - 1);
                });
            });

            logFileLabel.setText(file.getPath());
        });
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
