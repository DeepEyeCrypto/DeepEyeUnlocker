package com.deepeye.otg.feature.forensics

import javax.inject.Inject

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────────────────────
// Timeline Builder — Forensic Event Timeline Construction
// DeepEye OTG — Forensics Module (Part 6)
// ──────────────────────────────────────────────────────────────

private const val TAG = "TimelineBuilder"

/**
 * A single event in the forensic timeline.
 */
data class TimelineEvent(
    val timestamp: Long,
    val source: String,        // which artifact or system produced this event
    val category: EventCategory,
    val action: String,        // e.g. "file_modified", "app_launched", "backup_created"
    val description: String,
    val artifactId: String? = null,
    val artifactPath: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val confidence: EventConfidence = EventConfidence.MEDIUM
) : Comparable<TimelineEvent> {
    override fun compareTo(other: TimelineEvent): Int = timestamp.compareTo(other.timestamp)
}

enum class EventCategory {
    FILE_SYSTEM,
    APPLICATION,
    SYSTEM,
    NETWORK,
    USB,
    BACKUP,
    USER_ACTION,
    SECURITY,
    UNKNOWN
}

enum class EventConfidence {
    HIGH,       // Timestamp from authoritative source
    MEDIUM,     // File modification time or derived
    LOW,        // Inferred or approximate
    UNKNOWN
}

/**
 * Complete forensic timeline.
 */
data class ForensicTimeline(
    val id: String,
    val events: List<TimelineEvent>,
    val earliestEvent: Long?,
    val latestEvent: Long?,
    val sourceArtifactCount: Int,
    val eventCount: Int,
    val generatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * Builds forensic timelines from indexed artifacts.
 *
 * Correlates timestamps from multiple sources:
 * - File system modification/creation times
 * - Database record timestamps
 * - Log entry timestamps
 * - Backup metadata
 *
 * Produces a unified chronological view.
 */
class TimelineBuilder @Inject constructor() {

    private val events = mutableListOf<TimelineEvent>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)

    /**
     * Build timeline from a set of indexed artifacts.
     */
    fun buildFromIndex(indexResult: IndexResult): ForensicTimeline {
        events.clear()

        for (artifact in indexResult.artifacts) {
            addFileEvents(artifact)
        }

        events.sort()

        val timeline = ForensicTimeline(
            id = "tl_${System.currentTimeMillis()}",
            events = events.toList(),
            earliestEvent = events.firstOrNull()?.timestamp,
            latestEvent = events.lastOrNull()?.timestamp,
            sourceArtifactCount = indexResult.totalFiles,
            eventCount = events.size
        )

        Log.i(TAG, "Built timeline: ${timeline.eventCount} events from " +
                "${timeline.sourceArtifactCount} artifacts")

        return timeline
    }

    /**
     * Add a custom event to the timeline.
     */
    fun addEvent(event: TimelineEvent) {
        events.add(event)
    }

    /**
     * Merge another timeline into this one.
     */
    fun merge(other: ForensicTimeline) {
        events.addAll(other.events)
        events.sort()
    }

    /**
     * Filter timeline by time range.
     */
    fun filter(
        startTime: Long? = null,
        endTime: Long? = null,
        categories: Set<EventCategory>? = null,
        searchQuery: String? = null
    ): List<TimelineEvent> {
        return events.filter { event ->
            val timeOk = (startTime == null || event.timestamp >= startTime) &&
                    (endTime == null || event.timestamp <= endTime)
            val catOk = categories == null || event.category in categories
            val searchOk = searchQuery == null ||
                    event.description.contains(searchQuery, ignoreCase = true) ||
                    event.source.contains(searchQuery, ignoreCase = true) ||
                    event.action.contains(searchQuery, ignoreCase = true)
            timeOk && catOk && searchOk
        }
    }

    /**
     * Group events by time interval (for histogram view).
     */
    fun groupByInterval(intervalMs: Long = 3_600_000): Map<Long, List<TimelineEvent>> {
        return events.groupBy { (it.timestamp / intervalMs) * intervalMs }
    }

    /**
     * Get event density (events per hour) for a given range.
     */
    fun getEventDensity(startTime: Long, endTime: Long): Double {
        val relevant = events.count { it.timestamp in startTime..endTime }
        val hours = (endTime - startTime).toDouble() / 3_600_000
        return if (hours > 0) relevant / hours else 0.0
    }

    // ── File Event Extraction ───────────────────────────────────

    private fun addFileEvents(artifact: ForensicArtifact) {
        // File modification event
        events.add(TimelineEvent(
            timestamp = artifact.lastModified,
            source = artifact.filename,
            category = categoryFromArtifactType(artifact.artifactType),
            action = "file_modified",
            description = "${artifact.filename} (${formatSize(artifact.sizeBytes)}) last modified",
            artifactId = artifact.id,
            artifactPath = artifact.path,
            metadata = buildMap {
                put("size", artifact.sizeBytes.toString())
                put("type", artifact.artifactType.name)
                artifact.sha256?.let { put("sha256", it) }
            }
        ))

        // File creation event (if available and different from modification)
        artifact.created?.let { created ->
            if (created != artifact.lastModified) {
                events.add(TimelineEvent(
                    timestamp = created,
                    source = artifact.filename,
                    category = categoryFromArtifactType(artifact.artifactType),
                    action = "file_created",
                    description = "${artifact.filename} created",
                    artifactId = artifact.id,
                    artifactPath = artifact.path,
                    confidence = EventConfidence.MEDIUM
                ))
            }
        }
    }

    private fun categoryFromArtifactType(type: ArtifactType): EventCategory = when (type) {
        ArtifactType.DATABASE -> EventCategory.APPLICATION
        ArtifactType.LOG -> EventCategory.SYSTEM
        ArtifactType.MEDIA -> EventCategory.USER_ACTION
        ArtifactType.DOCUMENT -> EventCategory.USER_ACTION
        ArtifactType.CONFIGURATION -> EventCategory.SYSTEM
        ArtifactType.CACHE -> EventCategory.APPLICATION
        ArtifactType.APP_DATA -> EventCategory.APPLICATION
        ArtifactType.SYSTEM_INFO -> EventCategory.SYSTEM
        ArtifactType.BACKUP_FRAGMENT -> EventCategory.BACKUP
        ArtifactType.CRASH_LOG -> EventCategory.SYSTEM
        ArtifactType.NETWORK -> EventCategory.NETWORK
        ArtifactType.KEYCHAIN_EXPORT -> EventCategory.SECURITY
        ArtifactType.UNKNOWN -> EventCategory.UNKNOWN
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)}KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))}MB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))}GB"
    }

    /**
     * Export timeline as formatted text (for reports).
     */
    fun exportText(): String = buildString {
        appendLine("═══════════════════════════════════════════════════════")
        appendLine("  Forensic Timeline Report")
        appendLine("  Generated: ${dateFormat.format(Date())}")
        appendLine("  Events: ${events.size}")
        appendLine("═══════════════════════════════════════════════════════")
        appendLine()

        for (event in events) {
            val timeStr = dateFormat.format(Date(event.timestamp))
            appendLine("[$timeStr] [${event.category}] ${event.action}")
            appendLine("  Source: ${event.source}")
            appendLine("  ${event.description}")
            if (event.metadata.isNotEmpty()) {
                event.metadata.forEach { (k, v) -> appendLine("  $k=$v") }
            }
            appendLine()
        }
    }
}
