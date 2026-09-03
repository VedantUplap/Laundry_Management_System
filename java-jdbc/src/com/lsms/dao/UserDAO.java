package com.lsms.dao;

import com.lsms.config.DBConnection;
import com.lsms.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO — Demonstrates JDBC PreparedStatement queries and authentication for USERS table.
 */
public class UserDAO {

    /**
     * Authenticates a user by email using JDBC PreparedStatement.
     */
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
     * Lists all registered system users via JDBC Statement.
     */
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

    /**
     * Inserts a new user with CUSTOMER role (ensures role enforcement via JDBC).
     */
    public int createCustomerUser(String username, String email, String passwordHash) throws SQLException {
        String sql = "INSERT INTO USERS (Username, Email, Password, Role) VALUES (?, ?, ?, 'customer')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, passwordHash);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return -1;
    }
}
