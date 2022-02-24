package ui.view.configuration;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import math.map.Map3d;
import parser.xdf.TableDefinition;
import preferences.kfldimx.KfldimxPreferences;
import preferences.kfldrl.KfldrlPreferences;
import preferences.kfmiop.KfmiopPreferences;
import preferences.kfmirl.KfmirlPreferences;
import preferences.kfvpdksd.KfvpdksdPreferences;
import preferences.kfzw.KfzwPreferences;
import preferences.kfzwop.KfzwopPreferences;
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
        MLHFM("MLHFM",  UNDEFINED),
        KFMIOP("KFMIOP",  UNDEFINED),
        KFMIRL("KFMIRL",  UNDEFINED),
        KFZWOP("KFZWOP",  UNDEFINED),
        KFZW("KFZW",  UNDEFINED),
        KFVPDKSD("KFVPDKSD",  UNDEFINED),
        WDKUGDN("WDKUGDN",  UNDEFINED),
        KFLDRL("KFLDR",  UNDEFINED),
        KFLDIMX("KFLDIMX",  UNDEFINED);

        private final String title;
        private final String definition;

        FieldTitle(String title, String definition) {
            this.title = title;
            this.definition = definition;
        }

        public String getTitle() {
            return title;
        }

        public String getDefinition() {
            return definition;
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

            switch (fieldTitle) {
                case MLHFM:
                    addMlhfm(fieldTitle, gbc, definitionLabel);
                    break;
                case KFMIOP:
                    addKfmiop(fieldTitle, gbc, definitionLabel);
                    break;
                case KFMIRL:
                    addKfmirl(fieldTitle, gbc, definitionLabel);
                    break;
                case KFZWOP:
                    addKfzwop(fieldTitle, gbc, definitionLabel);
                    break;
                case KFZW:
                    addKfzw(fieldTitle, gbc, definitionLabel);
                    break;
                case KFVPDKSD:
                    addKfvpdksd(fieldTitle, gbc, definitionLabel);
                    break;
                case WDKUGDN:
                    addWdkugdn(fieldTitle, gbc, definitionLabel);
                    break;
                case KFLDRL:
                    addKfldrl(fieldTitle, gbc, definitionLabel);
                    break;
                case KFLDIMX:
                    addKfldimx(fieldTitle, gbc, definitionLabel);
                    break;
            }
        }
    }

    private void addMlhfm(FieldTitle fieldTitle, GridBagConstraints gbc, JLabel definitionLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = MlhfmPreferences.getSelectedMap();

        MlhfmPreferences.registerOnSelectedMlhfmChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) { }

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.fst.getTableName()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        if(tableDefinition != null) {
            definitionLabel.setText(tableDefinition.fst.getTableName());
        }

        button.addActionListener(e -> {
            if(tableDefinition != null) {
                definitionLabel.setText(tableDefinition.fst.getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.fst, MlhfmPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, MlhfmPreferences::setSelectedMap);
            }
        });

        panel.add(button, gbc);
    }

    private void addKfmiop(FieldTitle fieldTitle, GridBagConstraints gbc, JLabel definitionLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = KfmiopPreferences.getSelectedMap();

        KfmiopPreferences.registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.fst.getTableName()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        if(tableDefinition != null) {
            definitionLabel.setText(tableDefinition.fst.getTableName());
        }

        button.addActionListener(e -> {
            if(tableDefinition != null) {
                definitionLabel.setText(tableDefinition.fst.getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.fst, KfmiopPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, KfmiopPreferences::setSelectedMap);
            }
        });

        panel.add(button, gbc);
    }

    private void addKfmirl(FieldTitle fieldTitle, GridBagConstraints gbc, JLabel definitionLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = KfmirlPreferences.getSelectedMap();

        KfmirlPreferences.registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.fst.getTableName()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        if(tableDefinition != null) {
            definitionLabel.setText(tableDefinition.fst.getTableName());
        }

        button.addActionListener(e -> {
            if(tableDefinition != null) {
                definitionLabel.setText(tableDefinition.fst.getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.fst, KfmirlPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, KfmirlPreferences::setSelectedMap);
            }
        });

        panel.add(button, gbc);
    }

    private void addKfzwop(FieldTitle fieldTitle, GridBagConstraints gbc, JLabel definitionLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = KfzwopPreferences.getSelectedMap();

        KfzwopPreferences.registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.fst.getTableName()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        if(tableDefinition != null) {
            definitionLabel.setText(tableDefinition.fst.getTableName());
        }

        button.addActionListener(e -> {
            if(tableDefinition != null) {
                definitionLabel.setText(tableDefinition.fst.getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.fst, KfzwopPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, KfzwopPreferences::setSelectedMap);
            }
        });

        panel.add(button, gbc);
    }

    private void addKfzw(FieldTitle fieldTitle, GridBagConstraints gbc, JLabel definitionLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = KfzwPreferences.getSelectedMap();

        KfzwopPreferences.registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.fst.getTableName()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        if(tableDefinition != null) {
            definitionLabel.setText(tableDefinition.fst.getTableName());
        }

        button.addActionListener(e -> {
            if(tableDefinition != null) {
                definitionLabel.setText(tableDefinition.fst.getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.fst, KfzwPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, KfzwPreferences::setSelectedMap);
            }
        });

        panel.add(button, gbc);
    }

    private void addKfvpdksd(FieldTitle fieldTitle, GridBagConstraints gbc, JLabel definitionLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = KfvpdksdPreferences.getSelectedMap();

        KfvpdksdPreferences.registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.fst.getTableName()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        if(tableDefinition != null) {
            definitionLabel.setText(tableDefinition.fst.getTableName());
        }

        button.addActionListener(e -> {
            if(tableDefinition != null) {
                definitionLabel.setText(tableDefinition.fst.getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.fst, KfvpdksdPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, KfvpdksdPreferences::setSelectedMap);
            }
        });

        panel.add(button, gbc);
    }

    private void addWdkugdn(FieldTitle fieldTitle, GridBagConstraints gbc, JLabel definitionLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = WdkugdnPreferences.getSelectedMap();

        WdkugdnPreferences.registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.fst.getTableName()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        if(tableDefinition != null) {
            definitionLabel.setText(tableDefinition.fst.getTableName());
        }

        button.addActionListener(e -> {
            if(tableDefinition != null) {
                definitionLabel.setText(tableDefinition.fst.getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.fst, WdkugdnPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, WdkugdnPreferences::setSelectedMap);
            }
        });

        panel.add(button, gbc);
    }

    private void addKfldimx(FieldTitle fieldTitle, GridBagConstraints gbc, JLabel definitionLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = KfldimxPreferences.getSelectedMap();

        KfldimxPreferences.registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.fst.getTableName()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        if(tableDefinition != null) {
            definitionLabel.setText(tableDefinition.fst.getTableName());
        }

        button.addActionListener(e -> {
            if(tableDefinition != null) {
                definitionLabel.setText(tableDefinition.fst.getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.fst, KfldimxPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, KfldimxPreferences::setSelectedMap);
            }
        });

        panel.add(button, gbc);
    }

    private void addKfldrl(FieldTitle fieldTitle, GridBagConstraints gbc, JLabel definitionLabel) {
        final JButton button = new JButton("Select Definition");
        Pair<TableDefinition, Map3d> tableDefinition = KfldrlPreferences.getSelectedMap();

        KfldrlPreferences.registerOnMapChanged(new Observer<Optional<Pair<TableDefinition, Map3d>>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                tableDefinitionMap3dPair.ifPresent(definitionMap3dPair -> definitionLabel.setText(definitionMap3dPair.fst.getTableName()));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        if(tableDefinition != null) {
            definitionLabel.setText(tableDefinition.fst.getTableName());
        }

        button.addActionListener(e -> {
            if(tableDefinition != null) {
                definitionLabel.setText(tableDefinition.fst.getTableName());
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", tableDefinition.fst, KfldrlPreferences::setSelectedMap);
            } else {
                MapPickerDialog.showDialog(panel, panel, "Select " + fieldTitle.getTitle(), "Map Selection", null, KfldrlPreferences::setSelectedMap);
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

        gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;

        gbc.insets = (x == 0) ? WEST_INSETS : EAST_INSETS;
        gbc.weightx = (x == 0) ? 0.1 : 1.0;
        gbc.weighty = 1.0;
        return gbc;
    }
}

