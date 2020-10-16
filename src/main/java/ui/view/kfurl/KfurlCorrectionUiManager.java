package ui.view.kfurl;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfurl.Kfurl;
import model.kfurl.KfurlCorrection;
import model.wdkugdn.Wdkugdn;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ui.map.map.MapTable;
import ui.viewmodel.kfurl.KfurlViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class KfurlCorrectionUiManager {

    private static final int CORRECTION_LINE_SERIES_INDEX = 0;
    private static final int CORRECTION_POINT_SERIES_INDEX = 1;

    private MapTable kfurl;
    private JPanel panel;

    private XYSeriesCollection correctionPointDataSet;
    private XYSeriesCollection correctionLineDataSet;
    private JFreeChart correctionChart;

    public KfurlCorrectionUiManager() {
        initPanel();

        KfurlViewModel.getInstance().getOutputSubject().subscribe(new Observer<KfurlCorrection>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override
            public void onNext(@NonNull KfurlCorrection kfurlCorrection) {
                setMap(kfurlCorrection.kfurl);
                drawAfrCorrectionChart(kfurlCorrection);
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
        return panel;
    }

    private void setMap(Map3d map) {
        kfurl.setMap(map);
    }

    private void initPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 0;

        panel.add(getTabbedPane(), c);
    }

    private JTabbedPane getTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addTab("KFURL", null, getMapPanel(), "Corrected KFURL");
        tabbedPane.addTab("KFURL Correction %", null, getCorrectionChartPanel(), "KFURL Correction");

        return tabbedPane;
    }

    private void drawAfrCorrectionChart(KfurlCorrection corrections) {

        XYPlot plot = (XYPlot) correctionChart.getPlot();

        correctionPointDataSet.removeAllSeries();
        correctionLineDataSet.removeAllSeries();

        generateCorrectionLineSeries(corrections.correction);
        plot.setDataset(CORRECTION_LINE_SERIES_INDEX, correctionLineDataSet);

        generateCorrectionPointSeries(corrections.corrections);
        plot.setDataset(CORRECTION_POINT_SERIES_INDEX, correctionPointDataSet);

    }

    private void generateCorrectionLineSeries(Double[][] corrections) {
        XYSeries afrCorrectionSeries = new XYSeries("Final KFURL Correction %");

        for(int i = 0; i < Kfurl.getXAxis().length; i++) {
            afrCorrectionSeries.add(Kfurl.getXAxis()[i], corrections[0][i]);
        }

        correctionLineDataSet.addSeries(afrCorrectionSeries);
    }

    private void generateCorrectionPointSeries(List<List<Double>> corrections) {
        XYSeries correctionsSeries = new XYSeries("KFRUL Corrections %");

        for (int i = 0; i < corrections.size(); i++) {
            for (Double value : corrections.get(i)) {
                correctionsSeries.add(Kfurl.getXAxis()[i], value);
            }
        }

        correctionPointDataSet.addSeries(correctionsSeries);
    }

    private JPanel getCorrectionChartPanel() {
        initCorrectionChart();
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

        panel.add(new ChartPanel(correctionChart), c);

        return panel;
    }

    private void initCorrectionChart() {
        correctionPointDataSet = new XYSeriesCollection();
        correctionLineDataSet = new XYSeriesCollection();

        correctionChart = ChartFactory.createScatterPlot(
                "Correction %",
                "RPM", "Correction %", new XYSeriesCollection());

        XYPlot plot = (XYPlot) correctionChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer correctionLineRenderer = new XYLineAndShapeRenderer(true, false);
        correctionLineRenderer.setSeriesPaint(0, Color.RED);

        plot.setRenderer(CORRECTION_LINE_SERIES_INDEX, correctionLineRenderer);

        XYLineAndShapeRenderer correctionPointRenderer = new XYLineAndShapeRenderer(false, true);
        correctionPointRenderer.setAutoPopulateSeriesShape(false);
        correctionPointRenderer.setDefaultShape(new Ellipse2D.Double(0, 0, 1, 1));
        correctionPointRenderer.setSeriesPaint(0, Color.BLUE);

        plot.setRenderer(CORRECTION_POINT_SERIES_INDEX, correctionPointRenderer);
    }

    private JPanel getMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        kfurl = MapTable.getMapTable(Kfurl.getYAxis(), Kfurl.getXAxis(), Kfurl.getMap());
        c.weightx = 1;
        c.gridx = 0;

        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("Corrected KFURL (Output)"), c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.top = 16;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollPane = kfurl.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(620, 245));

        panel.add(scrollPane, c);

        return panel;
    }
}
