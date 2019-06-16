package ui.view.kfmiop;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfmiop.Kfmiop;
import model.kfmirl.Kfmirl;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.viewmodel.kfmiop.KfmiopViewModel;

import javax.swing.*;
import java.awt.*;

public class KfmiopUiManager {
    private MapTable kfmirl;
    private MapTable kfmiop;

    private MapAxis kfmiopXAxis;

    private KfmiopViewModel viewModel;

    public KfmiopUiManager() {
        viewModel = new KfmiopViewModel();
        viewModel.getKfmiopXAxisPublishSubject().subscribe(new Observer<Double[]>() {

            @Override
            public void onNext(Double[] kfmiopXAxis) {
                KfmiopUiManager.this.kfmiopXAxis.setTableData(new Double[][]{kfmiopXAxis});

                Map3d kfmirlMap3d = new Map3d();
                kfmirlMap3d.xAxis = (Double[]) kfmirl.getColumnHeaders();
                kfmirlMap3d.yAxis = kfmirl.getRowHeaders();
                kfmirlMap3d.data = kfmirl.getData();

                Map3d kfmiopMap3d = new Map3d();
                kfmiopMap3d.xAxis = kfmiopXAxis;
                kfmiopMap3d.yAxis = Kfmiop.getStockKfmiopYAxis();
                kfmiopMap3d.data = Kfmiop.getStockKfmiopMap();

                viewModel.cacluateKfmiop(kfmirlMap3d, kfmiopMap3d);
            }

            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        viewModel.getKfmiopMapPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(Map3d map3d) {
                kfmiop.setColumnHeaders(map3d.xAxis);
                kfmiop.setRowHeaders(map3d.yAxis);
                kfmiop.setTableData(map3d.data);
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

        JPanel kfmirlPanel = new JPanel();
        kfmirlPanel.setPreferredSize(new Dimension(700, 500));

        kfmirlPanel.add(getKfmirlMapPanel(), new GridBagLayout());

        mainPanel.add(kfmirlPanel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;
        constraints.gridx = 1;
        constraints.gridy = 0;

        JPanel kfmiopPanel = new JPanel();
        kfmiopPanel.setPreferredSize(new Dimension(700, 500));

        kfmiopPanel.add(getKmfiopMapPanel(), new GridBagLayout());

        mainPanel.add(kfmiopPanel, constraints);

        return mainPanel;
    }

    private JPanel getKfmirlMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        initKfmirlMap();

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("KFMIRL"),c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollPane = kfmirl.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(715, 275));

        panel.add(scrollPane,c);

        return panel;
    }

    private JPanel getKmfiopMapPanel() {
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFMIOP"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.ipadx = -50;
        constraints.fill = GridBagConstraints.EAST;
        constraints.anchor = GridBagConstraints.EAST;

        kfmiop = MapTable.getMapTable(Kfmiop.getStockKfmiopYAxis(), Kfmiop.getStockKfmiopXAxis(), Kfmiop.getStockKfmiopMap());

        JScrollPane kfmiopMapScrollPane = kfmiop.getScrollPane();
        kfmiopMapScrollPane.setPreferredSize(new Dimension(705, 275));

        mapPanel.add(kfmiopMapScrollPane,constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.ipadx = 0;
        constraints.insets.top = 16;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        mapPanel.add(new JLabel("KFMIOP X-Axis"),constraints);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets.top = 0;
        constraints.fill = GridBagConstraints.CENTER;
        constraints.anchor = GridBagConstraints.CENTER;

        Double[][] kfmiopXAxisValues = new Double[1][];
        kfmiopXAxisValues[0] = Kfmiop.getStockKfmiopXAxis();

        kfmiopXAxis = MapAxis.getMapAxis(kfmiopXAxisValues);

        JScrollPane kfmiopXAxisScrollPane = kfmiopXAxis.getScrollPane();
        kfmiopXAxisScrollPane.setPreferredSize(new Dimension(554, 20));

        mapPanel.add(kfmiopXAxisScrollPane ,constraints);

        return mapPanel;
    }

    private void initKfmirlMap() {
        kfmirl = MapTable.getMapTable(Kfmirl.getStockKfmirlYAxis(), Kfmirl.getStockKfmirlXAxis(), Kfmirl.getStockKfmirlMap());
        kfmirl.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(Map3d map3d) {
                viewModel.recalcuateKfmiopXAxis(kfmirl.getData());
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
