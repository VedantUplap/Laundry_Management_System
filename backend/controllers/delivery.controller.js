const db = require('../config/db');

// GET /api/delivery-agents
const getAllAgents = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT da.*, 
        (SELECT COUNT(*) FROM DELIVERY d WHERE d.AgentID = da.AgentID AND d.DeliveryStatus NOT IN ('Delivered','Failed')) AS ActiveDeliveries
       FROM DELIVERY_AGENT da
       WHERE da.IsActive = 1
       ORDER BY da.AgentName`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch agents.' });
  }
};

// POST /api/delivery-agents
const createAgent = async (req, res) => {
  const { agentName, phone, vehicleNumber } = req.body;
  if (!agentName || !phone) return res.status(400).json({ success: false, message: 'agentName and phone are required.' });
  try {
    const [result] = await db.query(
      'INSERT INTO DELIVERY_AGENT (AgentName, Phone, VehicleNumber) VALUES (?, ?, ?)',
      [agentName, phone, vehicleNumber || null]
    );
    res.status(201).json({ success: true, message: 'Agent added.', agentID: result.insertId });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to add agent.' });
  }
};

// PUT /api/delivery-agents/:id
const updateAgent = async (req, res) => {
  const { agentName, phone, vehicleNumber, isActive } = req.body;
  try {
    const [result] = await db.query(
      `UPDATE DELIVERY_AGENT SET
        AgentName     = COALESCE(?, AgentName),
        Phone         = COALESCE(?, Phone),
        VehicleNumber = COALESCE(?, VehicleNumber),
        IsActive      = COALESCE(?, IsActive)
       WHERE AgentID = ?`,
      [agentName, phone, vehicleNumber, isActive != null ? isActive : null, req.params.id]
    );
    if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Agent not found.' });
    res.json({ success: true, message: 'Agent updated.' });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to update agent.' });
  }
};

// GET /api/deliveries
const getAllDeliveries = async (req, res) => {
  try {
    const { status, agentID } = req.query;
    let sql = `
      SELECT d.*, 
             CONCAT(c.FirstName,' ',c.LastName) AS CustomerName, c.Phone AS CustomerPhone, c.Address,
             da.AgentName, da.Phone AS AgentPhone, da.VehicleNumber,
             o.Status AS OrderStatus, o.TotalCost
      FROM DELIVERY d
      JOIN ORDERS o               ON o.OrderID    = d.OrderID
      JOIN CUSTOMER c             ON c.CustomerID = o.CustomerID
      LEFT JOIN DELIVERY_AGENT da ON da.AgentID   = d.AgentID
      WHERE 1=1`;
    const params = [];
    if (status)  { sql += ' AND d.DeliveryStatus = ?'; params.push(status); }
    if (agentID) { sql += ' AND d.AgentID = ?';        params.push(agentID); }
    sql += ' ORDER BY d.DeliveryID DESC';
    const [rows] = await db.query(sql, params);
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch deliveries.' });
  }
};

// PUT /api/deliveries/:id — assign agent / update status
const updateDelivery = async (req, res) => {
  const { agentID, deliveryStatus, pickupTime, deliveryTime } = req.body;
  try {
    const updates = [];
    const params  = [];
    if (agentID !== undefined)       { updates.push('AgentID = ?');        params.push(agentID); }
    if (deliveryStatus !== undefined){ updates.push('DeliveryStatus = ?'); params.push(deliveryStatus); }
    if (pickupTime !== undefined)    { updates.push('PickupTime = ?');     params.push(pickupTime); }
    if (deliveryTime !== undefined)  { updates.push('DeliveryTime = ?');   params.push(deliveryTime); }

    if (updates.length === 0) return res.status(400).json({ success: false, message: 'No fields to update.' });
    params.push(req.params.id);

    const [result] = await db.query(
      `UPDATE DELIVERY SET ${updates.join(', ')} WHERE DeliveryID = ?`,
      params
    );
    if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Delivery not found.' });
    res.json({ success: true, message: 'Delivery updated.' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to update delivery.' });
  }
};

module.exports = { getAllAgents, createAgent, updateAgent, getAllDeliveries, updateDelivery };
