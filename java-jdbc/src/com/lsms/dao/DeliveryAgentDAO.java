package com.lsms.dao;

import com.lsms.config.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeliveryAgentDAO — Manages delivery personnel and active task counts.
 */
public class DeliveryAgentDAO {

    public List<Map<String, Object>> getAllAgents() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT da.*, " +
            "  (SELECT COUNT(*) FROM DELIVERY d WHERE d.AgentID = da.AgentID AND d.DeliveryStatus NOT IN ('Delivered','Failed')) AS ActiveDeliveries " +
            "FROM DELIVERY_AGENT da " +
            "WHERE da.IsActive = 1 " +
            "ORDER BY da.AgentName";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("AgentID", rs.getInt("AgentID"));
                int userId = rs.getInt("UserID");
                map.put("UserID", rs.wasNull() ? null : userId);
                map.put("AgentName", rs.getString("AgentName"));
                map.put("Phone", rs.getString("Phone"));
                map.put("VehicleNumber", rs.getString("VehicleNumber"));
                map.put("IsActive", rs.getInt("IsActive"));
                map.put("ActiveDeliveries", rs.getInt("ActiveDeliveries"));
                list.add(map);
            }
        }
        return list;
    }

    public int createAgent(String agentName, String phone, String vehicleNumber) throws SQLException {
        String sql = "INSERT INTO DELIVERY_AGENT (AgentName, Phone, VehicleNumber) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, agentName);
            pstmt.setString(2, phone);
            pstmt.setString(3, vehicleNumber);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public boolean updateAgent(int agentId, String agentName, String phone, String vehicleNumber, Integer isActive) throws SQLException {
        String sql = "UPDATE DELIVERY_AGENT SET " +
                     "  AgentName     = COALESCE(?, AgentName), " +
                     "  Phone         = COALESCE(?, Phone), " +
                     "  VehicleNumber = COALESCE(?, VehicleNumber), " +
                     "  IsActive      = COALESCE(?, IsActive) " +
                     "WHERE AgentID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, agentName);
            pstmt.setString(2, phone);
            pstmt.setString(3, vehicleNumber);
            if (isActive != null) pstmt.setInt(4, isActive);
            else pstmt.setNull(4, Types.INTEGER);

            pstmt.setInt(5, agentId);
            return pstmt.executeUpdate() > 0;
        }
    }
}
