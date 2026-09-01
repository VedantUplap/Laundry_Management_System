const router = require('express').Router();
const c = require('../controllers/service.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

// Public: anyone can browse services
router.get('/',     c.getAllServices);
router.get('/:id',  c.getServiceById);

// Protected: admin only for mutations
router.post('/',    verifyToken, authorize('admin','staff'), c.createService);
router.put('/:id',  verifyToken, authorize('admin','staff'), c.updateService);
router.delete('/:id', verifyToken, authorize('admin'),       c.deleteService);

module.exports = router;
