const router = require('express').Router();
const c = require('../controllers/customer.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.use(verifyToken);

router.get('/',        authorize('admin','staff'),                      c.getAllCustomers);
router.get('/:id',     authorize('admin','staff','customer'),           c.getCustomerById);
router.put('/:id',     authorize('admin','staff','customer'),           c.updateCustomer);
router.delete('/:id',  authorize('admin'),                              c.deleteCustomer);
router.get('/:id/orders', authorize('admin','staff','customer'),        c.getCustomerOrders);

module.exports = router;
