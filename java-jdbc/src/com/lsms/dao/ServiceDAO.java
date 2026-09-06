package com.lsms.dao;

import com.lsms.config.DBConnection;
import com.lsms.models.ServiceType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ServiceDAO — CRUD operations for laundry service offerings (SERVICE_TYPE).
 */
public class ServiceDAO {

    public List<Map<String, Object>> getAllServices(boolean activeOnly) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM SERVICE_TYPE" + (activeOnly ? " WHERE IsActive = 1" : "") + " ORDER BY ServiceName";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("ServiceID", rs.getInt("ServiceID"));
                map.put("ServiceName", rs.getString("ServiceName"));
                map.put("Description", rs.getString("Description"));
                map.put("PricePerKg", rs.getBigDecimal("PricePerKg"));
                map.put("IsActive", rs.getInt("IsActive"));
                list.add(map);
            }
        }
        return list;
    }

    public Map<String, Object> getServiceById(int serviceId) throws SQLException {
        String sql = "SELECT * FROM SERVICE_TYPE WHERE ServiceID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, serviceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("ServiceID", rs.getInt("ServiceID"));
                    map.put("ServiceName", rs.getString("ServiceName"));
                    map.put("Description", rs.getString("Description"));
                    map.put("PricePerKg", rs.getBigDecimal("PricePerKg"));
                    map.put("IsActive", rs.getInt("IsActive"));
                    return map;
                }
            }
        }
        return null;
    }

    public int createService(String serviceName, String description, BigDecimal pricePerKg) throws SQLException {
        String sql = "INSERT INTO SERVICE_TYPE (ServiceName, Description, PricePerKg) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, serviceName);
            pstmt.setString(2, description);
            pstmt.setBigDecimal(3, pricePerKg);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public boolean updateService(int serviceId, String serviceName, String description, BigDecimal pricePerKg, Integer isActive) throws SQLException {
        String sql = "UPDATE SERVICE_TYPE SET " +
                     "  ServiceName = COALESCE(?, ServiceName), " +
                     "  Description = COALESCE(?, Description), " +
                     "  PricePerKg  = COALESCE(?, PricePerKg), " +
                     "  IsActive    = COALESCE(?, IsActive) " +
                     "WHERE ServiceID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, serviceName);
            pstmt.setString(2, description);
            if (pricePerKg != null) pstmt.setBigDecimal(3, pricePerKg);
            else pstmt.setNull(3, Types.DECIMAL);

            if (isActive != null) pstmt.setInt(4, isActive);
            else pstmt.setNull(4, Types.INTEGER);

            pstmt.setInt(5, serviceId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteService(int serviceId) throws SQLException {
        // Soft delete matching Node.js implementation
        String sql = "UPDATE SERVICE_TYPE SET IsActive = 0 WHERE ServiceID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, serviceId);
            return pstmt.executeUpdate() > 0;
        }
    }
}
