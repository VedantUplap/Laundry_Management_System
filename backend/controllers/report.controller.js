const db = require('../config/db');

// GET /api/reports/daily-revenue
const dailyRevenue = async (req, res) => {
  try {
    const { from, to } = req.query;
    let sql = `
      SELECT p.PaymentDate AS date,
             COUNT(p.PaymentID)  AS transactions,
             SUM(p.AmountPaid)   AS revenue
      FROM PAYMENT p
      WHERE 1=1`;
    const params = [];
    if (from) { sql += ' AND p.PaymentDate >= ?'; params.push(from); }
    if (to)   { sql += ' AND p.PaymentDate <= ?'; params.push(to); }
    sql += ' GROUP BY p.PaymentDate ORDER BY p.PaymentDate DESC';
    const [rows] = await db.query(sql, params);
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Report failed.' });
  }
};

// GET /api/reports/monthly-revenue
const monthlyRevenue = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT DATE_FORMAT(p.PaymentDate, '%Y-%m') AS month,
              COUNT(p.PaymentID)                   AS transactions,
              SUM(p.AmountPaid)                    AS revenue
       FROM PAYMENT p
       GROUP BY DATE_FORMAT(p.PaymentDate, '%Y-%m')
       ORDER BY month DESC
       LIMIT 12`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Report failed.' });
  }
};

// GET /api/reports/pending-deliveries
const pendingDeliveries = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT d.DeliveryID, d.OrderID, d.DeliveryStatus, d.PickupTime,
              CONCAT(c.FirstName,' ',c.LastName) AS CustomerName,
              c.Phone, c.Address,
              da.AgentName, da.Phone AS AgentPhone,
              o.OrderDate, o.TotalCost
       FROM DELIVERY d
       JOIN ORDERS o               ON o.OrderID    = d.OrderID
       JOIN CUSTOMER c             ON c.CustomerID = o.CustomerID
       LEFT JOIN DELIVERY_AGENT da ON da.AgentID   = d.AgentID
       WHERE d.DeliveryStatus NOT IN ('Delivered','Failed')
       ORDER BY o.OrderDate`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Report failed.' });
  }
};

// GET /api/reports/customer-history/:customerId
const customerHistory = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT o.OrderID, o.OrderDate, o.Status, o.TotalWeight, o.TotalCost,
              b.BillStatus, b.TotalAmount,
              COALESCE(SUM(p.AmountPaid), 0) AS TotalPaid
       FROM ORDERS o
       LEFT JOIN BILLING b  ON b.OrderID = o.OrderID
       LEFT JOIN PAYMENT p  ON p.BillID  = b.BillID
       WHERE o.CustomerID = ?
       GROUP BY o.OrderID, o.OrderDate, o.Status, o.TotalWeight, o.TotalCost,
                b.BillStatus, b.TotalAmount
       ORDER BY o.OrderDate DESC`,
      [req.params.customerId]
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Report failed.' });
  }
};

// GET /api/reports/inventory-usage
const inventoryUsage = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT i.ItemID, i.ItemName, i.Unit, i.QuantityAvailable, i.ReorderLevel,
              COUNT(ul.UsageID)           AS UsageCount,
              COALESCE(SUM(ul.QuantityUsed), 0) AS TotalUsed
       FROM INVENTORY i
       LEFT JOIN USAGE_LOG ul ON ul.ItemID = i.ItemID
       GROUP BY i.ItemID, i.ItemName, i.Unit, i.QuantityAvailable, i.ReorderLevel
       ORDER BY TotalUsed DESC`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Report failed.' });
  }
};

// GET /api/reports/popular-services
const popularServices = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT st.ServiceID, st.ServiceName, st.PricePerKg,
              COUNT(od.DetailID)          AS OrderCount,
              COALESCE(SUM(od.WeightKg),0)   AS TotalKg,
              COALESCE(SUM(od.ServiceCost),0) AS TotalRevenue
       FROM SERVICE_TYPE st
       LEFT JOIN ORDER_DETAILS od ON od.ServiceID = st.ServiceID
       GROUP BY st.ServiceID, st.ServiceName, st.PricePerKg
       ORDER BY OrderCount DESC`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Report failed.' });
  }
};

// GET /api/reports/outstanding-payments
const outstandingPayments = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT b.BillID, b.OrderID, b.BillDate, b.DueDate, b.TotalAmount, b.BillStatus,
              COALESCE(SUM(p.AmountPaid), 0)                             AS TotalPaid,
              b.TotalAmount - COALESCE(SUM(p.AmountPaid), 0)            AS BalanceDue,
              CONCAT(c.FirstName,' ',c.LastName)                         AS CustomerName,
              c.Phone,
              CASE WHEN b.DueDate < CURDATE() THEN 'Overdue' ELSE 'Current' END AS AgeStatus
       FROM BILLING b
       JOIN ORDERS o   ON o.OrderID    = b.OrderID
       JOIN CUSTOMER c ON c.CustomerID = o.CustomerID
       LEFT JOIN PAYMENT p ON p.BillID = b.BillID
       WHERE b.BillStatus != 'Paid'
       GROUP BY b.BillID, b.OrderID, b.BillDate, b.DueDate,
                b.TotalAmount, b.BillStatus, CustomerName, c.Phone
       ORDER BY b.DueDate`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Report failed.' });
  }
};

module.exports = { dailyRevenue, monthlyRevenue, pendingDeliveries, customerHistory, inventoryUsage, popularServices, outstandingPayments };
