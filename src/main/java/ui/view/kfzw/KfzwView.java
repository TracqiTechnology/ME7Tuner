package ui.view.kfzw;

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
import preferences.kfzw.KfzwPreferences;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.view.color.ColorMapGreenYellowRed;
import ui.view.listener.OnTabSelectedListener;
import ui.view.map.MapPickerDialog;
import ui.viewmodel.kfzw.KfzwViewModel;
import writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

public class KfzwView implements OnTabSelectedListener {

    private final MapTable kfzwInput = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);
    private final MapTable kfzwOutput = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);

    private final MapAxis kfmiopXAxis =  MapAxis.getMapAxis(new Double[1][0]);

    private Chart inputChart3d;
    private Chart outputChart3d;

    private JPanel panel;

    private final KfzwViewModel viewModel;

    private JLabel fileLabel;

    private boolean isKfzwInitialized;

    public KfzwView() {
        viewModel = new KfzwViewModel();
    }

    public JPanel getPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getInputPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets.top = 45;
        constraints.insets.left = 16;

        panel.add(getOutputPanel(), constraints);

        initViewModel();

        return panel;
    }

    private void initViewModel() {
        viewModel.register(new Observer<KfzwViewModel.KfzwModel>() {
            @Override
            public void onNext(@NonNull KfzwViewModel.KfzwModel model) {
                System.out.println("onNext()");
                if(model.getKfzw() != null && !isKfzwInitialized) {
                    kfzwInput.setColumnHeaders(model.getKfzw().snd.xAxis);
                    kfzwInput.setRowHeaders(model.getKfzw().snd.yAxis);
                    kfzwInput.setTableData(model.getKfzw().snd.zAxis);

                    Double[][] kfmiopXAxisValues = new Double[1][];
                    kfmiopXAxisValues[0] = model.getKfmiopXAxis();
                    kfmiopXAxis.setTableData(kfmiopXAxisValues);

                    fileLabel.setText(model.getKfzw().fst.getTableName());

                    isKfzwInitialized = true;
                }

                if(model.getOutputKfzw() != null) {
                    kfzwOutput.setColumnHeaders(model.getOutputKfzw().xAxis);
                    kfzwOutput.setRowHeaders(model.getOutputKfzw().yAxis);
                    kfzwOutput.setTableData(model.getOutputKfzw().zAxis);
                }
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private JPanel getInputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.ipadx = 0;

        panel.add(new JLabel("KFMIOP X-Axis (Input)"),constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 8;
        constraints.insets.left = 52;

        initXAxis();

        JScrollPane xAxisScrollPane = kfmiopXAxis.getScrollPane();
        xAxisScrollPane.setPreferredSize(new Dimension(615, 20));

        panel.add(xAxisScrollPane ,constraints);

        initMap();

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets.left = 52;
        constraints.insets.top = 0;

        panel.add(getHeader("KFZW (Input)", e -> showInChart3d()),constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets.left = 0;
        constraints.insets.top = 8;

        JScrollPane scrollPane = kfzwInput.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(710, 275));

        panel.add(scrollPane,constraints);

        constraints.gridy = 4;

        panel.add(getDefinitionButton(), constraints);

        constraints.gridy = 5;
        constraints.insets.top = 0;

        fileLabel = new JLabel("No Map Selected");
        panel.add(fileLabel, constraints);

        return panel;
    }

    private JPanel getOutputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.left = 58;

        panel.add(getHeader("KFZW (Output)", e -> showOutChart3d()),constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.left = 0;

        JScrollPane scrollPane = kfzwOutput.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(710, 275));

        panel.add(scrollPane,constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
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

    private void initXAxis() {
        kfmiopXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {

            @Override
            public void onNext(@NonNull Double[][] data) {
                viewModel.cacluateKfzw(kfzwInput.getMap3d(), data[0]);
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initMap() {
        kfzwInput.getPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(@NonNull Map3d map3d) {
                viewModel.cacluateKfzw(map3d, kfmiopXAxis.getData()[0]);
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

        Double[][] data = kfzwInput.getData();

        Double[] xAxis = (Double[]) kfzwInput.getColumnHeaders();
        Double[] yAxis = kfzwInput.getRowHeaders();

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

        panel.add((Component) outputChart3d.getCanvas(), c);

        return panel;
    }

    private void initOutputChart3d() {
        // Create a chart and add scatterAfr
        outputChart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        outputChart3d.getAxeLayout().setMainColor(org.jzy3d.colors.Color.BLACK);
        outputChart3d.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        outputChart3d.getAxeLayout().setXAxeLabel("Engine Load");
        outputChart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        outputChart3d.getAxeLayout().setZAxeLabel("Ignition Advance");

        NewtCameraMouseController controller = new NewtCameraMouseController(outputChart3d);

        Double[][] data = kfzwOutput.getData();

        Double[] xAxis = kfmiopXAxis.getData()[0];
        Double[] yAxis = kfzwInput.getRowHeaders();

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

        outputChart3d.getScene().add(surface, true);
    }

    private JButton getFileButton() {
        JButton button = new JButton("Write KFZW");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFZW to the binary?",
                    "Write KFZW",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfzwPreferences.getSelectedMap().fst, kfzwOutput.getMap3d());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        return button;
    }

    private JButton getDefinitionButton() {
        JButton button = new JButton("Set KFZW Definition");

        button.addActionListener(e -> {
            Pair<TableDefinition, Map3d> tableDefinition = KfmiopPreferences.getSelectedMap();

            if(tableDefinition != null) {
                MapPickerDialog.showDialog(panel, panel, "Select KFZW", "Map Selection", tableDefinition.fst, KfzwPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select KFZW", "Map Selection", null, KfzwPreferences::setSelectedMap);
            }
        });

        return button;
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
