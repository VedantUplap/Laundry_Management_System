const router = require('express').Router();
const { register, login, me } = require('../controllers/auth.controller');
const { verifyToken } = require('../middleware/auth.middleware');

router.post('/register', register);
router.post('/login',    login);
router.get('/me',        verifyToken, me);

module.exports = router;
