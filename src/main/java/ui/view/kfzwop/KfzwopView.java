package ui.view.kfzwop;


import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
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
import preferences.kfzwop.KfzwopPreferences;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.view.color.ColorMapGreenYellowRed;
import ui.view.listener.OnTabSelectedListener;
import ui.view.map.MapPickerDialog;
import ui.viewmodel.kfzwop.KfzwopViewModel;
import writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class KfzwopView implements OnTabSelectedListener {

    private final MapTable kfzwopInput = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);;
    private final MapTable kfzwopOutput = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);;

    private final MapAxis kfmiopXAxis =  MapAxis.getMapAxis(new Double[1][0]);

    private Chart inputChart3d;
    private Chart initOutput3d;

    private final KfzwopViewModel viewModel;

    private JPanel panel;
    private JLabel fileLabel;
    private boolean isKfzwopInitialized;

    public KfzwopView() {
        viewModel = new KfzwopViewModel();
    }

    public JPanel getPanel() {

        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getKfzwopInputPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets.top = 40;
        constraints.insets.left = 16;

        panel.add(getKfzwopOutMapPanel(), constraints);

        initViewModel();

        return panel;
    }

    private void initViewModel() {
        viewModel.register(new Observer<KfzwopViewModel.KfzwopModel>() {
            @Override
            public void onNext(@NonNull KfzwopViewModel.KfzwopModel model) {
                SwingUtilities.invokeLater(() -> {
                    if(model.getKfzwop() != null && !isKfzwopInitialized) {
                        kfzwopInput.setColumnHeaders(model.getKfzwop().snd.xAxis);
                        kfzwopInput.setRowHeaders(model.getKfzwop().snd.yAxis);
                        kfzwopInput.setTableData(model.getKfzwop().snd.zAxis);

                        Double[][] kfmiopXAxisValues = new Double[1][];
                        kfmiopXAxisValues[0] = model.getKfzwop().snd.xAxis;
                        kfmiopXAxis.setTableData(kfmiopXAxisValues);

                        fileLabel.setText(model.getKfzwop().fst.getTableName());

                        isKfzwopInitialized = true;
                    }

                    if(model.getOutputKfzwop() != null) {
                        kfzwopOutput.setColumnHeaders(model.getOutputKfzwop().xAxis);
                        kfzwopOutput.setRowHeaders(model.getOutputKfzwop().yAxis);
                        kfzwopOutput.setTableData(model.getOutputKfzwop().zAxis);
                    }
                });
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private JPanel getKfzwopInputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        initKfmirlMap();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.top = 16;

        panel.add(new JLabel("KFMIOP/KFZWOP X-Axis (Input)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 0;

        JScrollPane kfzwXAxisScrollPane = kfmiopXAxis.getScrollPane();
        kfzwXAxisScrollPane.setPreferredSize(new Dimension(615, 20));
        panel.add(kfzwXAxisScrollPane, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets.left = 52;

        panel.add(getHeader("KFZWOP (Input)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInChart3d();
            }
        }),constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.ipadx = -50;
        constraints.insets.left = 0;

        JScrollPane scrollPane = kfzwopInput.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(705, 275));

        panel.add(scrollPane,constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.ipadx = 0;
        constraints.insets.left = 0;
        constraints.insets.top = 16;

        panel.add(getKfmirlDefinitionButton(),constraints);

        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.ipadx = 0;
        constraints.insets.top = 0;

        fileLabel = new JLabel("No Map Selected");
        panel.add(fileLabel, constraints);

        return panel;
    }

    private JPanel getKfzwopOutMapPanel() {
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());

        initKfmirlXAxis();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.left = 58;

        mapPanel.add(getHeader("KFZWOP (Output)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOutChart3d();
            }
        }),constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.ipadx = -50;
        constraints.insets.left = 0;

        JScrollPane kfmiopMapScrollPane = kfzwopOutput.getScrollPane();
        kfmiopMapScrollPane.setPreferredSize(new Dimension(705, 275));

        mapPanel.add(kfmiopMapScrollPane,constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.ipadx = 0;
        constraints.insets.top = 16;

        mapPanel.add(getFileButton(),constraints);


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

    private void initKfmirlXAxis() {
        kfmiopXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {

            @Override
            public void onNext(@NonNull Double[][] data) {
                viewModel.cacluateKfzwop(kfzwopInput.getMap3d(), data[0]);
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initKfmirlMap() {
        kfzwopInput.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(@NonNull Map3d map3d) {
                viewModel.cacluateKfzwop(map3d, kfmiopXAxis.getData()[0]);
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void showInChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getInputChart3d());
        jd.setVisible(true);
    }

    private JPanel getInputChart3d() {
        initInputChart3d();
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

        panel.add((Component) inputChart3d.getCanvas(), c);

        return panel;
    }

    private void initInputChart3d() {
        // Create a chart and add scatterAfr
        inputChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        inputChart3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        inputChart3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        inputChart3d.getAxeLayout().setXAxeLabel("Engine Load");
        inputChart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        inputChart3d.getAxeLayout().setZAxeLabel("Ignition Advance");

        NewtCameraMouseController controller = new NewtCameraMouseController(inputChart3d);

        Double[][] data = kfzwopInput.getData();

        Double[] xAxis = (Double[]) kfzwopInput.getColumnHeaders();
        Double[] yAxis = kfzwopInput.getRowHeaders();

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

        inputChart3d.getScene().add(surface, true);
    }

    private void showOutChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getOutChart3d());
        jd.setVisible(true);
    }

    private JPanel getOutChart3d() {
        initOutputChart3d();
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

        panel.add((Component) initOutput3d.getCanvas(), c);

        return panel;
    }

    private void initOutputChart3d() {
        // Create a chart and add scatterAfr
        initOutput3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        initOutput3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        initOutput3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        initOutput3d.getAxeLayout().setXAxeLabel("Engine Load");
        initOutput3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        initOutput3d.getAxeLayout().setZAxeLabel("Ignition Advance");

        NewtCameraMouseController controller = new NewtCameraMouseController(initOutput3d);

        Double[][] data = kfzwopOutput.getData();

        Double[] xAxis = kfmiopXAxis.getData()[0];
        Double[] yAxis = kfzwopInput.getRowHeaders();

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

        initOutput3d.getScene().add(surface, true);
    }

    private JButton getKfmirlDefinitionButton() {
        JButton button = new JButton("Set KFZWOP Definition");

        button.addActionListener(e -> {
            Pair<TableDefinition, Map3d> tableDefinition = KfmiopPreferences.getSelectedMap();

            isKfzwopInitialized = false;

            if(tableDefinition != null) {
                MapPickerDialog.showDialog(panel, panel, "Select KFZWOP", "Map Selection", tableDefinition.fst, KfzwopPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select KFZWOP", "Map Selection", null, KfzwopPreferences::setSelectedMap);
            }
        });

        return button;
    }

    private JButton getFileButton() {
        JButton button = new JButton("Write KFZWOP");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFZWOP to the binary?",
                    "Write KFZWOP",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfzwopPreferences.getSelectedMap().fst, kfzwopOutput.getMap3d());
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
