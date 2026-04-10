package com.deepeye.otg.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bypass_history")
data class BypassHistoryEntry(
    @PrimaryKey val id:         String = java.util.UUID.randomUUID().toString(),
    val bypassId:               String,
    val carrier:                String,
    val method:                 String,
    val success:                Boolean,
    val deviceModel:            String,
    val timestamp:              Long = System.currentTimeMillis(),
    val logs:                   String = ""  // JSON-serialized log list
)

@Dao
interface BypassHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BypassHistoryEntry)

    @Query("SELECT * FROM bypass_history ORDER BY timestamp DESC LIMIT 100")
    fun getAll(): Flow<List<BypassHistoryEntry>>

    @Query("SELECT * FROM bypass_history WHERE success = 1 ORDER BY timestamp DESC")
    fun getSuccessful(): Flow<List<BypassHistoryEntry>>

    @Query("DELETE FROM bypass_history")
    suspend fun clearAll()
}

@Database(entities = [BypassHistoryEntry::class], version = 1)
abstract class DeepEyeDatabase : RoomDatabase() {
    abstract fun historyDao(): BypassHistoryDao

    companion object {
        @Volatile private var INSTANCE: DeepEyeDatabase? = null

        fun get(context: Context): DeepEyeDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, DeepEyeDatabase::class.java, "deepeye-db")
                    .build().also { INSTANCE = it }
            }
    }
}
