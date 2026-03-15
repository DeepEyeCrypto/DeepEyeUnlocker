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
            return OperationAvailability(enabled = false, reason = "Device disconnected", missingConnection = true)
        }

        // 2. Model Selection Requirement
        if (operation.requiresModel && sessionState.selectedModel == null) {
            return OperationAvailability(enabled = false, reason = "Requires target model selection", missingModel = true)
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

        // 4. Policy Tiers Evaluation (Owned device mode — all tiers open except NEVER)
        when (operation.policyTier) {
            PolicyTier.NEVER -> {
                return OperationAvailability(enabled = false, reason = "Administratively disabled", policyBlocked = true)
            }
            else -> {
                // All other tiers (RESTRICTED, POLICY, SAFE) are enabled in owned mode
            }
        }

        // Eligible
        return OperationAvailability(enabled = true, reason = null)
    }
}
