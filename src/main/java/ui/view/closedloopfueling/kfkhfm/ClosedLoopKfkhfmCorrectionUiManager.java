package ui.view.closedloopfueling.kfkhfm;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import model.closedloopfueling.kfkhfm.ClosedLoopKfkhfmCorrection;
import model.kfkhfm.Kfkhfm;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.AWTCameraMouseController;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.ScatterMultiColor;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import ui.view.closedloopfueling.kfkhfm.colormap.ColorMapTransparent;
import ui.view.fkfhfm.KfkhfmUiManager;
import ui.viewmodel.closedloopfueling.kfkhfm.ClosedLoopKfkhfmCorrectionViewModel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClosedLoopKfkhfmCorrectionUiManager {

    private Chart stdDevChart3d;
    private Chart afrCorrectionChart3d;

    private ScatterMultiColor scatterStdDev;

    private ScatterMultiColor scatterAfr;
    private Scatter scatterMeanAfr;
    private Scatter scatterModeAfr;

    private KfkhfmUiManager kfkhfmUiManager;


    ClosedLoopKfkhfmCorrectionUiManager() {
        kfkhfmUiManager = new KfkhfmUiManager();

        ClosedLoopKfkhfmCorrectionViewModel closedLoopKfkhfmCorrectionViewModel = ClosedLoopKfkhfmCorrectionViewModel.getInstance();
        closedLoopKfkhfmCorrectionViewModel.getPublishSubject().subscribe(new Observer<ClosedLoopKfkhfmCorrection>() {

            @Override
            public void onNext(ClosedLoopKfkhfmCorrection closedLoopKfkhfmCorrection) {
                kfkhfmUiManager.setMap(closedLoopKfkhfmCorrection.correctedKfkhfm);
                drawStdDevChart(closedLoopKfkhfmCorrection.filteredLoadDt);
                drawAfrCorrectionChart(closedLoopKfkhfmCorrection.correctionsAfr, closedLoopKfkhfmCorrection.meanAfr, closedLoopKfkhfmCorrection.modeAfr);
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
        JPanel correctionPanel = new JPanel();
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
        tabbedPane.addTab("KFKHFM", null, kfkhfmUiManager.getPanel(), "Corrected KFKHFM");
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

        panel.add((Component)stdDevChart3d.getCanvas(), c);

        return panel;
    }

    private JPanel getAfrCorrectionChartPanel() {
        init3dAfrCorrectionChart();

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

        //panel.add(new ChartPanel(afrCorrectionChart), c);
        panel.add((Component)afrCorrectionChart3d.getCanvas(), c);

        return panel;
    }

    private void initStdDevChart() {
        // Create a chart and add scatterAfr
        stdDevChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        stdDevChart3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        stdDevChart3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        stdDevChart3d.getAxeLayout().setXAxeLabel("Engine Load (rl_w)");
        stdDevChart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        stdDevChart3d.getAxeLayout().setZAxeLabel("dMAFv/dt");

        NewtCameraMouseController controller = new NewtCameraMouseController(stdDevChart3d);

        int size = 10;
        float x;
        float y;
        float z;

        Coord3d[] points = new Coord3d[size];

        Random r = new Random();
        r.setSeed(0);

        for(int i=0; i<size; i++){
            x = r.nextFloat() - 0.5f;
            y = r.nextFloat() - 0.5f;
            z = r.nextFloat() - 0.5f;
            points[i] = new Coord3d(x, y, z);
        }

        scatterStdDev = new ScatterMultiColor(points, new ColorMapper(new ColorMapTransparent(), 0, 0));

        stdDevChart3d.getScene().add(scatterStdDev, true);
    }


    private void init3dAfrCorrectionChart() {

        // Create a chart and add scatterAfr
        afrCorrectionChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        afrCorrectionChart3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        afrCorrectionChart3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        afrCorrectionChart3d.getAxeLayout().setXAxeLabel("Engine Load (rl_w)");
        afrCorrectionChart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        afrCorrectionChart3d.getAxeLayout().setZAxeLabel("Correction %");

        NewtCameraMouseController controller = new NewtCameraMouseController(afrCorrectionChart3d);

        int size = 10;
        float x;
        float y;
        float z;

        Coord3d[] points = new Coord3d[size];

        Random r = new Random();
        r.setSeed(0);

        for(int i=0; i<size; i++){
            x = r.nextFloat() - 0.5f;
            y = r.nextFloat() - 0.5f;
            z = r.nextFloat() - 0.5f;
            points[i] = new Coord3d(x, y, z);
        }

        scatterAfr = new ScatterMultiColor(points, new ColorMapper(new ColorMapTransparent(), 0, 0));

        afrCorrectionChart3d.getScene().add(scatterStdDev, true);
    }

    private void drawStdDevChart(List<List<List<Double>>> filteredLoadDt) {

        if(scatterStdDev != null) {
            stdDevChart3d.getScene().remove(scatterStdDev, true);
        }

        float x;
        float y;
        float z;
        List<Coord3d> points = new ArrayList<>();

        float max = 0;
        float min = 0;

        // Create scatterAfr points
        for (int i = 0; i < filteredLoadDt.size(); i++) {
            for (int j = 0; j < filteredLoadDt.get(i).size(); j++) {
                for (int k = 0; k < filteredLoadDt.get(i).get(j).size(); k++) {
                    x = Kfkhfm.getStockXAxis()[i].floatValue();
                    y = Kfkhfm.getStockYAxis()[j].floatValue();
                    z = filteredLoadDt.get(i).get(j).get(k).floatValue();

                    if(z > max) {
                        max = z;
                    }

                    if(z < min) {
                        min = z;
                    }

                    points.add(new Coord3d(x, y, z));
                }
            }
        }

        float minMax;

        if(Math.abs(max) > Math.abs(min)) {
            minMax = Math.abs(max);
        } else {
            minMax = Math.abs(min);
        }

        // Create a drawable scatterAfr with a colormap
        scatterStdDev = new ScatterMultiColor(points.toArray(new Coord3d[0]), new ColorMapper(new ColorMapRainbow(), -minMax, minMax));
        scatterStdDev.setWidth(3);

        stdDevChart3d.getScene().add(scatterStdDev, true);
    }

    private void drawAfrCorrectionChart(List<List<List<Double>>> correctionsAfr, List<List<List<Double>>> meanAfr, List<List<List<double[]>>> modeAfr) {
        addArfCorrectionsScatter(correctionsAfr);
        addMeanArfCorrectionsScatter(meanAfr);
        addModeArfCorrectionsScatter(modeAfr);
    }

    private void addArfCorrectionsScatter(List<List<List<Double>>> correctionsAfr) {
        if(scatterAfr != null) {
            afrCorrectionChart3d.getScene().remove(scatterAfr, true);
        }

        float x;
        float y;
        float z;
        List<Coord3d> points = new ArrayList<>();

        float max = 0;
        float min = 0;

        // Create scatterAfr points
        for (int i = 0; i < correctionsAfr.size(); i++) {
            for (int j = 0; j < correctionsAfr.get(i).size(); j++) {
                for (int k = 0; k < correctionsAfr.get(i).get(j).size(); k++) {
                    x = Kfkhfm.getStockXAxis()[i].floatValue();
                    y = Kfkhfm.getStockYAxis()[j].floatValue();
                    z = correctionsAfr.get(i).get(j).get(k).floatValue();

                    if(z > max) {
                        max = z;
                    }

                    if(z < min) {
                        min = z;
                    }

                    points.add(new Coord3d(x, y, z));
                }
            }
        }

        float minMax;

        if(Math.abs(max) > Math.abs(min)) {
            minMax = Math.abs(max);
        } else {
            minMax = Math.abs(min);
        }

        // Create a drawable scatterAfr with a colormap
        scatterAfr = new ScatterMultiColor(points.toArray(new Coord3d[0]), new ColorMapper(new ColorMapRainbow(), -minMax, minMax));
        scatterAfr.setWidth(2);

        afrCorrectionChart3d.getScene().add(scatterAfr, true);
    }

    private void addMeanArfCorrectionsScatter(List<List<List<Double>>> meanAfr) {
        if(scatterMeanAfr != null) {
            afrCorrectionChart3d.getScene().remove(scatterMeanAfr, true);
        }

        float x;
        float y;
        float z;
        List<Coord3d> points = new ArrayList<>();

        // Create scatterAfr points
        for (int i = 0; i < meanAfr.size(); i++) {
            for (int j = 0; j < meanAfr.get(i).size(); j++) {
                for (int k = 0; k < meanAfr.get(i).get(j).size(); k++) {
                    x = Kfkhfm.getStockXAxis()[i].floatValue();
                    y = Kfkhfm.getStockYAxis()[j].floatValue();
                    z = meanAfr.get(i).get(j).get(k).floatValue();

                    points.add(new Coord3d(x, y, z));
                }
            }
        }

        // Create a drawable scatterAfr with a colormap
        scatterMeanAfr = new Scatter(points.toArray(new Coord3d[0]), org.jzy3d.colors.Color.RED);
        scatterMeanAfr.setWidth(5);

        afrCorrectionChart3d.getScene().add(scatterMeanAfr, true);
    }

    private void addModeArfCorrectionsScatter(List<List<List<double[]>>> modeAfr) {
        if(scatterModeAfr != null) {
            afrCorrectionChart3d.getScene().remove(scatterModeAfr, true);
        }

        float x;
        float y;
        float z;
        List<Coord3d> points = new ArrayList<>();

        Mean mean = new Mean();

        // Create scatterAfr points
        for (int i = 0; i < modeAfr.size(); i++) {
            for (int j = 0; j < modeAfr.get(i).size(); j++) {
                for (int k = 0; k < modeAfr.get(i).get(j).size(); k++) {
                    x = Kfkhfm.getStockXAxis()[i].floatValue();
                    y = Kfkhfm.getStockYAxis()[j].floatValue();
                    z = (float) mean.evaluate(modeAfr.get(i).get(j).get(k), 0, modeAfr.get(i).get(j).get(k).length) ;

                    points.add(new Coord3d(x, y, z));
                }
            }
        }

        // Create a drawable scatterAfr with a colormap
        scatterModeAfr = new Scatter(points.toArray(new Coord3d[0]), org.jzy3d.colors.Color.BLUE);
        scatterModeAfr.setWidth(5);

        afrCorrectionChart3d.getScene().add(scatterModeAfr, true);
    }
}
