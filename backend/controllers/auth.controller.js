const bcrypt = require('bcryptjs');
const jwt    = require('jsonwebtoken');
const db     = require('../config/db');

// POST /api/auth/register
const register = async (req, res) => {
  const { firstName, lastName, phone, address, email, password, username } = req.body;

  if (!firstName || !lastName || !phone || !address || !email || !password) {
    return res.status(400).json({ success: false, message: 'All fields are required.' });
  }
  if (password.length < 6) {
    return res.status(400).json({ success: false, message: 'Password must be at least 6 characters.' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    // Check duplicate email
    const [existing] = await conn.query('SELECT UserID FROM USERS WHERE Email = ?', [email]);
    if (existing.length > 0) {
      await conn.rollback();
      return res.status(409).json({ success: false, message: 'Email already registered.' });
    }

    const uname  = username || email.split('@')[0];
    const hashed = await bcrypt.hash(password, 10);

    const [userResult] = await conn.query(
      'INSERT INTO USERS (Username, Email, Password, Role) VALUES (?, ?, ?, ?)',
      [uname, email, hashed, 'customer']
    );
    const newUserID = userResult.insertId;

    await conn.query(
      'INSERT INTO CUSTOMER (UserID, FirstName, LastName, Phone, Address) VALUES (?, ?, ?, ?, ?)',
      [newUserID, firstName, lastName, phone, address]
    );

    await conn.commit();

    // Generate token
    const [rows] = await conn.query(
      `SELECT u.UserID, u.Email, u.Role, c.CustomerID, c.FirstName, c.LastName
       FROM USERS u JOIN CUSTOMER c ON c.UserID = u.UserID WHERE u.UserID = ?`,
      [newUserID]
    );
    const user = rows[0];
    const token = jwt.sign(
      { userID: user.UserID, role: user.Role, customerID: user.CustomerID },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || '24h' }
    );

    res.status(201).json({
      success: true,
      message: 'Registration successful.',
      token,
      user: { userID: user.UserID, email: user.Email, role: user.Role, customerID: user.CustomerID, firstName: user.FirstName, lastName: user.LastName },
    });
  } catch (err) {
    await conn.rollback();
    console.error('Register error:', err);
    res.status(500).json({ success: false, message: 'Registration failed. Please try again.' });
  } finally {
    conn.release();
  }
};

// POST /api/auth/login
const login = async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ success: false, message: 'Email and password are required.' });
  }

  try {
    const [rows] = await db.query(
      `SELECT u.UserID, u.Email, u.Password, u.Role, u.IsActive,
              c.CustomerID, c.FirstName, c.LastName,
              da.AgentID
       FROM USERS u
       LEFT JOIN CUSTOMER c      ON c.UserID  = u.UserID
       LEFT JOIN DELIVERY_AGENT da ON da.UserID = u.UserID
       WHERE u.Email = ?`,
      [email]
    );

    if (rows.length === 0) {
      return res.status(401).json({ success: false, message: 'Invalid email or password.' });
    }

    const user = rows[0];
    if (!user.IsActive) {
      return res.status(403).json({ success: false, message: 'Account is inactive. Contact admin.' });
    }

    const match = await bcrypt.compare(password, user.Password);
    if (!match) {
      return res.status(401).json({ success: false, message: 'Invalid email or password.' });
    }

    const payload = {
      userID:     user.UserID,
      role:       user.Role,
      customerID: user.CustomerID || null,
      agentID:    user.AgentID    || null,
    };

    const token = jwt.sign(payload, process.env.JWT_SECRET, {
      expiresIn: process.env.JWT_EXPIRES_IN || '24h',
    });

    res.json({
      success: true,
      message: 'Login successful.',
      token,
      user: {
        userID:     user.UserID,
        email:      user.Email,
        role:       user.Role,
        customerID: user.CustomerID || null,
        agentID:    user.AgentID    || null,
        firstName:  user.FirstName  || null,
        lastName:   user.LastName   || null,
      },
    });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ success: false, message: 'Login failed. Please try again.' });
  }
};

// GET /api/auth/me
const me = async (req, res) => {
  try {
    const [rows] = await db.query(
      `SELECT u.UserID, u.Username, u.Email, u.Role,
              c.CustomerID, c.FirstName, c.LastName, c.Phone, c.Address,
              da.AgentID, da.AgentName, da.VehicleNumber
       FROM USERS u
       LEFT JOIN CUSTOMER c       ON c.UserID  = u.UserID
       LEFT JOIN DELIVERY_AGENT da ON da.UserID = u.UserID
       WHERE u.UserID = ?`,
      [req.user.userID]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'User not found.' });
    const u = rows[0];
    delete u.Password;
    res.json({ success: true, user: u });
  } catch (err) {
    console.error('Me error:', err);
    res.status(500).json({ success: false, message: 'Server error.' });
  }
};

module.exports = { register, login, me };
