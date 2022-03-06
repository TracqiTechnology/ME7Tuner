package presentation.map.map;

import io.reactivex.subjects.PublishSubject;
import domain.math.map.Map3d;
import domain.util.Debouncer;
import domain.util.Util;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class MapTable extends JList implements TableModelListener {
    private final JTable table;
    private Double[] rowHeaders;
    private Object[] columnHeaders;
    private Double[][] data;
    private DefaultTableModel tableModel;
    private final JScrollPane scrollPane;

    private final PublishSubject<Map3d> publishSubject;
    private final Debouncer debouncer;

    private Map3d map3d;

    private double maxValue;
    private double minValue;

    private boolean editable = true;

    @SuppressWarnings("unchecked")
    private MapTable(Double[] rowHeaders, Object[] columnHeaders, Double[][] data) {
        this.table = this.createTable(columnHeaders, data);
        // Handle Copy/Paste
        new MapTableExcelAdapter(this);
        this.rowHeaders = rowHeaders;
        this.columnHeaders = columnHeaders;
        this.data = data;
        this.debouncer = new Debouncer();

        map3d = new Map3d();
        if (columnHeaders instanceof Double[]) {
            map3d.xAxis = (Double[]) columnHeaders;
        }
        map3d.yAxis = rowHeaders;
        map3d.zAxis = data;

        this.publishSubject = PublishSubject.create();

        setAutoscrolls(false);
        setCellRenderer(new RowHeaderRenderer());
        setFixedCellHeight(table.getRowHeight());
        setFixedCellWidth(50);
        setFocusable(false);
        setModel(new TableListModel());
        setSelectionModel(table.getSelectionModel());
        table.getModel().addTableModelListener(this);
        scrollPane = new JScrollPane(table);
        scrollPane.setRowHeaderView(this);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        updateHeight();
    }

    private void updateHeight() {
        table.setPreferredScrollableViewportSize(
                new Dimension(
                        table.getPreferredSize().width,
                        table.getRowHeight() * rowHeaders.length));
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public PublishSubject<Map3d> getPublishSubject() {
        return publishSubject;
    }

    public JTable getTable() {
        return table;
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

    public void setMap(Map3d map3d) {
        this.map3d = map3d;
        setColumnHeaders(map3d.xAxis);
        setRowHeaders(map3d.yAxis);
        setTableData(map3d.zAxis);

        updateHeight();
    }

    public Map3d getMap3d() {
        return this.map3d;
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
        private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

        RowHeaderRenderer() {
            setHorizontalAlignment(CENTER);
            setOpaque(false);
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

            if(rowHeaders[index] != null) {
                setText(decimalFormat.format(rowHeaders[index]));
            } else {
                setText("0.0");
            }

            return this;
        }
    }

    /*
     *  Use the data row header properties to render each cell
     */
    private static class ColumnHeaderRenderer extends JLabel implements TableCellRenderer {

        private final DecimalFormat formatter = new DecimalFormat("#.##");

        ColumnHeaderRenderer() {
            setBorder(BorderFactory.createRaisedBevelBorder());
            setFont(getFont().deriveFont(11.0f));
            setHorizontalAlignment(CENTER);
            setForeground(Color.WHITE);
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            if (table.isColumnSelected(column)) {
                setBackground(Util.newColorWithAlpha(Color.YELLOW, 50));
            } else {
                setBackground(table.getTableHeader().getBackground());
            }

            try {
                setText(formatter.format(Double.parseDouble(value.toString())));
            } catch (NumberFormatException e) {
                setText("0.0");
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

        private final DecimalFormat formatter = new DecimalFormat("#.##");

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
            c.setForeground(Color.WHITE);

            if (value != null) {
                value = formatter.format(value);
            } else {
                value = 0.0;
            }

            boolean hasSelection = table.getSelectedColumns().length > 0;

            if (hasSelection && table.hasFocus()) {
                if (column >= table.getSelectedColumns()[0] && column <= table.getSelectedColumns()[table.getSelectedColumns().length - 1] && row >= table.getSelectedRows()[0] && row <= table.getSelectedRows()[table.getSelectedRows().length - 1]) {
                    setBackground(Util.newColorWithAlpha(Color.CYAN, 50));
                } else {
                    if (value instanceof String) {
                        double v = Double.parseDouble((String) value);
                        double norm;
                        if (maxValue - minValue != 0) {
                            norm = 1 - (v - minValue) / (maxValue - minValue);
                            setBackground(getColor(norm));
                        } else {
                            setBackground(Color.GREEN);
                        }
                    } else {
                        setBackground(null);
                    }
                }
            } else {
                if (value instanceof String) {
                    try {
                        double v = Double.parseDouble((String) value);
                        double norm;
                        if (maxValue - minValue != 0) {
                            norm = 1 - (v - minValue) / (maxValue - minValue);
                            setBackground(getColor(norm));
                        } else {
                            setBackground(Color.GREEN);
                        }
                    } catch (NumberFormatException e) {
                        setBackground(null);
                    }
                } else {
                    setBackground(null);
                }
            }

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        private Color getColor(double value) {
            double H = value * 0.4; // Hue (note 0.4 = Green, see huge chart below)
            double S = 0.9; // Saturation
            double B = 0.9; // Brightness

            return Color.getHSBColor((float) H, (float) S, (float) B);
        }
    }

    private void setRange(Double[][] data) {
        if (data != null) {
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            for (Double[] doubles : data) {
                for (Double value : doubles) {
                    if (value != null) {
                        max = Math.max(max, value);
                        min = Math.min(min, value);
                    }
                }
            }

            this.maxValue = max;
            this.minValue = min;
        } else {
            this.minValue = 0;
            this.maxValue = 0;
        }
    }

    public void setTableData(Double[][] data) {
        this.data = data;
        setRange(data);
        tableModel.setDataVector(this.data, this.columnHeaders);
        enforceTableColumnWidth(this.table);
        updateHeight();
    }

    public void setColumnHeaders(Double[] columnHeaders) {
        this.columnHeaders = columnHeaders;
        tableModel.setDataVector(this.data, this.columnHeaders);
        enforceTableColumnWidth(this.table);
    }

    public void setRowHeaders(Double[] rowHeaders) {
        this.rowHeaders = rowHeaders;
        updateHeight();
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    private JTable createTable(Object[] columnHeaders, final Double[][] data) {
        setRange(data);

        tableModel = new DefaultTableModel(data, columnHeaders) {
            @Override
            public Class<?> getColumnClass(int column) {
                return Double.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                //all cells false
                return editable;
            }
        };

        final JTable table = new JTable(tableModel) {
            @Override
            protected JTableHeader createDefaultTableHeader() {
                // subclassing to take advantage of super's auto-wiring
                // as ColumnModelListener
                return new JTableHeader(getColumnModel()) {

                    @Override
                    public void columnSelectionChanged(ListSelectionEvent e) {
                        repaint();
                    }

                };
            }
        };

        table.getTableHeader().setDefaultRenderer(new ColumnHeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.setIntercellSpacing(new Dimension(1,1));
        table.setShowGrid(true);
        table.setGridColor(Color.BLACK);
        table.setRowSelectionAllowed(false);

        enforceTableColumnWidth(table);

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

                debouncer.debounce(this, () -> {
                    map3d.xAxis = (Double[]) getColumnHeaders();
                    map3d.yAxis = getRowHeaders();
                    map3d.zAxis = values;

                    publishSubject.onNext(map3d);
                }, 100, TimeUnit.MILLISECONDS);
            }
        });

        return table;
    }

    public static MapTable getMapTable(Double[] rowHeaders, Object[] columnHeaders, final Double[][] data) {
        return new MapTable(rowHeaders, columnHeaders, data);
    }
}
