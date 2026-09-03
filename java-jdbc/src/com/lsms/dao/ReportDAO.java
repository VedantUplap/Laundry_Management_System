package com.lsms.dao;

import com.lsms.config.DBConnection;

import java.sql.*;

/**
 * ReportDAO — Demonstrates complex JDBC aggregate queries, GROUP BY, and metadata inspection.
 */
public class ReportDAO {

    public void printDashboardSummary() {
        String statsSql =
            "SELECT " +
            "  (SELECT COUNT(*) FROM CUSTOMER) AS totalCustomers, " +
            "  (SELECT COUNT(*) FROM ORDERS) AS totalOrders, " +
            "  (SELECT COUNT(*) FROM ORDERS WHERE Status = 'Pending') AS pendingOrders, " +
            "  (SELECT COUNT(*) FROM ORDERS WHERE Status = 'Delivered') AS completedOrders, " +
            "  (SELECT COALESCE(SUM(AmountPaid), 0) FROM PAYMENT) AS totalRevenue, " +
            "  (SELECT COUNT(*) FROM INVENTORY WHERE QuantityAvailable <= ReorderLevel) AS lowStockItems";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(statsSql)) {

            if (rs.next()) {
                System.out.println("\n=======================================================");
                System.out.println("          LSMS — JDBC DASHBOARD SUMMARY                ");
                System.out.println("=======================================================");
                System.out.printf("  Total Customers      : %d\n", rs.getInt("totalCustomers"));
                System.out.printf("  Total Orders         : %d\n", rs.getInt("totalOrders"));
                System.out.printf("  Pending Orders       : %d\n", rs.getInt("pendingOrders"));
                System.out.printf("  Delivered Orders     : %d\n", rs.getInt("completedOrders"));
                System.out.printf("  Total Revenue        : ₹%.2f\n", rs.getDouble("totalRevenue"));
                System.out.printf("  Low Stock Items      : %d\n", rs.getInt("lowStockItems"));
                System.out.println("=======================================================\n");
            }
        } catch (SQLException e) {
            System.err.println("JDBC Error in printDashboardSummary: " + e.getMessage());
        }
    }

    public void printMonthlyRevenueReport() {
        String sql =
            "SELECT DATE_FORMAT(PaymentDate, '%Y-%m') AS Month, " +
            "       COUNT(PaymentID) AS TotalTransactions, " +
            "       SUM(AmountPaid) AS TotalAmount " +
            "FROM PAYMENT " +
            "GROUP BY Month " +
            "ORDER BY Month DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Monthly Revenue Report (via JDBC) ---");
            System.out.printf("%-10s | %-18s | %-12s\n", "Month", "Transactions", "Total Revenue");
            System.out.println("---------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-10s | %-18d | ₹%-12.2f\n",
                    rs.getString("Month"),
                    rs.getInt("TotalTransactions"),
                    rs.getDouble("TotalAmount"));
            }
            System.out.println("---------------------------------------------\n");
        } catch (SQLException e) {
            System.err.println("JDBC Error in printMonthlyRevenueReport: " + e.getMessage());
        }
    }
}
