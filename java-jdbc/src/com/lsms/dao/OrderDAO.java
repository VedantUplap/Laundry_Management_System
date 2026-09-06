package com.lsms.dao;

import com.lsms.config.DBConnection;
import com.lsms.models.Order;
import com.lsms.models.ServiceType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * OrderDAO — Demonstrates complex JDBC queries, multi-table transactions (commit/rollback),
 * and relational synchronization between ORDERS, ORDER_DETAILS, BILLING, and DELIVERY.
 */
public class OrderDAO {

    public List<Map<String, Object>> getAllOrders(String status, Integer customerId, String from, String to) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT o.OrderID, o.CustomerID, o.OrderDate, o.PickupDate, o.DeliveryDate, " +
            "       o.Status, o.TotalWeight, o.TotalCost, o.Notes, " +
            "       CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, c.Phone, " +
            "       b.BillID, b.BillStatus " +
            "FROM ORDERS o " +
            "JOIN CUSTOMER c     ON c.CustomerID = o.CustomerID " +
            "LEFT JOIN BILLING b ON b.OrderID    = o.OrderID " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (customerId != null) {
            sql.append("AND o.CustomerID = ? ");
            params.add(customerId);
        }
        if (status != null && !status.isBlank()) {
            sql.append("AND o.Status = ? ");
            params.add(status.trim());
        }
        if (from != null && !from.isBlank()) {
            sql.append("AND o.OrderDate >= ? ");
            params.add(from.trim());
        }
        if (to != null && !to.isBlank()) {
            sql.append("AND o.OrderDate <= ? ");
            params.add(to.trim());
        }

