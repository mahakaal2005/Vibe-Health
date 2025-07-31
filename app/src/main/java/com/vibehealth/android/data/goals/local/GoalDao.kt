package com.vibehealth.android.data.goals.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for DailyGoals with comprehensive CRUD operations.
 * 
 * This DAO provides methods for saving, retrieving, and syncing goal data
 * with proper indexing for performance and offline-first capabilities.
 */
@Dao
interface GoalDao {

    /**
     * Insert or update daily goals for a user.
     * Uses REPLACE strategy to handle conflicts.
     * 
     * @param goals DailyGoalsEntity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: DailyGoalsEntity)

    /**
     * Insert or update multiple daily goals.
     * Useful for batch operations and sync.
     * 
     * @param goalsList List of DailyGoalsEntity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goalsList: List<DailyGoalsEntity>)

    /**
     * Get the most recent daily goals for a specific user.
     * Returns Flow for reactive updates.
     * 
     * @param userId User ID to get goals for
     * @return Flow of DailyGoalsEntity or null if not found
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE user_id = :userId 
        ORDER BY calculated_at DESC 
        LIMIT 1
    """)
    fun getCurrentGoalsForUser(userId: String): Flow<DailyGoalsEntity?>

    /**
     * Get daily goals for a specific user synchronously.
     * Useful for one-time operations.
     * 
     * @param userId User ID to get goals for
     * @return DailyGoalsEntity or null if not found
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE user_id = :userId 
        ORDER BY calculated_at DESC 
        LIMIT 1
    """)
    suspend fun getCurrentGoalsForUserSync(userId: String): DailyGoalsEntity?

    /**
     * Get all daily goals for a user ordered by calculation date.
     * Useful for history and analytics.
     * 
     * @param userId User ID to get goals for
     * @return Flow of list of DailyGoalsEntity
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE user_id = :userId 
        ORDER BY calculated_at DESC
    """)
    fun getAllGoalsForUser(userId: String): Flow<List<DailyGoalsEntity>>

    /**
     * Get daily goals calculated within a specific time range.
     * Useful for analytics and debugging.
     * 
     * @param userId User ID to get goals for
     * @param startDate Start of time range
     * @param endDate End of time range
     * @return List of DailyGoalsEntity within range
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE user_id = :userId 
        AND calculated_at BETWEEN :startDate AND :endDate
        ORDER BY calculated_at DESC
    """)
    suspend fun getGoalsInRange(
        userId: String, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): List<DailyGoalsEntity>

    /**
     * Delete daily goals for a specific user.
     * Useful for user data cleanup.
     * 
     * @param userId User ID to delete goals for
     * @return Number of deleted records
     */
    @Query("DELETE FROM daily_goals WHERE user_id = :userId")
    suspend fun deleteGoalsForUser(userId: String): Int

    /**
     * Delete old daily goals beyond a certain date.
     * Useful for data retention policies.
     * 
     * @param beforeDate Delete goals calculated before this date
     * @return Number of deleted records
     */
    @Query("DELETE FROM daily_goals WHERE calculated_at < :beforeDate")
    suspend fun deleteOldGoals(beforeDate: LocalDateTime): Int

    /**
     * Get all goals that need to be synced to cloud.
     * Used by sync service for offline-first functionality.
     * 
     * @return List of DailyGoalsEntity that are dirty
     */
    @Query("SELECT * FROM daily_goals WHERE is_dirty = 1")
    suspend fun getDirtyGoals(): List<DailyGoalsEntity>

    /**
     * Mark goals as synced by updating sync timestamp and dirty flag.
     * 
     * @param goalIds List of goal IDs that were successfully synced
     */
    @Query("""
        UPDATE daily_goals 
        SET is_dirty = 0, last_sync_at = :syncTime 
        WHERE id IN (:goalIds)
    """)
    suspend fun markGoalsAsSynced(goalIds: List<String>, syncTime: Long)

    /**
     * Mark goals as dirty for sync.
     * Useful when local changes need to be pushed to cloud.
     * 
     * @param goalIds List of goal IDs to mark as dirty
     */
    @Query("UPDATE daily_goals SET is_dirty = 1 WHERE id IN (:goalIds)")
    suspend fun markGoalsAsDirty(goalIds: List<String>)

    /**
     * Get count of goals for a specific user.
     * Useful for analytics and debugging.
     * 
     * @param userId User ID to count goals for
     * @return Number of goal records for user
     */
    @Query("SELECT COUNT(*) FROM daily_goals WHERE user_id = :userId")
    suspend fun getGoalsCountForUser(userId: String): Int

    /**
     * Check if user has any goals calculated.
     * Useful for onboarding flow decisions.
     * 
     * @param userId User ID to check
     * @return True if user has any goals
     */
    @Query("SELECT EXISTS(SELECT 1 FROM daily_goals WHERE user_id = :userId)")
    suspend fun hasGoalsForUser(userId: String): Boolean

    /**
     * Get the most recent calculation timestamp for a user.
     * Useful for determining if recalculation is needed.
     * 
     * @param userId User ID to check
     * @return LocalDateTime of most recent calculation or null
     */
    @Query("""
        SELECT calculated_at FROM daily_goals 
        WHERE user_id = :userId 
        ORDER BY calculated_at DESC 
        LIMIT 1
    """)
    suspend fun getLastCalculationTime(userId: String): LocalDateTime?

    /**
     * Delete a specific goal by ID.
     * Useful for cleanup and testing.
     * 
     * @param goalId Goal ID to delete
     * @return Number of deleted records (should be 1 or 0)
     */
    @Query("DELETE FROM daily_goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: String): Int

    /**
     * Update calculation source for existing goals.
     * Useful for data migration or correction.
     * 
     * @param userId User ID to update
     * @param newSource New calculation source
     * @return Number of updated records
     */
    @Query("""
        UPDATE daily_goals 
        SET calculation_source = :newSource, updated_at = :updateTime, is_dirty = 1
        WHERE user_id = :userId
    """)
    suspend fun updateCalculationSource(
        userId: String, 
        newSource: String, 
        updateTime: Long
    ): Int
}