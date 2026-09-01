const router = require('express').Router();
const { getDashboardStats } = require('../controllers/dashboard.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.get('/stats', verifyToken, authorize('admin','staff'), getDashboardStats);

module.exports = router;
