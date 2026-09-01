const db = require('../config/db');

// GET /api/orders
const getAllOrders = async (req, res) => {
  try {
    const { status, customerID, from, to } = req.query;

    let sql = `
      SELECT o.OrderID, o.CustomerID, o.OrderDate, o.PickupDate, o.DeliveryDate,
             o.Status, o.TotalWeight, o.TotalCost, o.Notes,
             CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, c.Phone,
             b.BillID, b.BillStatus
      FROM ORDERS o
      JOIN CUSTOMER c     ON c.CustomerID = o.CustomerID
      LEFT JOIN BILLING b ON b.OrderID    = o.OrderID
      WHERE 1=1`;
    const params = [];

    // Customers see only their own orders
    if (req.user.role === 'customer') {
      sql += ' AND o.CustomerID = ?';
      params.push(req.user.customerID);
    } else if (customerID) {
      sql += ' AND o.CustomerID = ?';
      params.push(customerID);
    }

    if (status)   { sql += ' AND o.Status = ?';         params.push(status); }
    if (from)     { sql += ' AND o.OrderDate >= ?';     params.push(from); }
    if (to)       { sql += ' AND o.OrderDate <= ?';     params.push(to); }

    sql += ' ORDER BY o.OrderDate DESC, o.OrderID DESC';

    const [rows] = await db.query(sql, params);
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch orders.' });
  }
};

// GET /api/orders/:id
const getOrderById = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT o.*, CONCAT(c.FirstName,' ',c.LastName) AS CustomerName,
              c.Phone, c.Address, u.Email
       FROM ORDERS o
       JOIN CUSTOMER c ON c.CustomerID = o.CustomerID
       JOIN USERS u    ON u.UserID     = c.UserID
       WHERE o.OrderID = ?`,
      [req.params.id]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Order not found.' });

    const order = rows[0];
    if (req.user.role === 'customer' && req.user.customerID !== order.CustomerID) {
      return res.status(403).json({ success: false, message: 'Access denied.' });
    }

    // Fetch order details
    const [details] = await db.query(
      `SELECT od.*, st.ServiceName, st.PricePerKg
       FROM ORDER_DETAILS od
       JOIN SERVICE_TYPE st ON st.ServiceID = od.ServiceID
       WHERE od.OrderID = ?`,
      [req.params.id]
    );
    order.details = details;

    res.json({ success: true, data: order });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch order.' });
  }
};

// POST /api/orders  — creates order + details in a transaction
const createOrder = async (req, res) => {
  const { customerID, pickupDate, notes, details } = req.body;

  if (!customerID || !details || !Array.isArray(details) || details.length === 0) {
    return res.status(400).json({ success: false, message: 'customerID and at least one order detail are required.' });
  }

  // Customers can only place orders for themselves
  if (req.user.role === 'customer' && req.user.customerID !== parseInt(customerID)) {
    return res.status(403).json({ success: false, message: 'Access denied.' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    // Create order
    const [orderResult] = await conn.query(
      `INSERT INTO ORDERS (CustomerID, OrderDate, PickupDate, Status, Notes)
       VALUES (?, CURDATE(), ?, 'Pending', ?)`,
      [customerID, pickupDate || null, notes || null]
    );
    const orderID = orderResult.insertId;

    // Insert each detail — fetch price from DB, never trust client price
    for (const item of details) {
      const { serviceID, garmentType, quantity, weightKg } = item;

      if (!serviceID || !garmentType || !quantity || !weightKg) {
        throw new Error('Each detail must have serviceID, garmentType, quantity, and weightKg.');
      }
      if (quantity <= 0 || weightKg <= 0) {
        throw new Error('Quantity and weight must be positive.');
      }

      // Get authoritative price from DB
      const [svc] = await conn.query(
        'SELECT PricePerKg, IsActive FROM SERVICE_TYPE WHERE ServiceID = ?',
        [serviceID]
      );
      if (svc.length === 0 || !svc[0].IsActive) {
        throw new Error(`Service ID ${serviceID} not found or inactive.`);
      }

      const serviceCost = parseFloat(weightKg) * parseFloat(svc[0].PricePerKg);

      await conn.query(
        `INSERT INTO ORDER_DETAILS (OrderID, ServiceID, GarmentType, Quantity, WeightKg, ServiceCost)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [orderID, serviceID, garmentType, quantity, weightKg, serviceCost]
      );
    }

    // Totals are recalculated by DB trigger; fetch updated values
    const [updated] = await conn.query(
      'SELECT TotalWeight, TotalCost FROM ORDERS WHERE OrderID = ?',
      [orderID]
    );

    // Auto-create a delivery record
    await conn.query(
      `INSERT INTO DELIVERY (OrderID, DeliveryStatus) VALUES (?, 'Pending')`,
      [orderID]
    );

    await conn.commit();

    res.status(201).json({
      success:     true,
      message:     'Order placed successfully.',
      orderID,
      totalWeight: updated[0].TotalWeight,
      totalCost:   updated[0].TotalCost,
    });
  } catch (err) {
    await conn.rollback();
    console.error('Create order error:', err);
    res.status(400).json({ success: false, message: err.message || 'Failed to create order.' });
  } finally {
    conn.release();
  }
};

