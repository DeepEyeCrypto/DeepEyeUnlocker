const express = require('express');
const router = express.Router();
const Job = require('../models/Job');

// Submit a new job log
router.post('/', async (req, res) => {
    const job = new Job(req.body);
    try {
        const newJob = await job.save();
        res.status(201).json({ status: 'Logged', id: newJob._id });
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

// Get global stats (e.g. success rate)
router.get('/stats', async (req, res) => {
    try {
        const total = await Job.countDocuments();
        const success = await Job.countDocuments({ status: 'Success' });
        res.json({
            totalJobs: total,
            successRate: total > 0 ? (success / total * 100).toFixed(2) + '%' : '0%'
        });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

module.exports = router;
