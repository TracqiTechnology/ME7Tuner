package ui.map.map;

import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import ui.map.ExcelAdapter;
import util.Debouncer;
import util.Util;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class MapTable extends JList implements TableModelListener {
    private JTable table;
    private Double[] rowHeaders;
    private Object[] columnHeaders;
    private Double[][] data;
    private DefaultTableModel tableModel;
    private JScrollPane scrollPane;

    private PublishSubject<Double[][]> publishSubject;
    private Debouncer debouncer;

    private int rollOverRowIndex = -1;
    private int rollOverColumnIndex = -1;

    @SuppressWarnings("unchecked")
    private MapTable(Double[] rowHeaders, Object[] columnHeaders, Double[][] data) {
        this.table = this.createTable(columnHeaders, data);
        this.rowHeaders = rowHeaders;
        this.columnHeaders = columnHeaders;
        this.data = data;
        this.debouncer = new Debouncer();

        this.publishSubject = PublishSubject.create();

        setAutoscrolls(false);
        setCellRenderer(new RowHeaderRenderer());
        setFixedCellHeight(table.getRowHeight());
        setFixedCellWidth(50);
        setFocusable(false);
        setModel(new TableListModel());
        setOpaque(false);
        setSelectionModel(table.getSelectionModel());
        table.getModel().addTableModelListener(this);
        RollOverListener rollOverListener = new RollOverListener();
        table.addMouseListener(rollOverListener);
        table.addMouseMotionListener(rollOverListener);
        scrollPane = new JScrollPane(table);
        scrollPane.setRowHeaderView(this);
        scrollPane.setMinimumSize(new Dimension(120, 100));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    public PublishSubject<Double[][]> getPublishSubject() {
        return publishSubject;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE) {
            repaint();
        }
    }

    public Double[][] getData() {
        return data;
    }

    public Object[] getColumnHeaders() {
        return columnHeaders;
    }

    public Double[] getRowHeaders() {
        return rowHeaders;
    }

    public void setMap(Map3d map) {
        setColumnHeaders(map.xAxis);
        setRowHeaders(map.yAxis);
        setTableData(map.data);
    }

    /*
     *  Use the data to implement the ListModel
     */
    class TableListModel extends AbstractListModel {
        public int getSize() {
            return table.getRowCount();
        }

        public Object getElementAt(int index) {
            return String.valueOf(index + 1);
        }
    }

    /*
     *  Use the data row header properties to render each cell
     */
    class RowHeaderRenderer extends DefaultListCellRenderer {
        private DecimalFormat decimalFormat = new DecimalFormat("#.####");

        RowHeaderRenderer() {
            setHorizontalAlignment(CENTER);
            setOpaque(true);
            setBorder(BorderFactory.createRaisedBevelBorder());
            setFont(table.getTableHeader().getFont());
            setBackground(table.getTableHeader().getBackground());
            setForeground(table.getTableHeader().getForeground());
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(Util.newColorWithAlpha(Color.YELLOW, 50));
            } else {
                setBackground(table.getTableHeader().getBackground());
            }

            setText(decimalFormat.format(rowHeaders[index]));

            return this;
        }
    }

    /*
     *  Use the data row header properties to render each cell
     */
    private class ColumnHeaderRenderer extends JLabel implements TableCellRenderer {

        ColumnHeaderRenderer() {
            setBorder(BorderFactory.createRaisedBevelBorder());
            setFont(getFont().deriveFont(11.0f));
            setOpaque(true);
            setHorizontalAlignment(CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            setText(value.toString());
            setFont(getFont().deriveFont(11.0f));

            boolean hasSelection = table.getSelectedColumns().length > 0;

            if (hasSelection) {
                if (column >= table.getSelectedColumns()[0] && column <= table.getSelectedColumns()[table.getSelectedColumns().length - 1]) {
                    setBackground(Util.newColorWithAlpha(Color.YELLOW, 50));
                } else {
                    setBackground(null);
                }
            } else {
                setBackground(null);
            }

            return this;
        }
    }

    private void enforceTableColumnWidth(JTable table) {
        TableColumn column;
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(55);
            column.setMaxWidth(55);

            DefaultTableCellRenderer centerRenderer = new DecimalFormatRenderer();
            centerRenderer.setHorizontalAlignment(JLabel.CENTER);
            column.setCellRenderer(centerRenderer);
        }
    }

    private class DecimalFormatRenderer extends DefaultTableCellRenderer {

        private final DecimalFormat formatter = new DecimalFormat("#.00");

        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (value != null) {
                value = formatter.format(value);
            } else {
                value = 0.0;
            }

            boolean hasSelection = table.getSelectedColumns().length > 0;

            if (hasSelection) {
                if (column >= table.getSelectedColumns()[0] && column <= table.getSelectedColumns()[table.getSelectedColumns().length - 1] && row >= table.getSelectedRows()[0] && row <= table.getSelectedRows()[table.getSelectedRows().length - 1]) {
                    setBackground(Util.newColorWithAlpha(Color.CYAN, 50));
                } else {
                    setBackground(null);
                }
            } else {
                setBackground(null);
            }

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    public void setTableData(Double[][] data) {
        this.data = data;

        tableModel.setDataVector(this.data, this.columnHeaders);

        enforceTableColumnWidth(this.table);
    }

    public void setColumnHeaders(Double[] columnHeaders) {
        this.columnHeaders = columnHeaders;
        tableModel.setDataVector(this.data, this.columnHeaders);

        enforceTableColumnWidth(this.table);
    }

    public void setRowHeaders(Double[] rowHeaders) {
        this.rowHeaders = rowHeaders;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    private class RollOverListener extends MouseInputAdapter {
        @Override
        public void mouseExited(MouseEvent e) {
            rollOverRowIndex = -1;
            rollOverColumnIndex = -1;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            rollOverRowIndex = table.rowAtPoint(e.getPoint());
            rollOverColumnIndex = table.columnAtPoint(e.getPoint());
            table.getTableHeader().updateUI();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            rollOverRowIndex = table.rowAtPoint(e.getPoint());
            rollOverColumnIndex = table.columnAtPoint(e.getPoint());
            table.getTableHeader().updateUI();
        }
    }

    private JTable createTable(Object[] columnHeaders, final Double[][] data) {
        tableModel = new DefaultTableModel(data, columnHeaders) {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<?> getColumnClass(int column) {
                if (columnHeaders instanceof Double[]) {
                    return Double.class;
                } else if (columnHeaders instanceof String[]) {
                    return String.class;
                }

                return Double.class;
            }
        };

        final JTable table = new JTable(tableModel)
        {
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
            {
                Component c = super.prepareRenderer(renderer, row, column);

                if(row == rollOverRowIndex && column == rollOverColumnIndex) {
                    c.setBackground(Color.BLUE);
                }

                return c;
            }
        };

        table.getTableHeader().setDefaultRenderer(new ColumnHeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setGridColor(Color.BLACK);
        table.setRowSelectionAllowed(false);

        enforceTableColumnWidth(table);

        // Handle Copy/Paste
        ExcelAdapter excelAdapter = new ExcelAdapter(table);

        table.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                Double[][] values = new Double[MapTable.this.data.length][];

                for (int i = 0; i < MapTable.this.data.length; i++) {
                    values[i] = new Double[MapTable.this.data[i].length];
                    for (int j = 0; j < MapTable.this.data[i].length; j++) {
                        values[i][j] = (Double) table.getModel().getValueAt(i, j);
                    }
                }

                MapTable.this.data = values;

                debouncer.debounce(this, new Runnable() {
                    @Override
                    public void run() {
                        publishSubject.onNext(values);
                    }
                }, 100, TimeUnit.MILLISECONDS);
            }
        });

        return table;
    }

    public static MapTable getMapTable(Double[] rowHeaders, Object[] columnHeaders, final Double[][] data) {
        return new MapTable(rowHeaders, columnHeaders, data);
    }
}
