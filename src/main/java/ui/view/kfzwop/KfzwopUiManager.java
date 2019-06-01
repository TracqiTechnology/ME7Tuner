package ui.view.kfzwop;


import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import math.map.Map3d;
import model.kfzwop.Kfzwop;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.viewmodel.kfzwop.KfzwopViewModel;

import javax.swing.*;
import java.awt.*;

public class KfzwopUiManager {

    private MapTable kfzwopIn;
    private MapTable kfzwopOut;

    private MapAxis kfzwopXAxis;

    private KfzwopViewModel viewModel;

    public KfzwopUiManager() {
        viewModel = new KfzwopViewModel();

        viewModel.getKfzwopMapPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(Map3d map3d) {
                kfzwopOut.setColumnHeaders(map3d.xAxis);
                kfzwopOut.setRowHeaders(map3d.yAxis);
                kfzwopOut.setTableData(map3d.data);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public JPanel getKfzwopCalculationPanel() {

        GridBagConstraints constraints = new GridBagConstraints();
        JPanel mainPanel = new JPanel();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 0;
        constraints.gridy = 0;

        JPanel kfzwopInPanel = new JPanel();
        kfzwopInPanel.setPreferredSize(new Dimension(700, 500));

        kfzwopInPanel.add(getKfzwopInMapPanel(), new GridBagLayout());

        mainPanel.add(kfzwopInPanel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 1;
        constraints.gridy = 0;

        JPanel kfzwopOutPanel = new JPanel();
        kfzwopOutPanel.setPreferredSize(new Dimension(700, 500));

        kfzwopOutPanel.add(getKfzwopOutMapPanel(), new GridBagLayout());

        mainPanel.add(kfzwopOutPanel, constraints);

        return mainPanel;
    }

    private JPanel getKfzwopInMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        initKfmirlMap();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("KFZWOP IN"),c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.ipadx = -50;
        c.fill = GridBagConstraints.EAST;
        c.anchor = GridBagConstraints.EAST;

        JScrollPane scrollPane = kfzwopIn.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(705, 275));

        panel.add(scrollPane,c);

        return panel;
    }

    private JPanel getKfzwopOutMapPanel() {
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFZWOP OUT"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.ipadx = -50;
        constraints.fill = GridBagConstraints.EAST;
        constraints.anchor = GridBagConstraints.EAST;

        kfzwopOut = MapTable.getMapTable(Kfzwop.getStockYAxis(), Kfzwop.getStockXAxis(), Kfzwop.getStockMap());

        JScrollPane kfmiopMapScrollPane = kfzwopOut.getScrollPane();
        kfmiopMapScrollPane.setPreferredSize(new Dimension(705, 275));

        mapPanel.add(kfmiopMapScrollPane,constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.ipadx = 0;
        constraints.insets.top = 16;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFZWOP X-Axis"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets.top = 0;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        initKfmirlXAxis();

        JScrollPane kfmiopXAxisScrollPane = kfzwopXAxis.getScrollPane();
        kfmiopXAxisScrollPane.setPreferredSize(new Dimension(554, 20));

        mapPanel.add(kfmiopXAxisScrollPane ,constraints);

        return mapPanel;
    }

    private void initKfmirlXAxis() {
        Double[][] kfmiopXAxisValues = new Double[1][];
        kfmiopXAxisValues[0] = Kfzwop.getStockXAxis();
        kfzwopXAxis = MapAxis.getMapAxis(kfmiopXAxisValues);

        kfzwopXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {

            @Override
            public void onNext(Double[][] data) {
                Map3d map3d = new Map3d();
                map3d.xAxis = Kfzwop.getStockXAxis();
                map3d.yAxis = kfzwopIn.getRowHeaders();
                map3d.data = kfzwopIn.getData();
                viewModel.cacluateKfzwop(map3d, data[0]);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initKfmirlMap() {
        kfzwopIn = MapTable.getMapTable(Kfzwop.getStockYAxis(), Kfzwop.getStockXAxis(), Kfzwop.getStockMap());

        kfzwopIn.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(Map3d map3d) {
                viewModel.cacluateKfzwop(map3d, kfzwopXAxis.getData()[0]);
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
