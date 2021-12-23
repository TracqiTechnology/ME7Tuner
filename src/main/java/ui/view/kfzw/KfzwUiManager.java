package ui.view.kfzw;


import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfzw.Kfzw;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.view.color.ColorMapGreenYellowRed;
import ui.view.listener.OnTabSelectedListener;
import ui.viewmodel.kfzw.KfzwViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class KfzwUiManager implements OnTabSelectedListener {

    private MapTable kfzwIn;
    private MapTable kfzwOut;

    private MapAxis kfzwXAxis;

    private Chart inChart3d;
    private Chart inOut3d;

    private final KfzwViewModel viewModel;

    public KfzwUiManager() {
        viewModel = new KfzwViewModel();

        viewModel.getKfzwMapPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(Map3d map3d) {
                kfzwOut.setColumnHeaders(map3d.xAxis);
                kfzwOut.setRowHeaders(map3d.yAxis);
                kfzwOut.setTableData(map3d.zAxis);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public JPanel getPanel() {

        GridBagConstraints constraints = new GridBagConstraints();
        JPanel mainPanel = new JPanel();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 0;
        constraints.gridy = 0;

        JPanel kfzwopInPanel = new JPanel();
        kfzwopInPanel.setPreferredSize(new Dimension(710, 500));

        kfzwopInPanel.add(getInMapPanel(), new GridBagLayout());

        mainPanel.add(kfzwopInPanel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 1;
        constraints.gridy = 0;

        JPanel kfzwopOutPanel = new JPanel();
        kfzwopOutPanel.setPreferredSize(new Dimension(710, 500));

        kfzwopOutPanel.add(getOutMapPanel(), new GridBagLayout());

        mainPanel.add(kfzwopOutPanel, constraints);

        return mainPanel;
    }

    private JPanel getInMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        initMap();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 52;
        c.insets.top = 68;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.WEST;

        panel.add(getHeader("KFZW (Input)",new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInChart3d();
            }
        }),c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.left = 0;
        c.insets.top = 8;
        c.fill = GridBagConstraints.EAST;
        c.anchor = GridBagConstraints.EAST;

        JScrollPane scrollPane = kfzwIn.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(710, 275));

        panel.add(scrollPane,c);

        return panel;
    }

    private JPanel getOutMapPanel() {
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.ipadx = 0;
        constraints.insets.top = 16;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFZW X-Axis (Input)"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 8;
        constraints.insets.left = 52;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        initXAxis();

        JScrollPane xAxisScrollPane = kfzwXAxis.getScrollPane();
        xAxisScrollPane.setPreferredSize(new Dimension(665, 20));

        mapPanel.add(xAxisScrollPane ,constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets.left = 58;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.WEST;

        mapPanel.add(getHeader("KFZW (Output)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOutChart3d();
            }
        }),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets.left = 0;
        constraints.fill = GridBagConstraints.EAST;
        constraints.anchor = GridBagConstraints.EAST;

        kfzwOut = MapTable.getMapTable(Kfzw.getYAxis(), Kfzw.getXAxis(), Kfzw.getMap());

        JScrollPane kfmiopMapScrollPane = kfzwOut.getScrollPane();
        kfmiopMapScrollPane.setPreferredSize(new Dimension(710, 275));

        mapPanel.add(kfmiopMapScrollPane,constraints);

        return mapPanel;
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

    private void initXAxis() {
        Double[][] kfmiXAxisValues = new Double[1][];
        kfmiXAxisValues[0] = Kfzw.getXAxis();
        kfzwXAxis = MapAxis.getMapAxis(kfmiXAxisValues);

        kfzwXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {

            @Override
            public void onNext(Double[][] data) {
                Map3d map3d = new Map3d();
                map3d.xAxis = Kfzw.getXAxis();
                map3d.yAxis = kfzwIn.getRowHeaders();
                map3d.zAxis = kfzwIn.getData();

                try {
                    viewModel.calculateKfzw(map3d, data[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                 }
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initMap() {
        kfzwIn = MapTable.getMapTable(Kfzw.getYAxis(), Kfzw.getXAxis(), Kfzw.getMap());

        kfzwIn.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(Map3d map3d) {
                viewModel.calculateKfzw(map3d, kfzwXAxis.getData()[0]);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void showInChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getInChart3d());
        jd.setVisible(true);
    }

    private JPanel getInChart3d() {
        initInChart3d();
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

        panel.add((Component) inChart3d.getCanvas(), c);

        return panel;
    }

    private void initInChart3d() {
        // Create a chart and add scatterAfr
        inChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        inChart3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        inChart3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        inChart3d.getAxeLayout().setXAxeLabel("Engine Load");
        inChart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        inChart3d.getAxeLayout().setZAxeLabel("Ignition Advance");

        NewtCameraMouseController controller = new NewtCameraMouseController(inChart3d);

        Double[][] data = kfzwIn.getData();

        Double[] xAxis = Kfzw.getXAxis();
        Double[] yAxis = Kfzw.getYAxis();

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

        inChart3d.getScene().add(surface, true);
    }

    private void showOutChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getOutChart3d());
        jd.setVisible(true);
    }

    private JPanel getOutChart3d() {
        initOutChart3d();
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

        panel.add((Component) inOut3d.getCanvas(), c);

        return panel;
    }

    private void initOutChart3d() {
        // Create a chart and add scatterAfr
        inOut3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        inOut3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        inOut3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        inOut3d.getAxeLayout().setXAxeLabel("Engine Load");
        inOut3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        inOut3d.getAxeLayout().setZAxeLabel("Ignition Advance");

        NewtCameraMouseController controller = new NewtCameraMouseController(inOut3d);

        Double[][] data = kfzwOut.getData();

        Double[] xAxis = kfzwXAxis.getData()[0];
        Double[] yAxis = Kfzw.getYAxis();

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

        inOut3d.getScene().add(surface, true);
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
