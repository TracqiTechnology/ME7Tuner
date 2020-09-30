package ui.view.kfkhfm;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfkhfm.Kfkhfm;
import ui.map.map.MapTable;
import ui.viewmodel.kfkhfm.KfkhfmViewModel;

import javax.swing.*;
import java.awt.*;

public class KfkhfmUiManager {

    private MapTable kfkhfm;
    private JPanel panel;

    public KfkhfmUiManager() {
        initPanel();
    }

    public KfkhfmUiManager(boolean shouldNotifyOnChange) {
        initPanel();
        if (shouldNotifyOnChange) {
            shouldNotifyOnChange();
        }
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setMap(Map3d map) {
        kfkhfm.setMap(map);
    }

    private void initPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        panel.add(getMapPanel(), c);
    }

    private void shouldNotifyOnChange() {
        KfkhfmViewModel.getInstance().setKfkhfm(kfkhfm.getMap3d());

        kfkhfm.getPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onSubscribe(Disposable disposable) {

            }

            @Override
            public void onNext(Map3d map3d) {
                KfkhfmViewModel.getInstance().setKfkhfm(map3d);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private JPanel getMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        kfkhfm = MapTable.getMapTable(Kfkhfm.getStockYAxis(), Kfkhfm.getStockXAxis(), Kfkhfm.getStockMap());

        c.weightx = 1;
        c.gridx = 0;

        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("KFKHFM"), c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.top = 16;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollPane = kfkhfm.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(820, 245));

        panel.add(scrollPane, c);

        return panel;
    }
}
