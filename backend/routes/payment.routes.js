const router = require('express').Router();
const c = require('../controllers/payment.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.use(verifyToken);

router.get('/',   authorize('admin','staff'),                           c.getAllPayments);
router.post('/',  authorize('admin','staff','customer'),                c.createPayment);

module.exports = router;
