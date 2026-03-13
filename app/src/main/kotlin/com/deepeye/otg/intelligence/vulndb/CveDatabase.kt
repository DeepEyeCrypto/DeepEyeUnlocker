package com.deepeye.otg.intelligence.vulndb

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ──────────────────────────────────────────────────────────────
// CVE Database — Room DB + DAO
// DeepEye OTG — CVE Intelligence Module (Part 1)
// ──────────────────────────────────────────────────────────────

/**
 * Room DAO for CVE intelligence operations.
 *
 * Query patterns optimized for:
 * - version-based lookups (which CVEs affect iOS 26.x?)
 * - component-based lookups (which WebKit CVEs exist?)
 * - exposure analysis (unpatched CVEs for a given version)
 * - confidence filtering (only show confirmed/high entries)
 */
@Dao
interface CveDao {

    // ── Insert / Update ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CveEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<CveEntry>)

    @Update
    suspend fun update(entry: CveEntry)

    @Delete
    suspend fun delete(entry: CveEntry)

    @Query("DELETE FROM cve_entries WHERE cveId = :cveId")
    suspend fun deleteById(cveId: String)

    @Query("DELETE FROM cve_entries")
    suspend fun deleteAll()

    // ── Basic queries ──

    @Query("SELECT * FROM cve_entries WHERE cveId = :cveId")
    suspend fun getById(cveId: String): CveEntry?

    @Query("SELECT * FROM cve_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CveEntry>>

    @Query("SELECT * FROM cve_entries ORDER BY updatedAt DESC")
    suspend fun getAll(): List<CveEntry>

    @Query("SELECT COUNT(*) FROM cve_entries")
    suspend fun count(): Int

    // ── Component-based queries ──

    @Query("SELECT * FROM cve_entries WHERE component = :component ORDER BY updatedAt DESC")
    suspend fun getByComponent(component: String): List<CveEntry>

    @Query("SELECT * FROM cve_entries WHERE component = :component ORDER BY updatedAt DESC")
    fun observeByComponent(component: String): Flow<List<CveEntry>>

    @Query("SELECT DISTINCT component FROM cve_entries ORDER BY component ASC")
    suspend fun getDistinctComponents(): List<String>

    // ── Version-based queries (uses LIKE for delimited list search) ──

    /**
     * Find CVEs that affect a specific iOS version.
     * Uses LIKE against the pipe-delimited affectedVersions field.
     */
    @Query("SELECT * FROM cve_entries WHERE ('|||' || affectedVersions || '|||') LIKE '%|||' || :version || '|||%' ORDER BY cvssScore DESC")
    suspend fun getAffectingVersion(version: String): List<CveEntry>

    @Query("SELECT * FROM cve_entries WHERE ('|||' || affectedVersions || '|||') LIKE '%|||' || :version || '|||%' ORDER BY cvssScore DESC")
    fun observeAffectingVersion(version: String): Flow<List<CveEntry>>

    /**
     * Find CVEs fixed in a specific iOS version.
     */
    @Query("SELECT * FROM cve_entries WHERE ('|||' || fixedVersions || '|||') LIKE '%|||' || :version || '|||%' ORDER BY cvssScore DESC")
    suspend fun getFixedInVersion(version: String): List<CveEntry>

    // ── Exploitation status queries ──

    @Query("SELECT * FROM cve_entries WHERE exploitationStatus = :status ORDER BY cvssScore DESC")
    suspend fun getByExploitationStatus(status: ExploitationStatus): List<CveEntry>

    @Query("SELECT * FROM cve_entries WHERE exploitationStatus = 'ACTIVE_EXPLOITATION' ORDER BY cvssScore DESC")
    suspend fun getActivelyExploited(): List<CveEntry>

    @Query("SELECT * FROM cve_entries WHERE exploitationStatus = 'ACTIVE_EXPLOITATION' ORDER BY cvssScore DESC")
    fun observeActivelyExploited(): Flow<List<CveEntry>>

    // ── Confidence filtering ──

    @Query("SELECT * FROM cve_entries WHERE confidence IN ('CONFIRMED', 'HIGH') ORDER BY updatedAt DESC")
    suspend fun getHighConfidence(): List<CveEntry>

    @Query("SELECT * FROM cve_entries WHERE reviewed = 0 ORDER BY importedAt DESC")
    suspend fun getUnreviewed(): List<CveEntry>

    // ── Vulnerability type queries ──

    @Query("SELECT * FROM cve_entries WHERE vulnerabilityType = :type ORDER BY cvssScore DESC")
    suspend fun getByVulnType(type: VulnerabilityType): List<CveEntry>

    // ── Exposure analysis queries ──

    /**
     * CVEs that affect [version] AND are NOT fixed in [version].
     * This is the core query for patch-state analysis.
     */
    @Query("""
        SELECT * FROM cve_entries 
        WHERE ('|||' || affectedVersions || '|||') LIKE '%|||' || :version || '|||%'
          AND ('|||' || fixedVersions || '|||') NOT LIKE '%|||' || :version || '|||%'
        ORDER BY cvssScore DESC
    """)
    suspend fun getUnpatchedForVersion(version: String): List<CveEntry>

    @Query("""
        SELECT * FROM cve_entries 
        WHERE ('|||' || affectedVersions || '|||') LIKE '%|||' || :version || '|||%'
          AND ('|||' || fixedVersions || '|||') NOT LIKE '%|||' || :version || '|||%'
        ORDER BY cvssScore DESC
    """)
    fun observeUnpatchedForVersion(version: String): Flow<List<CveEntry>>

    // ── Search ──

    @Query("""
        SELECT * FROM cve_entries 
        WHERE cveId LIKE '%' || :query || '%'
           OR component LIKE '%' || :query || '%'
           OR summary LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 50): List<CveEntry>

    // ── Statistics ──

    @Query("SELECT component, COUNT(*) as cnt FROM cve_entries GROUP BY component ORDER BY cnt DESC")
    suspend fun componentStats(): List<ComponentStat>

    @Query("""
        SELECT exploitationStatus, COUNT(*) as cnt 
        FROM cve_entries 
        GROUP BY exploitationStatus
    """)
    suspend fun exploitationStats(): List<ExploitationStat>
}

/** Helper data class for component stats query */
data class ComponentStat(
    val component: String,
    val cnt: Int
)

/** Helper data class for exploitation stats query */
data class ExploitationStat(
    val exploitationStatus: ExploitationStatus,
    val cnt: Int
)

// ──────────────────────────────────────────────────────────────
// Room Database
// ──────────────────────────────────────────────────────────────

@Database(
    entities = [CveEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CveTypeConverters::class)
abstract class CveDatabase : RoomDatabase() {
    abstract fun cveDao(): CveDao

    companion object {
        private const val DB_NAME = "deepeye_cve_intelligence.db"

        @Volatile
        private var INSTANCE: CveDatabase? = null

        /**
         * Thread-safe singleton accessor.
         * Uses a separate database from [AppDatabase] to keep
         * intelligence data isolated and independently versioned.
         */
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
