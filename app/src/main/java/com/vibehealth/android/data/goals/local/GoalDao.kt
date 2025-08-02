package com.vibehealth.android.data.goals.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * VIBE_FIX: Phase 2 - Room DAO for goals
 */
@Dao
interface GoalDao {
    
    @Query("SELECT * FROM daily_goals WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveGoalsForUser(userId: String): DailyGoalsEntity?
    
    @Query("SELECT * FROM daily_goals WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActiveGoalsForUserFlow(userId: String): Flow<DailyGoalsEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: DailyGoalsEntity)
    
    @Update
    suspend fun updateGoals(goals: DailyGoalsEntity)
    
    @Query("UPDATE daily_goals SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllGoalsForUser(userId: String)
    
    @Query("DELETE FROM daily_goals WHERE userId = :userId")
    suspend fun deleteAllGoalsForUser(userId: String)
    
    // VIBE_FIX: Phase 3 - Additional DAO methods needed by repository
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: DailyGoalsEntity)
    
    @Query("SELECT * FROM daily_goals WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    fun getCurrentGoalsForUser(userId: String): Flow<DailyGoalsEntity?>
    
    @Query("SELECT * FROM daily_goals WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentGoalsForUserSync(userId: String): DailyGoalsEntity?
    
    @Query("SELECT * FROM daily_goals WHERE isActive = 0")
    suspend fun getDirtyGoals(): List<DailyGoalsEntity>
    
    @Query("DELETE FROM daily_goals WHERE userId = :userId")
    suspend fun deleteGoalsForUser(userId: String): Int
    
    @Query("SELECT COUNT(*) > 0 FROM daily_goals WHERE userId = :userId")
    suspend fun hasGoalsForUser(userId: String): Boolean
    
    @Query("SELECT MAX(createdAt) FROM daily_goals WHERE userId = :userId")
    suspend fun getLastCalculationTime(userId: String): LocalDateTime?
    
    @Query("SELECT * FROM daily_goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllGoalsForUser(userId: String): Flow<List<DailyGoalsEntity>>
    
    @Query("DELETE FROM daily_goals WHERE createdAt < :cutoffDate")
    suspend fun deleteOldGoals(cutoffDate: LocalDateTime): Int
    
    @Query("UPDATE daily_goals SET isActive = 1 WHERE userId IN (:userIds)")
    suspend fun markGoalsAsSynced(userIds: List<String>)
}