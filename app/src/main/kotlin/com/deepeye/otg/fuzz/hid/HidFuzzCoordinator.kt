package com.deepeye.otg.fuzz.hid

import com.deepeye.otg.domain.models.ConnectionState
import com.deepeye.otg.usb.SessionCoordinator
import com.deepeye.otg.usb.UsbLifecycleManager
import com.deepeye.otg.data.db.dao.FuzzDao
import com.deepeye.otg.data.db.entities.FuzzFindingEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HidFuzzCoordinator @Inject constructor(
    private val lifecycleManager: UsbLifecycleManager,
    private val sessionCoordinator: SessionCoordinator,
    private val fuzzDao: FuzzDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fuzzJob: Job? = null
    private val mutator = HidMutator()

    private val _isFuzzing = MutableStateFlow(false)
    val isFuzzing = _isFuzzing.asStateFlow()

    private val _fuzzStats = MutableStateFlow(FuzzStats())
    val fuzzStats = _fuzzStats.asStateFlow()

    data class FuzzStats(
        val totalCases: Int = 0,
        val crashesFound: Int = 0,
        val lastCaseName: String = "",
        val currentSeedIndex: Int = 0
    )

    fun startFuzzing(deviceKey: String) {
        if (_isFuzzing.value) return
        _isFuzzing.value = true
        
        fuzzJob = scope.launch {
            Timber.i("[FUZZ] Starting HID Fuzzing on $deviceKey")
            val seeds = HidCorpus.getSeeds()
            var caseIndex = 0

            while (isActive && _isFuzzing.value) {
                val seed = seeds[caseIndex % seeds.size]
                val currentStrategy = HidMutator.Strategy.entries.toTypedArray().random()
                val payload = mutator.mutate(seed.payload, currentStrategy)
                
                _fuzzStats.update { it.copy(
                    totalCases = it.totalCases + 1,
                    lastCaseName = "${seed.name} + ${currentStrategy.name}",
                    currentSeedIndex = caseIndex % seeds.size
                )}

                val result = executeTransfer(deviceKey, payload)
                
                if (result is FuzzResult.Crash) {
                    Timber.e("[FUZZ] CRASH DETECTED! Type=${result.type} Case=${seed.name} Strategy=${currentStrategy.name}")
                    _fuzzStats.update { it.copy(crashesFound = it.crashesFound + 1) }
                    
                    // Persist to database
                    scope.launch {
                        fuzzDao.insertFinding(
                            FuzzFindingEntity(
                                sessionId = sessionCoordinator.getSessionId(),
                                timestamp = System.currentTimeMillis(),
                                type = "HID",
                                sourceSeed = seed.name,
                                mutationType = currentStrategy.name,
                                payloadHex = payload.joinToString("") { "%02X".format(it) },
                                crashType = result.type.name,
                                crashSignature = result.signature,
                                targetDeviceKey = deviceKey
                            )
                        )
                    }

                    // Re-syncing might be needed if device disconnected
                    delay(2000) 
                }

                caseIndex++
                delay(10) // High-speed fuzzing
            }
        }
    }

    fun stopFuzzing() {
        _isFuzzing.value = false
        fuzzJob?.cancel()
        Timber.i("[FUZZ] Fuzzing stopped")
    }

    private suspend fun executeTransfer(deviceKey: String, payload: ByteArray): FuzzResult {
        return try {
            val transport = lifecycleManager.getTransport(deviceKey) 
                ?: return FuzzResult.Crash(CrashType.USB_DISCONNECT, "Transport lost")

            // SET_REPORT (0x09) or SET_DESCRIPTOR (0x07) depending on target
            // For HID research, we often send malicious Descriptors via SET_DESCRIPTOR (standard) 
            // or vendor-specific HID reports.
            // Using SET_DESCRIPTOR (0x07) for HID (0x22 = Report Descriptor)
            val result = transport.controlTransfer(
                requestType = 0x21, // Host-to-Device | Class | Interface
                request = 0x09,      // SET_REPORT
                value = 0x0300,      // Report Type: Feature (0x03), Report ID: 0
                index = 0,
                buffer = payload,
                length = payload.size,
                timeout = 1000
            )

            if (result.isSuccess) {
                FuzzResult.Success(0)
            } else {
                // If the state changes to Disconnected, it's a finding
                if (sessionCoordinator.state.value is ConnectionState.Disconnected || 
                    sessionCoordinator.state.value is ConnectionState.Failed) {
                    FuzzResult.Crash(CrashType.USB_DISCONNECT, "Device dropped after payload")
                } else {
                    FuzzResult.Success(0) // Just a rejection
                }
            }
        } catch (e: Exception) {
            FuzzResult.Crash(CrashType.UNKNOWN, e.message ?: "Exception during transfer")
        }
    }
}
