package ui.view.closedloopfueling.kfkhfm;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.closedloopfueling.kfkhfm.ClosedLoopKfkhfmCorrection;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ui.view.fkfhfm.KfkhfmUiManager;
import ui.viewmodel.closedloopfueling.kfkhfm.ClosedLoopKfkhfmCorrectionViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Map;

public class ClosedLoopKfkhfmCorrectionUiManager {

    private static final int AFR_CORRECTION_POINT_SERIES_INDEX = 1;
    private static final int AFR_CORRECTION_LINE_SERIES_INDEX = 0;

    private JFreeChart stdDevChart;
    private JFreeChart afrCorrectionChart;
    private JPanel correctionPanel;
    private JPanel kfkhfmPanel;

    private XYSeriesCollection afrCorrectionPointDataSet;
    private XYSeriesCollection afrCorrectionLineDataSet;

    ClosedLoopKfkhfmCorrectionUiManager() {
        kfkhfmPanel = new KfkhfmUiManager().getPanel();

        ClosedLoopKfkhfmCorrectionViewModel closedLoopKfkhfmCorrectionViewModel = ClosedLoopKfkhfmCorrectionViewModel .getInstance();
        closedLoopKfkhfmCorrectionViewModel.getPublishSubject().subscribe(new Observer<ClosedLoopKfkhfmCorrection>() {

            @Override
            public void onNext(ClosedLoopKfkhfmCorrection closedLoopKfkhfmCorrection) {
                drawStdDevChart(closedLoopKfkhfmCorrection.filteredLoadDt, closedLoopKfkhfmCorrection.inputKfkhfm);
                drawAfrCorrectionChart(closedLoopKfkhfmCorrection.correctionsAfrMap, closedLoopKfkhfmCorrection.meanAfrMap, closedLoopKfkhfmCorrection.modeAfrMap);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() { }
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

        return correctionPanel;
    }

    private JTabbedPane getTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addTab("KFKHFM", null, kfkhfmPanel, "Corrected KFKHFM");
        tabbedPane.addTab("dMAFv/dt", null, getStdDevChartPanel(), "Derivative");
        tabbedPane.addTab("AFR Correction %", null, getAfrCorrectionChartPanel(), "AFR Correction");

        return tabbedPane;
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

    private void initStdDevChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        stdDevChart = ChartFactory.createScatterPlot(
                "Derivative",
                "Load", "dMAFv/dt", dataset);

        XYPlot plot = (XYPlot)stdDevChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        plot.setRenderer(renderer);

        plot.getRenderer().setSeriesShape(0, new Ellipse2D.Double(0,0,1,1));
        plot.getRenderer().setSeriesPaint(0, Color.BLUE);
    }

    private void initAfrCorrectionChart() {
        afrCorrectionPointDataSet = new XYSeriesCollection();
        afrCorrectionLineDataSet = new XYSeriesCollection();

        afrCorrectionChart = ChartFactory.createScatterPlot(
                "AFR Correction %",
                "Load", "Correction %", new XYSeriesCollection());

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

    private void drawStdDevChart(Map<Double, List<Double>> stdDev, Map3d kfkhfm) {

        Double[] loads = kfkhfm.xAxis;

        XYSeries series = new XYSeries("dMAFv/dt");

        for (Double load : loads) {
            List<Double> values = stdDev.get(load);

            for (Double value : values) {
                series.add(load, value);
            }
        }

        XYPlot plot = (XYPlot)stdDevChart.getPlot();
        ((XYSeriesCollection)plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection)plot.getDataset()).addSeries(series);
    }

    private void drawAfrCorrectionChart(Map<Double, List<Double>> correctionsAfrMap, Map<Double, Double> meanAfrMap, Map<Double, double[]> modeAfrMap) {

        XYPlot plot = (XYPlot) afrCorrectionChart.getPlot();

        afrCorrectionPointDataSet.removeAllSeries();
        afrCorrectionLineDataSet.removeAllSeries();

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
