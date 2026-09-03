const express   = require('express');
const cors      = require('cors');
const path      = require('path');
require('dotenv').config();

const app = express();

// ── Middleware ─────────────────────────────────────────────────────────────
app.use(cors({ origin: '*', credentials: true }));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// ── Serve static frontend and documentation ────────────────────────────────
app.use(express.static(path.join(__dirname, '../frontend')));
app.use('/docs', express.static(path.join(__dirname, '../docs')));

// ── API Routes ─────────────────────────────────────────────────────────────
app.use('/api/auth',            require('./routes/auth.routes'));
app.use('/api/customers',       require('./routes/customer.routes'));
app.use('/api/services',        require('./routes/service.routes'));
app.use('/api/orders',          require('./routes/order.routes'));
app.use('/api/billing',         require('./routes/billing.routes'));
app.use('/api/payments',        require('./routes/payment.routes'));
app.use('/api/inventory',       require('./routes/inventory.routes'));
app.use('/api/delivery-agents', require('./routes/deliveryAgent.routes'));
app.use('/api/deliveries',      require('./routes/delivery.routes'));
app.use('/api/feedback',        require('./routes/feedback.routes'));
app.use('/api/dashboard',       require('./routes/dashboard.routes'));
app.use('/api/reports',         require('./routes/report.routes'));

// ── SPA Catch-all (serve index.html for non-API routes) ───────────────────
app.get(/^\/(?!api).*/, (req, res) => {
  res.sendFile(path.join(__dirname, '../frontend/index.html'));
});

// ── Global Error Handler ───────────────────────────────────────────────────
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  const status  = err.status || 500;
  const message = process.env.NODE_ENV === 'production'
    ? 'An unexpected error occurred'
    : err.message;
  res.status(status).json({ success: false, message });
});

module.exports = app;
