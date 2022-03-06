package presentation.view.map;

import parser.xdf.XdfParser;
import parser.xdf.TableDefinition;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Use this modal dialog to let the user choose one string from a long
 * list.  See ListDialogRunner.java for an example of using ListDialog.
 * The basics:
 * <pre>
 * String[] choices = {"A", "long", "array", "of", "strings"};
 * String selectedName = ListDialog.showDialog(
 * componentInControllingFrame,
 * locatorComponent,
 * "A description of the list:",
 * "Dialog Title",
 * choices,
 * choices[0]);
 * </pre>
 */
public class MapPickerDialog extends JDialog
        implements ActionListener {
    private static MapPickerDialog dialog;
    private static TableDefinition value;
    private final JList<TableDefinition> list;
    private final OnSelectedChangedListener onSelectedChangedListener;
    private final java.util.List<TableDefinition> tableDefinitionList = new ArrayList<>();

    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     */
    public static TableDefinition showDialog(Component frameComp,
                                             Component locationComp,
                                             String labelText,
                                             String title,
                                             TableDefinition initialValue,
                                             OnSelectedChangedListener onSelectedChangedListener) {
        Frame frame = JOptionPane.getFrameForComponent(frameComp);
        dialog = new MapPickerDialog(frame,
                locationComp,
                labelText,
                title,
                initialValue,
                onSelectedChangedListener);
        dialog.setSize(new Dimension(700, 500));
        dialog.setVisible(true);
        return value;
    }

    public interface OnSelectedChangedListener {
        void onSelectionChanged(TableDefinition selected);
    }

    private void setValue(TableDefinition newValue) {
        value = newValue;
        list.setSelectedValue(value, true);
    }

    private MapPickerDialog(Frame frame,
                            Component locationComp,
                            String labelText,
                            String title,
                            TableDefinition initialValue,
                            OnSelectedChangedListener onSelectedChangedListener) {
        super(frame, title, true);

        this.onSelectedChangedListener = onSelectedChangedListener;

        //Create and initialize the buttons.
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        //
        final JButton setButton = new JButton("Set");
        setButton.setActionCommand("Set");
        setButton.addActionListener(this);
        getRootPane().setDefaultButton(setButton);

        DefaultListModel<TableDefinition> listModel = new DefaultListModel<>();

        for (TableDefinition tableDefinition : XdfParser.getInstance().getTableDefinitions()) {
            listModel.addElement(tableDefinition);
            tableDefinitionList.add(tableDefinition);
        }

        //main part of the dialog
        list = new JList<>(listModel);

        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(5);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setButton.doClick(); //emulate button click
                }
            }
        });
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 80));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        //Create a container so that we can add a title around
        //the scroll pane.  Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to bottom.
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        JLabel label = new JLabel(labelText);
        label.setLabelFor(list);
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(setButton);

        JTextField textField = createTextField();

        textField.setText(labelText.replaceFirst("Select ", ""));

        //Put everything together, using the content pane's BorderLayout.
        Container contentPane = getContentPane();
        contentPane.add(textField, BorderLayout.PAGE_START);
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);

        //Initialize values.
        setValue(initialValue);
        pack();
        setLocationRelativeTo(locationComp);
    }

    public void filterModel(DefaultListModel<TableDefinition> model, String filter) {
        for (TableDefinition definition : tableDefinitionList) {
            if (!definition.getTableName().toLowerCase(Locale.ROOT).startsWith(filter.toLowerCase(Locale.ROOT))) {
                if (model.contains(definition)) {
                    model.removeElement(definition);
                }
            } else {
                if (!model.contains(definition)) {
                    model.addElement(definition);
                }
            }
        }
    }

    //Handle clicks on the Set and Cancel buttons.
    public void actionPerformed(ActionEvent e) {
        if ("Set".equals(e.getActionCommand())) {
            MapPickerDialog.value = (list.getSelectedValue());
            onSelectedChangedListener.onSelectionChanged(MapPickerDialog.value);
        }
        MapPickerDialog.dialog.setVisible(false);
    }

    private JTextField createTextField() {
        final JTextField field = new JTextField(15);
        field.getDocument().addDocumentListener(new DocumentListener(){
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) {}
            private void filter() {
                String filter = field.getText();
                filterModel((DefaultListModel<TableDefinition>)list.getModel(), filter);
            }
        });
        return field;
    }
}
