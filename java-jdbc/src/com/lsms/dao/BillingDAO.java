package com.lsms.dao;

import com.lsms.config.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * BillingDAO — Handles invoice queries, line item breakdown, and bill creation.
 */
public class BillingDAO {

    public List<Map<String, Object>> getAllBilling() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT b.BillID, b.OrderID, b.BillDate, b.DueDate, b.TotalAmount, b.BillStatus, " +
            "       o.OrderDate, o.Status AS OrderStatus, " +
            "       CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, c.Phone, " +
            "       COALESCE(SUM(p.AmountPaid), 0) AS TotalPaid " +
            "FROM BILLING b " +
            "JOIN ORDERS o   ON o.OrderID    = b.OrderID " +
            "JOIN CUSTOMER c ON c.CustomerID = o.CustomerID " +
            "LEFT JOIN PAYMENT p ON p.BillID = b.BillID " +
            "GROUP BY b.BillID, b.OrderID, b.BillDate, b.DueDate, b.TotalAmount, b.BillStatus, " +
            "         o.OrderDate, o.Status, CustomerName, c.Phone " +
            "ORDER BY b.BillDate DESC";

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
                map.put("OrderDate", rs.getDate("OrderDate") != null ? rs.getDate("OrderDate").toString() : null);
                map.put("OrderStatus", rs.getString("OrderStatus"));
                map.put("CustomerName", rs.getString("CustomerName"));
                map.put("Phone", rs.getString("Phone"));
                map.put("TotalPaid", rs.getBigDecimal("TotalPaid"));
                list.add(map);
            }
        }
        return list;
    }

    public Map<String, Object> getBillingByOrderId(int orderId) throws SQLException {
        String billSql =
            "SELECT b.*, o.CustomerID, o.OrderDate, o.Status AS OrderStatus, o.TotalCost, " +
            "       CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, c.Phone, c.Address, u.Email " +
            "FROM BILLING b " +
            "JOIN ORDERS o   ON o.OrderID    = b.OrderID " +
            "JOIN CUSTOMER c ON c.CustomerID = o.CustomerID " +
            "JOIN USERS u    ON u.UserID     = c.UserID " +
            "WHERE b.OrderID = ?";

        String paymentsSql = "SELECT * FROM PAYMENT WHERE BillID = ? ORDER BY PaymentDate";
        String detailsSql =
            "SELECT od.*, st.ServiceName " +
            "FROM ORDER_DETAILS od " +
            "JOIN SERVICE_TYPE st ON st.ServiceID = od.ServiceID " +
            "WHERE od.OrderID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmtBill = conn.prepareStatement(billSql);
             PreparedStatement pstmtPayments = conn.prepareStatement(paymentsSql);
             PreparedStatement pstmtDetails = conn.prepareStatement(detailsSql)) {

            pstmtBill.setInt(1, orderId);
            Map<String, Object> billMap = null;

            try (ResultSet rs = pstmtBill.executeQuery()) {
                if (rs.next()) {
                    billMap = new HashMap<>();
                    billMap.put("BillID", rs.getInt("BillID"));
                    billMap.put("OrderID", rs.getInt("OrderID"));
                    billMap.put("BillDate", rs.getDate("BillDate") != null ? rs.getDate("BillDate").toString() : null);
                    billMap.put("DueDate", rs.getDate("DueDate") != null ? rs.getDate("DueDate").toString() : null);
                    billMap.put("TotalAmount", rs.getBigDecimal("TotalAmount"));
                    billMap.put("BillStatus", rs.getString("BillStatus"));
                    billMap.put("CustomerID", rs.getInt("CustomerID"));
                    billMap.put("OrderDate", rs.getDate("OrderDate") != null ? rs.getDate("OrderDate").toString() : null);
                    billMap.put("OrderStatus", rs.getString("OrderStatus"));
                    billMap.put("TotalCost", rs.getBigDecimal("TotalCost"));
                    billMap.put("CustomerName", rs.getString("CustomerName"));
                    billMap.put("Phone", rs.getString("Phone"));
                    billMap.put("Address", rs.getString("Address"));
                    billMap.put("Email", rs.getString("Email"));
                }
            }

            if (billMap == null) return null;

            // Fetch payments
            pstmtPayments.setInt(1, (Integer) billMap.get("BillID"));
            List<Map<String, Object>> paymentsList = new ArrayList<>();
            try (ResultSet rs = pstmtPayments.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("PaymentID", rs.getInt("PaymentID"));
                    p.put("BillID", rs.getInt("BillID"));
                    p.put("PaymentDate", rs.getDate("PaymentDate") != null ? rs.getDate("PaymentDate").toString() : null);
                    p.put("AmountPaid", rs.getBigDecimal("AmountPaid"));
                    p.put("PaymentMethod", rs.getString("PaymentMethod"));
                    p.put("TransactionID", rs.getString("TransactionID"));
                    paymentsList.add(p);
                }
            }
            billMap.put("payments", paymentsList);

            // Fetch order details
            pstmtDetails.setInt(1, orderId);
            List<Map<String, Object>> detailsList = new ArrayList<>();
            try (ResultSet rs = pstmtDetails.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> d = new HashMap<>();
                    d.put("DetailID", rs.getInt("DetailID"));
                    d.put("OrderID", rs.getInt("OrderID"));
                    d.put("ServiceID", rs.getInt("ServiceID"));
                    d.put("GarmentType", rs.getString("GarmentType"));
                    d.put("Quantity", rs.getInt("Quantity"));
                    d.put("WeightKg", rs.getBigDecimal("WeightKg"));
                    d.put("ServiceCost", rs.getBigDecimal("ServiceCost"));
                    d.put("ServiceName", rs.getString("ServiceName"));
                    detailsList.add(d);
                }
            }
            billMap.put("orderDetails", detailsList);

            return billMap;
        }
    }

    public int createBilling(int orderId, String dueDate) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Check not already billed
            String existsSql = "SELECT BillID FROM BILLING WHERE OrderID = ?";
            try (PreparedStatement eStmt = conn.prepareStatement(existsSql)) {
                eStmt.setInt(1, orderId);
                try (ResultSet rs = eStmt.executeQuery()) {
                    if (rs.next()) return -2; // duplicate
                }
            }

            // Get total cost
            String orderSql = "SELECT TotalCost FROM ORDERS WHERE OrderID = ?";
            BigDecimal totalCost;
            try (PreparedStatement oStmt = conn.prepareStatement(orderSql)) {
                oStmt.setInt(1, orderId);
                try (ResultSet rs = oStmt.executeQuery()) {
                    if (!rs.next()) return -1; // order not found
                    totalCost = rs.getBigDecimal("TotalCost");
                }
            }

            String due = (dueDate != null && !dueDate.isBlank()) ? dueDate : LocalDate.now().plusDays(7).toString();

            String insertSql = "INSERT INTO BILLING (OrderID, BillDate, DueDate, TotalAmount, BillStatus) VALUES (?, CURDATE(), ?, ?, 'Unpaid')";
            try (PreparedStatement iStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                iStmt.setInt(1, orderId);
                iStmt.setDate(2, java.sql.Date.valueOf(due));
                iStmt.setBigDecimal(3, totalCost);
                iStmt.executeUpdate();

                try (ResultSet rs = iStmt.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return -1;
    }
}
