package presentation.view.wdkugdn;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import domain.math.map.Map3d;
import data.preferences.bin.BinFilePreferences;
import data.preferences.wdkugdn.WdkugdnPreferences;
import presentation.map.map.MapTable;
import presentation.viewmodel.wdkugdn.WdkugdnViewModel;
import data.writer.BinWriter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class WdkugdnView {

    private final MapTable wdkudgnTable = MapTable.getMapTable(new Double[1], new Double[12], new Double[1][12]);
    private JPanel panel;
    private JLabel wdkugdnFileLabel;
    private final EngineDisplacementPanel engineDisplacementPanel = new EngineDisplacementPanel(new EngineDisplacementPanel.OnValueChangedListener() {
        @Override
        public void onValueChanged(EngineDisplacementPanel.FieldTitle fieldTitle) {
            viewModel.calculateWdkugdn();
        }
    });

    private final WdkugdnViewModel viewModel = new WdkugdnViewModel();

    public WdkugdnView() {
        initPanel();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setMap(Map3d map) {
        wdkudgnTable.setMap(map);
    }

    private void initPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        panel.add(getTablePanel(), c);

        initViewModel();
    }

    private JPanel getTablePanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(engineDisplacementPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(new JLabel("WDKUGDN (Output)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(wdkudgnTable.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets.top = 16;
        panel.add(getWdkugdnActionPanel(), constraints);

        return panel;
    }

    private void initViewModel() {
        viewModel.registerOnChange(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull WdkugdnViewModel.WdkugnModel wdkugnModel) {
                if (wdkugnModel.getWdkugdn() != null) {
                    wdkudgnTable.setMap(wdkugnModel.getWdkugdn());
                }

                if (wdkugnModel.getWdkudgnDefinitionTitle() != null) {
                    wdkugdnFileLabel.setText(wdkugnModel.getWdkudgnDefinitionTitle());
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

    private JPanel getWdkugdnActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        wdkugdnFileLabel = new JLabel("No Table Defined");
        panel.add(wdkugdnFileLabel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(getWriteFileButton(), constraints);

        return panel;
    }

    private JButton getWriteFileButton() {
        JButton button = new JButton("Write WDKUGDN");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write WDKUGDN to the binary?",
                    "Write WDKUGDN",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), WdkugdnPreferences.getInstance().getSelectedMap().getFirst(), wdkudgnTable.getMap3d());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        return button;
    }
}
