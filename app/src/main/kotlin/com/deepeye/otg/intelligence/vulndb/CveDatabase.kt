package com.deepeye.otg.intelligence.vulndb

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ──────────────────────────────────────────────────────────────
// CVE Database — Room DB + DAO
// DeepEye OTG — Intelligence Module (Part 1)
// ──────────────────────────────────────────────────────────────

@Dao
interface CveDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CveEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<CveEntry>)

    @Query("SELECT * FROM cve_entries WHERE cveId = :cveId")
    suspend fun getById(cveId: String): CveEntry?

    @Query("SELECT * FROM cve_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CveEntry>>

    @Query("SELECT * FROM cve_entries ORDER BY updatedAt DESC")
    suspend fun getAll(): List<CveEntry>

    @Query("SELECT * FROM cve_entries ORDER BY updatedAt DESC")
    fun getAllSync(): List<CveEntry>

    @Query("DELETE FROM cve_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM cve_entries WHERE component = :component ORDER BY updatedAt DESC")
    suspend fun getByComponent(component: String): List<CveEntry>

    @Query("SELECT DISTINCT component FROM cve_entries ORDER BY component ASC")
    suspend fun getDistinctComponents(): List<String>

    @Query("SELECT * FROM cve_entries WHERE exploitedInWild = 1 ORDER BY cvssScore DESC")
    suspend fun getActivelyExploited(): List<CveEntry>

    @Query("""
        SELECT * FROM cve_entries 
        WHERE cveId LIKE '%' || :query || '%'
           OR component LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 50): List<CveEntry>

    // Statistics helpers
    @Query("SELECT COUNT(*) FROM cve_entries")
    suspend fun count(): Int

    @Query("SELECT component, COUNT(*) as cnt FROM cve_entries GROUP BY component")
    suspend fun getComponentStats(): List<ComponentStat>

    @Query("SELECT * FROM cve_entries WHERE affectedVersions LIKE '%' || :iosVersion || '%' ORDER BY updatedAt DESC")
    fun observeUnpatchedForVersion(iosVersion: String): Flow<List<CveEntry>>

    @Query("SELECT exploitedInWild as status, COUNT(*) as cnt FROM cve_entries GROUP BY exploitedInWild")
    suspend fun getExploitationStats(): List<ExploitationStat>
}

data class ExploitationStat(
    val status: String,
    val cnt: Int
)

data class ComponentStat(
    val component: String,
    val cnt: Int
)

@Database(
    entities = [CveEntry::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(CveTypeConverters::class)
abstract class CveDatabase : RoomDatabase() {
    abstract fun cveDao(): CveDao

    companion object {
        private const val DB_NAME = "deepeye_cve_intelligence_v2.db"

        @Volatile
        private var INSTANCE: CveDatabase? = null

        fun getInstance(context: Context): CveDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CveDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
