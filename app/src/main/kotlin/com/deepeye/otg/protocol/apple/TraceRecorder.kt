package com.deepeye.otg.protocol.apple

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

// ──────────────────────────────────────────────────────────────
// Trace Recorder — Protocol Trace Capture + JSON Export
// DeepEye OTG — Protocol / Apple Module (Part 3)
// ──────────────────────────────────────────────────────────────

private const val TAG = "TraceRecorder"

/**
 * Direction of data transfer in a protocol trace.
 */
enum class TraceDirection {
    /** Host → Device (OUT) */
    HOST_TO_DEVICE,
    /** Device → Host (IN) */
    DEVICE_TO_HOST,
    /** Not a data transfer (event/state change) */
    EVENT
}

/**
 * Single entry in a protocol trace.
 *
 * Can represent:
 * - USB control/bulk transfers
 * - Session events (connect, disconnect, state changes)
 * - Protocol-level messages (lockdown, usbmux, etc.)
 * - Raw byte captures
 */
data class TraceEntry(
    /** Monotonic sequence number within this recording session */
    val sequenceNumber: Long,
    /** Wall-clock timestamp (epoch millis) */
    val timestamp: Long = System.currentTimeMillis(),
    /** High-resolution timestamp (nanos from System.nanoTime) */
    val nanoTimestamp: Long = System.nanoTime(),
    /** Transfer direction */
    val direction: TraceDirection,
    /** Entry type (e.g. "CONTROL_TRANSFER", "BULK_IN", "EVENT", "LOCKDOWN_MSG") */
    val entryType: String,
    /** Protocol layer (e.g. "USB", "USBMUX", "LOCKDOWN", "SESSION") */
    val protocolLayer: String = "USB",
    /** Human-readable description */
    val description: String,
    /** Raw data bytes (hex encoded for serialization) */
    val dataHex: String? = null,
    /** Data length in bytes */
    val dataLength: Int = 0,
    /** USB endpoint address (if applicable) */
    val endpoint: Int? = null,
    /** USB request type / bRequest (for control transfers) */
    val requestType: Int? = null,
    /** USB wValue (for control transfers) */
    val wValue: Int? = null,
    /** USB wIndex (for control transfers) */
    val wIndex: Int? = null,
    /** Status / return code */
    val status: Int? = null,
    /** Duration of the operation in microseconds */
    val durationMicros: Long? = null,
    /** Additional key-value metadata */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Recording session metadata.
 */
data class RecordingSession(
    val sessionId: String,
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    val deviceId: String,
    val deviceDescription: String,
    val notes: String = "",
    var entryCount: Long = 0
)

/**
 * Recording state.
 */
enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

/**
 * Protocol trace recorder for USB research.
 *
 * Captures:
 * - USB transfers (control, bulk, interrupt)
 * - Session events (from [UsbAppleSession])
 * - Protocol-level messages
 * - Timing data for latency analysis
 *
 * Design:
 * - Lock-free concurrent queue for hot-path recording
 * - Background flush to disk
 * - JSON export format compatible with common protocol analyzers
 * - No personal data capture beyond device identifiers
 * - Configurable max buffer size to prevent OOM
 *
 * Export formats:
 * - JSON (primary, structured)
 * - JSONL (line-delimited, for streaming analysis)
 */
class TraceRecorder(
    private val outputDir: File,
    private val maxBufferEntries: Int = 50_000
) {
    private val _state = MutableStateFlow(RecordingState.IDLE)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _entryCount = MutableStateFlow(0L)
    val entryCount: StateFlow<Long> = _entryCount.asStateFlow()

    private val buffer = ConcurrentLinkedQueue<TraceEntry>()
    private var sequenceCounter = 0L
    private var currentSession: RecordingSession? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    // ── Session Control ─────────────────────────────────────────

    /**
     * Start a new recording session.
     *
     * @param deviceId unique identifier for the device being traced
     * @param description human-readable session description
     * @param notes optional analyst notes
     * @return session ID
     */
    fun startRecording(
        deviceId: String,
        description: String,
        notes: String = ""
    ): String {
        if (_state.value == RecordingState.RECORDING) {
            Log.w(TAG, "Already recording — stopping previous session first")
            stopRecording()
        }

        val sessionId = "trace_${dateFormat.format(Date())}_${deviceId.take(8)}"

        currentSession = RecordingSession(
            sessionId = sessionId,
            deviceId = deviceId,
            deviceDescription = description,
            notes = notes
        )

        buffer.clear()
        sequenceCounter = 0L
        _entryCount.value = 0L
        _state.value = RecordingState.RECORDING

        Log.i(TAG, "Recording started: $sessionId")
        recordInternal(
            direction = TraceDirection.EVENT,
            entryType = "SESSION_START",
            description = "Recording session started: $description",
            metadata = mapOf("device_id" to deviceId)
        )

        return sessionId
    }

    /**
     * Stop the current recording session.
     * Does NOT auto-export — call [exportJson] separately.
     */
    fun stopRecording() {
        if (_state.value != RecordingState.RECORDING &&
            _state.value != RecordingState.PAUSED
        ) {
            return
        }

        recordInternal(
            direction = TraceDirection.EVENT,
            entryType = "SESSION_END",
            description = "Recording session ended"
        )

        currentSession?.endTime = System.currentTimeMillis()
        currentSession?.entryCount = sequenceCounter
        _state.value = RecordingState.STOPPED

        Log.i(TAG, "Recording stopped: ${sequenceCounter} entries captured")
    }

    /**
     * Pause recording (entries are discarded while paused).
     */
    fun pauseRecording() {
        if (_state.value == RecordingState.RECORDING) {
            _state.value = RecordingState.PAUSED
            Log.i(TAG, "Recording paused")
        }
    }

    /**
     * Resume a paused recording.
     */
    fun resumeRecording() {
        if (_state.value == RecordingState.PAUSED) {
            _state.value = RecordingState.RECORDING
            Log.i(TAG, "Recording resumed")
        }
    }

    // ── Recording Methods ───────────────────────────────────────

    /**
     * Record a USB control transfer.
     */
    fun recordControlTransfer(
        direction: TraceDirection,
        requestType: Int,
        request: Int,
        wValue: Int,
        wIndex: Int,
        data: ByteArray?,
        status: Int,
        durationMicros: Long? = null
    ) {
        recordInternal(
            direction = direction,
            entryType = "CONTROL_TRANSFER",
            description = "Control: reqType=0x${requestType.toString(16)} " +
                    "req=0x${request.toString(16)} " +
                    "wVal=0x${wValue.toString(16)} " +
                    "wIdx=0x${wIndex.toString(16)} " +
                    "status=$status",
            dataHex = data?.toHexString(),
            dataLength = data?.size ?: 0,
            requestType = requestType,
            wValue = wValue,
            wIndex = wIndex,
            status = status,
            durationMicros = durationMicros
        )
    }

    /**
     * Record a USB bulk transfer.
     */
    fun recordBulkTransfer(
        direction: TraceDirection,
        endpoint: Int,
        data: ByteArray?,
        length: Int,
        status: Int,
        durationMicros: Long? = null
    ) {
        recordInternal(
            direction = direction,
            entryType = if (direction == TraceDirection.HOST_TO_DEVICE) "BULK_OUT" else "BULK_IN",
            description = "Bulk ${if (direction == TraceDirection.HOST_TO_DEVICE) "OUT" else "IN"}: " +
                    "ep=0x${endpoint.toString(16)} len=$length status=$status",
            dataHex = data?.toHexString(),
            dataLength = length,
            endpoint = endpoint,
            status = status,
            durationMicros = durationMicros
        )
    }

    /**
     * Record a session event (from [UsbAppleSession]).
     */
    fun recordEvent(event: AppleSessionEvent) {
        recordInternal(
            direction = TraceDirection.EVENT,
            entryType = "SESSION_EVENT",
            protocolLayer = "SESSION",
            description = "[${event.type}] ${event.message}",
            metadata = event.data
        )
    }

    /**
     * Record a raw protocol message.
     */
    fun recordProtocolMessage(
        direction: TraceDirection,
        protocolLayer: String,
        messageType: String,
        data: ByteArray?,
        description: String = "",
        metadata: Map<String, String> = emptyMap()
    ) {
        recordInternal(
            direction = direction,
            entryType = messageType,
            protocolLayer = protocolLayer,
            description = description,
            dataHex = data?.toHexString(),
            dataLength = data?.size ?: 0,
            metadata = metadata
        )
    }

    /**
     * Record a free-form annotation / analyst note.
     */
    fun recordAnnotation(note: String) {
        recordInternal(
            direction = TraceDirection.EVENT,
            entryType = "ANNOTATION",
            protocolLayer = "ANALYST",
            description = note
        )
    }

    // ── Internal Recording ──────────────────────────────────────

    private fun recordInternal(
        direction: TraceDirection,
        entryType: String,
        description: String,
        protocolLayer: String = "USB",
        dataHex: String? = null,
        dataLength: Int = 0,
        endpoint: Int? = null,
        requestType: Int? = null,
        wValue: Int? = null,
        wIndex: Int? = null,
        status: Int? = null,
        durationMicros: Long? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        if (_state.value != RecordingState.RECORDING) return

        // Enforce buffer limit
        if (buffer.size >= maxBufferEntries) {
            // Drop oldest entries to make room
            buffer.poll()
        }

        val entry = TraceEntry(
            sequenceNumber = sequenceCounter++,
            direction = direction,
            entryType = entryType,
            protocolLayer = protocolLayer,
            description = description,
            dataHex = dataHex,
            dataLength = dataLength,
            endpoint = endpoint,
            requestType = requestType,
            wValue = wValue,
            wIndex = wIndex,
            status = status,
            durationMicros = durationMicros,
            metadata = metadata
        )
        buffer.add(entry)
        _entryCount.value = sequenceCounter
    }

    // ── Export ───────────────────────────────────────────────────

    /**
     * Export the current recording as a structured JSON file.
     *
     * @return the output [File], or null if export failed
     */
    suspend fun exportJson(): File? = withContext(Dispatchers.IO) {
        val session = currentSession ?: run {
            Log.w(TAG, "No active session to export")
            return@withContext null
        }

        try {
            outputDir.mkdirs()
            val outFile = File(outputDir, "${session.sessionId}.json")

            val root = JSONObject().apply {
                put("format_version", "1.0")
                put("generator", "DeepEye OTG TraceRecorder")
                put("session", JSONObject().apply {
                    put("session_id", session.sessionId)
                    put("device_id", session.deviceId)
                    put("device_description", session.deviceDescription)
                    put("start_time", session.startTime)
                    put("end_time", session.endTime)
                    put("entry_count", session.entryCount)
                    put("notes", session.notes)
                })

                val entries = JSONArray()
                for (entry in buffer) {
                    entries.put(entryToJson(entry))
                }
                put("entries", entries)
            }

            FileWriter(outFile).use { writer ->
                writer.write(root.toString(2))
            }

            Log.i(TAG, "Exported ${buffer.size} entries to ${outFile.absolutePath}")
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            null
        }
    }

    /**
     * Export as JSONL (line-delimited JSON) for streaming analysis.
     *
     * @return the output [File], or null if export failed
     */
    suspend fun exportJsonl(): File? = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext null

        try {
            outputDir.mkdirs()
            val outFile = File(outputDir, "${session.sessionId}.jsonl")

            FileWriter(outFile).use { writer ->
                // First line: session metadata
                val sessionMeta = JSONObject().apply {
                    put("_type", "session_meta")
                    put("session_id", session.sessionId)
                    put("device_id", session.deviceId)
                    put("start_time", session.startTime)
                }
                writer.appendLine(sessionMeta.toString())

                // Subsequent lines: one entry per line
                for (entry in buffer) {
                    writer.appendLine(entryToJson(entry).toString())
                }
            }

            Log.i(TAG, "Exported JSONL to ${outFile.absolutePath}")
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "JSONL export failed", e)
            null
        }
    }

    /**
     * Get all buffered entries (for in-app viewing).
     */
    fun getEntries(): List<TraceEntry> = buffer.toList()

    /**
     * Get recent entries (last N).
     */
    fun getRecentEntries(count: Int = 100): List<TraceEntry> =
        buffer.toList().takeLast(count)

    /**
     * Get session info.
     */
    fun getSessionInfo(): RecordingSession? = currentSession

    // ── Serialization ───────────────────────────────────────────

    private fun entryToJson(entry: TraceEntry): JSONObject = JSONObject().apply {
        put("seq", entry.sequenceNumber)
        put("ts", entry.timestamp)
        put("ns", entry.nanoTimestamp)
        put("dir", entry.direction.name)
        put("type", entry.entryType)
        put("layer", entry.protocolLayer)
        put("desc", entry.description)
        entry.dataHex?.let { put("data_hex", it) }
        if (entry.dataLength > 0) put("data_len", entry.dataLength)
        entry.endpoint?.let { put("ep", it) }
        entry.requestType?.let { put("req_type", it) }
        entry.wValue?.let { put("w_value", it) }
        entry.wIndex?.let { put("w_index", it) }
        entry.status?.let { put("status", it) }
        entry.durationMicros?.let { put("dur_us", it) }
        if (entry.metadata.isNotEmpty()) {
            put("meta", JSONObject(entry.metadata))
        }
    }

    // ── Statistics ──────────────────────────────────────────────

    /**
     * Get summary statistics for the current recording.
     */
    fun getStatistics(): TraceStatistics {
        val entries = buffer.toList()
        return TraceStatistics(
            totalEntries = entries.size.toLong(),
            controlTransfers = entries.count { it.entryType == "CONTROL_TRANSFER" }.toLong(),
            bulkInTransfers = entries.count { it.entryType == "BULK_IN" }.toLong(),
            bulkOutTransfers = entries.count { it.entryType == "BULK_OUT" }.toLong(),
            sessionEvents = entries.count { it.entryType == "SESSION_EVENT" }.toLong(),
            annotations = entries.count { it.entryType == "ANNOTATION" }.toLong(),
            totalDataBytes = entries.sumOf { it.dataLength }.toLong(),
            uniqueEndpoints = entries.mapNotNull { it.endpoint }.distinct().size,
            recordingDurationMs = if (entries.size >= 2)
                entries.last().timestamp - entries.first().timestamp
            else 0L
        )
    }
}

/**
 * Summary statistics for a trace recording.
 */
data class TraceStatistics(
    val totalEntries: Long,
    val controlTransfers: Long,
    val bulkInTransfers: Long,
    val bulkOutTransfers: Long,
    val sessionEvents: Long,
    val annotations: Long,
    val totalDataBytes: Long,
    val uniqueEndpoints: Int,
    val recordingDurationMs: Long
)

// ── Extensions ──────────────────────────────────────────────────

/**
 * Convert ByteArray to hex string for trace serialization.
 */
private fun ByteArray.toHexString(): String =
    joinToString("") { "%02X".format(it) }