// PUT /api/orders/:id/status
const updateOrderStatus = async (req, res) => {
  const { status } = req.body;
  const validStatuses = [
    'Pending','Picked Up','Received','Processing','Washing',
    'Drying','Ironing','Packed','Ready for Delivery','Out for Delivery','Delivered','Cancelled'
  ];
  if (!validStatuses.includes(status)) {
    return res.status(400).json({ success: false, message: 'Invalid status.' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    const [rows] = await conn.query('SELECT Status, CustomerID FROM ORDERS WHERE OrderID = ?', [req.params.id]);
    if (rows.length === 0) {
      await conn.rollback();
      return res.status(404).json({ success: false, message: 'Order not found.' });
    }

    await conn.query('UPDATE ORDERS SET Status = ? WHERE OrderID = ?', [status, req.params.id]);

    // Auto-generate billing when status reaches 'Packed'
    if (status === 'Packed') {
      const [existing] = await conn.query('SELECT BillID FROM BILLING WHERE OrderID = ?', [req.params.id]);
      if (existing.length === 0) {
        const [order] = await conn.query('SELECT TotalCost FROM ORDERS WHERE OrderID = ?', [req.params.id]);
        const dueDate = new Date();
        dueDate.setDate(dueDate.getDate() + 7);
        await conn.query(
          `INSERT INTO BILLING (OrderID, BillDate, DueDate, TotalAmount, BillStatus)
           VALUES (?, CURDATE(), ?, ?, 'Unpaid')`,
          [req.params.id, dueDate.toISOString().split('T')[0], order[0].TotalCost]
        );
      }
    }

    // Update delivery status in sync
    const deliveryStatusMap = {
      'Picked Up':          'Picked Up',
      'Out for Delivery':   'In Transit',
      'Delivered':          'Delivered',
      'Cancelled':          'Failed',
    };
    if (deliveryStatusMap[status]) {
      await conn.query(
        'UPDATE DELIVERY SET DeliveryStatus = ? WHERE OrderID = ?',
        [deliveryStatusMap[status], req.params.id]
      );
      if (status === 'Delivered') {
        await conn.query(
          'UPDATE DELIVERY SET DeliveryTime = NOW() WHERE OrderID = ?',
          [req.params.id]
        );
      }
    }

    await conn.commit();
    res.json({ success: true, message: `Order status updated to "${status}".` });
  } catch (err) {
    await conn.rollback();
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to update order status.' });
  } finally {
    conn.release();
  }
};

// DELETE /api/orders/:id  (cancel)
const cancelOrder = async (req, res) => {
  try {
    const [rows] = await db.query('SELECT Status FROM ORDERS WHERE OrderID = ?', [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Order not found.' });

    const nonCancellable = ['Washing','Drying','Ironing','Packed','Ready for Delivery','Out for Delivery','Delivered'];
    if (nonCancellable.includes(rows[0].Status)) {
      return res.status(400).json({ success: false, message: 'Order cannot be cancelled at this stage.' });
    }
    await db.query("UPDATE ORDERS SET Status = 'Cancelled' WHERE OrderID = ?", [req.params.id]);
    res.json({ success: true, message: 'Order cancelled.' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to cancel order.' });
  }
};

module.exports = { getAllOrders, getOrderById, createOrder, updateOrderStatus, cancelOrder };
