const router = require('express').Router();
const c = require('../controllers/inventory.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.use(verifyToken);
router.use(authorize('admin','staff'));

router.get('/',              c.getAllInventory);
router.get('/usage',         c.getUsageLogs);
router.get('/:id',           c.getInventoryById);
router.post('/',             c.createInventoryItem);
router.put('/:id',           c.updateInventoryItem);
router.delete('/:id',        c.deleteInventoryItem);
router.post('/:id/usage',    c.logUsage);

module.exports = router;
