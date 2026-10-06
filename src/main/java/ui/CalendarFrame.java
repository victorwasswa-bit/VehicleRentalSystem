package ui;

import com.toedter.calendar.JCalendar;
import util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;

/** A calendar. Click a day to see which vehicles are out on rent on that day. */
public class CalendarFrame extends JFrame {

    private final JCalendar calendar = new JCalendar();
    private final JLabel heading = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Customer", "Plate", "Model", "Rent Date", "Return Date", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public CalendarFrame() {
        setTitle("Rental Calendar");
        setSize(1000, 450);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        heading.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));

        add(heading, BorderLayout.NORTH);
        add(calendar, BorderLayout.WEST);
        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

        // Runs every time the user clicks a different day
        calendar.addPropertyChangeListener("calendar", e -> loadRentals());

        loadRentals();
    }

    private void loadRentals() {
        java.sql.Date day = new java.sql.Date(calendar.getDate().getTime());
        heading.setText("Vehicles on rent on "
                + new SimpleDateFormat("EEEE, d MMMM yyyy").format(day));
        model.setRowCount(0);

        String sql = "SELECT r.id, c.full_name, v.plate_number, v.model, "
                + "r.rent_date, r.return_date, r.status "
                + "FROM rentals r "
                + "JOIN customers c ON r.customer_id = c.id "
                + "JOIN vehicles v ON r.vehicle_id = v.id "
                + "WHERE r.rent_date <= ? AND (r.return_date IS NULL OR r.return_date >= ?) "
                + "ORDER BY r.rent_date";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, day);
            ps.setDate(2, day);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getDate(5), rs.getDate(6), rs.getString(7)});
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}