package ui.view.kfzw;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import parser.xdf.TableDefinition;
import preferences.bin.BinFilePreferences;
import preferences.kfmiop.KfmiopPreferences;
import preferences.kfzw.KfzwPreferences;
import ui.map.axis.MapAxis;
import ui.map.map.MapTable;
import ui.view.listener.OnTabSelectedListener;
import ui.view.map.MapPickerDialog;
import ui.viewmodel.kfzw.KfzwViewModel;
import writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class KfzwView implements OnTabSelectedListener {

    private final MapTable kfzwInput = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);
    private final MapTable kfzwOutput = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][]);

    private final MapAxis kfmiopXAxis =  MapAxis.getMapAxis(new Double[1][0]);

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
        viewModel.register(new Observer<KfzwViewModel.KfzwModel>() {
            @Override
            public void onNext(@NonNull KfzwViewModel.KfzwModel model) {
                if(model.getKfzw() != null && !isKfzwInitialized) {
                    kfzwInput.setColumnHeaders(model.getKfzw().snd.xAxis);
                    kfzwInput.setRowHeaders(model.getKfzw().snd.yAxis);
                    kfzwInput.setTableData(model.getKfzw().snd.zAxis);

                    Double[][] kfmiopXAxisValues = new Double[1][];
                    kfmiopXAxisValues[0] = model.getKfmiopXAxis();
                    kfmiopXAxis.setTableData(kfmiopXAxisValues);

                    fileLabel.setText(model.getKfzw().fst.getTableName());

                    isKfzwInitialized = true;
                }

                if(model.getOutputKfzw() != null) {
                    kfzwOutput.setColumnHeaders(model.getOutputKfzw().xAxis);
                    kfzwOutput.setRowHeaders(model.getOutputKfzw().yAxis);
                    kfzwOutput.setTableData(model.getOutputKfzw().zAxis);
                }
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private JPanel getInputPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(new JLabel("KFMIOP X-Axis (Input)"),constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        initXAxis();

        panel.add(kfmiopXAxis.getScrollPane() ,constraints);

        initMap();

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(getHeader("KFZW (Input)"),constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        panel.add(kfzwInput.getScrollPane(),constraints);

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

        panel.add(getHeader("KFZW (Output)"),constraints);

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
        panel.add(label,c);

        return panel;
    }

    private void initXAxis() {
        kfmiopXAxis.getPublishSubject().subscribe(new Observer<Double[][]>() {

            @Override
            public void onNext(@NonNull Double[][] data) {
                viewModel.cacluateKfzw(kfzwInput.getMap3d(), data[0]);
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initMap() {
        kfzwInput.getPublishSubject().subscribe(new Observer<Map3d>() {
            @Override
            public void onNext(@NonNull Map3d map3d) {
                viewModel.cacluateKfzw(map3d, kfmiopXAxis.getData()[0]);
            }

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
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
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfzwPreferences.getInstance().getSelectedMap().fst, kfzwOutput.getMap3d());
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
