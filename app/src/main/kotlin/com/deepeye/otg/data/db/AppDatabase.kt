package com.deepeye.otg.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.deepeye.otg.data.db.dao.ForensicDao
import com.deepeye.otg.data.db.dao.FuzzDao
import com.deepeye.otg.data.db.entities.DeviceEntity
import com.deepeye.otg.data.db.entities.FuzzFindingEntity
import com.deepeye.otg.data.db.entities.OperationLogEntity
import com.deepeye.otg.data.db.entities.SessionEntity

@Database(
    entities = [
        DeviceEntity::class,
        SessionEntity::class,
        OperationLogEntity::class,
        FuzzFindingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun forensicDao(): ForensicDao
    abstract fun fuzzDao(): FuzzDao
}
