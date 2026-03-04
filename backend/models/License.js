const mongoose = require('mongoose');

// ═══════════════════════════════════════════════════════════════════
//  License Model — tracks issued licenses with device binding
//
//  Each license grants a UserRole to a specific device (by deviceId).
//  The token is a signed string: DEEPEYE-{ROLE}-{EXPIRY}-{HMAC}
//  deviceId = Android Settings.Secure.ANDROID_ID or hardware serial.
// ═══════════════════════════════════════════════════════════════════

const LicenseSchema = new mongoose.Schema({
    // Granted role: CONSUMER, POWER_USER, TECHNICIAN, ENTERPRISE, DEV
    role: {
        type: String,
        required: true,
        enum: ['CONSUMER', 'POWER_USER', 'TECHNICIAN', 'ENTERPRISE', 'DEV'],
        default: 'CONSUMER'
    },

    // Device binding
    deviceId: {
        type: String,
        required: true,
        index: true
    },

    // The signed license token (sent to the app)
    token: {
        type: String,
        required: true,
        unique: true
    },

    // Customer info
    email: {
        type: String,
        default: ''
    },
    customerName: {
        type: String,
        default: ''
    },

    // Purchase / KYC info
    purchaseId: {
        type: String,
        default: ''
    },
    kycVerified: {
        type: Boolean,
        default: false
    },

    // Validity
    issuedAt: {
        type: Date,
        default: Date.now
    },
    expiresAt: {
        type: Date,
        required: true
    },
    revoked: {
        type: Boolean,
        default: false
    },
    revokedAt: {
        type: Date,
        default: null
    },
    revokeReason: {
        type: String,
        default: ''
    },

    // Activation tracking
    activatedAt: {
        type: Date,
        default: null
    },
    activationCount: {
        type: Number,
        default: 0
    },
    maxActivations: {
        type: Number,
        default: 3  // Allow re-activation up to 3 times (device wipe recovery)
    },

    // Audit
    lastCheckedAt: {
        type: Date,
        default: null
    },
    checkCount: {
        type: Number,
        default: 0
    }
});

// Indexes
LicenseSchema.index({ token: 1 });
LicenseSchema.index({ deviceId: 1, role: 1 });
LicenseSchema.index({ email: 1 });

// Virtual: is this license currently valid?
LicenseSchema.virtual('isValid').get(function () {
    if (this.revoked) return false;
    if (new Date() > this.expiresAt) return false;
    return true;
});

// Ensure virtuals are included in JSON
LicenseSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('License', LicenseSchema);
