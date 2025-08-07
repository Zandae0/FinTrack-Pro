const express = require('express');
const { getAllUsers, changeUserRole } = require('../controllers/userController');
const { verifyToken, isAdmin } = require('../middleware/authMiddleware');

const router = express.Router();

router.get('/', getAllUsers); // Only accessible by admin
router.patch('/change-role/:uid', changeUserRole); // Only accessible by admin

module.exports = router;
