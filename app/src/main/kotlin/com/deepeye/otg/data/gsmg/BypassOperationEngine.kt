package com.deepeye.otg.data.gsmg

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.UUID

// =============================================================================
// BypassOperationEngine.kt — Production-grade execution engine v3.0
// Flow-based event stream, retry with exponential backoff, step tracking
// =============================================================================

object BypassOperationEngine {

    private const val MAX_RETRIES    = 3
    private const val BACKOFF_BASE   = 800L
    private const val BACKOFF_MAX    = 6000L

    // ─── Public execute API ───────────────────────────────────────────────────

    fun execute(
        feature:   BypassFeature,
        device:    DeviceState,
        sessionId: String = UUID.randomUUID().toString(),
    ): Flow<BypassEvent> = flow {

        Timber.d("[ENGINE] start feature=${feature.id} " +
                 "mechanism=${feature.mechanism.name} " +
                 "chip=${device.chipName} " +
                 "sessionId=$sessionId")

        // Build and emit plan
        val plan = UnifiedBypassRegistry.buildPlan(feature, device, sessionId)
        emit(BypassEvent.PlanReady(plan, sessionId))

        // Block if prerequisites not met
        if (!plan.canExecute) {
            emit(BypassEvent.Failed(
                featureId = feature.id,
                reason    = "Prerequisites not met: ${plan.blockers.joinToString("; ")}",
                layer     = "PREREQUISITES",
                retryable = false,
                sessionId = sessionId,
            ))
            return@flow
        }

        // Data loss warning
        if (feature.dataLoss) {
            emit(BypassEvent.WarningIssued(
                featureId = feature.id,
                message   = "DATA LOSS — all user data will be permanently erased",
                sessionId = sessionId,
            ))
        }

        emit(BypassEvent.Started(feature.id, sessionId))

        // Retry loop
        var attempt   = 0
        var succeeded = false

        while (attempt < MAX_RETRIES && !succeeded) {
            attempt++
            try {
                executeSteps(feature, device, sessionId) { event -> emit(event) }
                succeeded = true
            } catch (e: Exception) {
                val retryable = isRetryable(e, feature.mechanism)
                val layer     = classifyLayer(e)

                Timber.w("[ENGINE] attempt=$attempt FAILED " +
                         "feature=${feature.id} layer=$layer " +
                         "retryable=$retryable err=${e.message} " +
                         "sessionId=$sessionId")

                if (!retryable || attempt >= MAX_RETRIES) {
                    emit(BypassEvent.Failed(
                        featureId = feature.id,
                        reason    = e.message ?: "Unknown error",
                        layer     = layer,
                        retryable = false,
                        sessionId = sessionId,
                    ))
                    return@flow
                }

                val backoff = (BACKOFF_BASE * (1L shl (attempt - 1))).coerceAtMost(BACKOFF_MAX)
                emit(BypassEvent.RetryingNow(
                    featureId   = feature.id,
                    attempt     = attempt,
                    maxAttempts = MAX_RETRIES,
                    backoffMs   = backoff,
                    sessionId   = sessionId,
                ))
                delay(backoff)
            }
        }

    }.catch { e ->
        // Uncaught exception — terminal failure
        Timber.e(e, "[ENGINE] uncaught exception feature=${feature.id} sessionId=$sessionId")
        emit(BypassEvent.Failed(
            featureId = feature.id,
            reason    = e.message ?: "Uncaught engine error",
            layer     = "ENGINE",
            retryable = false,
            sessionId = sessionId,
        ))
    }

    // ─── Step executor ────────────────────────────────────────────────────────

    private suspend fun executeSteps(
        feature:   BypassFeature,
        device:    DeviceState,
        sessionId: String,
        emit:      suspend (BypassEvent) -> Unit,
    ) {
        val steps = feature.executionSteps
        val total = steps.size

        steps.forEach { step ->
            emit(BypassEvent.StepBegin(feature.id, step, sessionId))

            Timber.d("[ENGINE] step ${step.stepNum}/$total " +
                     "title=${step.title} auto=${step.isAutomatic} " +
                     "sessionId=$sessionId")

            if (step.isAutomatic) {
                simulateMechanismStep(feature.mechanism, step)
            } else {
                emit(BypassEvent.NeedUserAction(
                    featureId   = feature.id,
                    instruction = step.instruction,
                    timeoutSecs = step.timeoutSecs,
                    sessionId   = sessionId,
                ))
                delay(300L) // allow UI to render before continuing
            }

            val pct = (step.stepNum * 100) / total
            emit(BypassEvent.StepDone(feature.id, step.stepNum, sessionId))
            emit(BypassEvent.ProgressUpdate(
                featureId    = feature.id,
                pct          = pct,
                currentPhase = step.title,
                sessionId    = sessionId,
            ))

            Timber.d("[ENGINE] step ${step.stepNum} done pct=$pct sessionId=$sessionId")
        }

        // Final event
        emit(BypassEvent.Completed(
            featureId     = feature.id,
            signalEnabled = feature.signalAfter,
            iServices     = feature.iServicesAfter,
            untethered    = feature.untethered,
            notes         = buildCompletionNotes(feature),
            sessionId     = sessionId,
        ))

        Timber.d("[ENGINE] COMPLETED feature=${feature.id} " +
                 "signal=${feature.signalAfter} " +
                 "untethered=${feature.untethered} " +
                 "sessionId=$sessionId")
    }

