package ui.view.openloopfueling;

import contract.AfrLogFileContract;
import contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import model.airflow.AirflowEstimation;
import model.openloopfueling.util.AfrLogUtil;
import model.openloopfueling.util.Me7LogUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import preferences.filechooser.AfrFileChooserPreferences;
import preferences.filechooser.OpenLoopFileChooserPreferences;
import preferences.openloopfueling.OpenLoopFuelingLogFilterPreferences;
import ui.viewmodel.openloopfueling.OpenLoopFuelingLogViewModel;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenLoopFuelingLogView {

    private static final int ME7_FUELING_DATA_SERIES_INDEX = 0;
    private static final int AFR_FUELING_AIRFLOW_DATA_SERIES_INDEX = 1;

    private static final int MEASURED_AIRFLOW_DATA_SERIES_INDEX = 0;
    private static final int ESTIMATED_AIRFLOW_DATA_SERIES_INDEX = 1;

    private JFreeChart fuelingChart;
    private JFreeChart airflowChart;
    private JPanel openLoopLogPanel;
    private JLabel me7FileLabel;
    private JLabel afrFileLabel;
    private final OpenLoopFuelingLogViewModel viewModel;

    private final XYSeriesCollection me7FuelingDataset;
    private final XYSeriesCollection afrFuelingDataset;

    private final XYSeriesCollection measuredAirflowDataset;
    private final XYSeriesCollection estimatedAirflowDataset;

    private File me7LogFile;
    private File afrLogFile;

    public OpenLoopFuelingLogView() {

        me7FuelingDataset = new XYSeriesCollection();
        afrFuelingDataset = new XYSeriesCollection();

        measuredAirflowDataset = new XYSeriesCollection();
        estimatedAirflowDataset = new XYSeriesCollection();

        viewModel = new OpenLoopFuelingLogViewModel();
        viewModel.register(new Observer<OpenLoopFuelingLogViewModel.OpenLoopFuelingLogModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull OpenLoopFuelingLogViewModel.OpenLoopFuelingLogModel openLoopFuelingLogModel) {
                drawMe7FuelingLogChart(openLoopFuelingLogModel.getMe7Logs());
                drawAfrFuelingLogChart(openLoopFuelingLogModel.getAfrLogs());
                AirflowEstimation airflowEstimation = openLoopFuelingLogModel.getAirflowEstimation();
                if(airflowEstimation != null) {
                    drawMeasuredAirflowChart(airflowEstimation.measuredAirflowGramsPerSecondLogs, airflowEstimation.measuredRpmLogs);
                    drawEstimatedAirflowChart(airflowEstimation.estimatedAirflowGramsPerSecondLogs, airflowEstimation.measuredRpmLogs);
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private JTabbedPane getTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addTab("Fueling", null, getFuelingChartPanel(), "Fueling");
        tabbedPane.addTab("Airflow", null, getAirflowChartPanel(), "Airflow");

        return tabbedPane;
    }

    public JPanel getMe7LogPanel() {
        openLoopLogPanel = new JPanel();
        openLoopLogPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1.0;
        c.gridheight = 1;

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.95;
        openLoopLogPanel.add(getTabbedPane(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.05;
        openLoopLogPanel.add(getActionPanel(), c);

        return openLoopLogPanel;
    }

    private JPanel getAirflowChartPanel() {
        initAirflowChart();

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

        panel.add(new ChartPanel(airflowChart), c);

        return panel;
    }

    private JPanel getFuelingChartPanel() {
        initFuelingChart();

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

        panel.add(new ChartPanel(fuelingChart), c);

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

        panel.add(getFilePanel(), c);

        return panel;
    }

    private JPanel getFilePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 32);

        JButton button = getMe7FileButton();
        panel.add(button, c);

        c.gridx = 0;
        c.gridy = 1;

        me7FileLabel = new JLabel("No File Selected");
        panel.add(me7FileLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 32, 0, 0);

        button = getAfrFileButton();
        panel.add(button, c);

        c.gridx = 1;
        c.gridy = 1;

        afrFileLabel = new JLabel("No File Selected");
        panel.add(afrFileLabel, c);

        return panel;
    }


    private JButton getConfigureFilterButton() {
        JButton button = new JButton("Configure Filter");

        button.addActionListener(e -> {
            OpenLoopFuelingMe7LogFilterConfigPanel filterConfigPane = new OpenLoopFuelingMe7LogFilterConfigPanel();

            int result = JOptionPane.showConfirmDialog(openLoopLogPanel, filterConfigPane, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                for (OpenLoopFuelingMe7LogFilterConfigPanel.FieldTitle fieldTitle : OpenLoopFuelingMe7LogFilterConfigPanel.FieldTitle.values()) {
                    switch (fieldTitle) {
                        case MIN_THROTTLE_ANGLE:
                           OpenLoopFuelingLogFilterPreferences.setMinThrottleAnglePreference(Double.parseDouble(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                        case MIN_RPM:
                            OpenLoopFuelingLogFilterPreferences.setMinRpmPreference(Double.parseDouble(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                        case MIN_ME7_POINTS:
                            OpenLoopFuelingLogFilterPreferences.setMinMe7PointsPreference(Integer.parseInt(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                        case MIN_AFR_POINTS:
                            OpenLoopFuelingLogFilterPreferences.setMinAfrPointsPreference(Integer.parseInt(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                        case MAX_AFR:
                            OpenLoopFuelingLogFilterPreferences.setMaxAfrPreference(Double.parseDouble(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                        case FUEL_INJECTOR_SIZE:
                            OpenLoopFuelingLogFilterPreferences.setFuelInjectorSizePreference(Double.parseDouble(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                        case FUEL_DENSITY:
                            OpenLoopFuelingLogFilterPreferences.setGasolineGramsPerCubicCentimeterPreference(Double.parseDouble(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                        case NUM_FUEL_INJECTORS:
                            OpenLoopFuelingLogFilterPreferences.setNumFuelInjectorsPreference(Double.parseDouble(filterConfigPane.getFieldText(fieldTitle)));
                            break;
                    }
                }

                if(this.me7LogFile != null) {
                    loadMe7File(this.me7LogFile);
                }

                if(this.afrLogFile != null) {
                    loadAfrFile(this.afrLogFile);
                }
            }
        });

        return button;
    }

    private JButton getMe7FileButton() {
        JButton button = new JButton("Load ME7 Logs");

        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new CSVFileFilter());
            fc.setCurrentDirectory(OpenLoopFileChooserPreferences.getDirectory());

            int returnValue = fc.showOpenDialog(openLoopLogPanel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                this.me7LogFile = fc.getSelectedFile();
                loadMe7File(this.me7LogFile);
                OpenLoopFileChooserPreferences.setDirectory(this.me7LogFile.getParentFile());
            }
        });

        return button;
    }

    private void loadMe7File(File file) {
        viewModel.loadMe7File(file);
        me7FileLabel.setText(file.getName());
    }

    private JButton getAfrFileButton() {
        JButton button = new JButton("Load Zeitronix Logs");

        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new CSVFileFilter());
            fc.setCurrentDirectory(AfrFileChooserPreferences.getDirectory());

            int returnValue = fc.showOpenDialog(openLoopLogPanel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                this.afrLogFile = fc.getSelectedFile();
                loadAfrFile(this.afrLogFile);
                AfrFileChooserPreferences.setDirectory(this.afrLogFile.getParentFile());
            }
        });

        return button;
    }

    private void loadAfrFile(File file) {
        viewModel.loadAfrFile(file);
        afrFileLabel.setText(file.getName());
    }

    private void initFuelingChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        fuelingChart = ChartFactory.createScatterPlot(
                "Fueling",
                "RPM", "AFR", dataset);

        XYPlot plot = (XYPlot) fuelingChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer rendererMe7 = new XYLineAndShapeRenderer(true, false);
        rendererMe7.setAutoPopulateSeriesPaint(false);
        rendererMe7.setDefaultPaint(Color.BLUE);
        plot.setRenderer(ME7_FUELING_DATA_SERIES_INDEX, rendererMe7);

        XYLineAndShapeRenderer rendererAfr = new XYLineAndShapeRenderer(true, false);
        rendererAfr.setAutoPopulateSeriesPaint(false);
        rendererAfr.setDefaultPaint(Color.RED);
        plot.setRenderer(AFR_FUELING_AIRFLOW_DATA_SERIES_INDEX, rendererAfr);
    }

    private void initAirflowChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        airflowChart = ChartFactory.createScatterPlot(
                "Airflow",
                "RPM", "g/sec", dataset);

        XYPlot plot = (XYPlot) airflowChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer rendererMeasured = new XYLineAndShapeRenderer(true, false);
        rendererMeasured.setAutoPopulateSeriesPaint(false);
        rendererMeasured.setDefaultPaint(Color.BLUE);
        plot.setRenderer(MEASURED_AIRFLOW_DATA_SERIES_INDEX, rendererMeasured);

        XYLineAndShapeRenderer rendererEstimated = new XYLineAndShapeRenderer(true, false);
        rendererEstimated.setAutoPopulateSeriesPaint(false);
        rendererEstimated.setDefaultPaint(Color.RED);
        plot.setRenderer(ESTIMATED_AIRFLOW_DATA_SERIES_INDEX, rendererEstimated);
    }

    private void drawMeasuredAirflowChart(List<List<Double>> measuredAirflowGramsPerSecondLogs, List<List<Double>> measuredRpmLogs) {
        measuredAirflowDataset.removeAllSeries();

        int logCount = 1;
        for(int i = 0; i < measuredAirflowGramsPerSecondLogs.size(); i++) {
            List<Double> measuredAirflowLog = measuredAirflowGramsPerSecondLogs.get(i);
            List<Double> rpmLog = measuredRpmLogs.get(i);

            XYSeries series = new XYSeries("Measured Airflow " + logCount++);

            for(int j = 0; j < measuredAirflowLog.size(); j++) {
                series.add(rpmLog.get(j), measuredAirflowLog.get(j));
            }

            measuredAirflowDataset.addSeries(series);
        }

        XYPlot plot = (XYPlot) airflowChart.getPlot();
        plot.setDataset(MEASURED_AIRFLOW_DATA_SERIES_INDEX, measuredAirflowDataset);
    }

    private void drawEstimatedAirflowChart(List<List<Double>> estimatedAirflowGramsPerSecondLogs, List<List<Double>> measuredRpmLogs) {
        estimatedAirflowDataset.removeAllSeries();

        int logCount = 1;
        for(int i = 0; i < estimatedAirflowGramsPerSecondLogs.size(); i++) {
            List<Double> measuredAirflowLog = estimatedAirflowGramsPerSecondLogs.get(i);
            List<Double> rpmLog = measuredRpmLogs.get(i);

            XYSeries series = new XYSeries("Estimated Airflow " + logCount++);

            for(int j = 0; j < measuredAirflowLog.size(); j++) {
                series.add(rpmLog.get(j), measuredAirflowLog.get(j));
            }

            estimatedAirflowDataset.addSeries(series);
        }

        XYPlot plot = (XYPlot) airflowChart.getPlot();
        plot.setDataset(ESTIMATED_AIRFLOW_DATA_SERIES_INDEX, estimatedAirflowDataset);
    }

    private void drawMe7FuelingLogChart(Map<Me7LogFileContract.Header, List<Double>> me7LogMap) {
        me7FuelingDataset.removeAllSeries();

        if(me7LogMap == null) {
            return;
        }

        List<Map<Me7LogFileContract.Header, List<Double>>> me7LogList = Me7LogUtil.findMe7Logs(me7LogMap, OpenLoopFuelingLogFilterPreferences.getMinThrottleAnglePreference(), 0, OpenLoopFuelingLogFilterPreferences.getMinMe7PointsPreference(), OpenLoopFuelingLogFilterPreferences.getMinMe7PointsPreference());

        int logCount = 1;
        for (Map<Me7LogFileContract.Header, List<Double>> map : me7LogList) {
            XYSeries series = new XYSeries("Desired AFR " + logCount++);

            List<Double> requestedAfr = map.get(Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER);
            List<Double> rpm = map.get(Me7LogFileContract.Header.RPM_COLUMN_HEADER);

            for (int i = 0; i < rpm.size(); i++) {
                series.add(rpm.get(i).doubleValue(), requestedAfr.get(i)*14.7);
            }

            me7FuelingDataset.addSeries(series);
        }

        XYPlot plot = (XYPlot) fuelingChart.getPlot();
        plot.setDataset(ME7_FUELING_DATA_SERIES_INDEX, me7FuelingDataset);
    }

    private void drawAfrFuelingLogChart(Map<String, List<Double>> afrLogMap) {
        afrFuelingDataset.removeAllSeries();

        if(afrLogMap == null) {
            return;
        }

        List<Map<String, List<Double>>> afrLogList = AfrLogUtil.findAfrLogs(afrLogMap, 80, 2000, 15, 150);

        int logCount = 1;
        for (Map<String, List<Double>> map : afrLogList) {
            XYSeries series = new XYSeries("Actual AFR " + logCount++);

            List<Double> actualAfr = map.get(AfrLogFileContract.AFR_HEADER);
            List<Double> rpm = map.get(AfrLogFileContract.RPM_HEADER);

            for (int i = 0; i < rpm.size(); i++) {
                series.add(rpm.get(i).doubleValue(), actualAfr.get(i).doubleValue());
            }

            afrFuelingDataset.addSeries(series);
        }

        XYPlot plot = (XYPlot) fuelingChart.getPlot();
        plot.setDataset(AFR_FUELING_AIRFLOW_DATA_SERIES_INDEX, afrFuelingDataset);
    }

    private class CSVFileFilter extends FileFilter {
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String name = f.getName();
            String[] parts = name.split("\\.");
            if(parts.length > 0) {
                String ext = parts[parts.length - 1];
                return ext.trim().equalsIgnoreCase("csv");
            }

            return false;
        }

        @Override
        public String getDescription() {
            return "CSV";
        }
    }
}
