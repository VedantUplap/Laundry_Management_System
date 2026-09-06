package com.lsms.dao;

import com.lsms.config.DBConnection;
import com.lsms.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserDAO — Demonstrates JDBC PreparedStatement queries, transactions, and authentication for USERS table.
 */
public class UserDAO {

    public User findByEmail(String email) {
        String sql = "SELECT UserID, Username, Email, Role, IsActive, CreatedAt FROM USERS WHERE Email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("UserID"));
                    user.setUsername(rs.getString("Username"));
                    user.setEmail(rs.getString("Email"));
                    user.setRole(rs.getString("Role"));
                    user.setActive(rs.getBoolean("IsActive"));
                    user.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("JDBC Error in findByEmail: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves user credentials and linked Customer / Delivery Agent profile for authentication.
     */
    public Map<String, Object> getUserForLogin(String email) throws SQLException {
        String sql = "SELECT u.UserID, u.Email, u.Password, u.Role, u.IsActive, " +
                     "       c.CustomerID, c.FirstName, c.LastName, da.AgentID " +
                     "FROM USERS u " +
                     "LEFT JOIN CUSTOMER c ON c.UserID = u.UserID " +
                     "LEFT JOIN DELIVERY_AGENT da ON da.UserID = u.UserID " +
                     "WHERE u.Email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userID", rs.getInt("UserID"));
                    map.put("email", rs.getString("Email"));
                    map.put("password", rs.getString("Password"));
                    map.put("role", rs.getString("Role"));
                    map.put("isActive", rs.getBoolean("IsActive"));

                    int custId = rs.getInt("CustomerID");
                    map.put("customerID", rs.wasNull() ? null : custId);

                    int agentId = rs.getInt("AgentID");
                    map.put("agentID", rs.wasNull() ? null : agentId);

                    map.put("firstName", rs.getString("FirstName"));
                    map.put("lastName", rs.getString("LastName"));
                    return map;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves user profile details (GET /api/auth/me).
     */
    public Map<String, Object> getUserProfile(int userId) throws SQLException {
        String sql = "SELECT u.UserID, u.Username, u.Email, u.Role, " +
                     "       c.CustomerID, c.FirstName, c.LastName, c.Phone, c.Address, " +
                     "       da.AgentID, da.AgentName, da.VehicleNumber " +
                     "FROM USERS u " +
                     "LEFT JOIN CUSTOMER c ON c.UserID = u.UserID " +
                     "LEFT JOIN DELIVERY_AGENT da ON da.UserID = u.UserID " +
                     "WHERE u.UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("UserID", rs.getInt("UserID"));
                    map.put("Username", rs.getString("Username"));
                    map.put("Email", rs.getString("Email"));
                    map.put("Role", rs.getString("Role"));

                    int custId = rs.getInt("CustomerID");
                    map.put("CustomerID", rs.wasNull() ? null : custId);
                    map.put("FirstName", rs.getString("FirstName"));
                    map.put("LastName", rs.getString("LastName"));
                    map.put("Phone", rs.getString("Phone"));
                    map.put("Address", rs.getString("Address"));

                    int agentId = rs.getInt("AgentID");
                    map.put("AgentID", rs.wasNull() ? null : agentId);
                    map.put("AgentName", rs.getString("AgentName"));
                    map.put("VehicleNumber", rs.getString("VehicleNumber"));
                    return map;
                }
            }
        }
        return null;
    }

    /**
     * Registers a new customer using a JDBC Transaction across USERS and CUSTOMER tables.
     */
    public Map<String, Object> registerCustomer(String firstName, String lastName, String phone,
                                                String address, String email, String passwordHash,
                                                String username) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin JDBC Transaction

            // 1. Check if email exists
            String checkEmailSql = "SELECT UserID FROM USERS WHERE Email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkEmailSql)) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return null; // Duplicate email
                    }
                }
            }

            // 2. Insert into USERS
            String uname = (username != null && !username.isBlank()) ? username : email.split("@")[0];
            String insertUserSql = "INSERT INTO USERS (Username, Email, Password, Role) VALUES (?, ?, ?, 'customer')";
            int newUserId = -1;
            try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, uname);
                userStmt.setString(2, email);
                userStmt.setString(3, passwordHash);
                userStmt.executeUpdate();

                try (ResultSet rs = userStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        newUserId = rs.getInt(1);
                    }
                }
            }

            if (newUserId <= 0) {
                conn.rollback();
                throw new SQLException("Failed to retrieve generated UserID.");
            }

            // 3. Insert into CUSTOMER
            String insertCustSql = "INSERT INTO CUSTOMER (UserID, FirstName, LastName, Phone, Address) VALUES (?, ?, ?, ?, ?)";
            int newCustomerId = -1;
            try (PreparedStatement custStmt = conn.prepareStatement(insertCustSql, Statement.RETURN_GENERATED_KEYS)) {
                custStmt.setInt(1, newUserId);
                custStmt.setString(2, firstName);
                custStmt.setString(3, lastName);
                custStmt.setString(4, phone);
                custStmt.setString(5, address);
                custStmt.executeUpdate();

                try (ResultSet rs = custStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        newCustomerId = rs.getInt(1);
                    }
                }
            }

            conn.commit(); // Commit Transaction

            Map<String, Object> result = new HashMap<>();
            result.put("userID", newUserId);
            result.put("customerID", newCustomerId);
            result.put("email", email);
            result.put("role", "customer");
            result.put("firstName", firstName);
            result.put("lastName", lastName);
            return result;

        } catch (SQLException e) {
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

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT UserID, Username, Email, Role, IsActive, CreatedAt FROM USERS ORDER BY UserID ASC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("UserID"));
                user.setUsername(rs.getString("Username"));
                user.setEmail(rs.getString("Email"));
                user.setRole(rs.getString("Role"));
                user.setActive(rs.getBoolean("IsActive"));
                user.setCreatedAt(rs.getTimestamp("CreatedAt"));
                list.add(user);
            }
        } catch (SQLException e) {
            System.err.println("JDBC Error in getAllUsers: " + e.getMessage());
        }
        return list;
    }
}
