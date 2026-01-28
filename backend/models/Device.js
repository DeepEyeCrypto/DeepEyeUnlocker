const mongoose = require('mongoose');

const DeviceSchema = new mongoose.Schema({
    brand: { type: String, required: true },
    model: { type: String, required: true },
    chipset: { type: String, required: true },
    supportedOps: [String],
    addedBy: { type: String, default: 'Community' },
    createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Device', DeviceSchema);
