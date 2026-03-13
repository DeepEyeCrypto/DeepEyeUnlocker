package com.deepeye.otg.data.repository

import com.deepeye.otg.data.db.dao.ForensicDao
import com.deepeye.otg.data.db.entities.DeviceEntity
import com.deepeye.otg.data.db.entities.OperationLogEntity
import com.deepeye.otg.data.db.entities.SessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForensicRepository @Inject constructor(
    private val forensicDao: ForensicDao
) {
    fun getAllDevices(): Flow<List<DeviceEntity>> = forensicDao.getAllDevices()

    suspend fun recordDevice(device: DeviceEntity) {
        forensicDao.insertDevice(device)
    }

    suspend fun startSession(deviceKey: String, mode: String): Long {
        val session = SessionEntity(
            deviceKey = deviceKey,
            startTimestamp = System.currentTimeMillis(),
            connectionMode = mode
        )
        return forensicDao.insertSession(session)
    }

    suspend fun logOperation(sessionId: Long, type: String, details: String, result: String, path: String? = null) {
        val log = OperationLogEntity(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            operationType = type,
            details = details,
            result = result,
            artifactPath = path
        )
        forensicDao.insertLog(log)
    }

    fun getSessions(deviceKey: String): Flow<List<SessionEntity>> = forensicDao.getSessionsForDevice(deviceKey)

    fun getLogs(sessionId: Long): Flow<List<OperationLogEntity>> = forensicDao.getLogsForSession(sessionId)
}
