package com.deepeye.otg.data.db.dao

import androidx.room.*
import com.deepeye.otg.data.db.entities.DeviceEntity
import com.deepeye.otg.data.db.entities.OperationLogEntity
import com.deepeye.otg.data.db.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForensicDao {
    // ── Devices ───────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Query("SELECT * FROM devices ORDER BY lastDetectedAt DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE deviceKey = :key LIMIT 1")
    suspend fun getDeviceByKey(key: String): DeviceEntity?

    // ── Sessions ──────────────────────────────────────────────
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE deviceKey = :deviceKey ORDER BY startTimestamp DESC")
    fun getSessionsForDevice(deviceKey: String): Flow<List<SessionEntity>>

    // ── Operation Logs ────────────────────────────────────────
    @Insert
    suspend fun insertLog(log: OperationLogEntity)

    @Query("SELECT * FROM operation_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsForSession(sessionId: Long): Flow<List<OperationLogEntity>>

    @Query("SELECT COUNT(*) FROM operation_logs")
    suspend fun getTotalOperationCount(): Int
}
