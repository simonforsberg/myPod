package org.example.logging;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LoggingConnectionFactory {

    private static final String URL = "jdbc:mysql://localhost:3306/myPodDB";
    private static final String USER = "user";
    private static final String PASSWORD = "pass";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Ensure driver is loaded
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
