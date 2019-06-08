package ui.view.closedloopfueling.kfkhfm;


import derivative.Derivative;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfkhfm.Kfkhfm;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.AWTCameraMouseController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.AbstractColorMap;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.ScatterMultiColor;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.legends.colorbars.AWTColorbarLegend;
import preferences.closedloopfueling.ClosedLoopFuelingLogFilterPreferences;
import preferences.filechooser.FileChooserPreferences;
import ui.view.closedloopfueling.ClosedLoopMe7LogFilterConfigPanel;
import ui.view.closedloopfueling.kfkhfm.colormap.ColorMapTransparent;
import ui.viewmodel.KfkhfmViewModel;
import ui.viewmodel.closedloopfueling.ClosedLoopFuelingMe7LogViewModel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ClosedLoopKfkhfmMe7LogUiManager {

    private Chart stdDevChart3d;
    private ScatterMultiColor scatterStdDev;

    private JPanel closedLoopLogPanel;
    private JLabel fileLabel;
    private ClosedLoopFuelingMe7LogViewModel closedLoopViewModel;

    private Map3d kfkhfm;
    private File me7LogFile;

    public ClosedLoopKfkhfmMe7LogUiManager() {
        closedLoopViewModel = ClosedLoopFuelingMe7LogViewModel.getInstance();
        closedLoopViewModel.getPublishSubject().subscribe(new Observer<Map<String, List<Double>>>() {
            @Override
            public void onNext(Map<String, List<Double>> me7LogMap) {
                if(kfkhfm != null) {
                    drawChart(Derivative.getDtMap3d(me7LogMap, kfkhfm));
                }
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        KfkhfmViewModel kfkhfmViewModel = KfkhfmViewModel.getInstance();
        kfkhfmViewModel.getKfkhfmBehaviorSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(Map3d kfkhfm) {
                ClosedLoopKfkhfmMe7LogUiManager.this.kfkhfm = kfkhfm;
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public JPanel getMe7LogPanel() {

        closedLoopLogPanel = new JPanel();
        closedLoopLogPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;

        c.gridwidth = 1;
        c.gridheight = 1;

        c.weightx = 1.0;
        c.weighty = 0.95;

        c.gridx = 0;
        c.gridy = 0;

        closedLoopLogPanel.add(getChartPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.05;
        closedLoopLogPanel.add(getActionPanel(), c);

        return closedLoopLogPanel;
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

        panel.add((Component)stdDevChart3d.getCanvas(), c);

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

        JButton button = getConfigureFilterButton();
        panel.add(button, c);

        c.weightx = 0.9;
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;

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

                    if(this.me7LogFile != null) {
                        loadMe7File(this.me7LogFile);
                    }
                }
            }
        });

        return button;
    }

    private JButton getFileButton() {
        JButton button = new JButton("Load Logs");

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
        // Create a chart and add scatterAfr
        stdDevChart3d = AWTChartComponentFactory.chart(Quality.Advanced, IChartComponentFactory.Toolkit.swing);
        stdDevChart3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        stdDevChart3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        stdDevChart3d.getAxeLayout().setXAxeLabel("Engine Load (rl_w)");
        stdDevChart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        stdDevChart3d.getAxeLayout().setZAxeLabel("dMAFv/dt");

        AWTCameraMouseController controller = new AWTCameraMouseController(stdDevChart3d);
        Component canvas = (Component) stdDevChart3d.getCanvas();
        canvas.addMouseListener(controller);
        canvas.addMouseMotionListener(controller);
        canvas.addMouseWheelListener(controller);

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

    private void drawChart(List<List<List<Double>>> filteredLoadDt) {
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
}
