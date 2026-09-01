const router = require('express').Router();
const c = require('../controllers/feedback.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const { authorize }   = require('../middleware/role.middleware');

router.use(verifyToken);

router.get('/',     authorize('admin','staff'),      c.getAllFeedback);
router.get('/my',   authorize('customer'),           c.getMyFeedback);
router.post('/',    authorize('customer'),           c.submitFeedback);

module.exports = router;
