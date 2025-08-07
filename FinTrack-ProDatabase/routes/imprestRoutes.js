const express = require('express');
const router = express.Router();
const imprestController = require('../controllers/imprestController');

router.post('/', imprestController.createImprestFund);
router.get('/', imprestController.getImprestFunds);
router.put('/:id', imprestController.updateImprestFund);
router.delete('/:id', imprestController.deleteImprestFund);

module.exports = router;
