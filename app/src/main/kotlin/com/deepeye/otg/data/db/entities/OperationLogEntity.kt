package com.deepeye.otg.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@androidx.room.Entity(
    tableName = "operation_logs",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["sessionId"])]
)
data class OperationLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val operationType: String, // DUMP, SHELL, REPAIR
    val details: String,
    val result: String, // SUCCESS, FAILED
    val artifactPath: String? = null // Path to dump/log file
)
