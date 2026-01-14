package org.example;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class loggerManager {

    private static final Logger logger = LoggerFactory.getLogger(loggerManager.class);

    // JDBC connection details
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "myPodLogs";
    private static final String USER = "user";
    private static final String PASSWORD = "password";

    public static void setup() {
        try {
            // Step 1: Connect to MySQL server (no database selected yet)
            try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {

                // Create database if it does not exist
                String createDbSQL = "CREATE DATABASE IF NOT EXISTS " + DB_NAME;
                stmt.executeUpdate(createDbSQL);
                System.out.println("Database checked/created successfully.");
            }

            // Step 2: Connect to the specific database
            try (Connection conn = DriverManager.getConnection(JDBC_URL + DB_NAME, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {

                // Create table if it does not exist
                String createTableSQL = """
                       CREATE TABLE IF NOT EXISTS logs (
                       id bigserial NOT NULL,
                       log_date_time timestamp NULL,
                       "level" varchar(255) NULL,
                       clazz varchar(255) NULL,
                       log text NULL,
                       "exception" text NULL,
                       job_run_id varchar(255) NULL,
                       created_by varchar(50) NOT NULL DEFAULT 'system'::character varying,
                       created_date timestamp NOT NULL DEFAULT now(),
                       last_modified_by varchar(50) NULL,
                       last_modified_date timestamp NULL,
                       CONSTRAINT log_pkey PRIMARY KEY (id)
                        )
                       """;
                stmt.executeUpdate(createTableSQL);
                System.out.println("Table checked/created successfully.");
            }

        } catch (SQLException e) {
            logger.error("Database error: {}", e.getMessage());
        }
    }
}

