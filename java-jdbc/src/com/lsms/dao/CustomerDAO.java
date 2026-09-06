package com.lsms.dao;

import com.lsms.config.DBConnection;
import com.lsms.models.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomerDAO — Demonstrates JDBC CRUD operations, PreparedStatement parameterized queries,
 * and relational JOINs on CUSTOMER and USERS tables.
 */
public class CustomerDAO {

    public List<Map<String, Object>> getAllCustomers(String search) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT c.CustomerID, c.FirstName, c.LastName, c.Phone, c.Address, " +
            "       u.Email, u.IsActive, u.CreatedAt " +
            "FROM CUSTOMER c JOIN USERS u ON u.UserID = c.UserID "
        );

        List<String> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append("WHERE c.FirstName LIKE ? OR c.LastName LIKE ? OR c.Phone LIKE ? OR u.Email LIKE ? ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }
        sql.append("ORDER BY c.CustomerID DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("CustomerID", rs.getInt("CustomerID"));
                    map.put("FirstName", rs.getString("FirstName"));
                    map.put("LastName", rs.getString("LastName"));
                    map.put("Phone", rs.getString("Phone"));
                    map.put("Address", rs.getString("Address"));
                    map.put("Email", rs.getString("Email"));
                    map.put("IsActive", rs.getInt("IsActive"));
                    map.put("CreatedAt", rs.getTimestamp("CreatedAt"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT CustomerID, UserID, FirstName, LastName, Phone, Address FROM CUSTOMER ORDER BY CustomerID ASC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Customer c = new Customer(
                    rs.getInt("CustomerID"),
                    rs.getInt("UserID"),
                    rs.getString("FirstName"),
                    rs.getString("LastName"),
                    rs.getString("Phone"),
                    rs.getString("Address")
                );
                list.add(c);
            }
        } catch (SQLException e) {
            System.err.println("JDBC Error in getAllCustomers: " + e.getMessage());
        }
        return list;
    }

    public Map<String, Object> getCustomerById(int customerId) throws SQLException {
        String sql = "SELECT c.CustomerID, c.FirstName, c.LastName, c.Phone, c.Address, " +
                     "       u.Email, u.Username, u.IsActive, u.CreatedAt " +
                     "FROM CUSTOMER c JOIN USERS u ON u.UserID = c.UserID " +
                     "WHERE c.CustomerID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("CustomerID", rs.getInt("CustomerID"));
                    map.put("FirstName", rs.getString("FirstName"));
                    map.put("LastName", rs.getString("LastName"));
                    map.put("Phone", rs.getString("Phone"));
                    map.put("Address", rs.getString("Address"));
                    map.put("Email", rs.getString("Email"));
                    map.put("Username", rs.getString("Username"));
                    map.put("IsActive", rs.getInt("IsActive"));
                    map.put("CreatedAt", rs.getTimestamp("CreatedAt"));
                    return map;
                }
            }
        }
        return null;
    }

    public boolean updateCustomer(int customerId, String firstName, String lastName, String phone, String address) throws SQLException {
        String sql = "UPDATE CUSTOMER SET FirstName = ?, LastName = ?, Phone = ?, Address = ? WHERE CustomerID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, phone);
            pstmt.setString(4, address);
            pstmt.setInt(5, customerId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deactivateCustomer(int customerId) throws SQLException {
        String sql = "UPDATE USERS u JOIN CUSTOMER c ON c.UserID = u.UserID SET u.IsActive = 0 WHERE c.CustomerID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Map<String, Object>> getCustomerOrders(int customerId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT o.*, b.BillID, b.BillStatus, b.TotalAmount AS BilledAmount " +
                     "FROM ORDERS o " +
                     "LEFT JOIN BILLING b ON b.OrderID = o.OrderID " +
                     "WHERE o.CustomerID = ? " +
                     "ORDER BY o.OrderDate DESC, o.OrderID DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("OrderID", rs.getInt("OrderID"));
                    map.put("CustomerID", rs.getInt("CustomerID"));
                    map.put("OrderDate", rs.getDate("OrderDate") != null ? rs.getDate("OrderDate").toString() : null);
                    map.put("PickupDate", rs.getDate("PickupDate") != null ? rs.getDate("PickupDate").toString() : null);
                    map.put("DeliveryDate", rs.getDate("DeliveryDate") != null ? rs.getDate("DeliveryDate").toString() : null);
                    map.put("Status", rs.getString("Status"));
                    map.put("TotalWeight", rs.getBigDecimal("TotalWeight"));
                    map.put("TotalCost", rs.getBigDecimal("TotalCost"));
                    map.put("Notes", rs.getString("Notes"));
                    int billId = rs.getInt("BillID");
                    map.put("BillID", rs.wasNull() ? null : billId);
                    map.put("BillStatus", rs.getString("BillStatus"));
                    map.put("BilledAmount", rs.getBigDecimal("BilledAmount"));
                    list.add(map);
                }
            }
        }
        return list;
    }
}
