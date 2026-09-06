package com.lsms.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — Centralized JDBC Connection Factory for LSMS.
 * Demonstrates standard JDBC DriverManager connection management.
 */
public class DBConnection {

    // Explicitly load the MySQL JDBC Driver class
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    /**
     * Obtains a new JDBC database Connection using parameters from AppConfig.
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            AppConfig.getJdbcUrl(),
            AppConfig.getDbUser(),
            AppConfig.getDbPassword()
        );
    }

    /**
     * Utility method to test database connectivity.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
