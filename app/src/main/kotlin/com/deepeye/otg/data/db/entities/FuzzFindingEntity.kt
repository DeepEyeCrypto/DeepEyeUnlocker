package com.deepeye.otg.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuzz_findings",
    indices = [Index(value = ["sessionId"])]
)
data class FuzzFindingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String, // correlated with UUID from SessionCoordinator
    val timestamp: Long,
    val type: String,      // HID, WEBKIT, KERNEL
    val sourceSeed: String,
    val mutationType: String,
    val payloadHex: String,
    val crashType: String, // USB_DISCONNECT, PANIC, HANG
    val crashSignature: String,
    val targetDeviceKey: String // VID:PID:SERIAL or similar
)