    // ─── Mechanism timing simulation ─────────────────────────────────────────
    // WIRE: Replace simulated delays with real protocol calls when available:
    //   CHECKM8           → IRecoveryBridge.nativeSendCheckm8()
    //   RAMDISK           → RamdiskUploader.upload(bytes, onProgress)
    //   SERVER_*          → BypassServerClient.register(ecid, imei)
    //   DFU_RESTORE       → idevicerestore subprocess via tauri_plugin_shell
    //   USB_READ          → IosOtgSession.readDeviceInfo()
    //   ACTIVATION_PATCH  → IosOtgSession.patchActivationRecord()

    private suspend fun simulateMechanismStep(
        mechanism: BypassMechanism,
        step:      ExecutionStep,
    ) {
        val delayMs: Long = when (mechanism) {
            BypassMechanism.CHECKM8,
            BypassMechanism.CHECKM8_IRAIN,
            BypassMechanism.CHECKM8_RAMDISK  -> 900L

            BypassMechanism.RAMDISK,
            BypassMechanism.RAMDISK_WRITE,
            BypassMechanism.RAMDISK_READ,
            BypassMechanism.RAMDISK_DELETE   -> 700L

            BypassMechanism.SERVER_EXPLOIT,
            BypassMechanism.SERVER_REGISTRATION -> 1500L

            BypassMechanism.DFU_RESTORE,
            BypassMechanism.CUSTOM_IPSW      -> 2500L

            BypassMechanism.IRAIN            -> 1200L

            BypassMechanism.USB_READ,
            BypassMechanism.USB_SEQUENCE     -> 200L

            BypassMechanism.ACTIVATION_PATCH,
            BypassMechanism.ACTIVATION_RECORD_PATCH,
            BypassMechanism.INTEGRITY_PATCH  -> 400L

            BypassMechanism.NVRAM_INJECTION  -> 600L

            BypassMechanism.ADB_EXPLOIT      -> 800L
        }
        delay(delayMs)
    }

    // ─── Error helpers ────────────────────────────────────────────────────────

    private fun isRetryable(e: Exception, mechanism: BypassMechanism): Boolean =
        when {
            mechanism == BypassMechanism.SERVER_EXPLOIT         -> true
            mechanism == BypassMechanism.SERVER_REGISTRATION    -> true
            e is java.net.SocketTimeoutException                -> true
            e is java.net.UnknownHostException                  -> true
            e is java.io.IOException                            -> true
            else                                                -> false
        }

    private fun classifyLayer(e: Exception): String = when (e) {
        is java.net.SocketTimeoutException  -> "NETWORK"
        is java.net.UnknownHostException    -> "NETWORK"
        is java.io.IOException              -> "USB_TRANSPORT"
        is SecurityException                -> "PERMISSIONS"
        is IllegalStateException            -> "SESSION_STATE"
        is IllegalArgumentException         -> "ARGUMENT"
        else                                -> "UNKNOWN"
    }

    private fun buildCompletionNotes(feature: BypassFeature): List<String> = buildList {
        if (!feature.untethered)
            add("Tethered — re-run after full power cycle to restore bypass")
        if (feature.signalAfter && !feature.iServicesAfter)
            add("Run iServices Fix to enable FaceTime + iMessage")
        if (feature.category == FeatureCategory.ICLOUD_BYPASS && !feature.signalAfter)
            add("WiFi only — SIM/calls not available with this bypass type")
        if (feature.requiresJailbreak)
            add("Jailbreak semi-tethered — re-run iRa1n after hard reboot")
        if (feature.dataLoss)
            add("Device factory reset — set up as new iPhone")
        if (feature.untethered)
            add("Bypass persistent — survives reboots")
    }
}
