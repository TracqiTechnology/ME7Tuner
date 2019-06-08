package ui.view.mlhfm;


import contract.MlhfmFileContract;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map2d;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import preferences.filechooser.FileChooserPreferences;
import ui.map.map.MapTable;
import ui.viewmodel.MlhfmViewModel;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class MlhfmUiManager {

    private JFreeChart chart;
    private JPanel mlhfmPanel;
    private JLabel fileLabel;
    private MlhfmViewModel mlhfmViewModel;
    private MapTable mapTable;

    public MlhfmUiManager() {
        mlhfmViewModel = MlhfmViewModel.getInstance();
        mlhfmViewModel.getMlhfmPublishSubject().subscribe(new Observer<Map2d>() {
            @Override
            public void onNext(Map2d mlhfmMap) {
                drawChart(mlhfmMap);
                drawMapTable(mlhfmMap);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        mlhfmViewModel.getFilePublishSubject().subscribe(new Observer<File>() {
            @Override
            public void onNext(File file) {
                fileLabel.setText(file.getAbsolutePath());
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public JPanel getMlhfmPanel() {
        initChart();
        initMapTable();

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

        return mlhfmPanel;
    }

    private JPanel getMapTablePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 1));
        panel.setMinimumSize(new Dimension(120, 100));
        panel.setMaximumSize(new Dimension(120, 100));
        panel.setPreferredSize(new Dimension(120, 100));
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

        JButton button = getFileButton();
        panel.add(button, c);

        c.gridx = 0;
        c.gridy = 1;

        fileLabel = new JLabel("No File Selected");
        panel.add(fileLabel, c);

        return panel;
    }


    private JButton getFileButton() {
        JButton button = new JButton("Load MLHFM");

        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new CSVFileFilter());
            fc.setCurrentDirectory(FileChooserPreferences.getDirectory());

            int returnValue = fc.showOpenDialog(mlhfmPanel);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fc.getSelectedFile();
                mlhfmViewModel.loadFile(selectedFile);
                FileChooserPreferences.setDirectory(selectedFile.getParentFile());
            }
        });

        return button;
    }

    private void initChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        chart = ChartFactory.createScatterPlot(
                "MHLFM",
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
    }

    private void drawMapTable(Map2d mlhfmMap) {
        List<Double> voltage = Arrays.asList(mlhfmMap.axis);
        List<Double> kghr = Arrays.asList(mlhfmMap.data);
        Double[][] data = new Double[kghr.size()][1];

        for(int i = 0; i < data.length; i++) {
            data[i][0] = kghr.get(i);
        }

        mapTable.setRowHeaders(voltage.toArray(new Double[0]));
        mapTable.setTableData(data);
    }

    private void drawChart(Map2d mlhfmMap) {
        List<Double> voltage = Arrays.asList(mlhfmMap.axis);
        List<Double> kghr = Arrays.asList(mlhfmMap.data);

        XYSeries series = new XYSeries("MLHFM");

        for (int i = 0; i < voltage.size(); i++) {
            series.add(voltage.get(i), kghr.get(i));
        }

        XYPlot plot = (XYPlot)chart.getPlot();
        ((XYSeriesCollection)plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection)plot.getDataset()).addSeries(series);
    }

    private class CSVFileFilter extends FileFilter {
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String name = f.getName();
            String[] parts = name.split("\\.");
            if(parts.length > 0) {
                String ext = parts[parts.length - 1];
                return ext.trim().equalsIgnoreCase("csv");
            }

            return false;
        }

        @Override
        public String getDescription() {
            return "CSV";
        }
    }
}
