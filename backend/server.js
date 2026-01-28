const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

// Routes
app.use('/api/devices', require('./routes/devices'));
app.use('/api/logs', require('./routes/logs'));

// Resource Download Route (Mock)
app.get('/api/resource/:filename', (req, res) => {
    res.json({
        url: `https://storage.deepeyeunlocker.io/files/${req.params.filename}`,
        checksum: "abc123sha256"
    });
});

app.listen(PORT, () => {
    console.log(`DeepEye Server running on port ${PORT}`);
});
