package presentation.view.kfzw;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import domain.math.map.Map3d;
import data.preferences.bin.BinFilePreferences;
import data.preferences.kfzw.KfzwPreferences;
import presentation.map.axis.MapAxis;
import presentation.map.map.MapTable;
import presentation.view.listener.OnTabSelectedListener;
import presentation.viewmodel.kfzw.KfzwViewModel;
import data.writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class KfzwView implements OnTabSelectedListener {

    private final MapTable kfzwInput = MapTable.getMapTable(new Double[16], new Double[12], new Double[16][12]);
    private final MapTable kfzwOutput = MapTable.getMapTable(new Double[16], new Double[12], new Double[16][12]);

    private final MapAxis kfmiopXAxis = MapAxis.getMapAxis(new Double[1][11]);

    private JPanel panel;

    private final KfzwViewModel viewModel;

    private JLabel fileLabel;

    private boolean isKfzwInitialized;

    public KfzwView() {
        viewModel = new KfzwViewModel();
    }

    public JPanel getPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getInputPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets.top = 75;
        constraints.insets.left = 16;

        panel.add(getOutputPanel(), constraints);

        initViewModel();

        return panel;
    }

    private void initViewModel() {
        viewModel.register(new Observer<>() {
            @Override
            public void onNext(@NonNull KfzwViewModel.KfzwModel model) {
                if (model.getInputKfzw() == null) {
                    Map3d defaultKfzw = new Map3d(new Double[12], new Double[16], new Double[16][12]);

                    kfzwInput.setMap(defaultKfzw);
                    kfzwInput.setMap(defaultKfzw);
                    kfmiopXAxis.setTableData(new Double[1][11]);

                    fileLabel.setText("No Definition Selected");

                    return;
                }

                if (!isKfzwInitialized) {
                    kfzwInput.setColumnHeaders(model.getInputKfzw().getSecond().xAxis);
                    kfzwInput.setRowHeaders(model.getInputKfzw().getSecond().yAxis);
                    kfzwInput.setTableData(model.getInputKfzw().getSecond().zAxis);

                    Double[][] kfmiopXAxisValues = new Double[1][];
                    kfmiopXAxisValues[0] = model.getKfmiopXAxis();
                    kfmiopXAxis.setTableData(kfmiopXAxisValues);

                    fileLabel.setText(model.getInputKfzw().getFirst().getTableName());

                    isKfzwInitialized = true;
                }

                if (model.getOutputKfzw() != null) {
                    kfzwOutput.setColumnHeaders(model.getOutputKfzw().xAxis);
                    kfzwOutput.setRowHeaders(model.getOutputKfzw().yAxis);
                    kfzwOutput.setTableData(model.getOutputKfzw().zAxis);
                }
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private JPanel getInputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(new JLabel("KFMIOP X-Axis (Input)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        initXAxis();

        panel.add(kfmiopXAxis.getScrollPane(), constraints);

        initMap();

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(getHeader("KFZW (Input)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        panel.add(kfzwInput.getScrollPane(), constraints);

        constraints.gridy = 4;

        fileLabel = new JLabel("No Map Selected");
        panel.add(fileLabel, constraints);

        return panel;
    }

    private JPanel getOutputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("KFZW (Output)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(kfzwOutput.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(getFileButton(), constraints);

        return panel;
    }

    private JPanel getHeader(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;

        JLabel label = new JLabel(title);
        panel.add(label, c);

        return panel;
    }

    private void initXAxis() {
        kfmiopXAxis.getPublishSubject().subscribe(new Observer<>() {

            @Override
            public void onNext(@NonNull Double[][] data) {
                if(data[0][0] != null) {
                    SwingUtilities.invokeLater(() -> viewModel.calculateKfzw(kfzwInput.getMap3d(), data[0]));
                }
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private void initMap() {
        kfzwInput.getPublishSubject().subscribe(new Observer<>() {
            @Override
            public void onNext(@NonNull Map3d map3d) {
                if(kfmiopXAxis.getData()[0][0] != null) {
                    SwingUtilities.invokeLater(() -> viewModel.calculateKfzw(map3d, kfmiopXAxis.getData()[0]));
                }
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private JButton getFileButton() {
        JButton button = new JButton("Write KFZW");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFZW to the binary?",
                    "Write KFZW",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfzwPreferences.getInstance().getSelectedMap().getFirst(), kfzwOutput.getMap3d());
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
