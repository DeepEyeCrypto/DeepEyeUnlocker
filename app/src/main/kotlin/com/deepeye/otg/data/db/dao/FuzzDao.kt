package com.deepeye.otg.data.db.dao

import androidx.room.*
import com.deepeye.otg.data.db.entities.FuzzFindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuzzDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinding(finding: FuzzFindingEntity)

    @Query("SELECT * FROM fuzz_findings ORDER BY timestamp DESC")
    fun getAllFindings(): Flow<List<FuzzFindingEntity>>

    @Query("SELECT * FROM fuzz_findings WHERE type = :type ORDER BY timestamp DESC")
    fun getFindingsByType(type: String): Flow<List<FuzzFindingEntity>>

    @Query("SELECT COUNT(*) FROM fuzz_findings")
    suspend fun getCount(): Int

    @Delete
    suspend fun deleteFinding(finding: FuzzFindingEntity)
}
