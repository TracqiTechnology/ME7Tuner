package ui.view.kfzw;


import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfzw.Kfzw;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.viewmodel.kfzw.KfzwViewModel;

import javax.swing.*;
import java.awt.*;

public class KfzwUiManager {

    private MapTable kfzwIn;
    private MapTable kfzwOut;

    private MapAxis kfzwXAxis;

    private KfzwViewModel viewModel;

    public KfzwUiManager() {
        viewModel = new KfzwViewModel();

        viewModel.getKfzwMapPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(Map3d map3d) {
                kfzwOut.setColumnHeaders(map3d.xAxis);
                kfzwOut.setRowHeaders(map3d.yAxis);
                kfzwOut.setTableData(map3d.data);
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
        kfzwopInPanel.setPreferredSize(new Dimension(710, 500));

        kfzwopInPanel.add(getInMapPanel(), new GridBagLayout());

        mainPanel.add(kfzwopInPanel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 1;
        constraints.gridy = 0;

        JPanel kfzwopOutPanel = new JPanel();
        kfzwopOutPanel.setPreferredSize(new Dimension(710, 500));

        kfzwopOutPanel.add(getOutMapPanel(), new GridBagLayout());

        mainPanel.add(kfzwopOutPanel, constraints);

        return mainPanel;
    }

    private JPanel getInMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        initMap();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("KFZW IN"),c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.EAST;
        c.anchor = GridBagConstraints.EAST;

        JScrollPane scrollPane = kfzwIn.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(710, 275));

        panel.add(scrollPane,c);

        return panel;
    }

    private JPanel getOutMapPanel() {
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFZW OUT"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.EAST;
        constraints.anchor = GridBagConstraints.EAST;

        kfzwOut = MapTable.getMapTable(Kfzw.getStockYAxis(), Kfzw.getStockXAxis(), Kfzw.getStockMap());

        JScrollPane kfmiopMapScrollPane = kfzwOut.getScrollPane();
        kfmiopMapScrollPane.setPreferredSize(new Dimension(710, 275));

        mapPanel.add(kfmiopMapScrollPane,constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.ipadx = 0;
        constraints.insets.top = 16;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFZW X-Axis"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets.top = 0;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        initXAxis();

        JScrollPane xAxisScrollPane = kfzwXAxis.getScrollPane();
        xAxisScrollPane.setPreferredSize(new Dimension(605, 20));

        mapPanel.add(xAxisScrollPane ,constraints);

        return mapPanel;
    }

    private void initXAxis() {
        Double[][] kfmiopXAxisValues = new Double[1][];
        kfmiopXAxisValues[0] = Kfzw.getStockXAxis();
        kfzwXAxis = MapAxis.getMapAxis(kfmiopXAxisValues);

        kfzwXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {

            @Override
            public void onNext(Double[][] data) {
                Map3d map3d = new Map3d();
                map3d.xAxis = Kfzw.getStockXAxis();
                map3d.yAxis = kfzwIn.getRowHeaders();
                map3d.data = kfzwIn.getData();
                viewModel.cacluateKfzw(map3d, data[0]);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initMap() {
        kfzwIn = MapTable.getMapTable(Kfzw.getStockYAxis(), Kfzw.getStockXAxis(), Kfzw.getStockMap());

        kfzwIn.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(Map3d map3d) {
                viewModel.cacluateKfzw(map3d, kfzwXAxis.getData()[0]);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }
}
