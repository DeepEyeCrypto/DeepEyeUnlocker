package com.deepeye.otg.research.fuzz

import javax.inject.Inject

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.jvm.JvmSuppressWildcards

// ──────────────────────────────────────────────────────────────
// Fuzzing Harness — Generic Lab-Only Framework
// DeepEye OTG — Research Module (Part 4)
//
// Purpose:  Reproduce crash conditions safely and collect
//           evidence WITHOUT exploitation.
// ──────────────────────────────────────────────────────────────

private const val TAG = "FuzzHarness"

/**
 * Fuzzing target surface type.
 */
enum class TargetSurface {
    USB_HID,
    USB_BULK,
    USB_CONTROL,
    NETWORK_TCP,
    NETWORK_UDP,
    FILE_FORMAT,
    IPC_MESSAGE,
    CUSTOM
}

/**
 * Harness execution state.
 */
enum class FuzzState {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPING,
    COMPLETED,
    ERROR
}

/**
 * A single fuzzing test case — input + metadata.
 */
data class FuzzTestCase(
    val id: String,
    val inputData: ByteArray,
    val parentId: String? = null,        // which corpus entry or mutation produced this
    val mutationStrategy: String? = null, // which mutator was used
    val generation: Int = 0,              // mutation depth from seed
    val createdAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FuzzTestCase) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

/**
 * Result of executing a single test case.
 */
