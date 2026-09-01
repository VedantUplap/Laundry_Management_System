const db = require('../config/db');

// GET /api/billing/:orderId
const getBillingByOrder = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT b.*,
              o.CustomerID, o.OrderDate, o.Status AS OrderStatus, o.TotalCost,
              CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, c.Phone, c.Address, u.Email
       FROM BILLING b
       JOIN ORDERS o   ON o.OrderID    = b.OrderID
       JOIN CUSTOMER c ON c.CustomerID = o.CustomerID
       JOIN USERS u    ON u.UserID     = c.UserID
       WHERE b.OrderID = ?`,
      [req.params.orderId]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Bill not found for this order.' });

    const bill = rows[0];
    if (req.user.role === 'customer' && req.user.customerID !== bill.CustomerID) {
      return res.status(403).json({ success: false, message: 'Access denied.' });
    }

    // Fetch payments
    const [payments] = await db.query(
      'SELECT * FROM PAYMENT WHERE BillID = ? ORDER BY PaymentDate',
      [bill.BillID]
    );
    bill.payments = payments;

    // Fetch order details
    const [details] = await db.query(
      `SELECT od.*, st.ServiceName FROM ORDER_DETAILS od
       JOIN SERVICE_TYPE st ON st.ServiceID = od.ServiceID
       WHERE od.OrderID = ?`,
      [req.params.orderId]
    );
    bill.orderDetails = details;

    res.json({ success: true, data: bill });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch billing.' });
  }
};

// POST /api/billing — manual bill generation
const createBilling = async (req, res) => {
  const { orderID, dueDate } = req.body;
  if (!orderID) return res.status(400).json({ success: false, message: 'orderID is required.' });

  try {
    // Check not already billed
    const [exists] = await db.query('SELECT BillID FROM BILLING WHERE OrderID = ?', [orderID]);
    if (exists.length > 0) return res.status(409).json({ success: false, message: 'Bill already exists for this order.' });

    const [order] = await db.query('SELECT TotalCost FROM ORDERS WHERE OrderID = ?', [orderID]);
    if (order.length === 0) return res.status(404).json({ success: false, message: 'Order not found.' });

    const due = dueDate || (() => {
      const d = new Date(); d.setDate(d.getDate() + 7); return d.toISOString().split('T')[0];
    })();

    const [result] = await db.query(
      `INSERT INTO BILLING (OrderID, BillDate, DueDate, TotalAmount, BillStatus)
       VALUES (?, CURDATE(), ?, ?, 'Unpaid')`,
      [orderID, due, order[0].TotalCost]
    );
    res.status(201).json({ success: true, message: 'Bill created.', billID: result.insertId });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to create bill.' });
  }
};

// GET /api/billing  (admin/staff — all bills)
const getAllBilling = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT b.*, o.OrderDate, o.Status AS OrderStatus,
              CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, c.Phone,
              COALESCE(SUM(p.AmountPaid), 0) AS TotalPaid
       FROM BILLING b
       JOIN ORDERS o   ON o.OrderID    = b.OrderID
       JOIN CUSTOMER c ON c.CustomerID = o.CustomerID
       LEFT JOIN PAYMENT p ON p.BillID = b.BillID
       GROUP BY b.BillID
       ORDER BY b.BillDate DESC`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch bills.' });
  }
};

module.exports = { getBillingByOrder, createBilling, getAllBilling };
