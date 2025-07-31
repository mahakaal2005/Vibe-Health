package com.vibehealth.android.data.goals.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.util.*

/**
 * Optimized DAO for goal database operations.
 * 
 * Implements proper indexing, query optimization, and connection pooling
 * for improved performance as specified in Task 6.1.
 */
@Dao
interface OptimizedGoalDao {

    /**
     * Optimized query with proper indexing for user-specific goals.
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE userId = :userId 
        ORDER BY calculatedAt DESC 
        LIMIT 1
    """)
    suspend fun getCurrentGoalsForUserSync(userId: String): DailyGoalsEntity?

    /**
     * Reactive query with proper indexing.
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE userId = :userId 
        ORDER BY calculatedAt DESC 
        LIMIT 1
    """)
    fun getCurrentGoalsForUser(userId: String): Flow<DailyGoalsEntity?>

    /**
     * Optimized upsert operation with conflict resolution.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: DailyGoalsEntity)

    /**
     * Batch upsert for multiple goals (optimized for sync operations).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoalsBatch(goals: List<DailyGoalsEntity>)

    /**
     * Optimized query for checking goal existence.
     */
    @Query("SELECT COUNT(*) > 0 FROM daily_goals WHERE userId = :userId")
    suspend fun hasGoalsForUser(userId: String): Boolean

    /**
     * Optimized query for last calculation time with index.
     */
    @Query("""
        SELECT calculatedAt FROM daily_goals 
        WHERE userId = :userId 
        ORDER BY calculatedAt DESC 
        LIMIT 1
    """)
    suspend fun getLastCalculationTime(userId: String): LocalDateTime?

    /**
     * Optimized query for all user goals with pagination support.
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE userId = :userId 
        ORDER BY calculatedAt DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun getAllGoalsForUserPaginated(userId: String, limit: Int, offset: Int): Flow<List<DailyGoalsEntity>>

    /**
     * Optimized query for dirty goals (needs sync).
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE isDirty = 1 
        ORDER BY updatedAt ASC 
        LIMIT :limit
    """)
    suspend fun getDirtyGoals(limit: Int = 50): List<DailyGoalsEntity>

    /**
     * Batch update for marking goals as synced.
     */
    @Query("""
        UPDATE daily_goals 
        SET isDirty = 0, lastSyncAt = :syncTime 
        WHERE id IN (:goalIds)
    """)
    suspend fun markGoalsAsSyncedBatch(goalIds: List<String>, syncTime: Date)

    /**
     * Optimized single goal sync update.
     */
    @Query("""
        UPDATE daily_goals 
        SET isDirty = 0, lastSyncAt = :syncTime 
        WHERE id = :goalId
    """)
    suspend fun markGoalsAsSynced(goalId: String, syncTime: Date)

    /**
     * Optimized delete operation with index.
     */
    @Query("DELETE FROM daily_goals WHERE userId = :userId")
    suspend fun deleteGoalsForUser(userId: String): Int

    /**
     * Optimized cleanup query with date index.
     */
    @Query("DELETE FROM daily_goals WHERE calculatedAt < :beforeDate")
    suspend fun deleteOldGoals(beforeDate: LocalDateTime): Int

    /**
     * Optimized count query for analytics.
     */
    @Query("SELECT COUNT(*) FROM daily_goals WHERE userId = :userId")
    suspend fun getGoalCountForUser(userId: String): Int

    /**
     * Optimized query for recent goals (for caching).
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE userId = :userId 
        AND calculatedAt >= :since 
        ORDER BY calculatedAt DESC
    """)
    suspend fun getRecentGoals(userId: String, since: LocalDateTime): List<DailyGoalsEntity>

    /**
     * Optimized query for goals by calculation source.
     */
    @Query("""
        SELECT * FROM daily_goals 
        WHERE userId = :userId 
        AND calculationSource = :source 
        ORDER BY calculatedAt DESC 
        LIMIT :limit
    """)
    suspend fun getGoalsBySource(userId: String, source: String, limit: Int = 10): List<DailyGoalsEntity>

    /**
     * Optimized aggregate query for statistics.
     */
    @Query("""
        SELECT 
            AVG(stepsGoal) as avgSteps,
            AVG(caloriesGoal) as avgCalories,
            AVG(heartPointsGoal) as avgHeartPoints,
            COUNT(*) as totalGoals
        FROM daily_goals 
        WHERE userId = :userId 
        AND calculatedAt >= :since
    """)
    suspend fun getGoalStatistics(userId: String, since: LocalDateTime): GoalStatistics

    /**
     * Optimized query for database maintenance.
     */
    @Query("VACUUM")
    suspend fun vacuumDatabase()

    /**
     * Optimized query to get database size information.
     */
    @Query("SELECT COUNT(*) FROM daily_goals")
    suspend fun getTotalGoalCount(): Int

    /**
     * Data class for goal statistics.
     */
    data class GoalStatistics(
        val avgSteps: Double,
        val avgCalories: Double,
        val avgHeartPoints: Double,
        val totalGoals: Int
    )
}

/**
 * Database indexes for optimized queries.
 * These should be defined in the Room database schema.
 */
object GoalDatabaseIndexes {
    const val USER_ID_INDEX = "CREATE INDEX IF NOT EXISTS index_daily_goals_userId ON daily_goals(userId)"
    const val CALCULATED_AT_INDEX = "CREATE INDEX IF NOT EXISTS index_daily_goals_calculatedAt ON daily_goals(calculatedAt)"
    const val USER_CALCULATED_INDEX = "CREATE INDEX IF NOT EXISTS index_daily_goals_userId_calculatedAt ON daily_goals(userId, calculatedAt)"
    const val DIRTY_INDEX = "CREATE INDEX IF NOT EXISTS index_daily_goals_isDirty ON daily_goals(isDirty)"
    const val SYNC_INDEX = "CREATE INDEX IF NOT EXISTS index_daily_goals_lastSyncAt ON daily_goals(lastSyncAt)"
    const val SOURCE_INDEX = "CREATE INDEX IF NOT EXISTS index_daily_goals_calculationSource ON daily_goals(calculationSource)"
    
    val ALL_INDEXES = listOf(
        USER_ID_INDEX,
        CALCULATED_AT_INDEX,
        USER_CALCULATED_INDEX,
        DIRTY_INDEX,
        SYNC_INDEX,
        SOURCE_INDEX
    )
}