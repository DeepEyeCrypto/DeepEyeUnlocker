const express = require('express');
const router = express.Router();
const Device = require('../models/Device');

// Get all supported devices
router.get('/', async (req, res) => {
    try {
        const devices = await Device.find();
        res.json(devices);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Add a new device to the matrix
router.post('/', async (req, res) => {
    const device = new Device(req.body);
    try {
        const newDevice = await device.save();
        res.status(201).json(newDevice);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

module.exports = router;
