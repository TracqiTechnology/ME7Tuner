package ui.view.closedloopfueling.mlhfm;

import derivative.Derivative;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map2d;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import preferences.closedloopfueling.ClosedLoopFuelingLogFilterPreferences;
import preferences.filechooser.FileChooserPreferences;
import ui.view.closedloopfueling.ClosedLoopMe7LogFilterConfigPanel;
import ui.viewmodel.MlhfmViewModel;
import ui.viewmodel.closedloopfueling.ClosedLoopFuelingMe7LogViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.List;
import java.util.Map;

public class ClosedLoopMlhfmMe7LogUiManager {

    private JFreeChart chart;
    private JPanel closedLoopLogPanel;
    private JLabel fileLabel;
    private ClosedLoopFuelingMe7LogViewModel closedLoopViewModel;

    private Map2d mlhfmMap;
    private File me7LogFile;

    ClosedLoopMlhfmMe7LogUiManager() {
        closedLoopViewModel = ClosedLoopFuelingMe7LogViewModel.getInstance();
        closedLoopViewModel.getPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> me7LogMap) {
                if (mlhfmMap != null) {
                    drawChart(me7LogMap, mlhfmMap);
                }
            }

            @Override
            public void onSubscribe(Disposable disposable) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        MlhfmViewModel mlhfmViewModel = MlhfmViewModel.getInstance();
        mlhfmViewModel.getMlhfmPublishSubject().subscribe(new Observer<Map2d>() {
            @Override
            public void onNext(Map2d mlhfmMap) {
                ClosedLoopMlhfmMe7LogUiManager.this.mlhfmMap = mlhfmMap;
            }

            @Override
            public void onSubscribe(Disposable disposable) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    JPanel getMe7LogPanel() {
        initChart();
        closedLoopLogPanel = new JPanel();
        closedLoopLogPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1.0;
        c.gridheight = 1;

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.95;
        closedLoopLogPanel.add(getChartPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.05;
        closedLoopLogPanel.add(getActionPanel(), c);

        return closedLoopLogPanel;
    }

    private JPanel getChartPanel() {
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

        panel.add(new ChartPanel(chart), c);

        return panel;
    }

    private JPanel getActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridwidth = 2;

        c.weightx = 0.1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 8, 0, 0);

        JButton button = getConfigureFilterButton();
        panel.add(button, c);

        c.weightx = 0.9;
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 0);

        button = getFileButton();
        panel.add(button, c);

        c.gridx = 1;
        c.gridy = 1;

        fileLabel = new JLabel("No File Selected");
        panel.add(fileLabel, c);

        return panel;
    }

    private JButton getConfigureFilterButton() {
        JButton button = new JButton("Configure Filter");

        button.addActionListener(e -> {
            ClosedLoopMe7LogFilterConfigPanel filterConfigPane = new ClosedLoopMe7LogFilterConfigPanel();

            int result = JOptionPane.showConfirmDialog(closedLoopLogPanel, filterConfigPane,
                    "", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                for (ClosedLoopMe7LogFilterConfigPanel.FieldTitle fieldTitle : ClosedLoopMe7LogFilterConfigPanel.FieldTitle.values()) {
                    switch (fieldTitle) {
                        case MIN_THROTTLE_ANGLE:
                            ClosedLoopFuelingLogFilterPreferences.setMinThrottleAnglePreference(Double.valueOf(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                        case MIN_RPM:
                            ClosedLoopFuelingLogFilterPreferences.setMinRpmPreference(Double.valueOf(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                        case MAX_VOLTAGE_DT:
                            ClosedLoopFuelingLogFilterPreferences.setMaxVoltageDtPreference(Double.valueOf(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                    }

                    if (this.me7LogFile != null) {
                        loadMe7File(this.me7LogFile);
                    }
                }
            }
        });

        return button;
    }

    private JButton getFileButton() {
        JButton button = new JButton("Load Logs");
        button.setToolTipText("Load Closed Loop ME7 Logs");

        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setCurrentDirectory(FileChooserPreferences.getDirectory());

            int returnValue = fc.showOpenDialog(closedLoopLogPanel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                this.me7LogFile = fc.getSelectedFile();
                loadMe7File(this.me7LogFile);
                FileChooserPreferences.setDirectory(this.me7LogFile.getParentFile());
            }
        });

        return button;
    }

    private void loadMe7File(File file) {
        closedLoopViewModel.loadDirectory(file);
        fileLabel.setText(file.getName());
    }

    private void initChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        chart = ChartFactory.createScatterPlot(
                "Derivative",
                "MAF Voltage", "dMAFv/dt", dataset);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        plot.setRenderer(renderer);

        plot.getRenderer().setSeriesShape(0, new Ellipse2D.Double(0, 0, 1, 1));
        plot.getRenderer().setSeriesPaint(0, Color.BLUE);
    }

    private void drawChart(Map<String, List<Double>> me7LogMap, Map2d mlhfmMap) {

        Map<Double, List<Double>> dtMap = Derivative.getDtMap2d(me7LogMap, mlhfmMap);
        Double[] voltages = mlhfmMap.axis;

        XYSeries series = new XYSeries("Derivative");

        for (Double voltage : voltages) {
            List<Double> values = dtMap.get(voltage);

            for (Double value : values) {
                series.add(voltage, value);
            }
        }

        XYPlot plot = (XYPlot) chart.getPlot();
        ((XYSeriesCollection) plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection) plot.getDataset()).addSeries(series);
    }


}
