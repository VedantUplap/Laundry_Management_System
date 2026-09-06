package com.lsms.dao;

import com.lsms.config.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FeedbackDAO — Handles customer rating, reviews, and validation against delivered orders.
 */
public class FeedbackDAO {

    public List<Map<String, Object>> getAllFeedback() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT f.FeedbackID, f.CustomerID, f.OrderID, f.Rating, f.Comments, f.FeedbackDate, " +
            "       CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, " +
            "       o.OrderDate, o.TotalCost " +
            "FROM FEEDBACK f " +
            "JOIN CUSTOMER c ON c.CustomerID = f.CustomerID " +
            "JOIN ORDERS o   ON o.OrderID    = f.OrderID " +
            "ORDER BY f.FeedbackDate DESC, f.FeedbackID DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("FeedbackID", rs.getInt("FeedbackID"));
                map.put("CustomerID", rs.getInt("CustomerID"));
                map.put("OrderID", rs.getInt("OrderID"));
                map.put("Rating", rs.getInt("Rating"));
                map.put("Comments", rs.getString("Comments"));
                map.put("FeedbackDate", rs.getDate("FeedbackDate") != null ? rs.getDate("FeedbackDate").toString() : null);
                map.put("CustomerName", rs.getString("CustomerName"));
                map.put("OrderDate", rs.getDate("OrderDate") != null ? rs.getDate("OrderDate").toString() : null);
                map.put("TotalCost", rs.getBigDecimal("TotalCost"));
                list.add(map);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getMyFeedback(int customerId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT f.FeedbackID, f.CustomerID, f.OrderID, f.Rating, f.Comments, f.FeedbackDate, " +
            "       o.OrderDate, o.TotalCost, o.Status AS OrderStatus " +
            "FROM FEEDBACK f " +
            "JOIN ORDERS o ON o.OrderID = f.OrderID " +
            "WHERE f.CustomerID = ? " +
            "ORDER BY f.FeedbackDate DESC, f.FeedbackID DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("FeedbackID", rs.getInt("FeedbackID"));
                    map.put("CustomerID", rs.getInt("CustomerID"));
                    map.put("OrderID", rs.getInt("OrderID"));
                    map.put("Rating", rs.getInt("Rating"));
                    map.put("Comments", rs.getString("Comments"));
                    map.put("FeedbackDate", rs.getDate("FeedbackDate") != null ? rs.getDate("FeedbackDate").toString() : null);
                    map.put("OrderDate", rs.getDate("OrderDate") != null ? rs.getDate("OrderDate").toString() : null);
                    map.put("TotalCost", rs.getBigDecimal("TotalCost"));
                    map.put("OrderStatus", rs.getString("OrderStatus"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    public int submitFeedback(int customerId, int orderId, int rating, String comments) throws Exception {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Verify order belongs to customer and is Delivered
            String verifySql = "SELECT OrderID, Status FROM ORDERS WHERE OrderID = ? AND CustomerID = ?";
            try (PreparedStatement vStmt = conn.prepareStatement(verifySql)) {
                vStmt.setInt(1, orderId);
                vStmt.setInt(2, customerId);
                try (ResultSet rs = vStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Order not found or does not belong to you.");
                    }
                    String status = rs.getString("Status");
                    if (!"Delivered".equalsIgnoreCase(status)) {
                        throw new IllegalStateException("Feedback can only be submitted for delivered orders.");
                    }
                }
            }

            // 2. Check for duplicate feedback
            String dupSql = "SELECT FeedbackID FROM FEEDBACK WHERE OrderID = ?";
            try (PreparedStatement dStmt = conn.prepareStatement(dupSql)) {
                dStmt.setInt(1, orderId);
                try (ResultSet rs = dStmt.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalStateException("Feedback already submitted for this order.");
                    }
                }
            }

            // 3. Insert feedback
            String insertSql = "INSERT INTO FEEDBACK (CustomerID, OrderID, Rating, Comments, FeedbackDate) VALUES (?, ?, ?, ?, CURDATE())";
            try (PreparedStatement iStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                iStmt.setInt(1, customerId);
                iStmt.setInt(2, orderId);
                iStmt.setInt(3, rating);
                iStmt.setString(4, comments);
                iStmt.executeUpdate();

                try (ResultSet rs = iStmt.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return -1;
    }
}
