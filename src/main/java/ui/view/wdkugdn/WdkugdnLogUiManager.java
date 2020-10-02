package ui.view.wdkugdn;


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
import ui.viewmodel.wdkugdn.WdkugdnViewModel;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

public class WdkugdnLogUiManager {

    private static final int MAF_DATA_SERIES_INDEX = 0;
    private static final int MAF_AT_THROTTLE_PLATE_DATA_SERIES_INDEX = 1;

    private JFreeChart mafChart;
    private JPanel logPanel;
    private JLabel me7FileLabel;

    private final XYSeriesCollection mafDataset;
    private final XYSeriesCollection mafAtThrottlePlateDataset;

    public WdkugdnLogUiManager() {
        mafDataset = new XYSeriesCollection();
        mafAtThrottlePlateDataset = new XYSeriesCollection();

        WdkugdnViewModel.getInstance().getMe7LogsSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override
            public void onNext(@NonNull Map<String, List<Double>> logs) {
                drawMafLogChart(logs);
                drawMafAtThrottlePlateLogChart(logs);
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
        logPanel = new JPanel();
        logPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1.0;
        c.gridheight = 1;

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.95;
        logPanel.add(getChartPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.05;
        logPanel.add(getActionPanel(), c);

        return logPanel;
    }

    private JPanel getChartPanel() {
        initChart();

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

        panel.add(new ChartPanel(mafChart), c);

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

        return panel;
    }

    private JButton getMe7FileButton() {
        JButton button = new JButton("Load ME7 Logs");

        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new CSVFileFilter());
            fc.setCurrentDirectory(FileChooserPreferences.getDirectory());

            int returnValue = fc.showOpenDialog(logPanel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File me7LogFile = fc.getSelectedFile();
                me7FileLabel.setText(me7LogFile.getName());
                WdkugdnViewModel.getInstance().loadMe7File(me7LogFile);
                FileChooserPreferences.setDirectory(me7LogFile.getParentFile());
            }
        });

        return button;
    }

    private void initChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        mafChart = ChartFactory.createScatterPlot(
                "MAF",
                "Timestamp", "g/sec", dataset);

        XYPlot plot = (XYPlot) mafChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer mafRenderer = new XYLineAndShapeRenderer(true, false);
        mafRenderer.setAutoPopulateSeriesPaint(false);
        mafRenderer.setDefaultPaint(Color.BLUE);
        plot.setRenderer(MAF_DATA_SERIES_INDEX, mafRenderer);

        XYLineAndShapeRenderer mafAtThrottlePlateRenderer = new XYLineAndShapeRenderer(true, false);
        mafAtThrottlePlateRenderer.setAutoPopulateSeriesPaint(false);
        mafAtThrottlePlateRenderer.setDefaultPaint(Color.RED);
        plot.setRenderer(MAF_AT_THROTTLE_PLATE_DATA_SERIES_INDEX, mafAtThrottlePlateRenderer);
    }

    private void drawMafLogChart(Map<String, List<Double>> logMap) {
        mafDataset.removeAllSeries();

        XYSeries series = new XYSeries("MAF (mshfm_w)");

        List<Double> maf = logMap.get(Me7LogFileContract.MAF_GRAMS_PER_SECOND_HEADER);
        List<Double> timestamp = logMap.get(Me7LogFileContract.TIME_COLUMN_HEADER);

        for (int i = 0; i < timestamp.size(); i++) {
            series.add(timestamp.get(i).doubleValue(), maf.get(i).doubleValue());
        }

        mafDataset.addSeries(series);

        XYPlot plot = (XYPlot) mafChart.getPlot();
        plot.setDataset(MAF_DATA_SERIES_INDEX, mafDataset);
    }

    private void drawMafAtThrottlePlateLogChart(Map<String, List<Double>> logMap) {
        mafAtThrottlePlateDataset.removeAllSeries();

        XYSeries series = new XYSeries("MAF At Throttle Plate (msdk_w");

        List<Double> mafAtThrottlePlate = logMap.get(Me7LogFileContract.MAF_AT_THROTTLE_PLATE);
        List<Double> timestamp = logMap.get(Me7LogFileContract.TIME_COLUMN_HEADER);

        for (int i = 0; i < timestamp.size(); i++) {
            series.add(timestamp.get(i), mafAtThrottlePlate.get(i));
        }

        mafAtThrottlePlateDataset.addSeries(series);

        XYPlot plot = (XYPlot) mafChart.getPlot();
        plot.setDataset(MAF_AT_THROTTLE_PLATE_DATA_SERIES_INDEX, mafAtThrottlePlateDataset);
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
