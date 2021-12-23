package ui.view.kfmiop;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfmiop.Kfmiop;
import model.kfmirl.Kfmirl;
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
import ui.viewmodel.kfmiop.KfmiopViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class KfmiopUiManager implements OnTabSelectedListener {
    private MapTable kfmirl;
    private MapTable kfmiop;

    private MapAxis kfmiopXAxis;
    private MapAxis kfzwXAxis;

    private Chart kfmirlChart3d;
    private Chart kfmiopChart3d;

    private final KfmiopViewModel viewModel;

    public KfmiopUiManager() {
        viewModel = new KfmiopViewModel();
        viewModel.getKfzwXAxisPublishSubject().subscribe(new Observer<Double[]>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override
            public void onNext(@NonNull Double[] kfzwXAxis) {
                KfmiopUiManager.this.kfzwXAxis.setTableData(new Double[][]{kfzwXAxis});
            }

            @Override
            public void onError(@NonNull Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        viewModel.getKfmiopXAxisPublishSubject().subscribe(new Observer<Double[]>() {

            @Override
            public void onNext(Double[] kfmiopXAxis) {
                KfmiopUiManager.this.kfmiopXAxis.setTableData(new Double[][]{kfmiopXAxis});

                Map3d kfmirlMap3d = new Map3d();
                kfmirlMap3d.xAxis = (Double[]) kfmirl.getColumnHeaders();
                kfmirlMap3d.yAxis = kfmirl.getRowHeaders();
                kfmirlMap3d.zAxis = kfmirl.getData();

                Map3d kfmiopMap3d = new Map3d();
                kfmiopMap3d.xAxis = kfmiopXAxis;
                kfmiopMap3d.yAxis = Kfmiop.getYAxis();
                kfmiopMap3d.zAxis = Kfmiop.getMap();

                viewModel.cacluateKfmiop(kfmirlMap3d, kfmiopMap3d);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        viewModel.getKfmiopMapPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(Map3d map3d) {
                kfmiop.setColumnHeaders(map3d.xAxis);
                kfmiop.setRowHeaders(map3d.yAxis);
                kfmiop.setTableData(map3d.zAxis);
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

        JPanel kfmirlPanel = new JPanel();
        kfmirlPanel.setPreferredSize(new Dimension(700, 500));

        kfmirlPanel.add(getKfmirlMapPanel(), new GridBagLayout());

        mainPanel.add(kfmirlPanel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 1;
        constraints.gridy = 0;

        JPanel kfmiopPanel = new JPanel();
        kfmiopPanel.setPreferredSize(new Dimension(710, 500));

        kfmiopPanel.add(getKmfiopMapPanel(), new GridBagLayout());

        mainPanel.add(kfmiopPanel, constraints);

        viewModel.recalcuateKfmiopXAxis(kfmirl.getData());

        return mainPanel;
    }

    private JPanel getKfmirlMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        initKfmirlMap();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.WEST;
        c.insets.left = 52;
        c.insets.top = 128;

        panel.add(getHeader("KFMIRL (Input)",new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showKfmirlChart3d();
            }
        }),c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;
        c.insets.left = 0;
        c.insets.top = 8;

        JScrollPane scrollPane = kfmirl.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(715, 275));

        panel.add(scrollPane,c);

        return panel;
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

    private JPanel getKmfiopMapPanel() {
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());

        constraints.insets.left = 52;

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.ipadx = 0;
        constraints.insets.top = 16;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFZW X-Axis (Output)"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy =1;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets.top = 8;

        Double[][] kfzwXAxisValues = new Double[1][];
        kfzwXAxisValues[0] = Kfzw.getXAxis();

        kfzwXAxis = MapAxis.getMapAxis(kfzwXAxisValues);

        JScrollPane kfzwXAxisScrollPane = kfzwXAxis.getScrollPane();
        kfzwXAxisScrollPane.setPreferredSize(new Dimension(670, 20));

        mapPanel.add(kfzwXAxisScrollPane ,constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.ipadx = 0;
        constraints.insets.top = 16;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFMIOP/KFZWOP X-Axis (Output)"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets.top = 8;

        Double[][] kfmiopXAxisValues = new Double[1][];
        kfmiopXAxisValues[0] = Kfmiop.getXAxis();

        kfmiopXAxis = MapAxis.getMapAxis(kfmiopXAxisValues);

        JScrollPane kfmiopXAxisScrollPane = kfmiopXAxis.getScrollPane();
        kfmiopXAxisScrollPane.setPreferredSize(new Dimension(608, 20));

        mapPanel.add(kfmiopXAxisScrollPane ,constraints);

        constraints.insets.left = 0;
        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets.left = 84;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.WEST;

        mapPanel.add(getHeader("KFMIOP (Output)",new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showKfmiopChart3d();
            }
        }),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.ipadx = -50;
        constraints.insets.left = 0;
        constraints.fill = GridBagConstraints.EAST;
        constraints.anchor = GridBagConstraints.CENTER;

        kfmiop = MapTable.getMapTable(Kfmiop.getYAxis(), Kfmiop.getXAxis(), Kfmiop.getMap());

        JScrollPane kfmiopMapScrollPane = kfmiop.getScrollPane();
        kfmiopMapScrollPane.setPreferredSize(new Dimension(705, 275));

        mapPanel.add(kfmiopMapScrollPane,constraints);

        return mapPanel;
    }

    private void initKfmirlMap() {
        kfmirl = MapTable.getMapTable(Kfmirl.getStockKfmirlYAxis(), Kfmirl.getStockKfmirlXAxis(), Kfmirl.getStockKfmirlMap());
        kfmirl.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(Map3d map3d) {
                viewModel.recalcuateKfmiopXAxis(kfmirl.getData());
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void showKfmirlChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getKfmirlChart3d());
        jd.setVisible(true);
    }

    private JPanel getKfmirlChart3d() {
        initKfmirlChart3d();
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

        panel.add((Component) kfmirlChart3d.getCanvas(), c);

        return panel;
    }

    private void initKfmirlChart3d() {
        // Create a chart and add scatterAfr
        kfmirlChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        kfmirlChart3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        kfmirlChart3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        kfmirlChart3d.getAxeLayout().setXAxeLabel("Torque Request (mifa)");
        kfmirlChart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        kfmirlChart3d.getAxeLayout().setZAxeLabel("Specified Load (rlsol)");

        NewtCameraMouseController controller = new NewtCameraMouseController(kfmirlChart3d);

        Double[][] data = kfmirl.getData();

        Double[] xAxis = Kfmirl.getStockKfmirlXAxis();
        Double[] yAxis = Kfmirl.getStockKfmirlYAxis();

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

        kfmirlChart3d.getScene().add(surface, true);
    }

    private void showKfmiopChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getKfmiopChart3d());
        jd.setVisible(true);
    }

    private JPanel getKfmiopChart3d() {
        initKfmiopChart3d();
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

        panel.add((Component) kfmiopChart3d.getCanvas(), c);

        return panel;
    }

    private void initKfmiopChart3d() {
        // Create a chart and add scatterAfr
        kfmiopChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        kfmiopChart3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        kfmiopChart3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        kfmiopChart3d.getAxeLayout().setXAxeLabel("Actual Load (rl_w)");
        kfmiopChart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        kfmiopChart3d.getAxeLayout().setZAxeLabel("Actual Torque (mibas)");

        NewtCameraMouseController controller = new NewtCameraMouseController(kfmiopChart3d);

        Double[][] data = kfmiop.getData();

        Double[] xAxis = kfmiopXAxis.getData()[0];
        Double[] yAxis = Kfmiop.getYAxis();

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

        kfmiopChart3d.getScene().add(surface, true);
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
