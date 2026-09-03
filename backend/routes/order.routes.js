const router = require('express').Router();
const c = require('../controllers/order.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.use(verifyToken);

router.get('/',         authorize('admin','staff','customer','driver'), c.getAllOrders);
router.get('/:id',      authorize('admin','staff','customer','driver'), c.getOrderById);
router.post('/',        authorize('admin','staff','customer'),          c.createOrder);
router.put('/:id/status', authorize('admin','staff'),                  c.updateOrderStatus);
router.delete('/:id',   authorize('admin','staff','customer'),         c.cancelOrder);

module.exports = router;
