package presentation.view.kfzwop;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import domain.math.map.Map3d;
import data.preferences.bin.BinFilePreferences;
import data.preferences.kfzwop.KfzwopPreferences;
import presentation.map.axis.MapAxis;
import presentation.map.map.MapTable;
import presentation.view.listener.OnTabSelectedListener;
import presentation.viewmodel.kfzwop.KfzwopViewModel;
import data.writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class KfzwopView implements OnTabSelectedListener {

    private final MapTable kfzwopInput = MapTable.getMapTable(new Double[16], new Double[11], new Double[16][11]);
    private final MapTable kfzwopOutput = MapTable.getMapTable(new Double[16], new Double[11], new Double[16][11]);

    private final MapAxis kfmiopXAxis = MapAxis.getMapAxis(new Double[1][11]);

    private final KfzwopViewModel viewModel;

    private JPanel panel;
    private JLabel fileLabel;

    public KfzwopView() {
        viewModel = new KfzwopViewModel();
    }

    public JPanel getPanel() {

        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getKfzwopInputPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets.top = 75;
        constraints.insets.left = 16;

        panel.add(getKfzwopOutMapPanel(), constraints);

        initViewModel();

        return panel;
    }

    private void initViewModel() {
        viewModel.register(new Observer<>() {
            @Override
            public void onNext(@NonNull KfzwopViewModel.KfzwopModel model) {
                SwingUtilities.invokeLater(() -> {
                    if (model.getKfzwop() == null) {
                        Map3d defaultKfzwop = new Map3d(new Double[11], new Double[16], new Double[16][11]);

                        kfzwopInput.setMap(defaultKfzwop);
                        kfzwopOutput.setMap(defaultKfzwop);
                        kfmiopXAxis.setTableData(new Double[1][11]);

                        fileLabel.setText("No Definition Selected");

                        return;
                    }

                    if (!kfzwopInput.getMap3d().equals(model.getKfzwop().getSecond())) {
                        kfzwopInput.setMap(model.getKfzwop().getSecond());

                        Double[][] kfmiopXAxisValues = new Double[1][];
                        kfmiopXAxisValues[0] = model.getKfzwop().getSecond().xAxis;
                        kfmiopXAxis.setTableData(kfmiopXAxisValues);

                        fileLabel.setText(model.getKfzwop().getFirst().getTableName());

                    }

                    if (model.getOutputKfzwop() != null) {
                        kfzwopOutput.setMap(model.getOutputKfzwop());
                    }
                });
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

    private JPanel getKfzwopInputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        initKfmirlMap();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(new JLabel("KFMIOP/KFZWOP X-Axis (Input)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(kfmiopXAxis.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(getHeader("KFZWOP (Input)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        panel.add(kfzwopInput.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;

        fileLabel = new JLabel("No Map Selected");
        panel.add(fileLabel, constraints);

        return panel;
    }

    private JPanel getKfzwopOutMapPanel() {
        GridBagConstraints constraints = new GridBagConstraints();

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());

        initKfmirlXAxis();

        constraints.gridx = 0;
        constraints.gridy = 0;

        mapPanel.add(getHeader("KFZWOP (Output)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        mapPanel.add(kfzwopOutput.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        mapPanel.add(getFileButton(), constraints);


        return mapPanel;
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

    private void initKfmirlXAxis() {
        kfmiopXAxis.getPublishSubject().subscribe(new Observer<>() {

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Double[][] data) {
                if(data[0][0] != null) {
                    SwingUtilities.invokeLater(() -> viewModel.calculateKfzwop(kfzwopInput.getMap3d(), data[0]));
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private void initKfmirlMap() {
        kfzwopInput.getPublishSubject().subscribe(new Observer<>() {

            @Override
            public void onNext(@NonNull Map3d map3d) {
                if(kfmiopXAxis.getData()[0][0] != null) {
                    SwingUtilities.invokeLater(() -> viewModel.calculateKfzwop(map3d, kfmiopXAxis.getData()[0]));
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
        JButton button = new JButton("Write KFZWOP");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFZWOP to the binary?",
                    "Write KFZWOP",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfzwopPreferences.getInstance().getSelectedMap().getFirst(), kfzwopOutput.getMap3d());
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
