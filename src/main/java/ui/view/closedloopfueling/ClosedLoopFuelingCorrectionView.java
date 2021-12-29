package ui.view.closedloopfueling;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.closedloopfueling.ClosedLoopFuelingCorrection;
import model.mlhfm.MlhfmFitter;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import preferences.bin.BinFilePreferences;
import preferences.mlhfm.MlhfmMapPreferences;
import ui.map.map.MapTable;
import ui.viewmodel.closedloopfueling.ClosedLoopFuelingCorrectionViewModel;
import writer.BinWriter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClosedLoopFuelingCorrectionView {

    private static final int CORRECTION_POINT_SERIES_INDEX = 1;
    private static final int CORRECTION_LINE_SERIES_INDEX = 0;

    private JFreeChart mlfhmChart;
    private JFreeChart stdDevChart;
    private JFreeChart afrCorrectionChart;
    private JPanel correctionPanel;
    private ClosedLoopFuelingCorrection closedLoopFuelingCorrection;
    private MapTable mapTable;

    private XYSeriesCollection afrCorrectionPointDataSet;
    private XYSeriesCollection afrCorrectionLineDataSet;

    private int polynomialDegree = 6;

    ClosedLoopFuelingCorrectionView() {
        ClosedLoopFuelingCorrectionViewModel closedLoopFuelingCorrectionViewModel = new ClosedLoopFuelingCorrectionViewModel();
        closedLoopFuelingCorrectionViewModel.registerMLHFMOnChange(new Observer<ClosedLoopFuelingCorrection>() {

            @Override
            public void onNext(@NonNull ClosedLoopFuelingCorrection closedLoopFuelingCorrection) {
                ClosedLoopFuelingCorrectionView.this.closedLoopFuelingCorrection = closedLoopFuelingCorrection;
                drawMlhfmChart(closedLoopFuelingCorrection.inputMlhfm, closedLoopFuelingCorrection.correctedMlhfm);
                drawMapTable(closedLoopFuelingCorrection.correctedMlhfm);
                drawStdDevChart(closedLoopFuelingCorrection.filteredVoltageDt, closedLoopFuelingCorrection.correctedMlhfm);
                drawAfrCorrectionChart(closedLoopFuelingCorrection.correctionsAfrMap, closedLoopFuelingCorrection.meanAfrMap, closedLoopFuelingCorrection.modeAfrMap, closedLoopFuelingCorrection.correctedAfrMap);
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
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
        tabbedPane.addTab("dMAFv/dt", null, getStdDevChartPanel(), "Derivative");
        tabbedPane.addTab("AFR Correction %", null, getAfrCorrectionChartPanel(), "AFR Correction");

        return tabbedPane;
    }

    private JPanel getMapTablePanel() {
        initMapTable();
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 1));
        panel.setMinimumSize(new Dimension(125, 100));
        panel.setMaximumSize(new Dimension(125, 100));
        panel.setPreferredSize(new Dimension(125, 100));
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

    private JPanel getStdDevChartPanel() {
        initStdDevChart();
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

        panel.add(new ChartPanel(stdDevChart), c);

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

        panel.add(getFitMlhfmPanel(), c);

        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(10, 0, 0, 0);

        panel.add(getFileButton(), c);

        return panel;
    }

    private void initMlhfmChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        mlfhmChart = ChartFactory.createScatterPlot(
                "Corrected MLHFM",
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

    private void initStdDevChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        stdDevChart = ChartFactory.createScatterPlot(
                "Derivative",
                "Voltage", "dMAFv/dt", dataset);

        XYPlot plot = (XYPlot) stdDevChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        plot.setRenderer(renderer);

        plot.getRenderer().setSeriesShape(0, new Ellipse2D.Double(0, 0, 1, 1));
        plot.getRenderer().setSeriesPaint(0, Color.BLUE);
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

        XYLineAndShapeRenderer correctionLineRenderer = new XYLineAndShapeRenderer(true, false);
        correctionLineRenderer.setSeriesPaint(0, Color.RED);
        correctionLineRenderer.setSeriesPaint(1, Color.GREEN);
        correctionLineRenderer.setSeriesPaint(2, Color.MAGENTA);

        plot.setRenderer(CORRECTION_LINE_SERIES_INDEX, correctionLineRenderer);

        XYLineAndShapeRenderer correctionPointRenderer = new XYLineAndShapeRenderer(false, true);
        correctionPointRenderer.setAutoPopulateSeriesShape(false);
        correctionPointRenderer.setDefaultShape(new Ellipse2D.Double(0, 0, 1, 1));
        correctionPointRenderer.setSeriesPaint(0, Color.BLUE);

        plot.setRenderer(CORRECTION_POINT_SERIES_INDEX, correctionPointRenderer);
    }

    private JButton getFileButton() {
        JButton button = new JButton("Write MLHFM");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    correctionPanel,
                    "Are you sure you want to write MLHFM to the binary?",
                    "Write MLHFM",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), MlhfmMapPreferences.getSelectedMlhfmTableDefinition().fst, closedLoopFuelingCorrection.fitMlhfm);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        return button;
    }

    private JPanel getFitMlhfmPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        JLabel label = new JLabel("Polynomial Degree: ");
        panel.add(label, c);

        c.gridx = 1;

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(6, 1, 100, 1));

        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                polynomialDegree = (int) spinner.getValue();
            }
        });

        panel.add(spinner, c);

        c.gridx = 2;
        c.insets = new Insets(0, 8, 0, 0);

        panel.add(getFitMlhfmButton(), c);

        return panel;
    }

    private JButton getFitMlhfmButton() {
        JButton button = new JButton("Fit MLHFM");
        button.setToolTipText("Smooth the curve by fitting a polynomial.");

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Map3d correctedFitMlhfm = MlhfmFitter.fitMlhfm(closedLoopFuelingCorrection.correctedMlhfm, polynomialDegree);
                closedLoopFuelingCorrection = new ClosedLoopFuelingCorrection(closedLoopFuelingCorrection.inputMlhfm, closedLoopFuelingCorrection.correctedMlhfm, correctedFitMlhfm, closedLoopFuelingCorrection.filteredVoltageDt, closedLoopFuelingCorrection.correctionsAfrMap, closedLoopFuelingCorrection.meanAfrMap, closedLoopFuelingCorrection.modeAfrMap, closedLoopFuelingCorrection.correctedAfrMap);
                drawMlhfmChart(closedLoopFuelingCorrection.inputMlhfm, correctedFitMlhfm);
                drawMapTable(correctedFitMlhfm);
            }
        });

        return button;
    }

    private void initMapTable() {
        mapTable = MapTable.getMapTable(new Double[0], new String[]{"kg/hr"}, new Double[0][]);
    }

    private void drawMapTable(Map3d mlhfmMap) {
        mapTable.setRowHeaders(mlhfmMap.yAxis);
        mapTable.setTableData(mlhfmMap.zAxis);
        mapTable.invalidate();
    }

    private void drawMlhfmChart(Map3d inputMlhfm, Map3d correctedMlhfm) {
        List<Double> voltage = Arrays.asList(inputMlhfm.yAxis);
        List<Double> kghr = new ArrayList<>();

        for (int i = 0; i < inputMlhfm.zAxis.length; i++) {
            kghr.add(inputMlhfm.zAxis[i][0]);
        }

        XYSeries inputMlhfmSeries = new XYSeries("MLHFM");

        for (int i = 0; i < voltage.size(); i++) {
            inputMlhfmSeries.add(voltage.get(i), kghr.get(i));
        }

        voltage = Arrays.asList(correctedMlhfm.yAxis);

        kghr.clear();

        for (int i = 0; i < correctedMlhfm.zAxis.length; i++) {
            kghr.add(correctedMlhfm.zAxis[i][0]);
        }

        XYSeries correctedMlhfmSeries = new XYSeries("Corrected MLHFM");

        for (int i = 0; i < voltage.size(); i++) {
            correctedMlhfmSeries.add(voltage.get(i), kghr.get(i));
        }

        XYPlot plot = (XYPlot) mlfhmChart.getPlot();
        ((XYSeriesCollection) plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection) plot.getDataset()).addSeries(inputMlhfmSeries);
        ((XYSeriesCollection) plot.getDataset()).addSeries(correctedMlhfmSeries);
    }

    private void drawStdDevChart(Map<Double, List<Double>> stdDev, Map3d mlhfm) {

        Double[] voltages = mlhfm.yAxis;

        XYSeries series = new XYSeries("dMAFv/dt");

        for (Double voltage : voltages) {
            List<Double> values = stdDev.get(voltage);

            for (Double value : values) {
                series.add(voltage, value);
            }
        }

        XYPlot plot = (XYPlot) stdDevChart.getPlot();
        ((XYSeriesCollection) plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection) plot.getDataset()).addSeries(series);
    }

    private void drawAfrCorrectionChart(Map<Double, List<Double>> correctionsAfrMap, Map<Double, Double> meanAfrMap, Map<Double, double[]> modeAfrMap, Map<Double, Double> correctedAfrMap) {

        XYPlot plot = (XYPlot) afrCorrectionChart.getPlot();

        afrCorrectionPointDataSet.removeAllSeries();
        afrCorrectionLineDataSet.removeAllSeries();

        generateFinalAfrCorrectionSeries(correctedAfrMap);
        generateModeAfrCorrectionSeries(modeAfrMap);
        generateMeanAfrCorrectionSeries(meanAfrMap);
        plot.setDataset(CORRECTION_LINE_SERIES_INDEX, afrCorrectionLineDataSet);

        generateRawAfrCorrections(correctionsAfrMap);
        plot.setDataset(CORRECTION_POINT_SERIES_INDEX, afrCorrectionPointDataSet);
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

        for (Double voltage : meanAfrMap.keySet()) {
            meanAfrCorrectionSeries.add(voltage, meanAfrMap.get(voltage));
        }

        afrCorrectionLineDataSet.addSeries(meanAfrCorrectionSeries);
    }

    private void generateModeAfrCorrectionSeries(Map<Double, double[]> modeAfrMap) {
        Mean mean = new Mean();

        XYSeries modeAfrCorrectionSeries = new XYSeries("Mode AFR Correction %");

        for (Double voltage : modeAfrMap.keySet()) {
            double[] mode = modeAfrMap.get(voltage);
            modeAfrCorrectionSeries.add(voltage.doubleValue(), mean.evaluate(mode, 0, mode.length));
        }

        afrCorrectionLineDataSet.addSeries(modeAfrCorrectionSeries);
    }

    private void generateFinalAfrCorrectionSeries(Map<Double, Double> correctedAfrMap) {
        XYSeries afrCorrectionSeries = new XYSeries("Final AFR Correction %");

        for (Double voltage : correctedAfrMap.keySet()) {
            afrCorrectionSeries.add(voltage, correctedAfrMap.get(voltage));
        }

        afrCorrectionLineDataSet.addSeries(afrCorrectionSeries);
    }
}
