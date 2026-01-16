package org.example.logging;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LoggingConnection {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    private static volatile boolean started = false;
    private static final Object lock = new Object();

    static {
        config.setJdbcUrl( "jdbc:mysql://localhost:3306/myPodDB" );
        config.setUsername( "user" );
        config.setPassword( "pass" );
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        ds = new HikariDataSource( config );
    }

    private LoggingConnection() {}

    private static void setupLoggingTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS app_logs (" +
                                "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT," +
                                "level VARCHAR(50) NOT NULL," +
                                "message TEXT NOT NULL," +
                                "error_details TEXT NULL," +
                                "timestamp DATETIME NOT NULL," +
                                "PRIMARY KEY (id)" +
                                ")";

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()){

            stmt.executeUpdate(createTableSQL);

            started = true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    public static Connection getConnection() throws SQLException {
        if(!started){
            synchronized (lock){
                if(!started){
                    setupLoggingTable();
                }
            }
        }
        return ds.getConnection();
    }
}
