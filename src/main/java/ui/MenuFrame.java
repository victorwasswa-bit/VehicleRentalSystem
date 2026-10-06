package ui;

import javax.swing.*;
import java.awt.*;

public class MenuFrame extends JFrame {

    public MenuFrame() {
        setTitle("Vehicle Rental System - Main Menu");
        setSize(760, 420);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel heading = new JLabel("Vehicle Rental System", SwingConstants.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 4, 15, 15));
        grid.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Each button holds {label shown, table name in the database}
        grid.add(groupButton("Users & Employees", new String[][]{
                {"Users", "users"}, {"Employees", "employees"}}));
        grid.add(groupButton("Branches", new String[][]{
                {"Branches", "branches"}}));
        grid.add(groupButton("Customers", new String[][]{
                {"Customers", "customers"}, {"Reviews", "reviews"}}));
        grid.add(groupButton("Vehicles", new String[][]{
                {"Brands", "brands"}, {"Vehicle Categories", "vehicle_categories"},
                {"Vehicles", "vehicles"}}));
        grid.add(groupButton("Reservations & Rentals", new String[][]{
                {"Reservations", "reservations"}, {"Rentals", "rentals"}}));
        grid.add(groupButton("Payments & Fines", new String[][]{
                {"Payments", "payments"}, {"Fines", "fines"}}));
        grid.add(groupButton("Maintenance, Insurance & Damages", new String[][]{
                {"Maintenance", "maintenance"}, {"Insurance", "insurance"},
                {"Damages", "damages"}}));

        JButton btnReports = new JButton("Reports & Calender");
        btnReports.addActionListener(e -> {JPopupMenu menu = new JPopupMenu();
            JMenuItem report = new JMenuItem("Rentals Report");
            report.addActionListener(ev -> new ReportFrame().setVisible(true));
            JMenuItem calender = new JMenuItem("Rental Calender");
            calender.addActionListener(ev -> new CalendarFrame().setVisible(true));
            menu.add(report);
            menu.add(calender);
            menu.show(btnReports, 0, btnReports.getHeight());

        });
        grid.add(btnReports);

        add(grid, BorderLayout.CENTER);
    }

    // Makes a button. One table: opens it. Several tables: shows a small pick-list.
    private JButton groupButton(String label, String[][] items) {
        JButton button = new JButton("<html><center>" + label + "</center></html>");

        button.addActionListener(e -> {
            if (items.length == 1) {
                new TableFrame(items[0][0], items[0][1]).setVisible(true);
                return;
            }
            JPopupMenu menu = new JPopupMenu();
            for (String[] item : items) {
                JMenuItem menuItem = new JMenuItem(item[0]);
                menuItem.addActionListener(ev ->
                        new TableFrame(item[0], item[1]).setVisible(true));
                menu.add(menuItem);
            }
            menu.show(button, 0, button.getHeight());
        });
        return button;
    }
}