data class FuzzResult(
    val testCaseId: String,
    val crashed: Boolean,
    val crashSignature: String? = null,
    val exitCode: Int? = null,
    val stdout: String = "",
    val stderr: String = "",
    val durationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Session-level fuzzing statistics.
 */
data class FuzzSessionStats(
    val sessionId: String,
    val totalExecutions: Long = 0,
    val totalCrashes: Long = 0,
    val uniqueCrashes: Long = 0,
    val executionsPerSecond: Double = 0.0,
    val corpusSize: Int = 0,
    val coveragePercentage: Double? = null,
    val elapsedMs: Long = 0,
    val startedAt: Long = 0,
    val lastUpdateAt: Long = System.currentTimeMillis()
)

/**
 * Configuration for a fuzzing session.
 */
data class FuzzConfig(
    val sessionId: String = "fuzz_${System.currentTimeMillis()}",
    val targetSurface: TargetSurface = TargetSurface.USB_HID,
    val maxIterations: Long = 100_000,
    val maxDurationMs: Long = 3_600_000, // 1 hour default
    val maxInputSize: Int = 4096,
    val timeoutPerCaseMs: Long = 5_000,
    val saveAllInputs: Boolean = false,
    val saveCrashInputs: Boolean = true,
    val mutationDepth: Int = 10,
    val parallelWorkers: Int = 1,
    val outputDir: File? = null,
    val notes: String = ""
)

/**
 * Interface for target-specific execution logic.
 *
 * Implementations provide the actual delivery mechanism
 * for test cases to the target surface.
 *
 * Examples:
 * - USB HID descriptor injection to a connected device
 * - TCP packet send to a network parser
 * - File format parser invocation
 */
interface FuzzTarget {
    /** Human-readable name for this target */
    val name: String

    /** Target surface type */
    val surface: TargetSurface

    /**
     * Initialize the target (open connections, allocate resources).
     * @return true if initialization successful
     */
    suspend fun initialize(): Boolean

    /**
     * Execute a single test case against the target.
     *
     * Implementations must:
     * - Deliver the input data to the target
     * - Monitor for crashes/hangs
     * - Collect any output/logs
     * - Return within the configured timeout
     *
     * @param testCase the test case to execute
     * @return result of the execution
     */
    suspend fun execute(testCase: FuzzTestCase): FuzzResult

    /**
     * Reset the target to a clean state between test cases.
     */
    suspend fun reset()

    /**
     * Release all resources.
     */
    suspend fun teardown()
}

/**
 * Generic fuzzing harness that orchestrates:
 * 1. Corpus loading
 * 2. Mutation
 * 3. Test case execution
 * 4. Crash detection + classification
 * 5. Result persistence
 *
 * Does NOT contain exploit logic — only crash reproduction
 * and evidence collection.
 */
class FuzzHarness @Inject constructor(
    private val config: FuzzConfig,
    private val target: FuzzTarget,
    private val corpusManager: CorpusManager,
    private val mutators: List<@JvmSuppressWildcards Mutator>,
    private val crashClassifier: CrashClassifier,
    private val reproRecorder: ReproRecorder
) {
    private val _state = MutableStateFlow(FuzzState.IDLE)
    val state: StateFlow<FuzzState> = _state.asStateFlow()

    private val _stats = MutableStateFlow(FuzzSessionStats(sessionId = config.sessionId))
    val stats: StateFlow<FuzzSessionStats> = _stats.asStateFlow()

    private val _crashes = MutableStateFlow<List<CrashClassifier.ClassifiedCrash>>(emptyList())
    val crashes: StateFlow<List<CrashClassifier.ClassifiedCrash>> = _crashes.asStateFlow()

    private var fuzzJob: Job? = null
    private var startTimeMs = 0L
    private var totalExecs = 0L
    private var totalCrashes = 0L
    private val uniqueCrashSignatures = mutableSetOf<String>()

    // ── Lifecycle ───────────────────────────────────────────────

    /**
     * Start the fuzzing session.
     */
    suspend fun start() {
        if (_state.value == FuzzState.RUNNING) {
            Log.w(TAG, "Already running")
            return
        }

        Log.i(TAG, "Starting fuzz session: ${config.sessionId}")
        Log.i(TAG, "Target: ${target.name} (${config.targetSurface})")
        Log.i(TAG, "Max iterations: ${config.maxIterations}")

        if (!target.initialize()) {
            Log.e(TAG, "Target initialization failed")
            _state.value = FuzzState.ERROR
            return
        }

        _state.value = FuzzState.RUNNING
        startTimeMs = System.currentTimeMillis()

        reproRecorder.startSession(config)

        fuzzJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                runFuzzLoop()
            } catch (e: CancellationException) {
                Log.i(TAG, "Fuzz session cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Fuzz session error", e)
                _state.value = FuzzState.ERROR
            } finally {
                target.teardown()
                reproRecorder.endSession(buildFinalStats())
                if (_state.value == FuzzState.RUNNING || _state.value == FuzzState.STOPPING) {
                    _state.value = FuzzState.COMPLETED
                }
            }
        }
    }

    /**
     * Stop the fuzzing session gracefully.
     */
    fun stop() {
        _state.value = FuzzState.STOPPING
        fuzzJob?.cancel()
        Log.i(TAG, "Stopping fuzz session after $totalExecs executions, $totalCrashes crashes")
    }

    /**
     * Pause fuzzing.
     */
    fun pause() {
        if (_state.value == FuzzState.RUNNING) {
            _state.value = FuzzState.PAUSED
            Log.i(TAG, "Paused")
        }
    }

    /**
     * Resume fuzzing.
     */
    fun resume() {
        if (_state.value == FuzzState.PAUSED) {
            _state.value = FuzzState.RUNNING
            Log.i(TAG, "Resumed")
        }
    }

    // ── Core Loop ───────────────────────────────────────────────

    private suspend fun runFuzzLoop() {
        val seeds = corpusManager.loadSeeds()
        if (seeds.isEmpty()) {
            Log.w(TAG, "No seed corpus — generating random seeds")
            corpusManager.generateRandomSeeds(count = 10, maxSize = config.maxInputSize)
        }

        var iteration = 0L

        while (
            _state.value == FuzzState.RUNNING &&
            iteration < config.maxIterations &&
            (System.currentTimeMillis() - startTimeMs) < config.maxDurationMs
        ) {
            // Wait if paused
            while (_state.value == FuzzState.PAUSED) {
                delay(100)
            }
            if (_state.value != FuzzState.RUNNING) break

            // 1. Pick a base input from corpus
            val baseInput = corpusManager.pickRandom()
                ?: corpusManager.loadSeeds().firstOrNull()
                ?: break

            // 2. Mutate
            val mutator = mutators.random()
            val mutatedData = mutator.mutate(
                baseInput.inputData,
                config.maxInputSize
            )

            val testCase = FuzzTestCase(
                id = "tc_${config.sessionId}_$iteration",
                inputData = mutatedData,
                parentId = baseInput.id,
                mutationStrategy = mutator.name,
                generation = baseInput.generation + 1
            )

            // 3. Execute
            val result = try {
                withTimeout(config.timeoutPerCaseMs) {
                    target.execute(testCase)
                }
            } catch (e: TimeoutCancellationException) {
                FuzzResult(
                    testCaseId = testCase.id,
                    crashed = false,
                    stderr = "TIMEOUT after ${config.timeoutPerCaseMs}ms",
                    durationMs = config.timeoutPerCaseMs
                )
            }

            totalExecs++

            // 4. Handle crash
            if (result.crashed) {
                totalCrashes++
                val classified = crashClassifier.classify(testCase, result)

                if (classified.signature !in uniqueCrashSignatures) {
                    uniqueCrashSignatures.add(classified.signature)
                    Log.w(TAG, "NEW CRASH [${classified.bucket}]: ${classified.signature}")

                    // Save crash input
                    if (config.saveCrashInputs) {
                        corpusManager.saveCrashInput(testCase, classified)
                    }

                    val currentCrashes = _crashes.value.toMutableList()
                    currentCrashes.add(classified)
                    _crashes.value = currentCrashes
                }

                reproRecorder.recordCrash(testCase, result, classified)

                // Reset target after crash
                target.reset()
            }

            // 5. Maybe add to corpus (if input discovered new behavior)
            if (config.saveAllInputs || result.crashed) {
                corpusManager.addToCorpus(testCase)
            }

            // 6. Update stats periodically
            if (iteration % 100 == 0L) {
                updateStats(iteration)
            }

            iteration++
        }

        updateStats(iteration)
        Log.i(TAG, "Fuzz loop complete: $totalExecs execs, " +
                "${uniqueCrashSignatures.size} unique crashes")
    }

    // ── Stats ───────────────────────────────────────────────────

    private fun updateStats(iteration: Long) {
        val elapsedMs = System.currentTimeMillis() - startTimeMs
        val execsPerSec = if (elapsedMs > 0) {
            totalExecs.toDouble() / (elapsedMs / 1000.0)
        } else 0.0

        _stats.value = FuzzSessionStats(
            sessionId = config.sessionId,
            totalExecutions = totalExecs,
            totalCrashes = totalCrashes,
            uniqueCrashes = uniqueCrashSignatures.size.toLong(),
            executionsPerSecond = execsPerSec,
            corpusSize = corpusManager.corpusSize(),
            elapsedMs = elapsedMs,
            startedAt = startTimeMs
        )
    }

    private fun buildFinalStats(): FuzzSessionStats = _stats.value.copy(
        lastUpdateAt = System.currentTimeMillis()
    )
}
