package ui.view.kfmirl;

import model.kfmirl.Kfmirl;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.NewtCameraMouseController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import ui.map.map.MapTable;
import ui.view.color.ColorMapGreenYellowRed;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class KfmirlUiManager {
    private MapTable kfmirl;
    private DesiredLoadCalculatorPanel desiredLoadCalculatorPanel;
    private int minLoadIndex = 6;
    private Chart chart3d;

    public JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.PAGE_START;

        panel.add(getKmfirlGeneratorPanel(), c);

        c.gridx = 0;
        c.gridy = 1;
        c.insets.top = 16;

        panel.add(getMinLoadPanel(),c);

        c.gridx = 0;
        c.gridy = 2;
        c.insets.top = 16;

        panel.add(getKfmirlMapPanel(), c);

        c.gridy = 3;

        panel.add(getChard3dButton(), c);

        c.gridy = 4;

        panel.add(getHelpPanel(), c);

        return panel;
    }

    private JPanel getMinLoadPanel() {
        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;

        panel.add(new JLabel("Minimum Torque"),c);

        c.gridx = 1;
        c.gridy = 0;
        c.insets.left = 8;

        JComboBox<Double> loadList = new JComboBox<>(Kfmirl.getStockKfmirlXAxis());
        loadList.setSelectedIndex(minLoadIndex);
        loadList.setToolTipText("Minimum load at which the correction will be applied");

        loadList.addActionListener (new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                minLoadIndex = loadList.getSelectedIndex();
                kfmirl.setTableData(Kfmirl.getScaledKfmirlMap(Double.parseDouble(desiredLoadCalculatorPanel.getFieldText(DesiredLoadCalculatorPanel.FieldTitle.MAX_DESIRED_LOAD)), minLoadIndex));
            }
        });

        panel.add(loadList, c);

        return panel;
    }

    private JPanel getKfmirlMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        kfmirl = MapTable.getMapTable(Kfmirl.getStockKfmirlYAxis(), Kfmirl.getStockKfmirlXAxis(), Kfmirl.getStockKfmirlMap());

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("KFMIRL"),c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.top = 16;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollPane = kfmirl.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(715, 275));

        panel.add(scrollPane ,c);

        return panel;
    }

    private JPanel getKmfirlGeneratorPanel() {

        desiredLoadCalculatorPanel = new DesiredLoadCalculatorPanel(new DesiredLoadCalculatorPanel.OnValueChangedListener() {
            @Override
            public void onValueChanged(DesiredLoadCalculatorPanel.FieldTitle fieldTitle) {

                switch (fieldTitle) {
                    case MAX_DESIRED_BOOST:
                        kfmirl.setTableData(Kfmirl.getScaledKfmirlMap(Double.parseDouble(desiredLoadCalculatorPanel.getFieldText(DesiredLoadCalculatorPanel.FieldTitle.MAX_DESIRED_LOAD)), minLoadIndex));
                        break;
                    case KFURL:
                        kfmirl.setTableData(Kfmirl.getScaledKfmirlMap(Double.parseDouble(desiredLoadCalculatorPanel.getFieldText(DesiredLoadCalculatorPanel.FieldTitle.MAX_DESIRED_LOAD)), minLoadIndex));
                        break;
                }
            }
        });

        return desiredLoadCalculatorPanel;
    }

    private JEditorPane getHelpPanel() {
        JEditorPane jep = new JEditorPane();
        jep.setContentType("text/html");//set content as html
        jep.setText("<a href='https://github.com/KalebKE/ME7Tuner#kfmirl-torque-request-to-loadfill-request'>Load Request KFMIRL User Guide</a>.");
        jep.setOpaque(false);

        jep.setEditable(false);//so its not editable

        jep.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(hle.getURL().toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        return jep;
    }

    private JButton getChard3dButton() {
        JButton jButton = new JButton("3D Chart");
        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showChart3d();
            }
        });

        return jButton;
    }

    private void showChart3d() {
        JDialog jd = new JDialog();
        jd.setSize(500,500);
        jd.setLocationRelativeTo(null);
        jd.add(getChart3d());
        jd.setVisible(true);
    }

    private JPanel getChart3d() {
        initChart3d();
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

        panel.add((Component)chart3d.getCanvas(), c);

        return panel;
    }

    private void initChart3d() {
        // Create a chart and add scatterAfr
        chart3d = AWTChartComponentFactory.chart(Quality.Nicest, IChartComponentFactory.Toolkit.newt);
        chart3d.getAxeLayout().setMainColor(Color.BLACK);
        chart3d.getView().setBackgroundColor(Color.WHITE);
        chart3d.getAxeLayout().setXAxeLabel("Torque Request (mifa)");
        chart3d.getAxeLayout().setYAxeLabel("Engine RPM (nmot)");
        chart3d.getAxeLayout().setZAxeLabel("Specified Load (rlsol)");

        NewtCameraMouseController controller = new NewtCameraMouseController(chart3d);

        Double[][] data = kfmirl.getData();

        Double[] xAxis = Kfmirl.getStockKfmirlXAxis();
        Double[] yAxis = Kfmirl.getStockKfmirlYAxis();

        ArrayList<org.jzy3d.plot3d.primitives.Polygon> polygons = new ArrayList<>();
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

        chart3d.getScene().add(surface, true);
    }
}
