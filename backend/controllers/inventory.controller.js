const db = require('../config/db');

// GET /api/inventory
const getAllInventory = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT *, 
        CASE
          WHEN QuantityAvailable = 0             THEN 'Out of Stock'
          WHEN QuantityAvailable <= ReorderLevel THEN 'Low Stock'
          ELSE 'In Stock'
        END AS StockStatus
       FROM INVENTORY ORDER BY ItemName`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch inventory.' });
  }
};

// GET /api/inventory/:id
const getInventoryById = async (req, res) => {
  try {
    const [rows] = await db.query('SELECT * FROM INVENTORY WHERE ItemID = ?', [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Item not found.' });
    res.json({ success: true, data: rows[0] });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Server error.' });
  }
};

// POST /api/inventory
const createInventoryItem = async (req, res) => {
  const { itemName, unit, quantityAvailable, reorderLevel, costPerUnit } = req.body;
  if (!itemName || !unit) {
    return res.status(400).json({ success: false, message: 'itemName and unit are required.' });
  }
  if (quantityAvailable < 0 || reorderLevel < 0 || costPerUnit < 0) {
    return res.status(400).json({ success: false, message: 'Quantities and cost cannot be negative.' });
  }
  try {
    const [result] = await db.query(
      `INSERT INTO INVENTORY (ItemName, Unit, QuantityAvailable, ReorderLevel, CostPerUnit)
       VALUES (?, ?, ?, ?, ?)`,
      [itemName, unit, quantityAvailable || 0, reorderLevel || 0, costPerUnit || 0]
    );
    res.status(201).json({ success: true, message: 'Inventory item added.', itemID: result.insertId });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ success: false, message: 'Item name already exists.' });
    }
    res.status(500).json({ success: false, message: 'Failed to add item.' });
  }
};

// PUT /api/inventory/:id
const updateInventoryItem = async (req, res) => {
  const { itemName, unit, quantityAvailable, reorderLevel, costPerUnit } = req.body;
  try {
    const [result] = await db.query(
      `UPDATE INVENTORY SET
        ItemName          = COALESCE(?, ItemName),
        Unit              = COALESCE(?, Unit),
        QuantityAvailable = COALESCE(?, QuantityAvailable),
        ReorderLevel      = COALESCE(?, ReorderLevel),
        CostPerUnit       = COALESCE(?, CostPerUnit)
       WHERE ItemID = ?`,
      [itemName, unit, quantityAvailable, reorderLevel, costPerUnit, req.params.id]
    );
    if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Item not found.' });
    res.json({ success: true, message: 'Inventory updated.' });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to update inventory.' });
  }
};

// DELETE /api/inventory/:id
const deleteInventoryItem = async (req, res) => {
  try {
    const [result] = await db.query('DELETE FROM INVENTORY WHERE ItemID = ?', [req.params.id]);
    if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Item not found.' });
    res.json({ success: true, message: 'Item deleted.' });
  } catch (err) {
    if (err.code === 'ER_ROW_IS_REFERENCED_2') {
      return res.status(400).json({ success: false, message: 'Cannot delete item with usage history.' });
    }
    res.status(500).json({ success: false, message: 'Failed to delete item.' });
  }
};

// POST /api/inventory/:id/usage — log item usage for an order
const logUsage = async (req, res) => {
  const { orderID, quantityUsed } = req.body;
  if (!orderID || !quantityUsed || quantityUsed <= 0) {
    return res.status(400).json({ success: false, message: 'orderID and positive quantityUsed are required.' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    const [item] = await conn.query('SELECT QuantityAvailable FROM INVENTORY WHERE ItemID = ?', [req.params.id]);
    if (item.length === 0) { await conn.rollback(); return res.status(404).json({ success: false, message: 'Item not found.' }); }
    if (item[0].QuantityAvailable < quantityUsed) {
      await conn.rollback();
      return res.status(400).json({ success: false, message: 'Insufficient stock.' });
    }

    await conn.query(
      'INSERT INTO USAGE_LOG (OrderID, ItemID, QuantityUsed, UsageDate) VALUES (?, ?, ?, CURDATE())',
      [orderID, req.params.id, quantityUsed]
    );
    // Trigger handles inventory deduction; fetch updated qty
    const [updated] = await conn.query('SELECT QuantityAvailable FROM INVENTORY WHERE ItemID = ?', [req.params.id]);
    await conn.commit();

    res.status(201).json({
      success: true,
      message: 'Usage logged.',
      remainingQty: updated[0].QuantityAvailable,
    });
  } catch (err) {
    await conn.rollback();
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to log usage.' });
  } finally {
    conn.release();
  }
};

// GET /api/inventory/usage
const getUsageLogs = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT ul.*, i.ItemName, i.Unit, o.Status AS OrderStatus
       FROM USAGE_LOG ul
       JOIN INVENTORY i ON i.ItemID  = ul.ItemID
       JOIN ORDERS o    ON o.OrderID = ul.OrderID
       ORDER BY ul.UsageDate DESC, ul.UsageID DESC`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch usage logs.' });
  }
};

module.exports = { getAllInventory, getInventoryById, createInventoryItem, updateInventoryItem, deleteInventoryItem, logUsage, getUsageLogs };
