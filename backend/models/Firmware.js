const mongoose = require('mongoose');

const FirmwareSchema = new mongoose.Schema({
    model: { type: String, required: true },
    version: { type: String, required: true },
    region: { type: String, default: 'Global' },
    downloadUrl: { type: String, required: true },
    filesize: Number,
    checksum: String,
    downloads: { type: Number, default: 0 }
});

module.exports = mongoose.model('Firmware', FirmwareSchema);
