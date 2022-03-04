package ui.view.configuration;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import org.apache.commons.math3.util.Pair;
import parser.xdf.TableDefinition;
import preferences.MapPreference;
import preferences.MapPreferenceManager;
import preferences.kfldimx.KfldimxPreferences;
import preferences.kfldrl.KfldrlPreferences;
import preferences.kfmiop.KfmiopPreferences;
import preferences.kfmirl.KfmirlPreferences;
import preferences.kfvpdksd.KfvpdksdPreferences;
import preferences.kfwdkmsn.KfwdkmsnPreferences;
import preferences.kfzw.KfzwPreferences;
import preferences.kfzwop.KfzwopPreferences;
import preferences.krkte.KrktePreferences;
import preferences.mlhfm.MlhfmPreferences;
import preferences.wdkugdn.WdkugdnPreferences;
import ui.view.map.MapPickerDialog;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class TableDefinitionView {
    private static final Insets WEST_INSETS = new Insets(5, 0, 5, 5);
    private static final Insets EAST_INSETS = new Insets(5, 5, 5, 0);

    private static final String UNDEFINED = "Undefined";

    private final JPanel panel = new JPanel();

    enum FieldTitle {
        KRKTE("KRKTE", "-", UNDEFINED, KrktePreferences.getInstance()),
        MLHFM("MLHFM", "-", UNDEFINED, MlhfmPreferences.getInstance()),
        KFMIOP("KFMIOP", "-", UNDEFINED, KfmiopPreferences.getInstance()),
        KFMIRL("KFMIRL", "-", UNDEFINED, KfmirlPreferences.getInstance()),
        KFZWOP("KFZWOP", "-", UNDEFINED, KfzwopPreferences.getInstance()),
        KFZW("KFZW", "-", UNDEFINED, KfzwPreferences.getInstance()),
        KFVPDKSD("KFVPDKSD", "-", UNDEFINED, KfvpdksdPreferences.getInstance()),
        WDKUGDN("WDKUGDN", "-", UNDEFINED, WdkugdnPreferences.getInstance()),
        KFWDKMSN("KFWDKMSN", "-", UNDEFINED, KfwdkmsnPreferences.getInstance()),
        KFLDRL("KFLDRL", "-", UNDEFINED, KfldrlPreferences.getInstance()),
        KFLDIMX("KFLDIMX", "-", UNDEFINED, KfldimxPreferences.getInstance());

        private final String title;
        private final String units;
        private final String definition;
        private final MapPreference mapPreference;

        FieldTitle(String title, String units, String definition, MapPreference mapPreference) {
            this.title = title;
            this.units = units;
            this.definition = definition;
            this.mapPreference = mapPreference;
        }

        public String getTitle() {
            return title;
        }

        public String getUnits() {
            return units;
        }

        public String getDefinition() {
            return definition;
        }

        public MapPreference getMapPreference() {
            return mapPreference;
        }
    }

    public JPanel getPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;

        panel.add(this.panel, constraints);

        return panel;
    }

    public TableDefinitionView() {
        initialize();

        MapPreferenceManager.registerOnClear(new Observer<Boolean>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Boolean aBoolean) {
                initialize();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initialize() {
        panel.removeAll();
        panel.revalidate();
        panel.repaint();

        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("MAP Definitions"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        GridBagConstraints gbc;

        for (int i = 0; i < FieldTitle.values().length; i++) {
            FieldTitle fieldTitle = FieldTitle.values()[i];
            gbc = createGbc(0, i);
            panel.add(new JLabel(fieldTitle.getTitle() + ":", JLabel.LEFT), gbc);

            gbc = createGbc(1, i);
            JLabel definitionLabel = new JLabel(fieldTitle.getDefinition(), JLabel.LEFT);
            panel.add(definitionLabel, gbc);

            gbc = createGbc(2, i);
            JLabel unitLabel = new JLabel(fieldTitle.getUnits(), JLabel.LEFT);
            panel.add(unitLabel, gbc);

            gbc = createGbc(3, i);

            addField(fieldTitle, fieldTitle.mapPreference, gbc, definitionLabel, unitLabel);
        }
    }

    private void addField(FieldTitle fieldTitle, MapPreference preference, GridBagConstraints gbc, JLabel definitionLabel, JLabel unitLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = preference.getSelectedMap();

       preference.registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.getFirst().getTableName()));
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> unitLabel.setText(definitionMap3dPair.getFirst().getZAxis().getUnit()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });

        if (tableDefinition != null) {
            definitionLabel.setText(tableDefinition.getFirst().getTableName());
            unitLabel.setText(tableDefinition.getFirst().getZAxis().getUnit());
        }

        button.addActionListener(e -> {
            if (tableDefinition != null) {
                definitionLabel.setText(tableDefinition.getFirst().getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.getFirst(), preference::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, preference::setSelectedMap);
            }
        });

        panel.add(button, gbc);
    }

    private GridBagConstraints createGbc(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.anchor = (x < 3) ? GridBagConstraints.WEST : GridBagConstraints.EAST;

        gbc.insets = (x < 3) ? WEST_INSETS : EAST_INSETS;
        gbc.weightx = (x < 3) ? 0.1 : 1.0;
        gbc.weighty = 1.0;
        return gbc;
    }
}

