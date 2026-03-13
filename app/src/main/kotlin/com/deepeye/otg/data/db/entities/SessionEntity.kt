package com.deepeye.otg.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@androidx.room.Entity(
    tableName = "sessions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["deviceKey"],
            childColumns = ["deviceKey"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["deviceKey"])]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val deviceKey: String,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val connectionMode: String, // ADB, BROM, etc.
    val resultStatus: String = "ACTIVE",
    val summary: String? = null
)
