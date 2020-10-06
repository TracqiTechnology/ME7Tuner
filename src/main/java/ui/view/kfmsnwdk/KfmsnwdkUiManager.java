package ui.view.kfmsnwdk;


import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.Inverse;
import math.map.Map3d;
import model.kfmsnwdk.Kfmsnwdk;
import model.kfwdkmsn.Kfwdkmsn;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import ui.map.map.MapTable;
import ui.view.color.ColorMapGreenYellowRed;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

public class KfmsnwdkUiManager {

    private MapTable kfmsnwdk;
    private MapTable kfwdkmsn;

    private Chart kfmsnwdkChart3d;
    private Chart KfwdkmsnChart3d;

    public JPanel getPanel() {

        GridBagConstraints constraints = new GridBagConstraints();
        JPanel mainPanel = new JPanel();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 0;
        constraints.gridy = 0;

        JPanel kfmsnwdkPanel = new JPanel();
        kfmsnwdkPanel.setPreferredSize(new Dimension(700, 500));

        kfmsnwdkPanel.add(getKfmsnwdkMapPanel(), new GridBagLayout());

        mainPanel.add(kfmsnwdkPanel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 1;
        constraints.gridy = 0;

        JPanel kfwdkmsnPanel = new JPanel();
        kfwdkmsnPanel.setPreferredSize(new Dimension(700, 500));

        kfwdkmsnPanel.add(getKfwdkmsnMapPanel(), new GridBagLayout());

        mainPanel.add(kfwdkmsnPanel, constraints);

        kfmsnwdk.getPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override
            public void onNext(@NonNull Map3d map3d) {
                Map3d result = Map3d.transpose(Inverse.calculateInverse(Map3d.transpose(new Map3d(map3d)), Map3d.transpose(new Map3d(kfwdkmsn.getMap3d()))));
                for(Double[] array: result.data) {
                    System.out.println(Arrays.toString(array));
                }
                kfwdkmsn.setMap(result);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        return mainPanel;
    }

    private JPanel getKfwdkmsnMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        initKfwdkmsnMap();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets.left = 52;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.WEST;

        panel.add(getHeader("KFWDKMSN (Output)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showKfwdkmsnChart3d();
            }
        }),c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.ipadx = -50;
        c.insets.left = 0;
        c.fill = GridBagConstraints.EAST;
        c.anchor = GridBagConstraints.EAST;

        JScrollPane scrollPane = kfwdkmsn.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(705, 275));

        panel.add(scrollPane,c);

        return panel;
    }

    private JPanel getKfmsnwdkMapPanel() {
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.left = 458;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.WEST;

        mapPanel.add(getHeader("KFMSNWDK (Input)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showKfmsnwdkChart3d();
            }
        }),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.ipadx = -50;
        constraints.insets.left = 400;
        constraints.fill = GridBagConstraints.EAST;
        constraints.anchor = GridBagConstraints.EAST;

        kfmsnwdk = MapTable.getMapTable(Kfmsnwdk.getYAxis(), Kfmsnwdk.getXAxis(), Kfmsnwdk.getMap());

        JScrollPane kfmiopMapScrollPane = kfmsnwdk.getScrollPane();
        kfmiopMapScrollPane.setPreferredSize(new Dimension(705, 275));

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

    private void initKfwdkmsnMap() {
        kfwdkmsn = MapTable.getMapTable(Kfwdkmsn.getYAxis(), Kfwdkmsn.getXAxis(), Kfwdkmsn.getMap());

        kfwdkmsn.getPublishSubject().subscribe(new Observer<Map3d>() {

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

    private void showKfmsnwdkChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getKfmsnwdkChart3d());
        jd.setVisible(true);
    }

    private JPanel getKfmsnwdkChart3d() {
        initKfmsnwdkChart3d();
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

        panel.add((Component) kfmsnwdkChart3d.getCanvas(), c);

        return panel;
    }

    private void initKfmsnwdkChart3d() {
        // Create a chart and add scatterAfr
        kfmsnwdkChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        kfmsnwdkChart3d.getAxeLayout().setMainColor(Color.BLACK);
        kfmsnwdkChart3d.getView().setBackgroundColor(Color.WHITE);
        kfmsnwdkChart3d.getAxeLayout().setXAxeLabel("Engine RPM (nmot)");
        kfmsnwdkChart3d.getAxeLayout().setYAxeLabel("Throttle Position");
        kfmsnwdkChart3d.getAxeLayout().setZAxeLabel("kg/hr");

        NewtCameraMouseController controller = new NewtCameraMouseController(kfmsnwdkChart3d);

        Double[][] data = kfmsnwdk.getData();

        Double[] xAxis = Kfmsnwdk.getXAxis();
        Double[] yAxis = Kfmsnwdk.getYAxis();

        ArrayList<Polygon> polygons = new ArrayList<>();
        for(int i = 0; i < xAxis.length -1; i++){
            for(int j = 0; j < yAxis.length -1; j++){
                Polygon polygon = new Polygon();
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

        kfmsnwdkChart3d.getScene().add(surface, true);
    }

    private void showKfwdkmsnChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getKfwdkmsnChart3d());
        jd.setVisible(true);
    }

    private JPanel getKfwdkmsnChart3d() {
        initKfwdkmsnChart3d();
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

        panel.add((Component) KfwdkmsnChart3d.getCanvas(), c);

        return panel;
    }

    private void initKfwdkmsnChart3d() {
        // Create a chart and add scatterAfr
        KfwdkmsnChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        KfwdkmsnChart3d.getAxeLayout().setMainColor(Color.BLACK);
        KfwdkmsnChart3d.getView().setBackgroundColor(Color.WHITE);
        KfwdkmsnChart3d.getAxeLayout().setXAxeLabel("Engine RPM (nmot)");
        KfwdkmsnChart3d.getAxeLayout().setYAxeLabel("kg/hr");
        KfwdkmsnChart3d.getAxeLayout().setZAxeLabel("Throttle Position");

        NewtCameraMouseController controller = new NewtCameraMouseController(KfwdkmsnChart3d);

        Double[][] data = kfwdkmsn.getData();

        Double[] xAxis = Kfwdkmsn.getXAxis();
        Double[] yAxis = Kfwdkmsn.getYAxis();

        ArrayList<Polygon> polygons = new ArrayList<>();
        for(int i = 0; i < xAxis.length -1; i++){
            for(int j = 0; j < yAxis.length -1; j++){
                Polygon polygon = new Polygon();
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

        KfwdkmsnChart3d.getScene().add(surface, true);
    }
}
