package com.lsms.dao;

import com.lsms.config.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeliveryDAO — Handles delivery tracking, agent assignment, and delivery status updates.
 */
public class DeliveryDAO {

    public List<Map<String, Object>> getAllDeliveries(String status, Integer agentId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT d.DeliveryID, d.OrderID, d.AgentID, d.PickupTime, d.DeliveryTime, d.DeliveryStatus, " +
            "       CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, c.Phone AS CustomerPhone, c.Address, " +
            "       da.AgentName, da.Phone AS AgentPhone, da.VehicleNumber, " +
            "       o.Status AS OrderStatus, o.TotalCost " +
            "FROM DELIVERY d " +
            "JOIN ORDERS o               ON o.OrderID    = d.OrderID " +
            "JOIN CUSTOMER c             ON c.CustomerID = o.CustomerID " +
            "LEFT JOIN DELIVERY_AGENT da ON da.AgentID   = d.AgentID " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append("AND d.DeliveryStatus = ? ");
            params.add(status.trim());
        }
        if (agentId != null) {
            sql.append("AND d.AgentID = ? ");
            params.add(agentId);
        }

        sql.append("ORDER BY d.DeliveryID DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("DeliveryID", rs.getInt("DeliveryID"));
                    map.put("OrderID", rs.getInt("OrderID"));
                    int agId = rs.getInt("AgentID");
                    map.put("AgentID", rs.wasNull() ? null : agId);
                    map.put("PickupTime", rs.getTimestamp("PickupTime") != null ? rs.getTimestamp("PickupTime").toString() : null);
                    map.put("DeliveryTime", rs.getTimestamp("DeliveryTime") != null ? rs.getTimestamp("DeliveryTime").toString() : null);
                    map.put("DeliveryStatus", rs.getString("DeliveryStatus"));
                    map.put("CustomerName", rs.getString("CustomerName"));
                    map.put("CustomerPhone", rs.getString("CustomerPhone"));
                    map.put("Address", rs.getString("Address"));
                    map.put("AgentName", rs.getString("AgentName"));
                    map.put("AgentPhone", rs.getString("AgentPhone"));
                    map.put("VehicleNumber", rs.getString("VehicleNumber"));
                    map.put("OrderStatus", rs.getString("OrderStatus"));
                    map.put("TotalCost", rs.getBigDecimal("TotalCost"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    public boolean updateDelivery(int deliveryId, Integer agentId, String deliveryStatus, String pickupTime, String deliveryTime) throws SQLException {
        List<String> setClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (agentId != null) {
            setClauses.add("AgentID = ?");
            params.add(agentId);
        }
        if (deliveryStatus != null && !deliveryStatus.isBlank()) {
            setClauses.add("DeliveryStatus = ?");
            params.add(deliveryStatus.trim());
        }
        if (pickupTime != null && !pickupTime.isBlank()) {
            setClauses.add("PickupTime = ?");
            params.add(Timestamp.valueOf(pickupTime.replace("T", " ")));
        }
        if (deliveryTime != null && !deliveryTime.isBlank()) {
            setClauses.add("DeliveryTime = ?");
            params.add(Timestamp.valueOf(deliveryTime.replace("T", " ")));
        }

        if (setClauses.isEmpty()) return false;

        String sql = "UPDATE DELIVERY SET " + String.join(", ", setClauses) + " WHERE DeliveryID = ?";
        params.add(deliveryId);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            return pstmt.executeUpdate() > 0;
        }
    }
}
