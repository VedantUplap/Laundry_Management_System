package com.lsms.dao;

import com.lsms.config.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InventoryDAO — Handles stock tracking, item CRUD, and transactional usage logging.
 */
public class InventoryDAO {

    public List<Map<String, Object>> getAllInventory() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT *, " +
            "  CASE " +
            "    WHEN QuantityAvailable = 0             THEN 'Out of Stock' " +
            "    WHEN QuantityAvailable <= ReorderLevel THEN 'Low Stock' " +
            "    ELSE 'In Stock' " +
            "  END AS StockStatus " +
            "FROM INVENTORY ORDER BY ItemName";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("ItemID", rs.getInt("ItemID"));
                map.put("ItemName", rs.getString("ItemName"));
                map.put("Unit", rs.getString("Unit"));
                map.put("QuantityAvailable", rs.getBigDecimal("QuantityAvailable"));
                map.put("ReorderLevel", rs.getBigDecimal("ReorderLevel"));
                map.put("CostPerUnit", rs.getBigDecimal("CostPerUnit"));
                map.put("StockStatus", rs.getString("StockStatus"));
                list.add(map);
            }
        }
        return list;
    }

    public Map<String, Object> getInventoryById(int itemId) throws SQLException {
        String sql = "SELECT * FROM INVENTORY WHERE ItemID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("ItemID", rs.getInt("ItemID"));
                    map.put("ItemName", rs.getString("ItemName"));
                    map.put("Unit", rs.getString("Unit"));
                    map.put("QuantityAvailable", rs.getBigDecimal("QuantityAvailable"));
                    map.put("ReorderLevel", rs.getBigDecimal("ReorderLevel"));
                    map.put("CostPerUnit", rs.getBigDecimal("CostPerUnit"));
                    return map;
                }
            }
        }
        return null;
    }

    public int createInventoryItem(String itemName, String unit, BigDecimal quantityAvailable, BigDecimal reorderLevel, BigDecimal costPerUnit) throws SQLException {
        String sql = "INSERT INTO INVENTORY (ItemName, Unit, QuantityAvailable, ReorderLevel, CostPerUnit) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, itemName);
            pstmt.setString(2, unit);
            pstmt.setBigDecimal(3, quantityAvailable != null ? quantityAvailable : BigDecimal.ZERO);
            pstmt.setBigDecimal(4, reorderLevel != null ? reorderLevel : BigDecimal.ZERO);
            pstmt.setBigDecimal(5, costPerUnit != null ? costPerUnit : BigDecimal.ZERO);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public boolean updateInventoryItem(int itemId, String itemName, String unit, BigDecimal quantityAvailable, BigDecimal reorderLevel, BigDecimal costPerUnit) throws SQLException {
        String sql = "UPDATE INVENTORY SET " +
                     "  ItemName          = COALESCE(?, ItemName), " +
                     "  Unit              = COALESCE(?, Unit), " +
                     "  QuantityAvailable = COALESCE(?, QuantityAvailable), " +
                     "  ReorderLevel      = COALESCE(?, ReorderLevel), " +
                     "  CostPerUnit       = COALESCE(?, CostPerUnit) " +
                     "WHERE ItemID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, itemName);
            pstmt.setString(2, unit);
            if (quantityAvailable != null) pstmt.setBigDecimal(3, quantityAvailable);
            else pstmt.setNull(3, Types.DECIMAL);

            if (reorderLevel != null) pstmt.setBigDecimal(4, reorderLevel);
            else pstmt.setNull(4, Types.DECIMAL);

            if (costPerUnit != null) pstmt.setBigDecimal(5, costPerUnit);
            else pstmt.setNull(5, Types.DECIMAL);

            pstmt.setInt(6, itemId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteInventoryItem(int itemId) throws SQLException {
        String sql = "DELETE FROM INVENTORY WHERE ItemID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public BigDecimal logUsage(int itemId, int orderId, BigDecimal quantityUsed) throws Exception {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin JDBC Transaction

            // 1. Verify item and check stock
            String checkSql = "SELECT QuantityAvailable FROM INVENTORY WHERE ItemID = ?";
            BigDecimal available;
            try (PreparedStatement cStmt = conn.prepareStatement(checkSql)) {
                cStmt.setInt(1, itemId);
                try (ResultSet rs = cStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new IllegalArgumentException("Item not found.");
                    }
                    available = rs.getBigDecimal("QuantityAvailable");
                }
            }

            if (available.compareTo(quantityUsed) < 0) {
                conn.rollback();
                throw new IllegalStateException("Insufficient stock.");
            }

            // 2. Insert into USAGE_LOG
            String insertSql = "INSERT INTO USAGE_LOG (OrderID, ItemID, QuantityUsed, UsageDate) VALUES (?, ?, ?, CURDATE())";
            try (PreparedStatement iStmt = conn.prepareStatement(insertSql)) {
                iStmt.setInt(1, orderId);
                iStmt.setInt(2, itemId);
                iStmt.setBigDecimal(3, quantityUsed);
                iStmt.executeUpdate();
            }

            // 3. MySQL trigger (trg_reduce_inventory_after_usage) deducts stock; query updated value
            BigDecimal remaining;
            try (PreparedStatement uStmt = conn.prepareStatement(checkSql)) {
                uStmt.setInt(1, itemId);
                try (ResultSet rs = uStmt.executeQuery()) {
                    rs.next();
                    remaining = rs.getBigDecimal("QuantityAvailable");
                }
            }

            conn.commit(); // Commit Transaction
            return remaining;

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

    public List<Map<String, Object>> getUsageLogs() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT ul.UsageID, ul.OrderID, ul.ItemID, ul.QuantityUsed, ul.UsageDate, " +
            "       i.ItemName, i.Unit, o.Status AS OrderStatus " +
            "FROM USAGE_LOG ul " +
            "JOIN INVENTORY i ON i.ItemID  = ul.ItemID " +
            "JOIN ORDERS o    ON o.OrderID = ul.OrderID " +
            "ORDER BY ul.UsageDate DESC, ul.UsageID DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("UsageID", rs.getInt("UsageID"));
                map.put("OrderID", rs.getInt("OrderID"));
                map.put("ItemID", rs.getInt("ItemID"));
                map.put("QuantityUsed", rs.getBigDecimal("QuantityUsed"));
                map.put("UsageDate", rs.getDate("UsageDate") != null ? rs.getDate("UsageDate").toString() : null);
                map.put("ItemName", rs.getString("ItemName"));
                map.put("Unit", rs.getString("Unit"));
                map.put("OrderStatus", rs.getString("OrderStatus"));
                list.add(map);
            }
        }
        return list;
    }
}
