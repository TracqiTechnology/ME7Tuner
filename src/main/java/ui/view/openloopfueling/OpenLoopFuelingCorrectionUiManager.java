package ui.view.openloopfueling;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map2d;
import model.mlhfm.MlhfmFitter;
import model.openloopfueling.correction.OpenLoopCorrection;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ui.map.map.MapTable;
import ui.viewmodel.openloopfueling.OpenLoopFuelingCorrectionViewModel;
import writer.MlhfmWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OpenLoopFuelingCorrectionUiManager {

    private static final int AFR_CORRECTION_POINT_SERIES_INDEX = 1;
    private static final int AFR_CORRECTION_LINE_SERIES_INDEX = 0;

    private JFreeChart mlfhmChart;
    private JFreeChart afrCorrectionChart;
    private JPanel correctionPanel;
    private OpenLoopCorrection openLoopCorrection;
    private MapTable mapTable;

    private XYSeriesCollection afrCorrectionPointDataSet;
    private XYSeriesCollection afrCorrectionLineDataSet;

    public OpenLoopFuelingCorrectionUiManager() {
        OpenLoopFuelingCorrectionViewModel.getInstance().getPublishSubject().subscribe(new Observer<OpenLoopCorrection>() {
            @Override
            public void onNext(OpenLoopCorrection openLoopCorrection) {
                OpenLoopFuelingCorrectionUiManager.this.openLoopCorrection = openLoopCorrection;
                drawMlhfmChart(openLoopCorrection.inputMlhfm, openLoopCorrection.correctedMlhfm);
                drawMapTable(openLoopCorrection.correctedMlhfm);
                drawAfrCorrectionChart(openLoopCorrection.correctionsAfrMap, openLoopCorrection.meanAfrMap, openLoopCorrection.modeAfrMap, openLoopCorrection.correctedAfrMap);
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

    JPanel getCorrectionPanel() {
        correctionPanel = new JPanel();
        correctionPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1.0;
        c.gridheight = 1;

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.95;
        correctionPanel.add(getTabbedPane(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.05;
        correctionPanel.add(getActionPanel(), c);

        return correctionPanel;
    }

    private JTabbedPane getTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addTab("MLHFM", null, getMlhfmChartPanel(), "Voltage to Kg/Hr");
        tabbedPane.addTab("AFR Correction %", null, getAfrCorrectionChartPanel(), "AFR Corrections");

        return tabbedPane;
    }

    private JPanel getMapTablePanel() {
        initMapTable();
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 1));
        panel.setMinimumSize(new Dimension(120, 100));
        panel.setMaximumSize(new Dimension(120, 100));
        panel.setPreferredSize(new Dimension(120, 100));
        panel.add(mapTable.getScrollPane());

        return panel;
    }

    private JPanel getMlhfmChartPanel() {
        initMlhfmChart();
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridheight = 1;

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.95;
        panel.add(new ChartPanel(mlfhmChart), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0.95;
        c.insets = new Insets(0, 8, 0, 0);
        panel.add(getMapTablePanel(), c);

        return panel;
    }

    private JPanel getAfrCorrectionChartPanel() {
        initAfrCorrectionChart();
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

        panel.add(new ChartPanel(afrCorrectionChart), c);

        return panel;
    }

    private JPanel getActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        JButton button = getFileButton();
        panel.add(button, c);

        c.gridx = 1;

        button = getFitMlhfmButton();
        panel.add(button, c);

        return panel;
    }

    private void initMlhfmChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        mlfhmChart = ChartFactory.createScatterPlot(
                "MHLFM",
                "Voltage", "kg/hr", dataset);

        XYPlot plot = (XYPlot) mlfhmChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);

        plot.getRenderer().setSeriesPaint(0, Color.BLUE);
        plot.getRenderer().setSeriesPaint(1, Color.RED);
    }

    private void initAfrCorrectionChart() {
        afrCorrectionPointDataSet = new XYSeriesCollection();
        afrCorrectionLineDataSet = new XYSeriesCollection();

        afrCorrectionChart = ChartFactory.createScatterPlot(
                "AFR Correction %",
                "Voltage", "Correction %", new XYSeriesCollection());

        XYPlot plot = (XYPlot) afrCorrectionChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer afrCorrectionLineRenderer = new XYLineAndShapeRenderer(true, false);
        afrCorrectionLineRenderer.setSeriesPaint(0, Color.RED);
        afrCorrectionLineRenderer.setSeriesPaint(1, Color.GREEN);
        afrCorrectionLineRenderer.setSeriesPaint(2, Color.MAGENTA);

        plot.setRenderer(AFR_CORRECTION_LINE_SERIES_INDEX, afrCorrectionLineRenderer);

        XYLineAndShapeRenderer afrCorrectionPointRenderer = new XYLineAndShapeRenderer(false, true);
        afrCorrectionPointRenderer.setAutoPopulateSeriesShape(false);
        afrCorrectionPointRenderer.setDefaultShape(new Ellipse2D.Double(0, 0, 1, 1));
        afrCorrectionPointRenderer.setSeriesPaint(0, Color.BLUE);

        plot.setRenderer(AFR_CORRECTION_POINT_SERIES_INDEX, afrCorrectionPointRenderer);
    }

    private JButton getFileButton() {
        JButton button = new JButton("Save MLHFM");

        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();

            int returnValue = fc.showSaveDialog(correctionPanel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fc.getSelectedFile();

                MlhfmWriter mlhfmWriter = new MlhfmWriter();
                mlhfmWriter.write(selectedFile, openLoopCorrection.correctedMlhfm);
            }
        });

        return button;
    }

    private JButton getFitMlhfmButton() {
        JButton button = new JButton("Fit MLHFM");

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Map2d correctedFitMlhfm = MlhfmFitter.fitMlhfm(openLoopCorrection.correctedMlhfm, 6);
                drawMlhfmChart(openLoopCorrection.inputMlhfm, correctedFitMlhfm);
                drawMapTable(correctedFitMlhfm);
            }
        });

        return button;
    }

    private void initMapTable() {
        mapTable = MapTable.getMapTable(new Double[0], new String[]{"kg/hr"}, new Double[0][]);
    }

    private void drawMapTable(Map2d mlhfmMap) {
        List<Double> voltage = Arrays.asList(mlhfmMap.axis);
        List<Double> kghr = Arrays.asList(mlhfmMap.data);
        Double[][] data = new Double[kghr.size()][1];

        for (int i = 0; i < data.length; i++) {
            data[i][0] = kghr.get(i);
        }

        mapTable.setRowHeaders(voltage.toArray(new Double[0]));
        mapTable.setTableData(data);
    }

    private void drawMlhfmChart(Map2d inputMlhfmMap,Map2d correctedMlhfmMap) {
        List<Double> voltage = Arrays.asList(inputMlhfmMap.axis);
        List<Double> kghr = Arrays.asList(inputMlhfmMap.data);

        XYSeries inputMlhfmSeries = new XYSeries("MLHFM");

        for (int i = 0; i < voltage.size(); i++) {
            inputMlhfmSeries.add(voltage.get(i), kghr.get(i));
        }

        voltage = Arrays.asList(correctedMlhfmMap.axis);
        kghr = Arrays.asList(correctedMlhfmMap.data);

        XYSeries correctedMlhfmSeries = new XYSeries("Corrected MLHFM");

        for (int i = 0; i < voltage.size(); i++) {
            correctedMlhfmSeries.add(voltage.get(i), kghr.get(i));
        }

        XYPlot plot = (XYPlot) mlfhmChart.getPlot();
        ((XYSeriesCollection) plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection) plot.getDataset()).addSeries(inputMlhfmSeries);
        ((XYSeriesCollection) plot.getDataset()).addSeries(correctedMlhfmSeries);
    }

    private void drawAfrCorrectionChart(Map<Double, List<Double>> correctionsAfrMap, Map<Double, Double> meanAfrMap, Map<Double, double[]> modeAfrMap, Map<Double, Double> correctedAfrMap) {

        XYPlot plot = (XYPlot) afrCorrectionChart.getPlot();

        afrCorrectionPointDataSet.removeAllSeries();
        afrCorrectionLineDataSet.removeAllSeries();

        generateFinalAfrCorrectionSeries(correctedAfrMap);
        generateModeAfrCorrectionSeries(modeAfrMap);
        generateMeanAfrCorrectionSeries(meanAfrMap);
        plot.setDataset(AFR_CORRECTION_LINE_SERIES_INDEX, afrCorrectionLineDataSet);

        generateRawAfrCorrections(correctionsAfrMap);
        plot.setDataset(AFR_CORRECTION_POINT_SERIES_INDEX, afrCorrectionPointDataSet);
    }

    private void generateRawAfrCorrections(Map<Double, List<Double>> correctionsAfrMap) {
        XYSeries afrCorrectionsSeries = new XYSeries("AFR Corrections %");

        for (Double voltage : correctionsAfrMap.keySet()) {
            List<Double> values = correctionsAfrMap.get(voltage);

            for (Double value : values) {
                afrCorrectionsSeries.add(voltage, value);
            }
        }

        afrCorrectionPointDataSet.addSeries(afrCorrectionsSeries);
    }

    private void generateMeanAfrCorrectionSeries(Map<Double, Double> meanAfrMap) {
        XYSeries meanAfrCorrectionSeries = new XYSeries("Mean AFR Correction %");

        for(Double voltage: meanAfrMap.keySet()) {
            meanAfrCorrectionSeries.add(voltage, meanAfrMap.get(voltage));
        }

        afrCorrectionLineDataSet.addSeries(meanAfrCorrectionSeries);
    }

    private void generateModeAfrCorrectionSeries(Map<Double, double[]> modeAfrMap) {
        Mean mean = new Mean();

        XYSeries modeAfrCorrectionSeries = new XYSeries("Mode AFR Correction %");

        for(Double voltage: modeAfrMap.keySet()) {
            double[] mode = modeAfrMap.get(voltage);
            modeAfrCorrectionSeries.add(voltage.doubleValue(), mean.evaluate(mode, 0, mode.length));
        }

        afrCorrectionLineDataSet.addSeries(modeAfrCorrectionSeries);
    }

    private void generateFinalAfrCorrectionSeries(Map<Double, Double> correctedAfrMap) {
        XYSeries afrCorrectionSeries = new XYSeries("Final AFR Correction %");

        for(Double voltage: correctedAfrMap.keySet()) {
            afrCorrectionSeries.add(voltage, correctedAfrMap.get(voltage));
        }

        afrCorrectionLineDataSet.addSeries(afrCorrectionSeries);
    }
}
