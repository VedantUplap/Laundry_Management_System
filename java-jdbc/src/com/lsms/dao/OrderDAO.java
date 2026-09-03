package com.lsms.dao;

import com.lsms.config.DBConnection;
import com.lsms.models.Order;
import com.lsms.models.ServiceType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderDAO — Demonstrates JDBC PreparedStatement queries, JOINs, and Transaction Management (commit/rollback).
 */
public class OrderDAO {

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.OrderID, o.CustomerID, CONCAT(c.FirstName, ' ', c.LastName) AS CustomerName, " +
                     "o.OrderDate, o.PickupDate, o.DeliveryDate, o.Status, o.TotalWeight, o.TotalCost, o.Notes " +
                     "FROM ORDERS o " +
                     "JOIN CUSTOMER c ON o.CustomerID = c.CustomerID " +
                     "ORDER BY o.OrderID DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Order order = new Order();
                order.setOrderId(rs.getInt("OrderID"));
                order.setCustomerId(rs.getInt("CustomerID"));
                order.setCustomerName(rs.getString("CustomerName"));
                order.setOrderDate(rs.getDate("OrderDate"));
                order.setPickupDate(rs.getDate("PickupDate"));
                order.setDeliveryDate(rs.getDate("DeliveryDate"));
                order.setStatus(rs.getString("Status"));
                order.setTotalWeight(rs.getBigDecimal("TotalWeight"));
                order.setTotalCost(rs.getBigDecimal("TotalCost"));
                order.setNotes(rs.getString("Notes"));
                list.add(order);
            }
        } catch (SQLException e) {
            System.err.println("JDBC Error in getAllOrders: " + e.getMessage());
        }
        return list;
    }

    public List<ServiceType> getAllServices() {
        List<ServiceType> list = new ArrayList<>();
        String sql = "SELECT ServiceID, ServiceName, Description, PricePerKg, IsActive FROM SERVICE_TYPE WHERE IsActive = 1";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ServiceType s = new ServiceType(
                    rs.getInt("ServiceID"),
                    rs.getString("ServiceName"),
                    rs.getString("Description"),
                    rs.getBigDecimal("PricePerKg"),
                    rs.getBoolean("IsActive")
                );
                list.add(s);
            }
        } catch (SQLException e) {
            System.err.println("JDBC Error in getAllServices: " + e.getMessage());
        }
        return list;
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE ORDERS SET Status = ? WHERE OrderID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("JDBC Error in updateOrderStatus: " + e.getMessage());
            return false;
        }
    }

    /**
     * Demonstrates JDBC Transaction Management:
     * Disables auto-commit, inserts Order + Order Detail, and commits atomically.
     */
    public int createOrderWithItem(int customerId, int serviceId, String garmentType, int quantity, BigDecimal weightKg, BigDecimal serviceCost, String notes) {
        String insertOrderSql = "INSERT INTO ORDERS (CustomerID, OrderDate, Status, TotalWeight, TotalCost, Notes) VALUES (?, CURRENT_DATE, 'Pending', ?, ?, ?)";
        String insertDetailSql = "INSERT INTO ORDER_DETAILS (OrderID, ServiceID, GarmentType, Quantity, WeightKg, ServiceCost) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin JDBC Transaction

            // 1. Insert Order
            int generatedOrderId = -1;
            try (PreparedStatement pstmtOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtOrder.setInt(1, customerId);
                pstmtOrder.setBigDecimal(2, weightKg);
                pstmtOrder.setBigDecimal(3, serviceCost);
                pstmtOrder.setString(4, notes);
                pstmtOrder.executeUpdate();

                try (ResultSet rs = pstmtOrder.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedOrderId = rs.getInt(1);
                    }
                }
            }

            if (generatedOrderId == -1) {
                conn.rollback();
                return -1;
            }

            // 2. Insert Order Details
            try (PreparedStatement pstmtDetail = conn.prepareStatement(insertDetailSql)) {
                pstmtDetail.setInt(1, generatedOrderId);
                pstmtDetail.setInt(2, serviceId);
                pstmtDetail.setString(3, garmentType);
                pstmtDetail.setInt(4, quantity);
                pstmtDetail.setBigDecimal(5, weightKg);
                pstmtDetail.setBigDecimal(6, serviceCost);
                pstmtDetail.executeUpdate();
            }

            conn.commit(); // Commit Transaction
            return generatedOrderId;

        } catch (SQLException e) {
            System.err.println("Transaction rolled back due to error: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return -1;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }
}
