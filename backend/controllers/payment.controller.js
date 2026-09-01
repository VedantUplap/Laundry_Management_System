const db = require('../config/db');

// GET /api/payments
const getAllPayments = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT p.*, b.OrderID, b.TotalAmount, b.BillStatus,
              CONCAT(c.FirstName,' ',c.LastName) AS CustomerName
       FROM PAYMENT p
       JOIN BILLING b  ON b.BillID    = p.BillID
       JOIN ORDERS o   ON o.OrderID   = b.OrderID
       JOIN CUSTOMER c ON c.CustomerID= o.CustomerID
       ORDER BY p.PaymentDate DESC`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch payments.' });
  }
};

// POST /api/payments
const createPayment = async (req, res) => {
  const { billID, amountPaid, paymentMethod, transactionID } = req.body;

  if (!billID || !amountPaid || !paymentMethod) {
    return res.status(400).json({ success: false, message: 'billID, amountPaid, and paymentMethod are required.' });
  }
  if (amountPaid <= 0) {
    return res.status(400).json({ success: false, message: 'Amount must be positive.' });
  }

  const validMethods = ['Cash','UPI','Card','Net Banking'];
  if (!validMethods.includes(paymentMethod)) {
    return res.status(400).json({ success: false, message: 'Invalid payment method.' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    const [bill] = await conn.query('SELECT * FROM BILLING WHERE BillID = ?', [billID]);
    if (bill.length === 0) {
      await conn.rollback();
      return res.status(404).json({ success: false, message: 'Bill not found.' });
    }
    if (bill[0].BillStatus === 'Paid') {
      await conn.rollback();
      return res.status(400).json({ success: false, message: 'Bill is already fully paid.' });
    }

    // Record payment
    const [result] = await conn.query(
      `INSERT INTO PAYMENT (BillID, PaymentDate, AmountPaid, PaymentMethod, TransactionID)
       VALUES (?, CURDATE(), ?, ?, ?)`,
      [billID, amountPaid, paymentMethod, transactionID || null]
    );

    // Recalculate total paid and update bill status
    const [totals] = await conn.query(
      'SELECT SUM(AmountPaid) AS total FROM PAYMENT WHERE BillID = ?',
      [billID]
    );
    const totalPaid = parseFloat(totals[0].total || 0);
    const billAmount = parseFloat(bill[0].TotalAmount);

    let newStatus;
    if (totalPaid >= billAmount)   newStatus = 'Paid';
    else if (totalPaid > 0)        newStatus = 'Partially Paid';
    else                           newStatus = 'Unpaid';

    await conn.query('UPDATE BILLING SET BillStatus = ? WHERE BillID = ?', [newStatus, billID]);

    await conn.commit();
    res.status(201).json({
      success:   true,
      message:   'Payment recorded successfully.',
      paymentID: result.insertId,
      newStatus,
      totalPaid,
      balance:   Math.max(0, billAmount - totalPaid),
    });
  } catch (err) {
    await conn.rollback();
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to record payment.' });
  } finally {
    conn.release();
  }
};

module.exports = { getAllPayments, createPayment };
