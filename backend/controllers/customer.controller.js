const db = require('../config/db');

// GET /api/customers
const getAllCustomers = async (req, res) => {
  try {
    const { search } = req.query;
    let sql = `SELECT c.CustomerID, c.FirstName, c.LastName, c.Phone, c.Address,
                      u.Email, u.IsActive, u.CreatedAt
               FROM CUSTOMER c JOIN USERS u ON u.UserID = c.UserID`;
    const params = [];
    if (search) {
      sql += ` WHERE c.FirstName LIKE ? OR c.LastName LIKE ? OR c.Phone LIKE ? OR u.Email LIKE ?`;
      const s = `%${search}%`;
      params.push(s, s, s, s);
    }
    sql += ' ORDER BY c.CustomerID DESC';
    const [rows] = await db.query(sql, params);
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch customers.' });
  }
};

// GET /api/customers/:id
const getCustomerById = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT c.CustomerID, c.FirstName, c.LastName, c.Phone, c.Address,
              u.Email, u.Username, u.IsActive, u.CreatedAt
       FROM CUSTOMER c JOIN USERS u ON u.UserID = c.UserID
       WHERE c.CustomerID = ?`,
      [req.params.id]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Customer not found.' });

    // Customers can only view their own profile
    if (req.user.role === 'customer' && req.user.customerID !== rows[0].CustomerID) {
      return res.status(403).json({ success: false, message: 'Access denied.' });
    }
    res.json({ success: true, data: rows[0] });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch customer.' });
  }
};

// PUT /api/customers/:id
const updateCustomer = async (req, res) => {
  const { firstName, lastName, phone, address } = req.body;
  const customerID = req.params.id;

  // Customers can only edit their own profile
  if (req.user.role === 'customer' && req.user.customerID !== parseInt(customerID)) {
    return res.status(403).json({ success: false, message: 'Access denied.' });
  }

  try {
    const [result] = await db.query(
      `UPDATE CUSTOMER SET FirstName = ?, LastName = ?, Phone = ?, Address = ? WHERE CustomerID = ?`,
      [firstName, lastName, phone, address, customerID]
    );
    if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Customer not found.' });
    res.json({ success: true, message: 'Customer updated successfully.' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to update customer.' });
  }
};

// DELETE /api/customers/:id
const deleteCustomer = async (req, res) => {
  try {
    const [result] = await db.query(
      'UPDATE USERS u JOIN CUSTOMER c ON c.UserID = u.UserID SET u.IsActive = 0 WHERE c.CustomerID = ?',
      [req.params.id]
    );
    if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Customer not found.' });
    res.json({ success: true, message: 'Customer deactivated.' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to deactivate customer.' });
  }
};

// GET /api/customers/:id/orders
const getCustomerOrders = async (req, res) => {
  const customerID = req.params.id;
  if (req.user.role === 'customer' && req.user.customerID !== parseInt(customerID)) {
    return res.status(403).json({ success: false, message: 'Access denied.' });
  }
  try {
    const [rows] = await db.query(
      `SELECT o.*, b.BillID, b.BillStatus, b.TotalAmount AS BilledAmount
       FROM ORDERS o
       LEFT JOIN BILLING b ON b.OrderID = o.OrderID
       WHERE o.CustomerID = ?
       ORDER BY o.OrderDate DESC`,
      [customerID]
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch orders.' });
  }
};

module.exports = { getAllCustomers, getCustomerById, updateCustomer, deleteCustomer, getCustomerOrders };
