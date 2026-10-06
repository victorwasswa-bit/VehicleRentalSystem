package ui;

import com.toedter.calendar.JDateChooser;
import util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One reusable window that shows ANY database table and lets the user
 * add, edit and delete rows. Give it a title and a table name.
 * Linked columns (like customer_id) become dropdowns showing names,
 * and DATE columns get a date picker.
 */
public class TableFrame extends JFrame {

    private final String tableName;

    // The table's data lives in the model; the JTable only displays it.
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;   // edit through the form, not by typing in the grid
        }
    };
    private final JTable table = new JTable(model);

    // Information about the columns, read from the database itself
    private final List<String> columnNames = new ArrayList<>();
    private final List<Integer> columnTypes = new ArrayList<>();
    private final List<Boolean> autoIncrement = new ArrayList<>();
    private final List<Boolean> nullable = new ArrayList<>();

    // column name -> {table it points to, column it points to}
    private final Map<String, String[]> foreignKeys = new HashMap<>();

    /** One entry in a dropdown: the real id plus the text the user sees. */
    private static class Option {
        final Object id;
        final String label;

        Option(Object id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public TableFrame(String title, String tableName) {
        this.tableName = tableName;

        setTitle(title);
        setSize(900, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);   // closes only this window
        setLocationRelativeTo(null);

        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnAdd = new JButton("Add");
        JButton btnEdit = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");
        JButton btnRefresh = new JButton("Refresh");

        btnAdd.addActionListener(e -> showForm(false));
        btnEdit.addActionListener(e -> showForm(true));
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> loadData());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(btnAdd);
        buttons.add(btnEdit);
        buttons.add(btnDelete);
        buttons.add(btnRefresh);
        add(buttons, BorderLayout.SOUTH);

        loadData();
    }

    // ---------- READ ----------
    private void loadData() {
        model.setRowCount(0);
        columnNames.clear();
        columnTypes.clear();
        autoIncrement.clear();
        nullable.clear();
        foreignKeys.clear();

        String sql = "SELECT * FROM `" + tableName + "`";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            for (int i = 1; i <= n; i++) {
                columnNames.add(md.getColumnName(i));
                columnTypes.add(md.getColumnType(i));
                autoIncrement.add(md.isAutoIncrement(i));
                nullable.add(md.isNullable(i) != ResultSetMetaData.columnNoNulls);
            }
            model.setColumnIdentifiers(columnNames.toArray());

            while (rs.next()) {
                Object[] row = new Object[n];
                for (int i = 0; i < n; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                model.addRow(row);
            }

            // Ask the database which columns are links to other tables
            try (ResultSet fk = con.getMetaData().getImportedKeys(con.getCatalog(), null, tableName)) {
                while (fk.next()) {
                    foreignKeys.put(fk.getString("FKCOLUMN_NAME"),
                            new String[]{fk.getString("PKTABLE_NAME"), fk.getString("PKCOLUMN_NAME")});
                }
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // Builds the list of choices for a dropdown, e.g. all customers as "1 - Test Customer"
    private List<Option> loadOptions(String refTable, String refColumn, boolean allowNone) {
        List<Option> options = new ArrayList<>();
        if (allowNone) {
            options.add(new Option(null, "(none)"));
        }
        String sql = "SELECT * FROM `" + refTable + "` ORDER BY 1";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int labelCol = -1;      // the first text column becomes the visible name
            for (int i = 1; i <= md.getColumnCount(); i++) {
                int type = md.getColumnType(i);
                boolean isText = type == Types.VARCHAR || type == Types.CHAR
                        || type == Types.LONGVARCHAR;
                if (isText && !md.getColumnName(i).equalsIgnoreCase(refColumn)) {
                    labelCol = i;
                    break;
                }
            }
            while (rs.next()) {
                Object id = rs.getObject(refColumn);
                String label = id + (labelCol > 0 ? " - " + rs.getString(labelCol) : "");
                options.add(new Option(id, label));
            }
        } catch (SQLException ex) {
            showError(ex);
        }
        return options;
    }

    // ---------- ADD / EDIT FORM ----------
    private void showForm(boolean edit) {
        int row = table.getSelectedRow();
        if (edit && row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.");
            return;
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        List<JComponent> inputs = new ArrayList<>();

        for (int i = 0; i < columnNames.size(); i++) {
            if (autoIncrement.get(i)) {      // the id is generated by the database
                inputs.add(null);
                continue;
            }
            Object current = edit ? model.getValueAt(row, i) : null;
            String[] link = foreignKeys.get(columnNames.get(i));
            JComponent input;

            if (link != null) {                                // a link to another table
                JComboBox<Option> combo = new JComboBox<>();
                for (Option option : loadOptions(link[0], link[1], nullable.get(i))) {
                    combo.addItem(option);
                    if (current != null && option.id != null
                            && option.id.toString().equals(current.toString())) {
                        combo.setSelectedItem(option);
                    }
                }
                input = combo;
            } else if (columnTypes.get(i) == Types.DATE) {     // a date
                JDateChooser chooser = new JDateChooser();
                chooser.setDateFormatString("yyyy-MM-dd");
                if (current != null) {
                    chooser.setDate(java.sql.Date.valueOf(current.toString()));
                }
                input = chooser;
            } else {                                           // anything else
                input = new JTextField(current == null ? "" : current.toString(), 20);
            }
            inputs.add(input);
            form.add(new JLabel(columnNames.get(i) + ":"));
            form.add(input);
        }

        int choice = JOptionPane.showConfirmDialog(this, form,
                edit ? "Edit record" : "Add record",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        if (edit) {
            updateRow(row, inputs);
        } else {
            insertRow(inputs);
        }
        loadData();
    }

    // Reads the value out of whichever input component was used
    private Object valueOf(JComponent input) {
        if (input instanceof JComboBox) {
            Option chosen = (Option) ((JComboBox<?>) input).getSelectedItem();
            return chosen == null ? null : chosen.id;
        }
        if (input instanceof JDateChooser) {
            java.util.Date d = ((JDateChooser) input).getDate();
            return d == null ? null : new java.sql.Date(d.getTime());
        }
        String text = ((JTextField) input).getText().trim();
        return text.isEmpty() ? null : text;
    }

    // ---------- CREATE ----------
    private void insertRow(List<JComponent> inputs) {
        List<String> cols = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            if (inputs.get(i) == null) continue;
            cols.add("`" + columnNames.get(i) + "`");
            values.add(valueOf(inputs.get(i)));
        }
        String marks = String.join(",", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO `" + tableName + "` (" + String.join(",", cols)
                + ") VALUES (" + marks + ")";
        execute(sql, values);
    }

    // ---------- UPDATE ----------
    private void updateRow(int row, List<JComponent> inputs) {
        List<String> sets = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            if (inputs.get(i) == null) continue;
            sets.add("`" + columnNames.get(i) + "` = ?");
            values.add(valueOf(inputs.get(i)));
        }
        values.add(model.getValueAt(row, 0));      // the id of the selected row
        String sql = "UPDATE `" + tableName + "` SET " + String.join(", ", sets)
                + " WHERE `" + columnNames.get(0) + "` = ?";
        execute(sql, values);
    }

    // ---------- DELETE ----------
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Delete the selected record?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        String sql = "DELETE FROM `" + tableName + "` WHERE `" + columnNames.get(0) + "` = ?";
        execute(sql, List.of(model.getValueAt(row, 0)));
        loadData();
    }

    // ---------- shared helpers ----------
    private void execute(String sql, List<Object> params) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error",
                JOptionPane.ERROR_MESSAGE);
    }
}