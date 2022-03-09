package presentation.view.kfmirl;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import domain.math.map.Map3d;
import data.preferences.bin.BinFilePreferences;
import data.preferences.kfmirl.KfmirlPreferences;
import presentation.map.axis.MapAxis;
import presentation.map.map.MapTable;
import presentation.view.listener.OnTabSelectedListener;
import presentation.viewmodel.kfmirl.KfmirlViewModel;
import data.writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class KfmirlView implements OnTabSelectedListener {
    private final MapTable kfmirl = MapTable.getMapTable(new Double[16], new Double[12], new Double[16][12]);
    private final MapTable kfmiop = MapTable.getMapTable(new Double[16], new Double[11], new Double[16][11]);

    private final MapAxis kfmiopXAxis =  MapAxis.getMapAxis(new Double[1][11]);

    private JPanel panel;
    private JLabel kfmirlFileLabel;
    private JLabel kfmiopFileLabel;

    private final KfmirlViewModel viewModel;

    private boolean kfmiopInitialized;

    private CompositeDisposable compositeDisposable;

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

        initMapObservers();

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

    private void initMapObservers() {
        compositeDisposable = new CompositeDisposable();

        kfmiop.getPublishSubject().subscribe(new Observer<>() {

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
                compositeDisposable.add(disposable);
            }

            @Override
            public void onNext(@NonNull Map3d map3d) {
                Map3d kfmiop = new Map3d(kfmiopXAxis.getData()[0], map3d.yAxis, map3d.zAxis);
                viewModel.calculateKfmirl(kfmiop);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        kfmiopXAxis.getPublishSubject().subscribe(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
                compositeDisposable.add(disposable);
            }

            @Override
            public void onNext(@NonNull Double[][] doubles) {
                kfmiop.setColumnHeaders(kfmiopXAxis.getData()[0]);
                Map3d kfmiopInput = new Map3d((Double[]) kfmiop.getColumnHeaders(), kfmiop.getRowHeaders(), kfmiop.getData());
                viewModel.calculateKfmirl(kfmiopInput);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private void initViewModel() {
        viewModel.register(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull KfmirlViewModel.KfmirlModel kfmirlModel) {
                SwingUtilities.invokeLater(() -> {
                    if (kfmirlModel.getKfmiop() != null && !kfmiopInitialized) {
                        Map3d kfmiopMap = kfmirlModel.getKfmiop().getSecond();
                        kfmiop.setColumnHeaders(kfmiopMap.xAxis);
                        kfmiop.setRowHeaders(kfmiopMap.yAxis);
                        kfmiop.setTableData(kfmiopMap.zAxis);

                        Double[][] xAxis = new Double[1][];
                        xAxis[0] = kfmiopMap.xAxis;
                        kfmiopXAxis.setTableData(xAxis);
                        kfmiopInitialized = true;
                    }

                    if (kfmirlModel.getKfmirl() != null && kfmirlModel.getKfmirl().getFirst() != null) {
                        kfmirlFileLabel.setText(kfmirlModel.getKfmirl().getFirst().getTableName());
                    } else {
                        kfmirlFileLabel.setText("No Definition Selected");
                    }

                    if (kfmirlModel.getKfmiop() != null && kfmirlModel.getKfmiop().getFirst() != null) {
                        kfmiopFileLabel.setText(kfmirlModel.getKfmiop().getFirst().getTableName());
                    } else {
                        compositeDisposable.dispose();

                        Map3d defaultKfmiop = new Map3d(new Double[11], new Double[16], new Double[16][11]);
                        Map3d defaultKfmirl = new Map3d(new Double[12], new Double[16], new Double[16][12]);

                        kfmiop.setMap(defaultKfmiop);
                        kfmirl.setMap(defaultKfmirl);
                        kfmiopXAxis.setTableData(new Double[1][11]);

                        kfmirlFileLabel.setText("No Definition Selected");

                        initMapObservers();
                    }

                    if (kfmirlModel.getOutputKfmirl() != null) {
                        Map3d kfmirlMap = kfmirlModel.getOutputKfmirl();
                        kfmirl.setColumnHeaders(kfmirlMap.xAxis);
                        kfmirl.setRowHeaders(kfmirlMap.yAxis);
                        kfmirl.setTableData(kfmirlMap.zAxis);
                    } else if (kfmirlModel.getKfmirl() != null) {
                        Map3d kfmirlMap = kfmirlModel.getKfmirl().getSecond();
                        kfmirl.setColumnHeaders(kfmirlMap.xAxis);
                        kfmirl.setRowHeaders(kfmirlMap.yAxis);
                        kfmirl.setTableData(kfmirlMap.zAxis);
                    }
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
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
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfmirlPreferences.getInstance().getSelectedMap().getFirst(), kfmirl.getMap3d());
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
