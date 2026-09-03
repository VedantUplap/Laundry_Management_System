const router = require('express').Router();
const c = require('../controllers/delivery.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.use(verifyToken);

router.get('/',      authorize('admin','staff','driver'),   c.getAllDeliveries);
router.put('/:id',   authorize('admin','staff','driver'),   c.updateDelivery);

module.exports = router;
