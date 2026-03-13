package com.deepeye.otg.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceKey: String, // vid:pid:serial
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String,
    val model: String,
    val firstDetectedAt: Long,
    val lastDetectedAt: Long,
    val chipsets: String? = null, // Comma-separated or JSON
    val status: String = "REGISTERED"
)
