package com.lsms.dao;

import com.lsms.config.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportDAO — Complex JDBC aggregation, GROUP BY, date formatting, and analytics.
 * Supplies structured data for all LSMS reports.
 */
public class ReportDAO {

    public List<Map<String, Object>> getDailyRevenue(String from, String to) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT p.PaymentDate AS date, " +
            "       COUNT(p.PaymentID)  AS transactions, " +
            "       SUM(p.AmountPaid)   AS revenue " +
            "FROM PAYMENT p " +
            "WHERE 1=1 "
        );

        List<String> params = new ArrayList<>();
        if (from != null && !from.isBlank()) {
            sql.append("AND p.PaymentDate >= ? ");
            params.add(from.trim());
        }
        if (to != null && !to.isBlank()) {
            sql.append("AND p.PaymentDate <= ? ");
            params.add(to.trim());
        }
        sql.append("GROUP BY p.PaymentDate ORDER BY p.PaymentDate DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", rs.getDate("date") != null ? rs.getDate("date").toString() : null);
                    map.put("transactions", rs.getInt("transactions"));
                    map.put("revenue", rs.getBigDecimal("revenue"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    public List<Map<String, Object>> getMonthlyRevenue() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT DATE_FORMAT(p.PaymentDate, '%Y-%m') AS month, " +
            "       COUNT(p.PaymentID)                   AS transactions, " +
            "       SUM(p.AmountPaid)                    AS revenue " +
            "FROM PAYMENT p " +
            "GROUP BY DATE_FORMAT(p.PaymentDate, '%Y-%m') " +
            "ORDER BY month DESC " +
            "LIMIT 12";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("month", rs.getString("month"));
                map.put("transactions", rs.getInt("transactions"));
                map.put("revenue", rs.getBigDecimal("revenue"));
                list.add(map);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getPendingDeliveries() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT d.DeliveryID, d.OrderID, d.DeliveryStatus, d.PickupTime, " +
            "       CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, " +
            "       c.Phone, c.Address, " +
            "       da.AgentName, da.Phone AS AgentPhone, " +
            "       o.OrderDate, o.TotalCost " +
            "FROM DELIVERY d " +
            "JOIN ORDERS o               ON o.OrderID     = d.OrderID " +
            "JOIN CUSTOMER c             ON c.CustomerID  = o.CustomerID " +
            "LEFT JOIN DELIVERY_AGENT da ON da.AgentID    = d.AgentID " +
            "WHERE d.DeliveryStatus NOT IN ('Delivered','Failed') " +
            "ORDER BY o.OrderDate";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("DeliveryID", rs.getInt("DeliveryID"));
                map.put("OrderID", rs.getInt("OrderID"));
                map.put("DeliveryStatus", rs.getString("DeliveryStatus"));
                map.put("PickupTime", rs.getTimestamp("PickupTime") != null ? rs.getTimestamp("PickupTime").toString() : null);
                map.put("CustomerName", rs.getString("CustomerName"));
                map.put("Phone", rs.getString("Phone"));
                map.put("Address", rs.getString("Address"));
                map.put("AgentName", rs.getString("AgentName"));
                map.put("AgentPhone", rs.getString("AgentPhone"));
                map.put("OrderDate", rs.getDate("OrderDate") != null ? rs.getDate("OrderDate").toString() : null);
                map.put("TotalCost", rs.getBigDecimal("TotalCost"));
                list.add(map);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getCustomerHistory(int customerId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT o.OrderID, o.OrderDate, o.Status, o.TotalWeight, o.TotalCost, " +
            "       b.BillStatus, b.TotalAmount, " +
            "       COALESCE(SUM(p.AmountPaid), 0) AS TotalPaid " +
            "FROM ORDERS o " +
            "LEFT JOIN BILLING b  ON b.OrderID = o.OrderID " +
            "LEFT JOIN PAYMENT p  ON p.BillID  = b.BillID " +
            "WHERE o.CustomerID = ? " +
            "GROUP BY o.OrderID, o.OrderDate, o.Status, o.TotalWeight, o.TotalCost, " +
            "         b.BillStatus, b.TotalAmount " +
            "ORDER BY o.OrderDate DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("OrderID", rs.getInt("OrderID"));
                    map.put("OrderDate", rs.getDate("OrderDate") != null ? rs.getDate("OrderDate").toString() : null);
                    map.put("Status", rs.getString("Status"));
                    map.put("TotalWeight", rs.getBigDecimal("TotalWeight"));
                    map.put("TotalCost", rs.getBigDecimal("TotalCost"));
                    map.put("BillStatus", rs.getString("BillStatus"));
                    map.put("TotalAmount", rs.getBigDecimal("TotalAmount"));
                    map.put("TotalPaid", rs.getBigDecimal("TotalPaid"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    public List<Map<String, Object>> getInventoryUsage() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT i.ItemID, i.ItemName, i.Unit, i.QuantityAvailable, i.ReorderLevel, " +
            "       COUNT(ul.UsageID)           AS UsageCount, " +
            "       COALESCE(SUM(ul.QuantityUsed), 0) AS TotalUsed " +
            "FROM INVENTORY i " +
            "LEFT JOIN USAGE_LOG ul ON ul.ItemID = i.ItemID " +
            "GROUP BY i.ItemID, i.ItemName, i.Unit, i.QuantityAvailable, i.ReorderLevel " +
            "ORDER BY TotalUsed DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("ItemID", rs.getInt("ItemID"));
                map.put("ItemName", rs.getString("ItemName"));
                map.put("Unit", rs.getString("Unit"));
                map.put("QuantityAvailable", rs.getBigDecimal("QuantityAvailable"));
                map.put("ReorderLevel", rs.getBigDecimal("ReorderLevel"));
                map.put("UsageCount", rs.getInt("UsageCount"));
                map.put("TotalUsed", rs.getBigDecimal("TotalUsed"));
                list.add(map);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getPopularServices() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT st.ServiceID, st.ServiceName, st.PricePerKg, " +
            "       COUNT(od.DetailID)          AS OrderCount, " +
            "       COALESCE(SUM(od.WeightKg),0)   AS TotalKg, " +
            "       COALESCE(SUM(od.ServiceCost),0) AS TotalRevenue " +
            "FROM SERVICE_TYPE st " +
            "LEFT JOIN ORDER_DETAILS od ON od.ServiceID = st.ServiceID " +
            "GROUP BY st.ServiceID, st.ServiceName, st.PricePerKg " +
            "ORDER BY OrderCount DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("ServiceID", rs.getInt("ServiceID"));
                map.put("ServiceName", rs.getString("ServiceName"));
                map.put("PricePerKg", rs.getBigDecimal("PricePerKg"));
                map.put("OrderCount", rs.getInt("OrderCount"));
                map.put("TotalKg", rs.getBigDecimal("TotalKg"));
                map.put("TotalRevenue", rs.getBigDecimal("TotalRevenue"));
                list.add(map);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getOutstandingPayments() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT b.BillID, b.OrderID, b.BillDate, b.DueDate, b.TotalAmount, b.BillStatus, " +
            "       COALESCE(SUM(p.AmountPaid), 0)                             AS TotalPaid, " +
            "       b.TotalAmount - COALESCE(SUM(p.AmountPaid), 0)            AS BalanceDue, " +
            "       CONCAT(c.FirstName,' ',c.LastName)                         AS CustomerName, " +
            "       c.Phone, " +
            "       CASE WHEN b.DueDate < CURDATE() THEN 'Overdue' ELSE 'Current' END AS AgeStatus " +
            "FROM BILLING b " +
            "JOIN ORDERS o   ON o.OrderID    = b.OrderID " +
            "JOIN CUSTOMER c ON c.CustomerID = o.CustomerID " +
            "LEFT JOIN PAYMENT p ON p.BillID = b.BillID " +
            "WHERE b.BillStatus != 'Paid' " +
            "GROUP BY b.BillID, b.OrderID, b.BillDate, b.DueDate, " +
            "         b.TotalAmount, b.BillStatus, CustomerName, c.Phone " +
            "ORDER BY b.DueDate";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("BillID", rs.getInt("BillID"));
                map.put("OrderID", rs.getInt("OrderID"));
                map.put("BillDate", rs.getDate("BillDate") != null ? rs.getDate("BillDate").toString() : null);
                map.put("DueDate", rs.getDate("DueDate") != null ? rs.getDate("DueDate").toString() : null);
                map.put("TotalAmount", rs.getBigDecimal("TotalAmount"));
                map.put("BillStatus", rs.getString("BillStatus"));
                map.put("TotalPaid", rs.getBigDecimal("TotalPaid"));
                map.put("BalanceDue", rs.getBigDecimal("BalanceDue"));
                map.put("CustomerName", rs.getString("CustomerName"));
                map.put("Phone", rs.getString("Phone"));
                map.put("AgeStatus", rs.getString("AgeStatus"));
                list.add(map);
            }
        }
        return list;
    }

    public void printDashboardSummary() {
        new DashboardDAO();
        try {
            Map<String, Object> stats = new DashboardDAO().getDashboardStats();
            System.out.println("\n=======================================================");
            System.out.println("          LSMS — JDBC DASHBOARD SUMMARY                ");
            System.out.println("=======================================================");
            System.out.printf("  Total Customers      : %s\n", stats.get("totalCustomers"));
            System.out.printf("  Total Orders         : %s\n", stats.get("totalOrders"));
            System.out.printf("  Pending Orders       : %s\n", stats.get("pendingOrders"));
            System.out.printf("  Delivered Orders     : %s\n", stats.get("completedOrders"));
            System.out.printf("  Total Revenue        : ₹%s\n", stats.get("totalRevenue"));
            System.out.printf("  Low Stock Items      : %s\n", stats.get("lowStockItems"));
            System.out.println("=======================================================\n");
        } catch (SQLException e) {
            System.err.println("JDBC Error in printDashboardSummary: " + e.getMessage());
        }
    }

    public void printMonthlyRevenueReport() {
        try {
            List<Map<String, Object>> rows = getMonthlyRevenue();
            System.out.println("\n--- Monthly Revenue Report (via JDBC) ---");
            System.out.printf("%-10s | %-18s | %-12s\n", "Month", "Transactions", "Total Revenue");
            System.out.println("---------------------------------------------");
            for (Map<String, Object> r : rows) {
                System.out.printf("%-10s | %-18s | ₹%-12s\n",
                    r.get("month"),
                    r.get("transactions"),
                    r.get("revenue"));
            }
            System.out.println("---------------------------------------------\n");
        } catch (SQLException e) {
            System.err.println("JDBC Error in printMonthlyRevenueReport: " + e.getMessage());
        }
    }
}