        sql.append("ORDER BY o.OrderDate DESC, o.OrderID DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

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
                    map.put("CustomerName", rs.getString("CustomerName"));
                    map.put("Phone", rs.getString("Phone"));
                    int billId = rs.getInt("BillID");
                    map.put("BillID", rs.wasNull() ? null : billId);
                    map.put("BillStatus", rs.getString("BillStatus"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    public Map<String, Object> getOrderById(int orderId) throws SQLException {
        String orderSql =
            "SELECT o.*, CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, " +
            "       c.Phone, c.Address, u.Email " +
            "FROM ORDERS o " +
            "JOIN CUSTOMER c ON c.CustomerID = o.CustomerID " +
            "JOIN USERS u    ON u.UserID     = c.UserID " +
            "WHERE o.OrderID = ?";

        String detailsSql =
            "SELECT od.*, st.ServiceName, st.PricePerKg " +
            "FROM ORDER_DETAILS od " +
            "JOIN SERVICE_TYPE st ON st.ServiceID = od.ServiceID " +
            "WHERE od.OrderID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmtOrder = conn.prepareStatement(orderSql);
             PreparedStatement pstmtDetails = conn.prepareStatement(detailsSql)) {

            pstmtOrder.setInt(1, orderId);
            Map<String, Object> orderMap = null;

            try (ResultSet rs = pstmtOrder.executeQuery()) {
                if (rs.next()) {
                    orderMap = new HashMap<>();
                    orderMap.put("OrderID", rs.getInt("OrderID"));
                    orderMap.put("CustomerID", rs.getInt("CustomerID"));
                    orderMap.put("OrderDate", rs.getDate("OrderDate") != null ? rs.getDate("OrderDate").toString() : null);
                    orderMap.put("PickupDate", rs.getDate("PickupDate") != null ? rs.getDate("PickupDate").toString() : null);
                    orderMap.put("DeliveryDate", rs.getDate("DeliveryDate") != null ? rs.getDate("DeliveryDate").toString() : null);
                    orderMap.put("Status", rs.getString("Status"));
                    orderMap.put("TotalWeight", rs.getBigDecimal("TotalWeight"));
                    orderMap.put("TotalCost", rs.getBigDecimal("TotalCost"));
                    orderMap.put("Notes", rs.getString("Notes"));
                    orderMap.put("CustomerName", rs.getString("CustomerName"));
                    orderMap.put("Phone", rs.getString("Phone"));
                    orderMap.put("Address", rs.getString("Address"));
                    orderMap.put("Email", rs.getString("Email"));
                }
            }

            if (orderMap == null) return null;

            pstmtDetails.setInt(1, orderId);
            List<Map<String, Object>> detailsList = new ArrayList<>();
            try (ResultSet rs = pstmtDetails.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> d = new HashMap<>();
                    d.put("DetailID", rs.getInt("DetailID"));
                    d.put("OrderID", rs.getInt("OrderID"));
                    d.put("ServiceID", rs.getInt("ServiceID"));
                    d.put("GarmentType", rs.getString("GarmentType"));
                    d.put("Quantity", rs.getInt("Quantity"));
                    d.put("WeightKg", rs.getBigDecimal("WeightKg"));
                    d.put("ServiceCost", rs.getBigDecimal("ServiceCost"));
                    d.put("ServiceName", rs.getString("ServiceName"));
                    d.put("PricePerKg", rs.getBigDecimal("PricePerKg"));
                    detailsList.add(d);
                }
            }
            orderMap.put("details", detailsList);
            return orderMap;
        }
    }

    /**
     * Creates an order with multiple details and auto-creates delivery in a single atomic JDBC transaction.
     */
    public Map<String, Object> createOrder(int customerId, String pickupDate, String notes,
                                          List<Map<String, Object>> details) throws Exception {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin JDBC Transaction

            // 1. Insert into ORDERS
            String insertOrderSql = "INSERT INTO ORDERS (CustomerID, OrderDate, PickupDate, Status, Notes) VALUES (?, CURDATE(), ?, 'Pending', ?)";
            int orderId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, customerId);
                if (pickupDate != null && !pickupDate.isBlank()) {
                    pstmt.setDate(2, java.sql.Date.valueOf(pickupDate.trim()));
                } else {
                    pstmt.setNull(2, Types.DATE);
                }
                pstmt.setString(3, notes);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) orderId = rs.getInt(1);
                }
            }

            if (orderId <= 0) {
                conn.rollback();
                throw new SQLException("Failed to create order record.");
            }

            // 2. Insert line items into ORDER_DETAILS
            String priceSql = "SELECT PricePerKg, IsActive FROM SERVICE_TYPE WHERE ServiceID = ?";
            String insertDetailSql = "INSERT INTO ORDER_DETAILS (OrderID, ServiceID, GarmentType, Quantity, WeightKg, ServiceCost) VALUES (?, ?, ?, ?, ?, ?)";

            for (Map<String, Object> item : details) {
                int serviceId = ((Number) item.get("serviceID")).intValue();
                String garmentType = (String) item.get("garmentType");
                int quantity = ((Number) item.get("quantity")).intValue();
                BigDecimal weightKg = new BigDecimal(item.get("weightKg").toString());

                if (quantity <= 0 || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
                    conn.rollback();
                    throw new IllegalArgumentException("Quantity and weight must be positive.");
                }

                // Authoritative price lookup
                BigDecimal pricePerKg;
                try (PreparedStatement pStmt = conn.prepareStatement(priceSql)) {
                    pStmt.setInt(1, serviceId);
                    try (ResultSet rs = pStmt.executeQuery()) {
                        if (!rs.next() || rs.getInt("IsActive") != 1) {
                            conn.rollback();
                            throw new IllegalArgumentException("Service ID " + serviceId + " not found or inactive.");
                        }
                        pricePerKg = rs.getBigDecimal("PricePerKg");
                    }
                }

                BigDecimal serviceCost = weightKg.multiply(pricePerKg);

                try (PreparedStatement dStmt = conn.prepareStatement(insertDetailSql)) {
                    dStmt.setInt(1, orderId);
                    dStmt.setInt(2, serviceId);
                    dStmt.setString(3, garmentType);
                    dStmt.setInt(4, quantity);
                    dStmt.setBigDecimal(5, weightKg);
                    dStmt.setBigDecimal(6, serviceCost);
                    dStmt.executeUpdate();
                }
            }

            // 3. MySQL Triggers recalculate TotalWeight and TotalCost; fetch updated values
            BigDecimal totalWeight = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            String checkTotalsSql = "SELECT TotalWeight, TotalCost FROM ORDERS WHERE OrderID = ?";
            try (PreparedStatement cStmt = conn.prepareStatement(checkTotalsSql)) {
                cStmt.setInt(1, orderId);
                try (ResultSet rs = cStmt.executeQuery()) {
                    if (rs.next()) {
                        totalWeight = rs.getBigDecimal("TotalWeight");
                        totalCost = rs.getBigDecimal("TotalCost");
                    }
                }
            }

            // 4. Auto-insert DELIVERY row
            String insertDeliverySql = "INSERT INTO DELIVERY (OrderID, DeliveryStatus) VALUES (?, 'Pending')";
            try (PreparedStatement delStmt = conn.prepareStatement(insertDeliverySql)) {
                delStmt.setInt(1, orderId);
                delStmt.executeUpdate();
            }

            conn.commit(); // Commit Transaction

            Map<String, Object> result = new HashMap<>();
            result.put("orderID", orderId);
            result.put("totalWeight", totalWeight);
            result.put("totalCost", totalCost);
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

    /**
     * Updates order status with auto-billing trigger when Packed and automatic delivery sync.
     */
    public boolean updateOrderStatus(int orderId, String newStatus) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin JDBC Transaction

            // 1. Check order exists
            String checkSql = "SELECT Status, CustomerID, TotalCost FROM ORDERS WHERE OrderID = ?";
            BigDecimal totalCost = BigDecimal.ZERO;
            try (PreparedStatement cStmt = conn.prepareStatement(checkSql)) {
                cStmt.setInt(1, orderId);
                try (ResultSet rs = cStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    totalCost = rs.getBigDecimal("TotalCost");
                }
            }

            // 2. Update status
            String updateSql = "UPDATE ORDERS SET Status = ? WHERE OrderID = ?";
            try (PreparedStatement uStmt = conn.prepareStatement(updateSql)) {
                uStmt.setString(1, newStatus);
                uStmt.setInt(2, orderId);
                uStmt.executeUpdate();
            }

            // 3. Auto-generate billing when status reaches 'Packed'
            if ("Packed".equalsIgnoreCase(newStatus)) {
                String billCheckSql = "SELECT BillID FROM BILLING WHERE OrderID = ?";
                boolean hasBill = false;
                try (PreparedStatement bStmt = conn.prepareStatement(billCheckSql)) {
                    bStmt.setInt(1, orderId);
                    try (ResultSet rs = bStmt.executeQuery()) {
                        hasBill = rs.next();
                    }
                }
                if (!hasBill) {
                    String createBillSql = "INSERT INTO BILLING (OrderID, BillDate, DueDate, TotalAmount, BillStatus) VALUES (?, CURDATE(), ?, ?, 'Unpaid')";
                    try (PreparedStatement cbStmt = conn.prepareStatement(createBillSql)) {
                        cbStmt.setInt(1, orderId);
                        cbStmt.setDate(2, java.sql.Date.valueOf(LocalDate.now().plusDays(7)));
                        cbStmt.setBigDecimal(3, totalCost);
                        cbStmt.executeUpdate();
                    }
                }
            }

            // 4. Update delivery status in sync
            Map<String, String> deliveryMap = new HashMap<>();
            deliveryMap.put("Picked Up", "Picked Up");
            deliveryMap.put("Out for Delivery", "In Transit");
            deliveryMap.put("Delivered", "Delivered");
            deliveryMap.put("Cancelled", "Failed");

            if (deliveryMap.containsKey(newStatus)) {
                String delStatus = deliveryMap.get(newStatus);
                String delSql = "UPDATE DELIVERY SET DeliveryStatus = ? WHERE OrderID = ?";
                try (PreparedStatement dStmt = conn.prepareStatement(delSql)) {
                    dStmt.setString(1, delStatus);
                    dStmt.setInt(2, orderId);
                    dStmt.executeUpdate();
                }

                if ("Delivered".equalsIgnoreCase(newStatus)) {
                    String delTimeSql = "UPDATE DELIVERY SET DeliveryTime = NOW() WHERE OrderID = ?";
                    try (PreparedStatement dtStmt = conn.prepareStatement(delTimeSql)) {
                        dtStmt.setInt(1, orderId);
                        dtStmt.executeUpdate();
                    }
                }
            }

            conn.commit(); // Commit Transaction
            return true;

        } catch (SQLException e) {
            System.err.println("JDBC Error in updateOrderStatus: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public boolean cancelOrder(int orderId) {
        String checkSql = "SELECT Status FROM ORDERS WHERE OrderID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement cStmt = conn.prepareStatement(checkSql)) {

            cStmt.setInt(1, orderId);
            try (ResultSet rs = cStmt.executeQuery()) {
                if (!rs.next()) return false;
                String currentStatus = rs.getString("Status");
                List<String> nonCancellable = Arrays.asList(
                    "Washing","Drying","Ironing","Packed","Ready for Delivery","Out for Delivery","Delivered"
                );
                if (nonCancellable.contains(currentStatus)) {
                    throw new IllegalStateException("Order cannot be cancelled at this stage (" + currentStatus + ").");
                }
            }

            return updateOrderStatus(orderId, "Cancelled");
        } catch (SQLException e) {
            System.err.println("JDBC Error in cancelOrder: " + e.getMessage());
            return false;
        }
    }

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

    public int createOrderWithItem(int customerId, int serviceId, String garmentType, int quantity, BigDecimal weightKg, BigDecimal serviceCost, String notes) {
        try {
            Map<String, Object> item = new HashMap<>();
            item.put("serviceID", serviceId);
            item.put("garmentType", garmentType);
            item.put("quantity", quantity);
            item.put("weightKg", weightKg);
            List<Map<String, Object>> details = Collections.singletonList(item);
            Map<String, Object> res = createOrder(customerId, null, notes, details);
            return (Integer) res.get("orderID");
        } catch (Exception e) {
            System.err.println("Error in createOrderWithItem: " + e.getMessage());
            return -1;
        }
    }
}
