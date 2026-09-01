const router = require('express').Router();
const c = require('../controllers/delivery.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.use(verifyToken);

router.get('/',      authorize('admin','staff'),     c.getAllAgents);
router.post('/',     authorize('admin'),             c.createAgent);
router.put('/:id',   authorize('admin','staff'),     c.updateAgent);

module.exports = router;
