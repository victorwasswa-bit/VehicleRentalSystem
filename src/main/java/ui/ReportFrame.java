package ui;

import com.toedter.calendar.JDateChooser;
import util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/** Rentals report: pick a From and To date, press Generate, get rows and totals. */
public class ReportFrame extends JFrame {

    private final JDateChooser fromChooser = new JDateChooser();
    private final JDateChooser toChooser = new JDateChooser();
    private final JLabel summary = new JLabel("Choose two dates and click Generate.");

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Customer", "Plate", "Model", "Rent Date",
                    "Return Date", "Total Cost", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public ReportFrame() {
        setTitle("Rentals Report");
        setSize(1000, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        fromChooser.setDateFormatString("yyyy-MM-dd");
        toChooser.setDateFormatString("yyyy-MM-dd");
        fromChooser.setPreferredSize(new Dimension(130, 26));
        toChooser.setPreferredSize(new Dimension(130, 26));

        JButton btnGenerate = new JButton("Generate");
        JButton btnPrint = new JButton("Print");
        btnGenerate.addActionListener(e -> generate());
        btnPrint.addActionListener(e -> print());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.add(new JLabel("From:"));
        top.add(fromChooser);
        top.add(new JLabel("To:"));
        top.add(toChooser);
        top.add(btnGenerate);
        top.add(btnPrint);

        summary.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        summary.setFont(summary.getFont().deriveFont(Font.BOLD, 13f));

        table.setRowHeight(24);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);
    }

    private void generate() {
        java.util.Date from = fromChooser.getDate();
        java.util.Date to = toChooser.getDate();

        if (from == null || to == null) {
            JOptionPane.showMessageDialog(this, "Please choose both dates.");
            return;
        }
        if (from.after(to)) {
            JOptionPane.showMessageDialog(this, "The From date must not be after the To date.");
            return;
        }

        model.setRowCount(0);

        String sql = "SELECT r.id, c.full_name, v.plate_number, v.model, "
                + "r.rent_date, r.return_date, r.total_cost, r.status "
                + "FROM rentals r "
                + "JOIN customers c ON r.customer_id = c.id "
                + "JOIN vehicles v ON r.vehicle_id = v.id "
                + "WHERE r.rent_date BETWEEN ? AND ? "
                + "ORDER BY r.rent_date";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, new java.sql.Date(from.getTime()));
            ps.setDate(2, new java.sql.Date(to.getTime()));

            int count = 0;
            double total = 0;

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getDate(5), rs.getDate(6), rs.getBigDecimal(7), rs.getString(8)});
                    count++;
                    total += rs.getDouble(7);
                }
            }
            summary.setText(count + " rental(s) found.   Total income: "
                    + String.format("%,.2f", total));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void print() {
        try {
            table.print();
        } catch (java.awt.print.PrinterException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Print error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}