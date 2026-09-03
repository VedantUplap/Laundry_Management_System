package com.lsms.dao;

import com.lsms.config.DBConnection;
import com.lsms.models.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomerDAO — Demonstrates JDBC CRUD operations on CUSTOMER table.
 */
public class CustomerDAO {

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

    public Customer getCustomerById(int customerId) {
        String sql = "SELECT CustomerID, UserID, FirstName, LastName, Phone, Address FROM CUSTOMER WHERE CustomerID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                        rs.getInt("CustomerID"),
                        rs.getInt("UserID"),
                        rs.getString("FirstName"),
                        rs.getString("LastName"),
                        rs.getString("Phone"),
                        rs.getString("Address")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("JDBC Error in getCustomerById: " + e.getMessage());
        }
        return null;
    }

    public boolean updateCustomer(int customerId, String phone, String address) {
        String sql = "UPDATE CUSTOMER SET Phone = ?, Address = ? WHERE CustomerID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, phone);
            pstmt.setString(2, address);
            pstmt.setInt(3, customerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("JDBC Error in updateCustomer: " + e.getMessage());
            return false;
        }
    }
}
