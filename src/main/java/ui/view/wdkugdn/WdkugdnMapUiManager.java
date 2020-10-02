package ui.view.wdkugdn;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.wdkugdn.Wdkugdn;
import ui.map.map.MapTable;
import ui.viewmodel.wdkugdn.WdkugdnViewModel;

import javax.swing.*;
import java.awt.*;

public class WdkugdnMapUiManager {

    private MapTable wdkudgn;
    private JPanel panel;

    public WdkugdnMapUiManager() {
        initPanel();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setMap(Map3d map) {
        wdkudgn.setMap(map);
    }

    private void initPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        panel.add(getMapPanel(), c);
    }

    private JPanel getMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        wdkudgn = MapTable.getMapTable(Wdkugdn.getYAxis(), Wdkugdn.getXAxis(), Wdkugdn.getMap());

        WdkugdnViewModel.getInstance().setMap(wdkudgn.getMap3d());

        wdkudgn.getPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onSubscribe(Disposable disposable) {

            }

            @Override
            public void onNext(Map3d map3d) {
                WdkugdnViewModel.getInstance().setMap(map3d);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        c.weightx = 1;
        c.gridx = 0;

        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        panel.add(new JLabel("WDKUGDN (Input)"), c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.top = 16;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollPane = wdkudgn.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(720, 245));

        panel.add(scrollPane, c);

        return panel;
    }
}
