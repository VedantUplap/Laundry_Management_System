-- =============================================================================
-- LSMS — SQL Views
-- =============================================================================
USE lsms;

-- View: Customer order history with billing status
CREATE OR REPLACE VIEW vw_customer_order_history AS
SELECT
    o.OrderID,
    o.CustomerID,
    CONCAT(c.FirstName, ' ', c.LastName)    AS CustomerName,
    c.Phone                                  AS CustomerPhone,
    o.OrderDate,
    o.PickupDate,
    o.DeliveryDate,
    o.Status                                 AS OrderStatus,
    o.TotalWeight,
    o.TotalCost,
    b.BillID,
    b.BillStatus,
    b.TotalAmount                            AS BilledAmount,
    COALESCE(SUM(p.AmountPaid), 0)           AS AmountPaid,
    (b.TotalAmount - COALESCE(SUM(p.AmountPaid), 0)) AS BalanceDue
FROM ORDERS o
JOIN CUSTOMER c      ON c.CustomerID = o.CustomerID
LEFT JOIN BILLING b  ON b.OrderID    = o.OrderID
LEFT JOIN PAYMENT p  ON p.BillID     = b.BillID
GROUP BY
    o.OrderID, o.CustomerID, CustomerName, CustomerPhone,
    o.OrderDate, o.PickupDate, o.DeliveryDate, o.Status,
    o.TotalWeight, o.TotalCost, b.BillID, b.BillStatus, b.TotalAmount;

-- View: Pending deliveries
CREATE OR REPLACE VIEW vw_pending_deliveries AS
SELECT
    d.DeliveryID,
    d.OrderID,
    d.DeliveryStatus,
    d.PickupTime,
    d.DeliveryTime,
    CONCAT(c.FirstName, ' ', c.LastName)    AS CustomerName,
    c.Phone                                  AS CustomerPhone,
    c.Address                                AS DeliveryAddress,
    da.AgentName,
    da.Phone                                 AS AgentPhone,
    da.VehicleNumber,
    o.Status                                 AS OrderStatus
FROM DELIVERY d
JOIN ORDERS o               ON o.OrderID     = d.OrderID
JOIN CUSTOMER c             ON c.CustomerID  = o.CustomerID
LEFT JOIN DELIVERY_AGENT da ON da.AgentID    = d.AgentID
WHERE d.DeliveryStatus NOT IN ('Delivered','Failed');

-- View: Outstanding payments (bills not fully paid)
CREATE OR REPLACE VIEW vw_outstanding_payments AS
SELECT
    b.BillID,
    b.OrderID,
    CONCAT(c.FirstName, ' ', c.LastName)    AS CustomerName,
    c.Phone,
    b.BillDate,
    b.DueDate,
    b.TotalAmount,
    COALESCE(SUM(p.AmountPaid), 0)           AS TotalPaid,
    (b.TotalAmount - COALESCE(SUM(p.AmountPaid), 0)) AS BalanceDue,
    b.BillStatus,
    CASE WHEN b.DueDate < CURDATE() AND b.BillStatus != 'Paid' THEN 'Overdue' ELSE 'Current' END AS PaymentAge
FROM BILLING b
JOIN ORDERS o     ON o.OrderID    = b.OrderID
JOIN CUSTOMER c   ON c.CustomerID = o.CustomerID
LEFT JOIN PAYMENT p ON p.BillID  = b.BillID
WHERE b.BillStatus != 'Paid'
GROUP BY
    b.BillID, b.OrderID, CustomerName, c.Phone,
    b.BillDate, b.DueDate, b.TotalAmount, b.BillStatus;

-- View: Inventory status with low-stock flag
CREATE OR REPLACE VIEW vw_inventory_status AS
SELECT
    ItemID,
    ItemName,
    Unit,
    QuantityAvailable,
    ReorderLevel,
    CostPerUnit,
    CASE
        WHEN QuantityAvailable = 0                    THEN 'Out of Stock'
        WHEN QuantityAvailable <= ReorderLevel        THEN 'Low Stock'
        ELSE 'In Stock'
    END AS StockStatus
FROM INVENTORY;

-- View: Service popularity (orders per service)
CREATE OR REPLACE VIEW vw_service_popularity AS
SELECT
    st.ServiceID,
    st.ServiceName,
    st.PricePerKg,
    COUNT(od.DetailID)                      AS TotalOrders,
    COALESCE(SUM(od.WeightKg), 0)           AS TotalWeightKg,
    COALESCE(SUM(od.ServiceCost), 0)        AS TotalRevenue
FROM SERVICE_TYPE st
LEFT JOIN ORDER_DETAILS od ON od.ServiceID = st.ServiceID
GROUP BY st.ServiceID, st.ServiceName, st.PricePerKg
ORDER BY TotalOrders DESC;

-- View: Daily revenue summary
CREATE OR REPLACE VIEW vw_daily_revenue AS
SELECT
    p.PaymentDate                           AS RevenueDate,
    COUNT(DISTINCT p.PaymentID)             AS TotalTransactions,
    SUM(p.AmountPaid)                       AS DailyRevenue
FROM PAYMENT p
GROUP BY p.PaymentDate
ORDER BY p.PaymentDate DESC;
