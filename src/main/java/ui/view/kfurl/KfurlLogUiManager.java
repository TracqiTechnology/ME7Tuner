package ui.view.kfurl;


import contract.AfrLogFileContract;
import contract.Me7LogFileContract;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import preferences.filechooser.FileChooserPreferences;
import ui.viewmodel.kfurl.KfurlViewModel;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

public class KfurlLogUiManager {

    private static final int ME7_BOOST_DATA_SERIES_INDEX = 0;
    private static final int ZEIT_BOOST_DATA_SERIES_INDEX = 1;

    private JFreeChart boostChart;
    private JPanel openLoopLogPanel;
    private JLabel me7FileLabel;
    private JLabel afrFileLabel;

    private JButton loadZeitLogsButton;

    private final XYSeriesCollection me7BoostDataset;
    private final XYSeriesCollection afrBoostDataset;

    public KfurlLogUiManager() {
        me7BoostDataset = new XYSeriesCollection();
        afrBoostDataset = new XYSeriesCollection();

        KfurlViewModel.getInstance().getMe7LogsSubject().subscribe(new Observer<Map<Me7LogFileContract.Header, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override
            public void onNext(@NonNull Map<Me7LogFileContract.Header, List<Double>> logs) {
                drawMe7BoostLogChart(logs);
                loadZeitLogsButton.setEnabled(true);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        KfurlViewModel.getInstance().getZeitLogsSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override
            public void onNext(@NonNull Map<String, List<Double>> logs) {
                drawZeitBoostLogChart(logs);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    public JPanel getPanel() {
        openLoopLogPanel = new JPanel();
        openLoopLogPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1.0;
        c.gridheight = 1;

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.95;
        openLoopLogPanel.add(getFuelingChartPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.05;
        openLoopLogPanel.add(getActionPanel(), c);

        return openLoopLogPanel;
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

        panel.add(new ChartPanel(boostChart), c);

        return panel;
    }

    private JPanel getActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 0.9;
        c.gridx = 0;
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

        loadZeitLogsButton = getAfrFileButton();
        loadZeitLogsButton.setEnabled(false);
        panel.add(loadZeitLogsButton, c);

        c.gridx = 1;
        c.gridy = 1;

        afrFileLabel = new JLabel("No File Selected");
        panel.add(afrFileLabel, c);

        return panel;
    }

    private JButton getMe7FileButton() {
        JButton button = new JButton("Load ME7 Logs");

        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new CSVFileFilter());
            fc.setCurrentDirectory(FileChooserPreferences.getDirectory());

            int returnValue = fc.showOpenDialog(openLoopLogPanel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File me7LogFile = fc.getSelectedFile();
                me7FileLabel.setText(me7LogFile.getName());
                KfurlViewModel.getInstance().loadMe7File(me7LogFile);
                FileChooserPreferences.setDirectory(me7LogFile.getParentFile());
            }
        });

        return button;
    }

    private JButton getAfrFileButton() {
        JButton button = new JButton("Load Zeitronix Logs");

        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new CSVFileFilter());
            fc.setCurrentDirectory(FileChooserPreferences.getDirectory());

            int returnValue = fc.showOpenDialog(openLoopLogPanel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File afrLogFile = fc.getSelectedFile();
                afrFileLabel.setText(afrLogFile.getName());
                KfurlViewModel.getInstance().loadAfrFile(afrLogFile);
                FileChooserPreferences.setDirectory(afrLogFile.getParentFile());
            }
        });

        return button;
    }

    private void initFuelingChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        boostChart = ChartFactory.createScatterPlot(
                "Boost",
                "Timestamp", "mbar", dataset);

        XYPlot plot = (XYPlot) boostChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer rendererMe7 = new XYLineAndShapeRenderer(true, false);
        rendererMe7.setAutoPopulateSeriesPaint(false);
        rendererMe7.setDefaultPaint(Color.BLUE);
        plot.setRenderer(ME7_BOOST_DATA_SERIES_INDEX, rendererMe7);

        XYLineAndShapeRenderer rendererAfr = new XYLineAndShapeRenderer(true, false);
        rendererAfr.setAutoPopulateSeriesPaint(false);
        rendererAfr.setDefaultPaint(Color.RED);
        plot.setRenderer(ZEIT_BOOST_DATA_SERIES_INDEX, rendererAfr);
    }

    private void drawMe7BoostLogChart(Map<Me7LogFileContract.Header, List<Double>> me7LogMap) {
        me7BoostDataset.removeAllSeries();

        XYSeries series = new XYSeries("Modeled Boost (ps_w)");

        List<Double> modeledBoost = me7LogMap.get(Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_MODELED_HEADER);
        List<Double> timestamp = me7LogMap.get(Me7LogFileContract.Header.TIME_COLUMN_HEADER);

        for (int i = 0; i < timestamp.size(); i++) {
            series.add(timestamp.get(i).doubleValue(), modeledBoost.get(i).doubleValue());
        }

        me7BoostDataset.addSeries(series);

        XYPlot plot = (XYPlot) boostChart.getPlot();
        plot.setDataset(ME7_BOOST_DATA_SERIES_INDEX, me7BoostDataset);
    }

    private void drawZeitBoostLogChart(Map<String, List<Double>> afrLogMap) {
        afrBoostDataset.removeAllSeries();

        XYSeries series = new XYSeries("Zeit Boost");

        List<Double> relativeBoost = afrLogMap.get(AfrLogFileContract.BOOST_HEADER);
        List<Double> timestamp = afrLogMap.get(AfrLogFileContract.TIMESTAMP);

        for (int i = 0; i < timestamp.size(); i++) {
            series.add(timestamp.get(i), relativeBoost.get(i));
        }

        afrBoostDataset.addSeries(series);

        XYPlot plot = (XYPlot) boostChart.getPlot();
        plot.setDataset(ZEIT_BOOST_DATA_SERIES_INDEX, afrBoostDataset);
    }

    private class CSVFileFilter extends FileFilter {
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String name = f.getName();
            String[] parts = name.split("\\.");
            if (parts.length > 0) {
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
