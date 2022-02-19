package ui.view.wdkugdn;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import parser.xdf.TableDefinition;
import preferences.kfwdkmsn.KfwdkmsnPreferences;
import preferences.wdkugdn.WdkugdnPreferences;
import ui.map.map.MapTable;
import ui.view.map.MapPickerDialog;
import ui.viewmodel.wdkugdn.WdkugdnViewModel;

import javax.swing.*;
import java.awt.*;

public class WdkugdnView {

    private final MapTable wdkudgn = MapTable.getMapTable(new Double[0], new Double[0], new Double[0][0]);
    private JPanel panel;
    private JLabel wdkugdnFileLabel;
    private JLabel kfwdkmsnFileLabel;
    private final EngineDisplacementPanel engineDisplacementPanel = new EngineDisplacementPanel(new EngineDisplacementPanel.OnValueChangedListener() {
        @Override
        public void onValueChanged(EngineDisplacementPanel.FieldTitle fieldTitle) {
            System.out.println("calculate");
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
        wdkudgn.setMap(map);
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

        panel.add(new JLabel("WDKUGDN (Output)"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(wdkudgn.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets.top = 16;
        panel.add(getWdkugdnActionPanel(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets.top = 16;
        panel.add( getKfwdkmsnActionPanel(), constraints);

        return panel;
    }

    private void initViewModel() {
        viewModel.registerOnChange(new Observer<WdkugdnViewModel.WdkugnModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull WdkugdnViewModel.WdkugnModel wdkugnModel) {
                if(wdkugnModel.getWdkugdn() != null) {
                    wdkudgn.setMap(wdkugnModel.getWdkugdn());
                }

                if(wdkugnModel.getKfwdkmsnDefinitionTitle() != null) {
                    kfwdkmsnFileLabel.setText(wdkugnModel.getKfwdkmsnDefinitionTitle());
                }

                if(wdkugnModel.getWdkudgnDefinitionTitle() != null) {
                    wdkugdnFileLabel.setText(wdkugnModel.getWdkudgnDefinitionTitle());
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private JPanel getWdkugdnActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        JButton button = getWdkugdnDefinitionButton();
        panel.add(button, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        wdkugdnFileLabel = new JLabel("No File Selected");
        panel.add(wdkugdnFileLabel, constraints);

        return panel;
    }


    private JButton getWdkugdnDefinitionButton() {
        JButton button = new JButton("Set WDKUGDN Definition");

        button.addActionListener(e -> {
            Pair<TableDefinition, Map3d> tableDefinition = WdkugdnPreferences.getSelectedMap();

            if(tableDefinition != null) {
                MapPickerDialog.showDialog(panel, panel, "Select WDKUGDN", "Map Selection", tableDefinition.fst, WdkugdnPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select WDKUGDN", "Map Selection", null, WdkugdnPreferences::setSelectedMap);
            }
        });

        return button;
    }

    private JPanel getKfwdkmsnActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        JButton button = getKfwdkmsnDefinitionButton();
        panel.add(button, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        kfwdkmsnFileLabel = new JLabel("No File Selected");
        panel.add(kfwdkmsnFileLabel, constraints);

        return panel;
    }


    private JButton getKfwdkmsnDefinitionButton() {
        JButton button = new JButton("Set KFMSNWDK Definition");

        button.addActionListener(e -> {
            Pair<TableDefinition, Map3d> tableDefinition = KfwdkmsnPreferences.getSelectedMap();

            if(tableDefinition != null) {
                MapPickerDialog.showDialog(panel, panel, "Select KFMSNWDK", "Map Selection", tableDefinition.fst, KfwdkmsnPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select KFMSNWDK", "Map Selection", null,KfwdkmsnPreferences::setSelectedMap);
            }
        });

        return button;
    }

}
