package org.example;

import util.DBConnection;
import java.sql.*;

public class Main {
    public static void main(String[] args) {
        String sql = "SELECT username FROM users";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                System.out.println(rs.getString("username"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}