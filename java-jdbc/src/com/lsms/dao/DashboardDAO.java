package com.lsms.dao;

import com.lsms.config.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * DashboardDAO — Aggregates metrics and statistics across the entire database for the admin/staff dashboard.
 */
public class DashboardDAO {

    public Map<String, Object> getDashboardStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Total Customers
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM CUSTOMER")) {
                stats.put("totalCustomers", rs.next() ? rs.getInt("total") : 0);
            }

            // 2. Total Orders
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM ORDERS")) {
                stats.put("totalOrders", rs.next() ? rs.getInt("total") : 0);
            }

            // 3. Pending Orders
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM ORDERS WHERE Status = 'Pending'")) {
                stats.put("pendingOrders", rs.next() ? rs.getInt("total") : 0);
            }

            // 4. Processing Orders
            try (ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) AS total FROM ORDERS WHERE Status IN ('Processing','Washing','Drying','Ironing','Picked Up','Received')")) {
                stats.put("processingOrders", rs.next() ? rs.getInt("total") : 0);
            }

            // 5. Completed Orders
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM ORDERS WHERE Status = 'Delivered'")) {
                stats.put("completedOrders", rs.next() ? rs.getInt("total") : 0);
            }

            // 6. Pending Deliveries
            try (ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) AS total FROM DELIVERY WHERE DeliveryStatus NOT IN ('Delivered','Failed')")) {
                stats.put("pendingDeliveries", rs.next() ? rs.getInt("total") : 0);
            }

            // 7. Total Revenue
            try (ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(AmountPaid), 0) AS total FROM PAYMENT")) {
                stats.put("totalRevenue", rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO);
            }

            // 8. Outstanding Payments
            String outstandingSql =
                "SELECT COALESCE(SUM(b.TotalAmount - COALESCE(p.paid,0)), 0) AS total " +
                "FROM BILLING b " +
                "LEFT JOIN (SELECT BillID, SUM(AmountPaid) AS paid FROM PAYMENT GROUP BY BillID) p ON p.BillID = b.BillID " +
                "WHERE b.BillStatus != 'Paid'";
            try (ResultSet rs = stmt.executeQuery(outstandingSql)) {
                stats.put("outstandingPayments", rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO);
            }

            // 9. Low Stock Items
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM INVENTORY WHERE QuantityAvailable <= ReorderLevel")) {
                stats.put("lowStockItems", rs.next() ? rs.getInt("total") : 0);
            }

            // 10. Most Popular Service
            String popularSql =
                "SELECT st.ServiceName, COUNT(od.DetailID) AS orderCount " +
                "FROM SERVICE_TYPE st " +
                "LEFT JOIN ORDER_DETAILS od ON od.ServiceID = st.ServiceID " +
                "GROUP BY st.ServiceID, st.ServiceName ORDER BY orderCount DESC LIMIT 1";
            try (ResultSet rs = stmt.executeQuery(popularSql)) {
                stats.put("mostPopularService", rs.next() ? rs.getString("ServiceName") : "N/A");
            }

            // 11. Average Rating
            try (ResultSet rs = stmt.executeQuery("SELECT ROUND(AVG(Rating),1) AS avg FROM FEEDBACK")) {
                stats.put("averageRating", rs.next() && rs.getObject("avg") != null ? rs.getDouble("avg") : 0.0);
            }
        }

        return stats;
    }
}
