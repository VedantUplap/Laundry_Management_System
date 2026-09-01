const db = require('../config/db');

// POST /api/feedback
const submitFeedback = async (req, res) => {
  const { orderID, rating, comments } = req.body;
  const customerID = req.user.customerID;

  if (!orderID || !rating) {
    return res.status(400).json({ success: false, message: 'orderID and rating are required.' });
  }
  if (rating < 1 || rating > 5) {
    return res.status(400).json({ success: false, message: 'Rating must be between 1 and 5.' });
  }

  try {
    // Verify order belongs to customer and is delivered
    const [orders] = await db.query(
      "SELECT OrderID, Status FROM ORDERS WHERE OrderID = ? AND CustomerID = ?",
      [orderID, customerID]
    );
    if (orders.length === 0) {
      return res.status(404).json({ success: false, message: 'Order not found.' });
    }
    if (orders[0].Status !== 'Delivered') {
      return res.status(400).json({ success: false, message: 'Feedback can only be submitted for delivered orders.' });
    }

    // Check for duplicate
    const [exists] = await db.query('SELECT FeedbackID FROM FEEDBACK WHERE OrderID = ?', [orderID]);
    if (exists.length > 0) {
      return res.status(409).json({ success: false, message: 'Feedback already submitted for this order.' });
    }

    const [result] = await db.query(
      `INSERT INTO FEEDBACK (CustomerID, OrderID, Rating, Comments, FeedbackDate)
       VALUES (?, ?, ?, ?, CURDATE())`,
      [customerID, orderID, rating, comments || null]
    );
    res.status(201).json({ success: true, message: 'Feedback submitted. Thank you!', feedbackID: result.insertId });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to submit feedback.' });
  }
};

// GET /api/feedback
const getAllFeedback = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT f.*, 
              CONCAT(c.FirstName,' ',c.LastName) AS CustomerName,
              o.OrderDate, o.TotalCost
       FROM FEEDBACK f
       JOIN CUSTOMER c ON c.CustomerID = f.CustomerID
       JOIN ORDERS o   ON o.OrderID    = f.OrderID
       ORDER BY f.FeedbackDate DESC`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch feedback.' });
  }
};

// GET /api/feedback/my (customer's own feedback)
const getMyFeedback = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT f.*, o.OrderDate, o.TotalCost, o.Status AS OrderStatus
       FROM FEEDBACK f
       JOIN ORDERS o ON o.OrderID = f.OrderID
       WHERE f.CustomerID = ?
       ORDER BY f.FeedbackDate DESC`,
      [req.user.customerID]
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch feedback.' });
  }
};

module.exports = { submitFeedback, getAllFeedback, getMyFeedback };
