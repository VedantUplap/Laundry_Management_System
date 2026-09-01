const router = require('express').Router();
const r = require('../controllers/report.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.use(verifyToken);
router.use(authorize('admin','staff'));

router.get('/daily-revenue',         r.dailyRevenue);
router.get('/monthly-revenue',       r.monthlyRevenue);
router.get('/pending-deliveries',    r.pendingDeliveries);
router.get('/customer-history/:customerId', r.customerHistory);
router.get('/inventory-usage',       r.inventoryUsage);
router.get('/popular-services',      r.popularServices);
router.get('/outstanding-payments',  r.outstandingPayments);

module.exports = router;
