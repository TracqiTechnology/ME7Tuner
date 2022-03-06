package presentation.view.plsol;

import domain.model.plsol.Airflow;
import domain.model.plsol.Horsepower;
import domain.model.plsol.Plsol;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import data.preferences.plsol.PlsolPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;


public class PlsolView {

    private JFreeChart plsolChart;
    private JFreeChart airflowChart;
    private JFreeChart powerChart;
    private JPanel panel;

    public PlsolView() {
        initPlsolChart();
        initAirflowChart();
        initPowerChart();
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

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addTab("Load", null, getPlsolChartPanel(), "Pressure -> Load");
        tabbedPane.addTab("Airflow", null, getAirflowChartPanel(), "Pressure -> Airflow");
        tabbedPane.addTab("Power", null, getPowerChartPanel(), "Pressure -> Horsepower");

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.95;
        panel.add(tabbedPane, c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 0.05;
        c.insets = new Insets(0, 0, 0 ,0);
        panel.add(getActionPanel(), c);
    }

    private JPanel getPlsolChartPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new ChartPanel(plsolChart));

        return panel;
    }

    private JPanel getAirflowChartPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new ChartPanel(airflowChart));

        return panel;
    }

    private JPanel getPowerChartPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new ChartPanel(powerChart));

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
        return new PlsolConstantsPanel(fieldTitle -> calculatePlsol());
    }

    private void calculatePlsol() {
        Plsol plsol = new Plsol(PlsolPreferences.getBarometricPressure(), PlsolPreferences.getIntakeAirTemperature(), PlsolPreferences.getKfurl());
        drawPlsolChart(plsol.getPoints());
        Airflow airflow = new Airflow(plsol.getPoints(), PlsolPreferences.getDisplacement(), PlsolPreferences.getRpm());
        drawAirflowChart(airflow.getPoints());
        Horsepower power = new Horsepower(airflow.getPoints());
        drawPowerChart(power.getPoints());
    }

    private void initPlsolChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        plsolChart = ChartFactory.createScatterPlot(
                "PLSOL",
                "Requested Load", "PSI", dataset);

        plsolChart.getTitle().setPaint(Color.decode("#F8F8F2"));
        plsolChart.setBackgroundPaint(Color.decode("#383c4a"));
        plsolChart.getLegend().setBackgroundPaint(Color.decode("#383c4a"));
        plsolChart.getLegend().setItemPaint(Color.decode("#F8F8F2"));

        XYPlot plot = (XYPlot) plsolChart.getPlot();
        plot.setBackgroundPaint(Color.decode("#383c4a"));
        plot.setDomainGridlinePaint(Color.decode("#F8F8F2"));
        plot.setRangeGridlinePaint(Color.decode("#F8F8F2"));
        plot.getDomainAxis().setLabelPaint(Color.decode("#F8F8F2"));
        plot.getDomainAxis().setTickLabelPaint(Color.decode("#F8F8F2"));
        plot.getRangeAxis().setLabelPaint(Color.decode("#F8F8F2"));
        plot.getRangeAxis().setTickLabelPaint(Color.decode("#F8F8F2"));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);

        plot.getRenderer().setSeriesPaint(0, Color.decode("#f57900"));
        plot.getRenderer().setSeriesPaint(1, Color.RED);
    }

    private void initAirflowChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        airflowChart = ChartFactory.createScatterPlot(
                "Airflow",
                "Requested Load", "g/sec", dataset);

        airflowChart.getTitle().setPaint(Color.decode("#F8F8F2"));
        airflowChart.setBackgroundPaint(Color.decode("#383c4a"));
        airflowChart.getLegend().setBackgroundPaint(Color.decode("#383c4a"));
        airflowChart.getLegend().setItemPaint(Color.decode("#F8F8F2"));

        XYPlot plot = (XYPlot) airflowChart.getPlot();
        plot.setBackgroundPaint(Color.decode("#383c4a"));
        plot.setDomainGridlinePaint(Color.decode("#F8F8F2"));
        plot.setRangeGridlinePaint(Color.decode("#F8F8F2"));
        plot.getDomainAxis().setLabelPaint(Color.decode("#F8F8F2"));
        plot.getDomainAxis().setTickLabelPaint(Color.decode("#F8F8F2"));
        plot.getRangeAxis().setLabelPaint(Color.decode("#F8F8F2"));
        plot.getRangeAxis().setTickLabelPaint(Color.decode("#F8F8F2"));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);
        plot.getRenderer().setSeriesPaint(0, Color.decode("#f57900"));
    }

    private void initPowerChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        powerChart = ChartFactory.createScatterPlot(
                "Horsepower",
                "Requested Load", "bhp", dataset);

        powerChart.getTitle().setPaint(Color.decode("#F8F8F2"));
        powerChart.setBackgroundPaint(Color.decode("#383c4a"));
        powerChart.getLegend().setBackgroundPaint(Color.decode("#383c4a"));
        powerChart.getLegend().setItemPaint(Color.decode("#F8F8F2"));

        XYPlot plot = (XYPlot) powerChart.getPlot();
        plot.setBackgroundPaint(Color.decode("#383c4a"));
        plot.setDomainGridlinePaint(Color.decode("#F8F8F2"));
        plot.setRangeGridlinePaint(Color.decode("#F8F8F2"));
        plot.getDomainAxis().setLabelPaint(Color.decode("#F8F8F2"));
        plot.getDomainAxis().setTickLabelPaint(Color.decode("#F8F8F2"));
        plot.getRangeAxis().setLabelPaint(Color.decode("#F8F8F2"));
        plot.getRangeAxis().setTickLabelPaint(Color.decode("#F8F8F2"));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);
        plot.getRenderer().setSeriesPaint(0, Color.decode("#f57900"));
    }

    private void drawPlsolChart(Point2D.Double[] points) {
        XYSeries absolute = new XYSeries("Requested Absolute");

        for (Point2D.Double point:points) {
            absolute.add(point.x, point.y*0.0145038);
        }

        XYSeries relative = new XYSeries("Requested Relative (Boost)");

        double barometricPressure = PlsolPreferences.getBarometricPressure();

        for (Point2D.Double point:points) {
            relative.add(point.x, (point.y-barometricPressure) *0.0145038);
        }

        XYPlot plot = (XYPlot) plsolChart.getPlot();
        ((XYSeriesCollection)plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection)plot.getDataset()).addSeries(absolute);
        ((XYSeriesCollection)plot.getDataset()).addSeries(relative);
    }

    private void drawAirflowChart(Point2D.Double[] points) {
        XYSeries absolute = new XYSeries("Airflow");

        for (Point2D.Double point:points) {
            absolute.add(point.x, point.y);
        }

        XYPlot plot = (XYPlot) airflowChart.getPlot();
        ((XYSeriesCollection)plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection)plot.getDataset()).addSeries(absolute);
    }

    private void drawPowerChart(Point2D.Double[] points) {
        XYSeries absolute = new XYSeries("Horsepower");

        for (Point2D.Double point:points) {
            absolute.add(point.x, point.y);
        }

        XYPlot plot = (XYPlot) powerChart.getPlot();
        ((XYSeriesCollection)plot.getDataset()).removeAllSeries();
        ((XYSeriesCollection)plot.getDataset()).addSeries(absolute);
    }
}
