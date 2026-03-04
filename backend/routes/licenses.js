const express = require('express');
const router = express.Router();
const License = require('../models/License');
const { signToken, verifyToken, expiryFromDays } = require('../config/tokenSigner');

// ═══════════════════════════════════════════════════════════════════
//  License API Routes
//
//  POST /api/licenses/issue   — Issue a new license (admin)
//  POST /api/licenses/activate — Activate token on device
//  POST /api/licenses/validate — Validate an existing token
//  POST /api/licenses/revoke   — Revoke a license (admin)
//  GET  /api/licenses/device/:deviceId — Get licenses for device
//  GET  /api/licenses/stats    — License statistics (admin)
// ═══════════════════════════════════════════════════════════════════

// ── POST /issue — Create and sign a new license ─────────────────

router.post('/issue', async (req, res) => {
    try {
        const { role, deviceId, email, customerName, purchaseId, durationDays, kycVerified } = req.body;

        if (!role || !deviceId) {
            return res.status(400).json({ error: 'role and deviceId are required' });
        }

        const validRoles = ['CONSUMER', 'POWER_USER', 'TECHNICIAN', 'ENTERPRISE', 'DEV'];
        if (!validRoles.includes(role)) {
            return res.status(400).json({ error: `Invalid role: ${role}. Must be one of: ${validRoles.join(', ')}` });
        }

        // KYC requirement for tier 3 roles
        if (['ENTERPRISE', 'DEV'].includes(role) && !kycVerified) {
            return res.status(403).json({
                error: `${role} license requires KYC verification`,
                kycRequired: true
            });
        }

        const days = durationDays || 365; // Default: 1 year
        const expiryMs = expiryFromDays(days);
        const token = signToken(role, expiryMs, deviceId);

        const license = new License({
            role,
            deviceId,
            token,
            email: email || '',
            customerName: customerName || '',
            purchaseId: purchaseId || '',
            kycVerified: kycVerified || false,
            expiresAt: new Date(expiryMs)
        });

        const saved = await license.save();

        res.status(201).json({
            success: true,
            license: {
                id: saved._id,
                token: saved.token,
                role: saved.role,
                deviceId: saved.deviceId,
                expiresAt: saved.expiresAt,
                isValid: saved.isValid
            }
        });
    } catch (err) {
        if (err.code === 11000) {
            return res.status(409).json({ error: 'Duplicate token — license already issued for this config' });
        }
        res.status(500).json({ error: err.message });
    }
});

// ── POST /activate — Activate a token on a device ───────────────

router.post('/activate', async (req, res) => {
    try {
        const { token, deviceId } = req.body;

        if (!token || !deviceId) {
            return res.status(400).json({ error: 'token and deviceId are required' });
        }

        // Step 1: Verify token signature
        const verification = verifyToken(token, deviceId);
        if (!verification.valid) {
            return res.status(403).json({
                error: 'Token verification failed',
                reason: verification.reason
            });
        }

        // Step 2: Find license in DB
        const license = await License.findOne({ token });
        if (!license) {
            return res.status(404).json({ error: 'License not found in database' });
        }

        // Step 3: Check revocation
        if (license.revoked) {
            return res.status(403).json({
                error: 'License has been revoked',
                revokedAt: license.revokedAt,
                reason: license.revokeReason
            });
        }

        // Step 4: Check device binding
        if (license.deviceId !== deviceId) {
            return res.status(403).json({
                error: 'Token is bound to a different device',
                expectedDevice: license.deviceId.substring(0, 6) + '...'
            });
        }

        // Step 5: Check activation count
        if (license.activationCount >= license.maxActivations) {
            return res.status(403).json({
                error: 'Maximum activations reached',
                maxActivations: license.maxActivations,
                activationCount: license.activationCount
            });
        }

        // Step 6: Activate
        license.activatedAt = new Date();
        license.activationCount += 1;
        await license.save();

        res.json({
            success: true,
            role: license.role,
            expiresAt: license.expiresAt,
            activationCount: license.activationCount,
            maxActivations: license.maxActivations
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ── POST /validate — Check if a token is still valid ────────────

router.post('/validate', async (req, res) => {
    try {
        const { token, deviceId } = req.body;

        if (!token || !deviceId) {
            return res.status(400).json({ error: 'token and deviceId are required' });
        }

        // Cryptographic verification
        const verification = verifyToken(token, deviceId);
        if (!verification.valid) {
            return res.json({
                valid: false,
                reason: verification.reason
            });
        }

        // DB check
        const license = await License.findOne({ token });
        if (!license) {
            return res.json({ valid: false, reason: 'License not in database' });
        }

        if (license.revoked) {
            return res.json({ valid: false, reason: 'License revoked' });
        }

        if (license.deviceId !== deviceId) {
            return res.json({ valid: false, reason: 'Device mismatch' });
        }

        // Update check timestamp
        license.lastCheckedAt = new Date();
        license.checkCount += 1;
        await license.save();

        res.json({
            valid: true,
            role: license.role,
            expiresAt: license.expiresAt,
            checkCount: license.checkCount
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ── POST /revoke — Revoke a license (admin) ─────────────────────

router.post('/revoke', async (req, res) => {
    try {
        const { token, reason } = req.body;

        if (!token) {
            return res.status(400).json({ error: 'token is required' });
        }

        const license = await License.findOne({ token });
        if (!license) {
            return res.status(404).json({ error: 'License not found' });
        }

        if (license.revoked) {
            return res.status(409).json({ error: 'License already revoked' });
        }

        license.revoked = true;
        license.revokedAt = new Date();
        license.revokeReason = reason || 'Admin revocation';
        await license.save();

        res.json({
            success: true,
            message: 'License revoked',
            revokedAt: license.revokedAt
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ── GET /device/:deviceId — Get all licenses for a device ───────

router.get('/device/:deviceId', async (req, res) => {
    try {
        const licenses = await License.find({
            deviceId: req.params.deviceId
        }).sort({ issuedAt: -1 });

        res.json({
            deviceId: req.params.deviceId,
            count: licenses.length,
            licenses: licenses.map(l => ({
                id: l._id,
                role: l.role,
                token: l.token.substring(0, 24) + '...', // Truncate for security
                isValid: l.isValid,
                expiresAt: l.expiresAt,
                revoked: l.revoked,
                activationCount: l.activationCount
            }))
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ── GET /stats — License statistics (admin) ─────────────────────

router.get('/stats', async (req, res) => {
    try {
        const total = await License.countDocuments();
        const active = await License.countDocuments({
            revoked: false,
            expiresAt: { $gt: new Date() }
        });
        const revoked = await License.countDocuments({ revoked: true });
        const expired = await License.countDocuments({
            revoked: false,
            expiresAt: { $lte: new Date() }
        });

        // Role breakdown
        const byRole = await License.aggregate([
            { $group: { _id: '$role', count: { $sum: 1 } } }
        ]);

        res.json({
            total,
            active,
            revoked,
            expired,
            byRole: byRole.reduce((acc, r) => { acc[r._id] = r.count; return acc; }, {})
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;
