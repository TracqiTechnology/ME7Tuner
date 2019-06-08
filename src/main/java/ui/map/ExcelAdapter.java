package ui.map;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;

/**
 * ExcelAdapter enables Copy-Paste Clipboard functionality on JTables.
 * The clipboard data format used by the adapter is compatible with
 * the clipboard format used by Excel. This provides for clipboard
 * interoperability between enabled JTables and Excel.
 */
public class ExcelAdapter implements ActionListener {

    private Clipboard system;
    private JTable jTable1;

    /**
     * The Excel Adapter is constructed with a
     * JTable on which it enables Copy-Paste and acts
     * as a Clipboard listener.
     */
    public ExcelAdapter(JTable myJTable) {
        jTable1 = myJTable;

        KeyStroke copy;
        KeyStroke paste;

        if(System.getProperty("os.name").toLowerCase().contains("mac")) {
            copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK, false);
            // Identifying the copy KeyStroke user can modify this
            // to copy on some other Key combination.
            paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK, false);
        } else {
            copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
            // Identifying the copy KeyStroke user can modify this
            // to copy on some other Key combination.
            paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK, false);
        }
        // Identifying the Paste KeyStroke user can modify this
        //to copy on some other Key combination.
        jTable1.registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);
        jTable1.registerKeyboardAction(this, "Paste", paste, JComponent.WHEN_FOCUSED);
        system = Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    /**
     * This method is activated on the Keystrokes we are listening to
     * in this implementation. Here it listens for Copy and Paste ActionCommands.
     * Selections comprising non-adjacent cells result in invalid selection and
     * then copy action cannot be performed.
     * Paste is done by aligning the upper left corner of the selection with the
     * 1st element in the current selection of the JTable.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().compareTo("Copy") == 0) {
            StringBuffer sbf = new StringBuffer();
            // Check to ensure we have selected only a contiguous block of
            // cells
            int numCols = jTable1.getSelectedColumnCount();
            int numRows = jTable1.getSelectedRowCount();
            int[] rowsSelected = jTable1.getSelectedRows();
            int[] colsSelected = jTable1.getSelectedColumns();
            if (!((numRows - 1 == rowsSelected[rowsSelected.length - 1] - rowsSelected[0] &&
                    numRows == rowsSelected.length) &&
                    (numCols - 1 == colsSelected[colsSelected.length - 1] - colsSelected[0] &&
                            numCols == colsSelected.length))) {
                JOptionPane.showMessageDialog(null, "Invalid Copy Selection",
                        "Invalid Copy Selection",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    sbf.append(jTable1.getValueAt(rowsSelected[i], colsSelected[j]));
                    if (j < numCols - 1) sbf.append("\t");
                }
                sbf.append("\n");
            }
            StringSelection stSel = new StringSelection(sbf.toString());
            system = Toolkit.getDefaultToolkit().getSystemClipboard();
            system.setContents(stSel, stSel);
        }
        if (e.getActionCommand().compareTo("Paste") == 0) {
            int startRow = (jTable1.getSelectedRows())[0];
            int startCol = (jTable1.getSelectedColumns())[0];
            try {
                String trString = (String) (system.getContents(this).getTransferData(DataFlavor.stringFlavor));
                StringTokenizer st1 = new StringTokenizer(trString, "\n");
                for (int i = 0; st1.hasMoreTokens(); i++) {
                    String rowString = st1.nextToken();
                    StringTokenizer st2 = new StringTokenizer(rowString, "\t");
                    for (int j = 0; st2.hasMoreTokens(); j++) {
                        String value = st2.nextToken();

                        if (startRow + i < jTable1.getRowCount() && startCol + j < jTable1.getColumnCount()) {
                            jTable1.setValueAt(Double.valueOf(value), startRow + i, startCol + j);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
