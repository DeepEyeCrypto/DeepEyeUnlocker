const crypto = require('crypto');

// ═══════════════════════════════════════════════════════════════════
//  License Token Signer — HMAC-SHA256 based token generation
//
//  Token format: DEEPEYE-{ROLE}-{EXPIRY_MS}-{HMAC_HEX}
//
//  The HMAC covers: "DEEPEYE-{ROLE}-{EXPIRY_MS}-{DEVICE_ID}"
//  so tokens are device-bound and tamper-proof.
//
//  In production: use RSA/EC keys and proper JWT. This is a
//  lightweight implementation suitable for the current phase.
// ═══════════════════════════════════════════════════════════════════

// Secret key — in production, load from env/vault
const SECRET = process.env.LICENSE_SECRET || 'deepeye-license-secret-v1-change-in-prod';

/**
 * Generate a signed license token.
 *
 * @param {string} role      - UserRole name (CONSUMER, TECHNICIAN, etc.)
 * @param {number} expiryMs  - Expiry timestamp in epoch milliseconds
 * @param {string} deviceId  - Device identifier for binding
 * @returns {string} Signed token: DEEPEYE-{ROLE}-{EXPIRY}-{HMAC}
 */
function signToken(role, expiryMs, deviceId) {
    const payload = `DEEPEYE-${role}-${expiryMs}-${deviceId}`;
    const hmac = crypto.createHmac('sha256', SECRET)
        .update(payload)
        .digest('hex')
        .substring(0, 16); // Truncate for readability

    return `DEEPEYE-${role}-${expiryMs}-${hmac}`;
}

/**
 * Verify a license token's HMAC signature.
 *
 * @param {string} token     - The token to verify
 * @param {string} deviceId  - Expected device ID
 * @returns {{ valid: boolean, role: string|null, expiryMs: number|null, reason: string }}
 */
function verifyToken(token, deviceId) {
    const parts = token.split('-');
    // Expected: DEEPEYE, ROLE, EXPIRY_MS, HMAC
    if (parts.length < 4 || parts[0] !== 'DEEPEYE') {
        return { valid: false, role: null, expiryMs: null, reason: 'Invalid token format' };
    }

    const role = parts[1];
    const expiryMs = parseInt(parts[2], 10);
    const providedHmac = parts.slice(3).join('-'); // In case HMAC contains dashes

    // Valid roles
    const validRoles = ['CONSUMER', 'POWER_USER', 'TECHNICIAN', 'ENTERPRISE', 'DEV'];
    if (!validRoles.includes(role)) {
        return { valid: false, role: null, expiryMs: null, reason: `Invalid role: ${role}` };
    }

    // Check expiry
    if (isNaN(expiryMs) || expiryMs <= 0) {
        return { valid: false, role, expiryMs: null, reason: 'Invalid expiry' };
    }
    if (Date.now() > expiryMs) {
        return { valid: false, role, expiryMs, reason: 'Token expired' };
    }

    // Verify HMAC
    const payload = `DEEPEYE-${role}-${expiryMs}-${deviceId}`;
    const expectedHmac = crypto.createHmac('sha256', SECRET)
        .update(payload)
        .digest('hex')
        .substring(0, 16);

    if (providedHmac !== expectedHmac) {
        return { valid: false, role, expiryMs, reason: 'HMAC mismatch — wrong device or tampered token' };
    }

    return { valid: true, role, expiryMs, reason: 'OK' };
}

/**
 * Generate an expiry timestamp N days from now.
 * @param {number} days
 * @returns {number} Epoch milliseconds
 */
function expiryFromDays(days) {
    return Date.now() + (days * 24 * 60 * 60 * 1000);
}

module.exports = { signToken, verifyToken, expiryFromDays };
