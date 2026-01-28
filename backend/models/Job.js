const mongoose = require('mongoose');

const JobSchema = new mongoose.Schema({
    deviceId: String, // Anonymized hash of device hardware info
    brand: String,
    model: String,
    chipset: String,
    operation: String,
    status: { type: String, enum: ['Success', 'Failed', 'Cancelled'] },
    errorCode: String,
    durationMs: Number,
    appVersion: String,
    createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Job', JobSchema);
