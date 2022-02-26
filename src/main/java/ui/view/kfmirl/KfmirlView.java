package ui.view.kfmirl;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import preferences.bin.BinFilePreferences;
import preferences.kfmirl.KfmirlPreferences;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.view.listener.OnTabSelectedListener;
import ui.viewmodel.kfmirl.KfmirlViewModel;
import writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class KfmirlView implements OnTabSelectedListener {
    private final MapTable kfmirl = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);
    private final MapTable kfmiop = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);

    private final MapAxis kfmiopXAxis =  MapAxis.getMapAxis(new Double[1][0]);

    private JPanel panel;
    private JLabel kfmirlFileLabel;
    private JLabel kfmiopFileLabel;

    private final KfmirlViewModel viewModel;

    private boolean kfmiopInitialized;

    public KfmirlView() {
        viewModel = new KfmirlViewModel();
    }

    public JPanel getPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.top = -110;

        panel.add(getKfmiopMapPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets.top = 0;
        constraints.insets.left = 20;

        panel.add(getKmfirlMapPanel(), constraints);

        initViewModel();

        return panel;
    }

    private JPanel getKfmiopMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        initKfmiopMap();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(new JLabel("KFMIOP X-Axis (Output)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(kfmiopXAxis.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(getHeader("KFMIOP (Input)"),constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        panel.add(kfmiop.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets.top = 16;

        kfmiopFileLabel = new JLabel("No Definition Selected");
        panel.add(kfmiopFileLabel, constraints);

        return panel;
    }

    private JPanel getKmfirlMapPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("KFMIRL (Output)"),constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(kfmirl.getScrollPane(),constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets.top = 16;

        kfmirlFileLabel = new JLabel("No Definition Selected");
        panel.add(kfmirlFileLabel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets.top = 16;

        panel.add(getWriteFileButton(), constraints);

        return panel;
    }

    private JPanel getHeader(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;

        JLabel label = new JLabel(title);
        panel.add(label,c);

        return panel;
    }

    private void initKfmiopMap() {
        kfmiop.getPublishSubject().subscribe(new Observer<Map3d>() {

            @Override
            public void onNext(@NonNull Map3d map3d) {
                Map3d kfmiop = new Map3d(kfmiopXAxis.getData()[0], map3d.yAxis, map3d.zAxis);
                viewModel.calculateKfmirl(kfmiop);
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        kfmiopXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Double[][] doubles) {
                kfmiop.setColumnHeaders(kfmiopXAxis.getData()[0]);
                Map3d kfmiopInput = new Map3d((Double[]) kfmiop.getColumnHeaders(), kfmiop.getRowHeaders(), kfmiop.getData());
                viewModel.calculateKfmirl(kfmiopInput);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initViewModel() {
        viewModel.register(new Observer<KfmirlViewModel.KfmirlModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull KfmirlViewModel.KfmirlModel kfmirlModel) {
                if(kfmirlModel.getKfmiop() != null && !kfmiopInitialized) {
                    Map3d kfmiopMap = kfmirlModel.getKfmiop().snd;
                    kfmiop.setColumnHeaders(kfmiopMap.xAxis);
                    kfmiop.setRowHeaders(kfmiopMap.yAxis);
                    kfmiop.setTableData(kfmiopMap.zAxis);

                    Double[][] xAxis = new Double[1][];
                    xAxis[0] = kfmiopMap.xAxis;
                    kfmiopXAxis.setTableData(xAxis);
                    kfmiopInitialized = true;
                }

                if(kfmirlModel.getKfmirl() != null && kfmirlModel.getKfmirl().fst != null) {
                    kfmirlFileLabel.setText(kfmirlModel.getKfmirl().fst.getTableName());
                }

                if(kfmirlModel.getKfmiop() != null && kfmirlModel.getKfmiop().fst != null) {
                    kfmiopFileLabel.setText(kfmirlModel.getKfmiop().fst.getTableName());
                }

                if(kfmirlModel.getOutputKfmirl() != null) {
                    Map3d kfmirlMap = kfmirlModel.getOutputKfmirl();
                    kfmirl.setColumnHeaders(kfmirlMap.xAxis);
                    kfmirl.setRowHeaders(kfmirlMap.yAxis);
                    kfmirl.setTableData(kfmirlMap.zAxis);
                } else if (kfmirlModel.getKfmirl() != null) {
                    Map3d kfmirlMap = kfmirlModel.getKfmirl().snd;
                    kfmirl.setColumnHeaders(kfmirlMap.xAxis);
                    kfmirl.setRowHeaders(kfmirlMap.yAxis);
                    kfmirl.setTableData(kfmirlMap.zAxis);
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private JButton getWriteFileButton() {
        JButton button = new JButton("Write KFMIRL");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFMIRL to the binary?",
                    "Write KFMIRL",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfmirlPreferences.getInstance().getSelectedMap().fst, kfmirl.getMap3d());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        return button;
    }

    @Override
    public void onTabSelected(boolean selected) {

    }
}
