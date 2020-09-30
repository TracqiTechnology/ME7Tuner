package ui.view.kfurl;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfurl.Kfurl;
import model.kfurl.KfurlCorrection;
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
import java.util.List;
import java.util.Map;

public class KfurlCorrectionUiManager {

    private static final int AFR_CORRECTION_LINE_SERIES_INDEX = 0;

    private MapTable kfurl;
    private JPanel panel;

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
                drawAfrCorrectionChart(kfurlCorrection.correction);
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
        tabbedPane.addTab("KFURL Correction %", null, getcorrectionChartPanel(), "KFURL Correction");

        return tabbedPane;
    }

    private void drawAfrCorrectionChart(Double[][] corrections) {

        XYPlot plot = (XYPlot) correctionChart.getPlot();

        correctionLineDataSet.removeAllSeries();

        generateCorrectionSeries(corrections);
        plot.setDataset(AFR_CORRECTION_LINE_SERIES_INDEX, correctionLineDataSet);

    }

    private void generateCorrectionSeries(Double[][] corrections) {
        XYSeries afrCorrectionSeries = new XYSeries("Final KFURL Correction %");

        for(int i = 0; i < Kfurl.getXAxis().length; i++) {
            afrCorrectionSeries.add(Kfurl.getXAxis()[i], corrections[0][i]);
        }

        correctionLineDataSet.addSeries(afrCorrectionSeries);
    }

    private JPanel getcorrectionChartPanel() {
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
        correctionLineDataSet = new XYSeriesCollection();

        correctionChart = ChartFactory.createScatterPlot(
                "Correction %",
                "RPM", "Correction %", new XYSeriesCollection());

        XYPlot plot = (XYPlot) correctionChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer afrCorrectionLineRenderer = new XYLineAndShapeRenderer(true, false);
        afrCorrectionLineRenderer.setSeriesPaint(0, Color.RED);

        plot.setRenderer(AFR_CORRECTION_LINE_SERIES_INDEX, afrCorrectionLineRenderer);
    }

    private JPanel getMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        kfurl = MapTable.getMapTable(Kfurl.getYAxis(), Kfurl.getXAxis(), Kfurl.getMap());

        kfurl.getPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onSubscribe(Disposable disposable) {

            }

            @Override
            public void onNext(Map3d map3d) {
                KfurlViewModel.getInstance().setMap(map3d);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        c.weightx = 1;
        c.gridx = 0;

        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("Corrected KFURL"), c);

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
