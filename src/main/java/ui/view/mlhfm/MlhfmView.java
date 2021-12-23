package ui.view.mlhfm;


import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import parser.xdf.TableDefinition;
import preferences.mlhfm.MlhfmMapPreferences;
import ui.map.map.MapTable;
import ui.view.map.MapPickerDialog;
import ui.viewmodel.mlmhfm.MlhfmViewModel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MlhfmView {

    private JFreeChart chart;
    private JPanel mlhfmPanel;
    private MlhfmViewModel mlhfmViewModel;
    private MapTable mapTable;
    private JLabel fileLabel;

    public MlhfmView() {
        initChart();
        initMapTable();
        initPanel();
        initViewModel();
    }

    public JPanel getMlhfmPanel() {
        return mlhfmPanel;
    }

    private void initPanel() {
        mlhfmPanel = new JPanel();
        mlhfmPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.95;
        mlhfmPanel.add(getChartPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0.95;
        c.insets = new Insets(0, 8, 0 ,0);
        mlhfmPanel.add(getMapTablePanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 0.05;
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 0 ,0);
        mlhfmPanel.add(getActionPanel(), c);
    }

    private void initViewModel() {
        mlhfmViewModel = new MlhfmViewModel();
        mlhfmViewModel.registerMLHFMOnChange(new Observer<MlhfmViewModel.MlfhmModel>() {
            @Override
            public void onNext(@NonNull MlhfmViewModel.MlfhmModel model) {
                if(model.isMapSelected()) {
                    fileLabel.setText(model.getTableDefinition().toString());
                    drawChart(model.getMap3d());
                    drawMapTable(model.getMap3d());
                } else {
                    System.out.println("No File Selected");
                    fileLabel.setText("No File Selected");
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

    private JPanel getMapTablePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 1));
        panel.setMinimumSize(new Dimension(125, 100));
        panel.setMaximumSize(new Dimension(125, 100));
        panel.setPreferredSize(new Dimension(125, 100));
        panel.add(mapTable.getScrollPane());

        return panel;
    }

    private JPanel getChartPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new ChartPanel(chart));

        return panel;
    }

    private JPanel getActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        JButton button = getDefinitionButton();
        panel.add(button, c);

        c.gridx = 0;
        c.gridy = 1;

        fileLabel = new JLabel("No File Selected");
        panel.add(fileLabel, c);

        return panel;
    }


    private JButton getDefinitionButton() {
        JButton button = new JButton("Load Definition");
        button.setToolTipText("Load the MLHFM definition.");

        button.addActionListener(e -> {
            Pair<TableDefinition, Map3d> tableDefinition = MlhfmMapPreferences.getSelectedMlhfmTableDefinition();

            if(tableDefinition != null) {
                MapPickerDialog.showDialog(mlhfmPanel, mlhfmPanel, "Select MLHFM", "Map Selection", tableDefinition.fst, MlhfmMapPreferences::setSelectedMlhfmTableDefinition);
            } else {
                MapPickerDialog.showDialog(mlhfmPanel, mlhfmPanel, "Select MLHFM", "Map Selection", null, MlhfmMapPreferences::setSelectedMlhfmTableDefinition);
            }
        });

        return button;
    }

    private void initChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        chart = ChartFactory.createScatterPlot(
                "Base MLHFM",
                "Voltage", "kg/hr", dataset);

        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);

        plot.getRenderer().setSeriesPaint(0, Color.BLUE);
    }

    private void initMapTable() {
        mapTable = MapTable.getMapTable(new Double[0], new String[]{"kg/hr"}, new Double[0][]);
        mapTable.setToolTipText("Base MLHFM");
    }

    private void drawMapTable(Map3d mlhfmMap) {
        mapTable.setRowHeaders(mlhfmMap.yAxis);
        mapTable.setTableData(mlhfmMap.zAxis);
        mapTable.invalidate();
    }

    private void drawChart(Map3d mlhfm) {
        List<Double> voltage = Arrays.asList(mlhfm.yAxis);
        List<Double> kghr = new ArrayList<>();

        for(int i = 0; i < mlhfm.zAxis.length; i++) {
            kghr.add(mlhfm.zAxis[i][0]);
        }

        XYSeries series = new XYSeries("MLHFM");

        for (int i = 0; i < voltage.size(); i++) {
            series.add(voltage.get(i), kghr.get(i));
        }

        XYPlot plot = (XYPlot)chart.getPlot();
        ((XYSeriesCollection)plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection)plot.getDataset()).addSeries(series);
    }
}
