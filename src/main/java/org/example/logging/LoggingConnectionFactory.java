package org.example.logging;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LoggingConnectionFactory {

    private static final String URL = "jdbc:mysql://localhost:3306/myPodDB";
    private static final String USER = "user";
    private static final String PASSWORD = "pass";

    public static Connection getConnection() throws SQLException {

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
