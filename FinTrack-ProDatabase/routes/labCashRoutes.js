const express = require('express');
const router = express.Router();
const labCashController = require('../controllers/labCashController');

router.post('/', labCashController.createLabCash);
router.get('/', labCashController.getLabCash);
router.put('/:id', labCashController.updateLabCash);
router.delete('/:id', labCashController.deleteLabCash);

module.exports = router;
