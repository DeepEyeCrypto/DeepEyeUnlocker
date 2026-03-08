package com.deepeye.otg.policy

import android.util.Log
import com.deepeye.otg.domain.models.DeepEyeOperation

// ═══════════════════════════════════════════════════════════════════
//  PolicyEngine — 4-tier × 5-role enforcement matrix
//  Every operation passes through here before reaching any engine.
//  TIER 4 (EXPLOIT) is always rejected at entry point.
// ═══════════════════════════════════════════════════════════════════

enum class UserRole(val level: Int, val label: String) {
    CONSUMER     (1, "Consumer"),
    POWER_USER   (2, "Power User"),
    TECHNICIAN   (3, "Technician"),
    ENTERPRISE   (4, "Enterprise"),
    DEV          (5, "Developer");
}

/**
 * Result of a policy check.
 * [allowed] = true means the engine can proceed.
 * [reason] records why it was denied (for audit log).
 */
data class PolicyDecision(
    val allowed: Boolean,
    val reason: String
)

/**
 * Central policy gate for all 24 DeepEyeOperations.
 *
 * Rule matrix:
 *   TIER 1 (SAFE)        → all roles allowed, no auth
 *   TIER 2 (POLICY)      → Technician / Enterprise / Dev (proof of ownership required)
 *   TIER 3 (RESTRICTED)  → Enterprise / Dev (KYC required)
 *   TIER 4 (EXPLOIT)     → always rejected — no role has access
 *
 * Abuse detection:
 *   - Tracks per-op invocations in a rolling window
 *   - Flags excessive serial checks (IMEI_CHECK) → >20/day
 */
object PolicyEngine {

    private const val TAG = "DeepEye-Policy"

    // Rolling invocation counter: op → list of timestamps (epoch ms)
    private val invocationLog = java.util.concurrent.ConcurrentHashMap<DeepEyeOperation, MutableList<Long>>()
    private const val RATE_WINDOW_MS = 86_400_000L  // 24h
    private const val IMEI_CHECK_LIMIT = 20

    // ── Tier → minimum role mapping ─────────────────────────────

    private val tierMinRole: Map<com.deepeye.otg.domain.models.PolicyTier, UserRole> = mapOf(
        com.deepeye.otg.domain.models.PolicyTier.SAFE to UserRole.CONSUMER,      // anyone
        com.deepeye.otg.domain.models.PolicyTier.POLICY to UserRole.TECHNICIAN,    // needs proof of ownership
        com.deepeye.otg.domain.models.PolicyTier.RESTRICTED to UserRole.ENTERPRISE    // needs KYC / enterprise role
        // NEVER -> always blocked
    )

    // ── Public API ──────────────────────────────────────────────

    /**
     * Check whether [role] is allowed to execute [op].
     * Must be called before every engine dispatch.
     */
    fun check(op: DeepEyeOperation, role: UserRole): PolicyDecision {
        // TIER NEVER — always blocked
        if (op.policyTier == com.deepeye.otg.domain.models.PolicyTier.NEVER) {
            log("[POLICY] DENIED: ${op.id} is TIER NEVER (EXPLOIT) — no role has access")
            return PolicyDecision(false, "NEVER tier operations are permanently blocked")
        }

        // Tier → minimum role
        val minRole = tierMinRole[op.policyTier]
            ?: return PolicyDecision(false, "Unknown tier ${op.policyTier}")

        if (role.level < minRole.level) {
            log("[POLICY] DENIED: ${op.id} requires ${minRole.label} (have: ${role.label})")
            return PolicyDecision(
                false,
                "${op.label} requires ${minRole.label} role or higher (current: ${role.label})"
            )
        }

        // Abuse / rate-limit check
        val abuseCheck = checkAbuse(op)
        if (!abuseCheck.allowed) {
            return abuseCheck
        }

        // Record invocation for audit
        recordInvocation(op)
        log("[POLICY] ALLOWED: ${op.id} (tier=${op.policyTier}, role=${role.label})")
        return PolicyDecision(true, "OK")
    }

    /**
     * Convenience: check + throw if denied.
     * Use in engine dispatch paths where you want fail-fast.
     */
    fun enforce(op: DeepEyeOperation, role: UserRole) {
        val decision = check(op, role)
        if (!decision.allowed) {
            throw PolicyDeniedException(op, role, decision.reason)
        }
    }

    /**
     * Get the required minimum role for an operation tier.
     * Returns null for NEVER (always blocked).
     */
    fun requiredRole(tier: com.deepeye.otg.domain.models.PolicyTier): UserRole? = tierMinRole[tier]

    /**
     * Returns true if the operation is SAFE (no auth required).
     */
    fun isSafe(op: DeepEyeOperation): Boolean = op.policyTier == com.deepeye.otg.domain.models.PolicyTier.SAFE

    // ── Abuse detection ─────────────────────────────────────────

    private fun checkAbuse(op: DeepEyeOperation): PolicyDecision {
        if (op != DeepEyeOperation.IMEI_CHECK) return PolicyDecision(true, "OK")

        val now = System.currentTimeMillis()
        val timestamps = invocationLog[op] ?: return PolicyDecision(true, "OK")

        // Prune entries older than window
        timestamps.removeAll { now - it > RATE_WINDOW_MS }

        if (timestamps.size >= IMEI_CHECK_LIMIT) {
            log("[POLICY] ABUSE: ${op.id} invoked ${timestamps.size}x in last 24h (limit=$IMEI_CHECK_LIMIT)")
            return PolicyDecision(
                false,
                "Rate limit: ${op.label} exceeded ${IMEI_CHECK_LIMIT} checks in 24 hours"
            )
        }
        return PolicyDecision(true, "OK")
    }

    private fun recordInvocation(op: DeepEyeOperation) {
        val now = System.currentTimeMillis()
        val list = invocationLog.getOrPut(op) { java.util.Collections.synchronizedList(mutableListOf()) }
        synchronized(list) {
            list.removeAll { now - it > RATE_WINDOW_MS }
            list.add(now)
        }
    }

    // ── Logging ─────────────────────────────────────────────────

    private fun log(msg: String) = Log.i(TAG, msg)
}

class PolicyDeniedException(
    val op: DeepEyeOperation,
    val role: UserRole,
    message: String
) : SecurityException(message)
