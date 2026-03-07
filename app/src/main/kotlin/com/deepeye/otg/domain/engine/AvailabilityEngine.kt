package com.deepeye.otg.domain.engine

import com.deepeye.otg.domain.models.*

object AvailabilityEngine {

    fun availabilityFor(
        operation: DeepEyeOperation,
        sessionState: SessionState,
        userRole: PolicyTier, // simplified for now: user's granted max capability
        hasOwnershipProof: Boolean = false,
        isEnterpriseOperator: Boolean = false
    ): OperationAvailability {

        // 1. Connection Requirement
        if (operation.requiresConnection && !sessionState.connected) {
            return OperationAvailability(enabled = false, reason = "Device disconnected")
        }

        // 2. Model Selection Requirement
        if (operation.requiresModel && sessionState.selectedModel == null) {
            return OperationAvailability(enabled = false, reason = "Requires target model selection")
        }

        // 3. Exact Mode Match (if applicable)
        if (operation.requiredModes.isNotEmpty() && !operation.requiredModes.contains(sessionState.deviceMode)) {
            val modesStr = operation.requiredModes.joinToString("/") { it.name }
            return OperationAvailability(
                enabled = false, 
                reason = "Requires $modesStr mode",
                currentModeMismatch = true
            )
        }

        // 4. Policy Tiers Evaluation
        when (operation.tier) {
            PolicyTier.NEVER -> {
                return OperationAvailability(enabled = false, reason = "Operation administratively disabled", policyBlocked = true)
            }
            PolicyTier.RESTRICTED -> {
                if (!isEnterpriseOperator && userRole != PolicyTier.RESTRICTED) {
                    return OperationAvailability(enabled = false, reason = "Requires Enterprise / Restricted role", policyBlocked = true)
                }
            }
            PolicyTier.POLICY -> {
                if (!hasOwnershipProof && !isEnterpriseOperator && userRole != PolicyTier.POLICY && userRole != PolicyTier.RESTRICTED) {
                     return OperationAvailability(enabled = false, reason = "Requires ownership validation or higher policy tier", policyBlocked = true)
                }
            }
            PolicyTier.SAFE -> {
                // Anyone can execute with prerequisites
            }
        }

        // Eligible
        return OperationAvailability(enabled = true, reason = null)
    }
}
