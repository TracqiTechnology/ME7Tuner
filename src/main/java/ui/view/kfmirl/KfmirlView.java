package ui.view.kfmirl;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfmirl.Kfmirl;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import parser.xdf.TableDefinition;
import preferences.bin.BinFilePreferences;
import preferences.kfmiop.KfmiopPreferences;
import preferences.kfmirl.KfmirlPreferences;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.view.color.ColorMapGreenYellowRed;
import ui.view.listener.OnTabSelectedListener;
import ui.view.map.MapPickerDialog;
import ui.viewmodel.kfmirl.KfmirlViewModel;
import writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

public class KfmirlView implements OnTabSelectedListener {
    private final MapTable kfmirl = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);
    private final MapTable kfmiop = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);

    private final MapAxis kfmiopXAxis =  MapAxis.getMapAxis(new Double[1][0]);

    private Chart kfmirlChart3d;
    private Chart kfmiopChart3d;

    private JPanel panel;
    private JLabel kfmirlFileLabel;
    private JLabel kfmiopFileLabel;

    private final KfmirlViewModel viewModel;

    private boolean kfmiopInitialized;

    public KfmirlView() {
        viewModel = new KfmirlViewModel();
    }

    public JPanel getPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.top = -95;

        panel.add(getKfmiopMapPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets.top = 0;
        constraints.insets.left = 20;

        panel.add(getKmfirlMapPanel(), constraints);

        initViewModel();

        return panel;
    }

    private JPanel getKfmiopMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        initKfmiopMap();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.top = 16;

        panel.add(new JLabel("KFMIOP X-Axis (Output)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 0;

        JScrollPane kfzwXAxisScrollPane = kfmiopXAxis.getScrollPane();
        kfzwXAxisScrollPane.setPreferredSize(new Dimension(615, 20));
        panel.add(kfzwXAxisScrollPane, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(getHeader("KFMIOP (Input)",new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showKfmirlChart3d();
            }
        }),constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        JScrollPane scrollPane = kfmiop.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(660, 275));

        panel.add(scrollPane,constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets.top = 16;

        panel.add(getKfmiopDefinitionButton(),constraints);

        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.insets.top = 0;

        kfmiopFileLabel = new JLabel("No Map Selected");
        panel.add(kfmiopFileLabel, constraints);

        return panel;
    }

    private JPanel getKmfirlMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("KFMIRL (Output)",new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showKfmiopChart3d();
            }
        }),constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        JScrollPane mapScrollPane = kfmirl.getScrollPane();
        mapScrollPane.setPreferredSize(new Dimension(715, 275));

        panel.add(mapScrollPane,constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets.top = 16;

        panel.add(getKfmirlDefinitionButton(),constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets.top = 0;

        kfmirlFileLabel = new JLabel("No Map Selected");
        panel.add(kfmirlFileLabel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets.top = 16;

        panel.add(getFileButton(), constraints);

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

    private void initKfmiopMap() {
        kfmiop.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(@NonNull Map3d map3d) {
                Map3d kfmiop = new Map3d(kfmiopXAxis.getData()[0], map3d.yAxis, map3d.zAxis);
                viewModel.calculateKfmirl(kfmiop);
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        kfmiopXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Double[][] doubles) {
                kfmiop.setColumnHeaders(kfmiopXAxis.getData()[0]);
                Map3d kfmiopInput = new Map3d((Double[]) kfmiop.getColumnHeaders(), kfmiop.getRowHeaders(), kfmiop.getData());
                viewModel.calculateKfmirl(kfmiopInput);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initViewModel() {
        viewModel.register(new Observer<KfmirlViewModel.KfmirlModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull KfmirlViewModel.KfmirlModel kfmirlModel) {
                if(kfmirlModel.getKfmiop() != null && !kfmiopInitialized) {
                    Map3d kfmiopMap = kfmirlModel.getKfmiop().snd;
                    kfmiop.setColumnHeaders(kfmiopMap.xAxis);
                    kfmiop.setRowHeaders(kfmiopMap.yAxis);
                    kfmiop.setTableData(kfmiopMap.zAxis);

                    Double[][] xAxis = new Double[1][];
                    xAxis[0] = kfmiopMap.xAxis;
                    kfmiopXAxis.setTableData(xAxis);
                    kfmiopInitialized = true;
                }

                if(kfmirlModel.getKfmirl() != null && kfmirlModel.getKfmirl().fst != null) {
                    kfmirlFileLabel.setText(kfmirlModel.getKfmirl().fst.getTableName());
                }

                if(kfmirlModel.getKfmiop() != null && kfmirlModel.getKfmiop().fst != null) {
                    kfmiopFileLabel.setText(kfmirlModel.getKfmiop().fst.getTableName());
                }

                if(kfmirlModel.getOutputKfmirl() != null) {
                    Map3d kfmirlMap = kfmirlModel.getOutputKfmirl();
                    kfmirl.setColumnHeaders(kfmirlMap.xAxis);
                    kfmirl.setRowHeaders(kfmirlMap.yAxis);
                    kfmirl.setTableData(kfmirlMap.zAxis);
                } else if (kfmirlModel.getKfmirl() != null) {
                    Map3d kfmirlMap = kfmirlModel.getKfmirl().snd;
                    kfmirl.setColumnHeaders(kfmirlMap.xAxis);
                    kfmirl.setRowHeaders(kfmirlMap.yAxis);
                    kfmirl.setTableData(kfmirlMap.zAxis);
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

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
        Double[] yAxis = (Double[]) kfmiop.getColumnHeaders();

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

//         Create the object to represent the function over the given range.
        final org.jzy3d.plot3d.primitives.Shape surface = new org.jzy3d.plot3d.primitives.Shape(polygons);
        surface.setColorMapper(new ColorMapper(new ColorMapGreenYellowRed(), surface.getBounds().getZmin(), surface.getBounds().getZmax()));
        surface.setFaceDisplayed(true);
        surface.setWireframeColor(Color.BLACK);
        surface.setWireframeDisplayed(true);

        kfmiopChart3d.getScene().add(surface, true);
    }

    private JButton getKfmiopDefinitionButton() {
        JButton button = new JButton("Set KFMIOP Definition");

        button.addActionListener(e -> {
            Pair<TableDefinition, Map3d> tableDefinition = KfmiopPreferences.getSelectedMap();

            if(tableDefinition != null) {
                MapPickerDialog.showDialog(panel, panel, "Select KFMIOP", "Map Selection", tableDefinition.fst, KfmiopPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select KFMIOP", "Map Selection", null, KfmiopPreferences::setSelectedMap);
            }
        });

        return button;
    }

    private JButton getKfmirlDefinitionButton() {
        JButton button = new JButton("Set KFMIRL Definition");

        button.addActionListener(e -> {
            Pair<TableDefinition, Map3d> tableDefinition = KfmiopPreferences.getSelectedMap();

            if(tableDefinition != null) {
                MapPickerDialog.showDialog(panel, panel, "Select KFMIRL", "Map Selection", tableDefinition.fst, KfmirlPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select KFMIRL", "Map Selection", null, KfmirlPreferences::setSelectedMap);
            }
        });

        return button;
    }

    private JButton getFileButton() {
        JButton button = new JButton("Write KFMIRL");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFMIRL to the binary?",
                    "Write KFMIRL",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfmirlPreferences.getSelectedMap().fst, kfmirl.getMap3d());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        return button;
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
