package com.lsms.dao;

import com.lsms.config.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PaymentDAO — Demonstrates transactional payment recording and automatic status updates on BILLING.
 */
public class PaymentDAO {

    public List<Map<String, Object>> getAllPayments() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT p.PaymentID, p.BillID, p.PaymentDate, p.AmountPaid, p.PaymentMethod, p.TransactionID, " +
            "       b.OrderID, b.TotalAmount, b.BillStatus, " +
            "       CONCAT(c.FirstName,' ',c.LastName) AS CustomerName " +
            "FROM PAYMENT p " +
            "JOIN BILLING b  ON b.BillID     = p.BillID " +
            "JOIN ORDERS o   ON o.OrderID    = b.OrderID " +
            "JOIN CUSTOMER c ON c.CustomerID = o.CustomerID " +
            "ORDER BY p.PaymentDate DESC, p.PaymentID DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("PaymentID", rs.getInt("PaymentID"));
                map.put("BillID", rs.getInt("BillID"));
                map.put("PaymentDate", rs.getDate("PaymentDate") != null ? rs.getDate("PaymentDate").toString() : null);
                map.put("AmountPaid", rs.getBigDecimal("AmountPaid"));
                map.put("PaymentMethod", rs.getString("PaymentMethod"));
                map.put("TransactionID", rs.getString("TransactionID"));
                map.put("OrderID", rs.getInt("OrderID"));
                map.put("TotalAmount", rs.getBigDecimal("TotalAmount"));
                map.put("BillStatus", rs.getString("BillStatus"));
                map.put("CustomerName", rs.getString("CustomerName"));
                list.add(map);
            }
        }
        return list;
    }

    public Map<String, Object> createPayment(int billId, BigDecimal amountPaid, String paymentMethod, String transactionId) throws Exception {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin JDBC Transaction

            // 1. Fetch bill details
            String billSql = "SELECT TotalAmount, BillStatus FROM BILLING WHERE BillID = ?";
            BigDecimal totalAmount;
            String currentStatus;
            try (PreparedStatement bStmt = conn.prepareStatement(billSql)) {
                bStmt.setInt(1, billId);
                try (ResultSet rs = bStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new IllegalArgumentException("Bill not found.");
                    }
                    totalAmount = rs.getBigDecimal("TotalAmount");
                    currentStatus = rs.getString("BillStatus");
                }
            }

            if ("Paid".equalsIgnoreCase(currentStatus)) {
                conn.rollback();
                throw new IllegalStateException("Bill is already fully paid.");
            }

            // 2. Insert Payment
            String insertSql = "INSERT INTO PAYMENT (BillID, PaymentDate, AmountPaid, PaymentMethod, TransactionID) VALUES (?, CURDATE(), ?, ?, ?)";
            int paymentId = -1;
            try (PreparedStatement pStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pStmt.setInt(1, billId);
                pStmt.setBigDecimal(2, amountPaid);
                pStmt.setString(3, paymentMethod);
                pStmt.setString(4, transactionId);
                pStmt.executeUpdate();

                try (ResultSet rs = pStmt.getGeneratedKeys()) {
                    if (rs.next()) paymentId = rs.getInt(1);
                }
            }

            // 3. Recalculate total paid
            String sumSql = "SELECT COALESCE(SUM(AmountPaid), 0) AS TotalPaid FROM PAYMENT WHERE BillID = ?";
            BigDecimal totalPaid = BigDecimal.ZERO;
            try (PreparedStatement sStmt = conn.prepareStatement(sumSql)) {
                sStmt.setInt(1, billId);
                try (ResultSet rs = sStmt.executeQuery()) {
                    if (rs.next()) {
                        totalPaid = rs.getBigDecimal("TotalPaid");
                    }
                }
            }

            // 4. Determine new bill status
            String newStatus;
            if (totalPaid.compareTo(totalAmount) >= 0) {
                newStatus = "Paid";
            } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                newStatus = "Partially Paid";
            } else {
                newStatus = "Unpaid";
            }

            String updateBillSql = "UPDATE BILLING SET BillStatus = ? WHERE BillID = ?";
            try (PreparedStatement uStmt = conn.prepareStatement(updateBillSql)) {
                uStmt.setString(1, newStatus);
                uStmt.setInt(2, billId);
                uStmt.executeUpdate();
            }

            conn.commit(); // Commit Transaction

            BigDecimal balance = totalAmount.subtract(totalPaid);
            if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;

            Map<String, Object> result = new HashMap<>();
            result.put("paymentID", paymentId);
            result.put("newStatus", newStatus);
            result.put("totalPaid", totalPaid);
            result.put("balance", balance);
            return result;

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
