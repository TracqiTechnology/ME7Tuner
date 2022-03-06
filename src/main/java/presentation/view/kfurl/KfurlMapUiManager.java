package presentation.view.kfurl;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import model.kfurl.Kfurl;
import presentation.map.map.MapTable;
import presentation.viewmodel.kfurl.KfurlViewModel;

import javax.swing.*;
import java.awt.*;

public class KfurlMapUiManager {

    private MapTable kfurl;
    private JPanel panel;

    public KfurlMapUiManager() {
        initPanel();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setMap(Map3d map) {
        kfurl.setMap(map);
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

        kfurl = MapTable.getMapTable(Kfurl.getYAxis(), Kfurl.getXAxis(), Kfurl.getMap());

        KfurlViewModel.getInstance().setMap(kfurl.getMap3d());

        kfurl.getPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onSubscribe(Disposable disposable) {

            }

            @Override
            public void onNext(Map3d map3d) {
                KfurlViewModel.getInstance().setMap(map3d);
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

        panel.add(new JLabel("KFURL (Input)"), c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.insets.top = 16;
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollPane = kfurl.getScrollPane();
        scrollPane.setPreferredSize(new Dimension(620, 245));

        panel.add(scrollPane, c);

        return panel;
    }
}
