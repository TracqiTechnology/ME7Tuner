package ui.view.kfzwop;


import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfzwop.Kfzwop;
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
import ui.viewmodel.kfzwop.KfzwopViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class KfzwopUiManager {

    private MapTable kfzwopIn;
    private MapTable kfzwopOut;

    private MapAxis kfzwopXAxis;

    private Chart inChart3d;
    private Chart inOut3d;

    private KfzwopViewModel viewModel;

    public KfzwopUiManager() {
        viewModel = new KfzwopViewModel();

        viewModel.getKfzwopMapPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(Map3d map3d) {
                kfzwopOut.setColumnHeaders(map3d.xAxis);
                kfzwopOut.setRowHeaders(map3d.yAxis);
                kfzwopOut.setTableData(map3d.data);
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
        kfzwopInPanel.setPreferredSize(new Dimension(700, 500));

        kfzwopInPanel.add(getKfzwopInMapPanel(), new GridBagLayout());

        mainPanel.add(kfzwopInPanel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 1;
        constraints.gridy = 0;

        JPanel kfzwopOutPanel = new JPanel();
        kfzwopOutPanel.setPreferredSize(new Dimension(700, 500));

        kfzwopOutPanel.add(getKfzwopOutMapPanel(), new GridBagLayout());

        mainPanel.add(kfzwopOutPanel, constraints);

        return mainPanel;
    }

    private JPanel getKfzwopInMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        initKfmirlMap();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets.top = 68;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("KFZWOP IN"),c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.ipadx = -50;
        c.insets.top = 8;
        c.fill = GridBagConstraints.EAST;
        c.anchor = GridBagConstraints.EAST;

        JScrollPane scrollPane = kfzwopIn.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(705, 275));

        panel.add(scrollPane,c);

        c.gridy = 2;
        c.ipadx = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(getIn3dButton(), c);

        return panel;
    }

    private JPanel getKfzwopOutMapPanel() {
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.ipadx = 0;
        constraints.insets.top = 16;
        constraints.insets.left = 54;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFZWOP X-Axis"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 8;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        initKfmirlXAxis();

        JScrollPane kfmiopXAxisScrollPane = kfzwopXAxis.getScrollPane();
        kfmiopXAxisScrollPane.setPreferredSize(new Dimension(608, 20));

        mapPanel.add(kfmiopXAxisScrollPane ,constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets.left = 0;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFZWOP OUT"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.ipadx = -50;
        constraints.fill = GridBagConstraints.EAST;
        constraints.anchor = GridBagConstraints.EAST;

        kfzwopOut = MapTable.getMapTable(Kfzwop.getStockYAxis(), Kfzwop.getStockXAxis(), Kfzwop.getStockMap());

        JScrollPane kfmiopMapScrollPane = kfzwopOut.getScrollPane();
        kfmiopMapScrollPane.setPreferredSize(new Dimension(705, 275));

        mapPanel.add(kfmiopMapScrollPane,constraints);

        constraints.gridy = 4;
        constraints.ipadx = 0;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(getOut3dButton(), constraints);

        return mapPanel;
    }

    private void initKfmirlXAxis() {
        Double[][] kfmiopXAxisValues = new Double[1][];
        kfmiopXAxisValues[0] = Kfzwop.getStockXAxis();
        kfzwopXAxis = MapAxis.getMapAxis(kfmiopXAxisValues);

        kfzwopXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {

            @Override
            public void onNext(Double[][] data) {
                Map3d map3d = new Map3d();
                map3d.xAxis = Kfzwop.getStockXAxis();
                map3d.yAxis = kfzwopIn.getRowHeaders();
                map3d.data = kfzwopIn.getData();
                viewModel.cacluateKfzwop(map3d, data[0]);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initKfmirlMap() {
        kfzwopIn = MapTable.getMapTable(Kfzwop.getStockYAxis(), Kfzwop.getStockXAxis(), Kfzwop.getStockMap());

        kfzwopIn.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(Map3d map3d) {
                viewModel.cacluateKfzwop(map3d, kfzwopXAxis.getData()[0]);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private JButton getIn3dButton() {
        JButton jButton = new JButton("3D Chart");
        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInChart3d();
            }
        });

        return jButton;
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

        Double[][] data = kfzwopIn.getData();

        Double[] xAxis = Kfzwop.getStockXAxis();
        Double[] yAxis = Kfzwop.getStockYAxis();

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

    private JButton getOut3dButton() {
        JButton jButton = new JButton("3D Chart");
        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOutChart3d();
            }
        });

        return jButton;
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

        Double[][] data = kfzwopOut.getData();

        Double[] xAxis = kfzwopXAxis.getData()[0];
        Double[] yAxis = Kfzwop.getStockYAxis();

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
}
