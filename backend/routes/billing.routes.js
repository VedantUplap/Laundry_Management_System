const router = require('express').Router();
const c = require('../controllers/billing.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.use(verifyToken);

router.get('/',                authorize('admin','staff'),              c.getAllBilling);
router.get('/order/:orderId',  authorize('admin','staff','customer'),  c.getBillingByOrder);
router.post('/',               authorize('admin','staff'),             c.createBilling);

module.exports = router;
