package ui.view.plsol;

import model.plsol.Plsol;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import preferences.plsol.PlsolPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;


public class PlsolView {

    private JFreeChart chart;
    private JPanel panel;

    public PlsolView() {
        initChart();
        initPanel();
        calculatePlsol();
    }

    public JPanel getPanel() {
        return panel;
    }

    private void initPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.95;
        panel.add(getChartPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 0.05;
        c.insets = new Insets(0, 0, 0 ,0);
        panel.add(getActionPanel(), c);
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

        panel.add(getConfigurationPanel(), c);
        return panel;
    }

    private JPanel getConfigurationPanel() {
        return new PlsolConstantsPanel(new PlsolConstantsPanel.OnValueChangedListener() {
            @Override
            public void onValueChanged(PlsolConstantsPanel.FieldTitle fieldTitle) {
                calculatePlsol();
            }
        });
    }

    private void calculatePlsol() {
        drawChart(new Plsol(PlsolPreferences.getBarometricPressure(), PlsolPreferences.getIntakeAirTemperature(), PlsolPreferences.getKfurl()).getPoints());
    }

    private void initChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        chart = ChartFactory.createScatterPlot(
                "PLSOL",
                "Requested Load", "PSI", dataset);

        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);

        plot.getRenderer().setSeriesPaint(0, Color.BLUE);
        plot.getRenderer().setSeriesPaint(1, Color.RED);
    }

    private void drawChart(Point2D.Double[] points) {
        XYSeries absolute = new XYSeries("Requested Absolute");

        for (Point2D.Double point:points) {
            absolute.add(point.x, point.y*0.0145038);
        }

        XYSeries relative = new XYSeries("Requested Relative (Boost)");

        double barometricPressure = PlsolPreferences.getBarometricPressure();

        for (Point2D.Double point:points) {
            relative.add(point.x, (point.y-barometricPressure) *0.0145038);
        }

        XYPlot plot = (XYPlot)chart.getPlot();
        ((XYSeriesCollection)plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection)plot.getDataset()).addSeries(absolute);
        ((XYSeriesCollection)plot.getDataset()).addSeries(relative);
    }
}
