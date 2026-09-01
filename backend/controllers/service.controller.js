const db = require('../config/db');

// GET /api/services
const getAllServices = async (req, res) => {
  try {
    const activeOnly = req.query.active === 'true';
    let sql = 'SELECT * FROM SERVICE_TYPE';
    if (activeOnly) sql += ' WHERE IsActive = 1';
    sql += ' ORDER BY ServiceName';
    const [rows] = await db.query(sql);
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch services.' });
  }
};

// GET /api/services/:id
const getServiceById = async (req, res) => {
  try {
    const [rows] = await db.query('SELECT * FROM SERVICE_TYPE WHERE ServiceID = ?', [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Service not found.' });
    res.json({ success: true, data: rows[0] });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to fetch service.' });
  }
};

// POST /api/services
const createService = async (req, res) => {
  const { serviceName, description, pricePerKg } = req.body;
  if (!serviceName || pricePerKg == null) {
    return res.status(400).json({ success: false, message: 'Service name and price are required.' });
  }
  if (pricePerKg < 0) {
    return res.status(400).json({ success: false, message: 'Price cannot be negative.' });
  }
  try {
    const [result] = await db.query(
      'INSERT INTO SERVICE_TYPE (ServiceName, Description, PricePerKg) VALUES (?, ?, ?)',
      [serviceName, description || null, pricePerKg]
    );
    res.status(201).json({ success: true, message: 'Service created.', serviceID: result.insertId });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ success: false, message: 'Service name already exists.' });
    }
    res.status(500).json({ success: false, message: 'Failed to create service.' });
  }
};

// PUT /api/services/:id
const updateService = async (req, res) => {
  const { serviceName, description, pricePerKg, isActive } = req.body;
  if (pricePerKg != null && pricePerKg < 0) {
    return res.status(400).json({ success: false, message: 'Price cannot be negative.' });
  }
  try {
    const [result] = await db.query(
      `UPDATE SERVICE_TYPE SET
        ServiceName  = COALESCE(?, ServiceName),
        Description  = COALESCE(?, Description),
        PricePerKg   = COALESCE(?, PricePerKg),
        IsActive     = COALESCE(?, IsActive)
       WHERE ServiceID = ?`,
      [serviceName, description, pricePerKg, isActive != null ? isActive : null, req.params.id]
    );
    if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Service not found.' });
    res.json({ success: true, message: 'Service updated.' });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to update service.' });
  }
};

// DELETE /api/services/:id  (soft delete)
const deleteService = async (req, res) => {
  try {
    const [result] = await db.query(
      'UPDATE SERVICE_TYPE SET IsActive = 0 WHERE ServiceID = ?',
      [req.params.id]
    );
    if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Service not found.' });
    res.json({ success: true, message: 'Service deactivated.' });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Failed to deactivate service.' });
  }
};

module.exports = { getAllServices, getServiceById, createService, updateService, deleteService };
