package presentation.view.ldrpid;

import data.contract.Me7LogFileContract;
import data.parser.bin.BinParser;
import data.preferences.MapPreferenceManager;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import domain.math.map.Map3d;
import domain.model.ldrpid.LdrpidCalculator;
import org.apache.commons.math3.util.Pair;
import data.parser.me7log.Me7LogParser;
import data.parser.xdf.TableDefinition;
import data.preferences.bin.BinFilePreferences;
import data.preferences.kfldimx.KfldimxPreferences;
import data.preferences.kfldrl.KfldrlPreferences;
import data.preferences.ldrpid.LdrpidPreferences;
import presentation.map.axis.MapAxis;
import presentation.map.map.MapTable;
import data.writer.BinWriter;
import presentation.viewmodel.kfzwop.KfzwopViewModel;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LdrpidView {
    private LdrpidCalculator.LdrpidResult ldrpidResult;

    private final MapTable nonLinearTable = MapTable.getMapTable(new Double[16], new Double[10], new Double[16][10]);
    private final MapTable linearTable = MapTable.getMapTable(new Double[16], new Double[10], new Double[16][10]);
    private final MapTable kfldrlTable = MapTable.getMapTable(new Double[16], new Double[10], new Double[16][10]);
    private final MapTable kfldimxTable = MapTable.getMapTable(new Double[16], new Double[8], new Double[16][8]);
    private final MapAxis kfldimxXAxis = MapAxis.getMapAxis(new Double[1][8]);

    private JLabel logFileLabel;

    private final JProgressBar dpb = new JProgressBar();
    private JPanel panel;

    public JPanel getPanel() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.BOTH;

        constraints.weightx = 0.5;
        constraints.weighty = 0.5;

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getNonLinearMapPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;

        panel.add(getLinearMapPanel(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 84;

        panel.add(getKflDrlMapPanel(), constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        panel.add(getKfldimxMapPanel(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.insets.top = 0;

        panel.add(getLogsButton(panel), constraints);

        initObservers();

        return panel;
    }

    private void initObservers() {

        BinParser.getInstance().registerMapListObserver(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull List<Pair<TableDefinition, Map3d>> pairs) {
                initKfldrlMap();
                initKflimxMap();
                initKfldimxAxis();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        KfldrlPreferences.getInstance().registerOnMapChanged(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                initKfldrlMap();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        KfldimxPreferences.getInstance().registerOnMapChanged(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull Optional<Pair<TableDefinition, Map3d>> tableDefinitionMap3dPair) {
                initKflimxMap();
                initKfldimxAxis();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        MapPreferenceManager.registerOnClear(new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
            }

            @Override
            public void onNext(@NonNull Boolean aBoolean) {
                SwingUtilities.invokeLater(() -> {
                    nonLinearTable.setMap(new Map3d(new Double[10], new Double[16], new Double[16][10]));
                    linearTable.setMap(new Map3d(new Double[10], new Double[16], new Double[16][10]));
                    kfldrlTable.setMap(new Map3d(new Double[10], new Double[16], new Double[16][10]));
                    kfldimxTable.setMap(new Map3d(new Double[8], new Double[16], new Double[16][8]));
                    kfldimxXAxis.setTableData(new Double[1][8]);
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

    private JPanel getHeader(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;

        JLabel label = new JLabel(title);
        panel.add(label, c);

        return panel;
    }

    private void initKfldimxAxis() {
        Double[][] kfldImxXAxisValues = new Double[1][];

        Pair<TableDefinition, Map3d> tableDefinition = KfldimxPreferences.getInstance().getSelectedMap();
        if(tableDefinition != null && tableDefinition.getSecond() != null) {
            kfldImxXAxisValues[0] = tableDefinition.getSecond().xAxis;
        }
        kfldimxXAxis.setTableData(kfldImxXAxisValues);
    }

    private JPanel getKfldimxMapPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(new JLabel("KFLDIMX X-Axis"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        initKfldimxAxis();

        panel.add(kfldimxXAxis.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        panel.add(getHeader("KFLDIMX"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;

        initKflimxMap();

        panel.add(kfldimxTable.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;

        panel.add(getWriteKfldimxFileButton(), constraints);

        return panel;
    }

    private JButton getWriteKfldimxFileButton() {
        JButton button = new JButton("Write KFLDIMX");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFLDIMX to the binary?",
                    "Write KFLDIMX",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfldimxPreferences.getInstance().getSelectedMap().getFirst(), kfldimxTable.getMap3d());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        return button;
    }

    private JPanel getKflDrlMapPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("KFLDRL"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        initKfldrlMap();

        panel.add(kfldrlTable.getScrollPane(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        panel.add(getWriteKfldrlFileButton(), constraints);

        return panel;
    }

    private JButton getWriteKfldrlFileButton() {
        JButton button = new JButton("Write KFLDRL");

        button.addActionListener(e -> {
            int returnValue = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to write KFLDRL to the binary?",
                    "Write KFLDRL",
                    JOptionPane.YES_NO_OPTION);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    BinWriter.getInstance().write(BinFilePreferences.getInstance().getFile(), KfldrlPreferences.getInstance().getSelectedMap().getFirst(), kfldrlTable.getMap3d());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        return button;
    }

    private JPanel getLogsButton(JPanel parent) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        JButton button = new JButton("Load ME7 Logs");
        button.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setCurrentDirectory(LdrpidPreferences.getDirectory());

            int returnValue = fc.showOpenDialog(parent);

            if (returnValue == JFileChooser.APPROVE_OPTION) {

                SwingUtilities.invokeLater(() -> {
                    LdrpidPreferences.setDirectory(fc.getSelectedFile().getParentFile());

                    logFileLabel.setText(fc.getSelectedFile().getPath());

                    SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        public Void doInBackground() {
                            Me7LogParser parser = new Me7LogParser();
                            Map<Me7LogFileContract.Header, List<Double>> values = parser.parseLogDirectory(Me7LogParser.LogType.LDRPID, fc.getSelectedFile(), (value, max) -> {
                                SwingUtilities.invokeLater(() -> {
                                    dpb.setMaximum(max);
                                    dpb.setValue(value);
                                    dpb.setVisible(value < max - 1);
                                });
                            });
                            Pair<TableDefinition, Map3d> kfldimxTableDefinition = KfldimxPreferences.getInstance().getSelectedMap();
                            Pair<TableDefinition, Map3d> kfldrlTableDefinition = KfldrlPreferences.getInstance().getSelectedMap();

                            ldrpidResult = LdrpidCalculator.caclulateLdrpid(values, kfldrlTableDefinition.getSecond(), kfldimxTableDefinition.getSecond());
                            return null;
                        }

                        @Override
                        public void done() {
                            nonLinearTable.setMap(ldrpidResult.nonLinearOutput);
                            linearTable.setMap(ldrpidResult.linearOutput);
                            kfldrlTable.setMap(ldrpidResult.kfldrl);

                            Double[][] kfldImxXAxisValues = new Double[1][];
                            kfldImxXAxisValues[0] = ldrpidResult.kfldimx.xAxis;
                            kfldimxXAxis.setTableData(kfldImxXAxisValues);
                            kfldimxTable.setMap(ldrpidResult.kfldimx);
                        }
                    };

                    worker.execute();
                });
            }
        });

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;

        panel.add(button, constraints);

        constraints.insets.top = 8;
        constraints.gridx = 0;
        constraints.gridy = 1;

        dpb.setIndeterminate(false);
        dpb.setVisible(false);
        panel.add(dpb, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;

        logFileLabel = new JLabel("No File Selected");
        panel.add(logFileLabel, constraints);

        return panel;
    }

    private JPanel getNonLinearMapPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("Non Linear Boost"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        initNonLinearMap();

        JScrollPane scrollPane = nonLinearTable.getScrollPane();

        panel.add(scrollPane, constraints);

        return panel;
    }

    private JPanel getLinearMapPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(getHeader("Linear Boost"), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets.top = 16;

        initLinearMap();

        JScrollPane kfmiopMapScrollPane = linearTable.getScrollPane();

        panel.add(kfmiopMapScrollPane, constraints);

        return panel;
    }

    private void initKflimxMap() {
        Pair<TableDefinition, Map3d> tableDefinition = KfldimxPreferences.getInstance().getSelectedMap();
        kfldimxTable.setEditable(false);
        if(tableDefinition != null && tableDefinition.getSecond() != null) {
            kfldimxTable.setMap(tableDefinition.getSecond());
        }
    }

    private void initKfldrlMap() {
        Pair<TableDefinition, Map3d> tableDefinition = KfldrlPreferences.getInstance().getSelectedMap();
        kfldrlTable.setEditable(false);
        if(tableDefinition != null && tableDefinition.getSecond() != null) {
            kfldrlTable.setMap(tableDefinition.getSecond());
        }
    }

    private void initNonLinearMap() {
        Pair<TableDefinition, Map3d> tableDefinition = KfldrlPreferences.getInstance().getSelectedMap();

        if(tableDefinition != null && tableDefinition.getSecond() != null) {
            Map3d nonLinearMap = new Map3d(tableDefinition.getSecond());
            nonLinearMap.zAxis = new Double[nonLinearMap.zAxis.length][nonLinearMap.zAxis[0].length];
            nonLinearTable.setMap(nonLinearMap);
        }

        nonLinearTable.getPublishSubject().subscribe(new Observer<>() {
            @Override
            public void onNext(@NonNull Map3d map3d) {

                Pair<TableDefinition, Map3d> kfldimxTableDefinition = KfldimxPreferences.getInstance().getSelectedMap();
                Pair<TableDefinition, Map3d> kfldrlTableDefinition = KfldrlPreferences.getInstance().getSelectedMap();

                Map3d linearMap3d = LdrpidCalculator.calculateLinearTable(map3d.zAxis, kfldrlTableDefinition.getSecond());
                Map3d kfldrlMap3d = LdrpidCalculator.calculateKfldrl(map3d.zAxis, linearMap3d.zAxis, kfldrlTableDefinition.getSecond());
                Map3d kfldimxMap3d = LdrpidCalculator.calculateKfldimx(map3d.zAxis, linearMap3d.zAxis, kfldrlTableDefinition.getSecond(), kfldimxTableDefinition.getSecond());

                linearTable.setMap(linearMap3d);
                kfldrlTable.setMap(kfldrlMap3d);

                Double[][] kfldImxXAxisValues = new Double[1][];
                kfldImxXAxisValues[0] = kfldimxMap3d.xAxis;
                kfldimxXAxis.setTableData(kfldImxXAxisValues);
                kfldimxTable.setMap(kfldimxMap3d);
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

    private void initLinearMap() {
        Pair<TableDefinition, Map3d> tableDefinition = KfldrlPreferences.getInstance().getSelectedMap();

        linearTable.setEditable(false);

        if(tableDefinition != null && tableDefinition.getSecond() != null) {
            Map3d linearMap = new Map3d(tableDefinition.getSecond());
            linearMap.zAxis = new Double[linearMap.zAxis.length][linearMap.zAxis[0].length];
            linearTable.setMap(linearMap);
        }
    }
}
