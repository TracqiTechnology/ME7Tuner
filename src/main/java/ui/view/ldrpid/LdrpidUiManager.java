package ui.view.ldrpid;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfldimx.Kfldimx;
import model.ldrpid.Kfldrl;
import model.ldrpid.LdrpidCalculator;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import parser.me7log.Me7LogParser;
import preferences.filechooser.FileChooserPreferences;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.view.color.ColorMapGreenYellowRed;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LdrpidUiManager {
    private LdrpidCalculator.LdrpidResult ldrpidResult;

    private MapTable nonLinearTable;
    private MapTable linearTable;
    private MapTable kfldrlTable;
    private MapTable kfldimxTable;

    private MapAxis kfldimxXAxis;

    private Chart inNonLinear3d;
    private Chart linearChart;
    private Chart kfldrlChart;

    public JPanel getPanel() {

        GridBagConstraints constraints = new GridBagConstraints();
        JPanel mainPanel = new JPanel();

        constraints.gridx = 0;
        constraints.gridy = 0;

        JPanel nonLinearPanel = new JPanel();
        nonLinearPanel.setPreferredSize(new Dimension(710, 310));

        nonLinearPanel.add(getNonLinearMapPanel(), new GridBagLayout());

        mainPanel.add(nonLinearPanel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;

        JPanel linearPanel = new JPanel();
        linearPanel.setPreferredSize(new Dimension(710, 310));

        linearPanel.add(getLinearMapPanel(), new GridBagLayout());

        mainPanel.add(linearPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        JPanel kfldrlPanel = new JPanel();
        linearPanel.setPreferredSize(new Dimension(710, 310));

        kfldrlPanel.add(getKflDrlMapPanel(), new GridBagLayout());

        mainPanel.add(kfldrlPanel, constraints);

        constraints.gridx = 2;
        constraints.gridy = 1;

        JPanel kfldimxPanel = new JPanel();
        linearPanel.setPreferredSize(new Dimension(710, 310));

        kfldimxPanel.add(getKfldimxMapPanel(), new GridBagLayout());

        mainPanel.add(kfldimxPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        mainPanel.add(getLogsButton(mainPanel), constraints);

        return mainPanel;
    }

    private JPanel getHeader(String title, ActionListener chartActionListener) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;

        JLabel label = new JLabel(title);
        panel.add(label,c);

        c.gridx = 1;

        java.net.URL imgURL = getClass().getResource("/insert_chart.png");
        ImageIcon icon = new ImageIcon(imgURL, "");
        JButton button = new JButton(icon);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.addActionListener(chartActionListener);
        panel.add(button, c);

        return panel;
    }

    private void initKfldimxAxis() {
        Double[][] kfldImxXAxisValues = new Double[1][];
        kfldImxXAxisValues[0] = Kfldimx.getStockXAxis();
        kfldimxXAxis = MapAxis.getMapAxis(kfldImxXAxisValues);

        kfldimxXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {

            @Override
            public void onNext(Double[][] data) {
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private JPanel getKfldimxMapPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 55;
        c.anchor = GridBagConstraints.WEST;

        panel.add(new JLabel("KFLDIMX X-Axis"),c);

        c.gridx = 0;
        c.gridy = 1;
        c.insets.left = -165;
        c.anchor = GridBagConstraints.CENTER;

        initKfldimxAxis();

        JScrollPane xAxisScrollPane = kfldimxXAxis.getScrollPane();
        xAxisScrollPane.setPreferredSize(new Dimension(443, 20));

        panel.add(xAxisScrollPane ,c);

        c.gridx = 0;
        c.gridy = 2;
        c.insets.left = 55;
        c.anchor = GridBagConstraints.WEST;

        panel.add(getHeader("KFLDIMX", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNonLinearChart3d();
            }
        }), c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 3;
        c.insets.left = 0;
        c.anchor = GridBagConstraints.EAST;

        initKflimxMap();

        JScrollPane scrollPane = kfldimxTable.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(710, 275));

        panel.add(scrollPane,c);

        return panel;
    }

    private JPanel getKflDrlMapPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 55;
        c.insets.top = 35;
        c.anchor = GridBagConstraints.WEST;

        panel.add(getHeader("KFLDRL", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showKfldrlChart3d();
            }
        }), c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.left = 0;
        c.insets.top = 0;
        c.anchor = GridBagConstraints.EAST;

        initKfldrlMap();

        JScrollPane scrollPane = kfldrlTable.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(710, 275));

        panel.add(scrollPane,c);

        return panel;
    }

    private JPanel getLogsButton(JPanel parent) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        JButton button = new JButton("Load Logs");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setCurrentDirectory(FileChooserPreferences.getDirectory());

                int returnValue = fc.showOpenDialog(parent);

                if (returnValue == JFileChooser.APPROVE_OPTION) {

                    final JDialog dlg = new JDialog();
                    JProgressBar dpb = new JProgressBar();
                    dpb.setIndeterminate(true);
                    dlg.add(BorderLayout.CENTER, dpb);
                    dlg.add(BorderLayout.NORTH, new JLabel("Progress..."));
                    dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    dlg.setSize(300, 75);
                    dlg.setLocationRelativeTo(null);
                    dlg.setVisible(true);

                    FileChooserPreferences.setDirectory(fc.getSelectedFile().getParentFile());

                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        public Void doInBackground() {
                            System.out.println("Starting Parse...");
                            Me7LogParser parser = new Me7LogParser();
                            Map<String, List<Double>> values = parser.parseLogDirectory(Me7LogParser.LogType.LDRPID, fc.getSelectedFile());
                            System.out.println("Parse Complete...");

                            LdrpidCalculator calculator = new LdrpidCalculator();
                            ldrpidResult = calculator.caclulateLdrpid(values);
                            return null;
                        }

                        @Override
                        public void done() {
                            nonLinearTable.setMap(ldrpidResult.nonLinearOutput);
                            linearTable.setMap(ldrpidResult.linearOutput);
                            kfldrlTable.setMap(ldrpidResult.kfldrl);

                            Double[][] kfldImxXAxisValues = new Double[1][];
                            kfldImxXAxisValues[0] = ldrpidResult.kfldimx.xAxis;
                            kfldimxXAxis.setTableData(kfldImxXAxisValues);
                            kfldimxTable.setMap(ldrpidResult.kfldimx);

                            dlg.setVisible(false);
                        }
                    };

                    worker.execute();
                }
            }
        });

        c.gridx = 0;
        c.gridy = 0;
        c.insets.top = 16;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(button, c);

        return panel;
    }

    private JPanel getNonLinearMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 55;
        c.anchor = GridBagConstraints.WEST;

        panel.add(getHeader("Non Linear Boost", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNonLinearChart3d();
            }
        }), c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.left = 0;
        c.anchor = GridBagConstraints.EAST;

        initNonLinearMap();

        JScrollPane scrollPane = nonLinearTable.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(710, 275));

        panel.add(scrollPane,c);

        return panel;
    }

    private JPanel getLinearMapPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 55;
        c.anchor = GridBagConstraints.WEST;

        panel.add(getHeader("Linear Boost", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showLinearChart3d();
            }
        }), c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 3;
        c.insets.left = 0;
        c.anchor = GridBagConstraints.EAST;

        initLinearMap();

        JScrollPane kfmiopMapScrollPane = linearTable.getScrollPane();
        kfmiopMapScrollPane.setPreferredSize(new Dimension(710, 275));

        panel.add(kfmiopMapScrollPane,c);

        return panel;
    }

    private void initKflimxMap() {
        kfldimxTable = MapTable.getMapTable(Kfldimx.getStockYAxis(), Kfldimx.getStockXAxis(), Kfldimx.getEmptyMap());

        kfldimxTable.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(Map3d map3d) {

            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initKfldrlMap() {
        kfldrlTable = MapTable.getMapTable(Kfldrl.getStockYAxis(), Kfldrl.getStockXAxis(), Kfldrl.getEmptyMap());

        kfldrlTable.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(Map3d map3d) {

            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initNonLinearMap() {
        nonLinearTable = MapTable.getMapTable(Kfldrl.getStockYAxis(), Kfldrl.getStockXAxis(), Kfldrl.getEmptyMap());

        nonLinearTable.getPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(Map3d map3d) {
                Map3d linearMap3d = LdrpidCalculator.calculateLinearTable(map3d.data);
                Map3d kfldrlMap3d = LdrpidCalculator.calculateKfldrl(map3d.data, linearMap3d.data);
                Map3d kfldimxMap3d = LdrpidCalculator.calculateKfldimx(map3d.data, linearMap3d.data);

                linearTable.setMap(linearMap3d);
                kfldrlTable.setMap(kfldrlMap3d);

                Double[][] kfldImxXAxisValues = new Double[1][];
                kfldImxXAxisValues[0] = kfldimxMap3d.xAxis;
                kfldimxXAxis.setTableData(kfldImxXAxisValues);
                kfldimxTable.setMap(kfldimxMap3d);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initLinearMap() {
        linearTable = MapTable.getMapTable(Kfldrl.getStockYAxis(), Kfldrl.getStockXAxis(), Kfldrl.getEmptyMap());

        linearTable.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(Map3d map3d) {

            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void showNonLinearChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getInNonLinear3d());
        jd.setVisible(true);
    }

    private JPanel getInNonLinear3d() {
        initNonLinearChart3d();
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

        panel.add((Component) inNonLinear3d.getCanvas(), c);

        return panel;
    }

    private void initNonLinearChart3d() {
        // Create a chart and add scatterAfr
        inNonLinear3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        inNonLinear3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        inNonLinear3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        inNonLinear3d.getAxeLayout().setXAxeLabel("Duty Cycle");
        inNonLinear3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        inNonLinear3d.getAxeLayout().setZAxeLabel("Relative Boost (psi)");

        NewtCameraMouseController controller = new NewtCameraMouseController(inNonLinear3d);

        Double[][] data = ldrpidResult.nonLinearOutput.data;

        Double[] xAxis = Kfldrl.getStockXAxis();
        Double[] yAxis = Kfldrl.getStockYAxis();

        ArrayList<Polygon> polygons = new ArrayList<>();
        for(int i = 0; i < xAxis.length -1; i++){
            for(int j = 0; j < yAxis.length -1; j++){
                org.jzy3d.plot3d.primitives.Polygon polygon = new org.jzy3d.plot3d.primitives.Polygon();
                polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(xAxis[i], yAxis[j], data[j][i])));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i], yAxis[j + 1], data[j + 1][i]) ));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i + 1], yAxis[j + 1], data[j+1][i+1]) ));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i + 1], yAxis[j], data[j][i+1])));
                polygons.add(polygon);
            }
        }

        // Create the object to represent the function over the given range.
        final org.jzy3d.plot3d.primitives.Shape surface = new org.jzy3d.plot3d.primitives.Shape(polygons);
        surface.setColorMapper(new ColorMapper(new ColorMapGreenYellowRed(), surface.getBounds().getZmin(), surface.getBounds().getZmax()));
        surface.setFaceDisplayed(true);
        surface.setWireframeColor(Color.BLACK);
        surface.setWireframeDisplayed(true);

        inNonLinear3d.getScene().add(surface, true);
    }

    private void showLinearChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getLinearChart3d());
        jd.setVisible(true);
    }

    private JPanel getLinearChart3d() {
        initLinearChart3d();
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

        panel.add((Component) linearChart.getCanvas(), c);

        return panel;
    }

    private void initLinearChart3d() {
        // Create a chart and add scatterAfr
        linearChart = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        linearChart.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        linearChart.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        linearChart.getAxeLayout().setXAxeLabel("Duty Cycle");
        linearChart.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        linearChart.getAxeLayout().setZAxeLabel("Relative Boost (psi)");

        NewtCameraMouseController controller = new NewtCameraMouseController(linearChart);

        Double[][] data = ldrpidResult.linearOutput.data;

        Double[] xAxis = Kfldrl.getStockXAxis();
        Double[] yAxis = Kfldrl.getStockYAxis();

        ArrayList<Polygon> polygons = new ArrayList<>();
        for(int i = 0; i < xAxis.length -1; i++){
            for(int j = 0; j < yAxis.length -1; j++){
                org.jzy3d.plot3d.primitives.Polygon polygon = new org.jzy3d.plot3d.primitives.Polygon();
                polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(xAxis[i], yAxis[j], data[j][i])));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i], yAxis[j + 1], data[j + 1][i]) ));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i + 1], yAxis[j + 1], data[j+1][i+1]) ));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i + 1], yAxis[j], data[j][i+1])));
                polygons.add(polygon);
            }
        }

        // Create the object to represent the function over the given range.
        final org.jzy3d.plot3d.primitives.Shape surface = new org.jzy3d.plot3d.primitives.Shape(polygons);
        surface.setColorMapper(new ColorMapper(new ColorMapGreenYellowRed(), surface.getBounds().getZmin(), surface.getBounds().getZmax()));
        surface.setFaceDisplayed(true);
        surface.setWireframeColor(Color.BLACK);
        surface.setWireframeDisplayed(true);

        linearChart.getScene().add(surface, true);
    }

    private void showKfldrlChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getKfldrlChart3d());
        jd.setVisible(true);
    }

    private JPanel getKfldrlChart3d() {
        initKfldrlChart3d();
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

        panel.add((Component) kfldrlChart.getCanvas(), c);

        return panel;
    }

    private void initKfldrlChart3d() {
        // Create a chart and add scatterAfr
        kfldrlChart = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        kfldrlChart.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        kfldrlChart.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        kfldrlChart.getAxeLayout().setXAxeLabel("Duty Cycle");
        kfldrlChart.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        kfldrlChart.getAxeLayout().setZAxeLabel("Duty Cycle");

        NewtCameraMouseController controller = new NewtCameraMouseController(kfldrlChart);

        Double[][] data = ldrpidResult.kfldrl.data;

        Double[] xAxis = Kfldrl.getStockXAxis();
        Double[] yAxis = Kfldrl.getStockYAxis();

        ArrayList<Polygon> polygons = new ArrayList<>();
        for(int i = 0; i < xAxis.length -1; i++){
            for(int j = 0; j < yAxis.length -1; j++){
                org.jzy3d.plot3d.primitives.Polygon polygon = new org.jzy3d.plot3d.primitives.Polygon();
                polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(xAxis[i], yAxis[j], data[j][i])));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i], yAxis[j + 1], data[j + 1][i]) ));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i + 1], yAxis[j + 1], data[j+1][i+1]) ));
                polygon.add(new org.jzy3d.plot3d.primitives.Point( new Coord3d(xAxis[i + 1], yAxis[j], data[j][i+1])));
                polygons.add(polygon);
            }
        }

        // Create the object to represent the function over the given range.
        final org.jzy3d.plot3d.primitives.Shape surface = new org.jzy3d.plot3d.primitives.Shape(polygons);
        surface.setColorMapper(new ColorMapper(new ColorMapGreenYellowRed(), surface.getBounds().getZmin(), surface.getBounds().getZmax()));
        surface.setFaceDisplayed(true);
        surface.setWireframeColor(Color.BLACK);
        surface.setWireframeDisplayed(true);

        kfldrlChart.getScene().add(surface, true);
    }
}
