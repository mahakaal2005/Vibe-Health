package com.vibehealth.android.data.progress

import android.content.Context
import androidx.room.*
import com.vibehealth.android.ui.progress.models.WeeklyProgressData
import com.vibehealth.android.ui.progress.models.DailyProgressData
import com.vibehealth.android.ui.progress.models.GoalAchievements
import com.vibehealth.android.ui.progress.models.WeeklyTotals
import com.vibehealth.android.ui.progress.models.SupportiveInsights
import com.vibehealth.android.domain.user.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProgressCache - Local caching system for offline-first progress data
 * 
 * This class provides immediate display of available historical data with
 * supportive sync messaging and encouraging offline indicators. Ensures users
 * can always access their wellness information with gentle guidance about
 * connectivity status.
 * 
 * Features:
 * - Room database integration for reliable local storage
 * - Immediate display of cached progress data
 * - Background synchronization with encouraging status updates
 * - Data consistency management between local and cloud storage
 * - Supportive offline messaging without creating anxiety
 */
@Singleton
class ProgressCache @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val progressDatabase: ProgressDatabase
) {
    
    /**
     * Caches weekly progress data for offline access
     */
    suspend fun cacheWeeklyProgress(weeklyData: WeeklyProgressData) {
        try {
            val cacheEntity = WeeklyProgressCacheEntity(
                weekStartDate = weeklyData.weekStartDate,
                totalSteps = weeklyData.weeklyTotals.totalSteps,
                totalCalories = weeklyData.weeklyTotals.totalCalories,
                totalHeartPoints = weeklyData.weeklyTotals.totalHeartPoints,
                activeDays = weeklyData.weeklyTotals.activeDays,
                celebratoryMessages = weeklyData.celebratoryMessages,
                lastUpdated = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            )
            
            progressDatabase.weeklyProgressDao().insertOrUpdate(cacheEntity)
            
            // Cache daily data as well
            weeklyData.dailyData.forEach { dailyData ->
                val dailyCacheEntity = DailyProgressCacheEntity(
                    date = dailyData.date,
                    weekStartDate = weeklyData.weekStartDate,
                    steps = dailyData.steps,
                    calories = dailyData.calories,
                    heartPoints = dailyData.heartPoints,
                    hasActivity = dailyData.hasActivity,
                    supportiveContext = dailyData.supportiveContext,
                    lastUpdated = System.currentTimeMillis()
                )
                
                progressDatabase.dailyProgressDao().insertOrUpdate(dailyCacheEntity)
            }
            
        } catch (e: Exception) {
            // Log error but don't fail the operation
            android.util.Log.w("ProgressCache", "Failed to cache progress data", e)
        }
    }
    
    /**
     * Retrieves cached weekly progress data with supportive context
     */
    suspend fun getCachedWeeklyProgress(weekStartDate: LocalDate): CachedProgressResult? {
        return try {
            val weeklyCache = progressDatabase.weeklyProgressDao().getWeeklyProgress(weekStartDate)
            val dailyCache = progressDatabase.dailyProgressDao().getDailyProgressForWeek(weekStartDate)
            
            if (weeklyCache != null) {
                val weeklyData = WeeklyProgressData(
                    weekStartDate = weeklyCache.weekStartDate,
                    dailyData = dailyCache.map { daily ->
                        DailyProgressData(
                            date = daily.date,
                            steps = daily.steps,
                            calories = daily.calories,
                            heartPoints = daily.heartPoints,
                            supportiveContext = daily.supportiveContext,
                            goalAchievements = createMockGoalAchievements(daily) // Would integrate with real goal data
                        )
                    },
                    weeklyTotals = createWeeklyTotals(weeklyCache),
                    supportiveInsights = createSupportiveInsights(weeklyCache),
                    celebratoryMessages = weeklyCache.celebratoryMessages
                )
                
                CachedProgressResult(
                    data = weeklyData,
                    lastUpdated = weeklyCache.lastUpdated,
                    syncStatus = weeklyCache.syncStatus,
                    supportiveMessage = createSupportiveCacheMessage(weeklyCache.lastUpdated, weeklyCache.syncStatus),
                    encouragingContext = "Your progress is safely stored and ready to inspire your wellness journey!"
                )
            } else null
            
        } catch (e: Exception) {
            android.util.Log.w("ProgressCache", "Failed to retrieve cached progress data", e)
            null
        }
    }
    
    /**
     * Marks cached data as needing sync with supportive messaging
     */
    suspend fun markForSync(weekStartDate: LocalDate, supportiveReason: String) {
        try {
            progressDatabase.weeklyProgressDao().updateSyncStatus(
                weekStartDate, 
                SyncStatus.NEEDS_SYNC,
                supportiveReason
            )
        } catch (e: Exception) {
            android.util.Log.w("ProgressCache", "Failed to mark data for sync", e)
        }
    }
    
    /**
     * Gets all data that needs synchronization
     */
    suspend fun getDataNeedingSync(): List<WeeklyProgressCacheEntity> {
        return try {
            progressDatabase.weeklyProgressDao().getDataNeedingSync()
        } catch (e: Exception) {
            android.util.Log.w("ProgressCache", "Failed to get data needing sync", e)
            emptyList()
        }
    }
    
    /**
     * Clears old cached data while preserving recent progress
     */
    suspend fun clearOldCache(keepRecentWeeks: Int = 4) {
        try {
            val cutoffDate = LocalDate.now().minusWeeks(keepRecentWeeks.toLong())
            progressDatabase.weeklyProgressDao().deleteOldData(cutoffDate)
            progressDatabase.dailyProgressDao().deleteOldData(cutoffDate)
        } catch (e: Exception) {
            android.util.Log.w("ProgressCache", "Failed to clear old cache", e)
        }
    }
    
    /**
     * Clears all cached progress data
     */
    suspend fun clearWeeklyProgress() {
        try {
            progressDatabase.weeklyProgressDao().deleteAll()
            progressDatabase.dailyProgressDao().deleteAll()
        } catch (e: Exception) {
            android.util.Log.w("ProgressCache", "Failed to clear progress cache", e)
        }
    }
    
    /**
     * Gets cache statistics for supportive user information
     */
    suspend fun getCacheStatistics(): CacheStatistics {
        return try {
            val totalWeeks = progressDatabase.weeklyProgressDao().getTotalWeeksCount()
            val syncedWeeks = progressDatabase.weeklyProgressDao().getSyncedWeeksCount()
            val needsSyncWeeks = progressDatabase.weeklyProgressDao().getNeedsSyncWeeksCount()
            val oldestData = progressDatabase.weeklyProgressDao().getOldestDataDate()
            val newestData = progressDatabase.weeklyProgressDao().getNewestDataDate()
            
            CacheStatistics(
                totalWeeksCached = totalWeeks,
                syncedWeeks = syncedWeeks,
                needsSyncWeeks = needsSyncWeeks,
                oldestDataDate = oldestData,
                newestDataDate = newestData,
                supportiveMessage = createCacheStatisticsMessage(totalWeeks, syncedWeeks, needsSyncWeeks)
            )
        } catch (e: Exception) {
            android.util.Log.w("ProgressCache", "Failed to get cache statistics", e)
            CacheStatistics(
                totalWeeksCached = 0,
                syncedWeeks = 0,
                needsSyncWeeks = 0,
                oldestDataDate = null,
                newestDataDate = null,
                supportiveMessage = "Your progress cache is ready to store your wellness journey!"
            )
        }
    }
    
    /**
     * Helper methods for creating supportive messaging
     */
    private fun createSupportiveCacheMessage(lastUpdated: Long, syncStatus: SyncStatus): String {
        val timeSinceUpdate = System.currentTimeMillis() - lastUpdated
        val hoursAgo = timeSinceUpdate / (1000 * 60 * 60)
        
        return when (syncStatus) {
            SyncStatus.SYNCED -> when {
                hoursAgo < 1 -> "Your progress is up to date and ready to inspire your wellness journey!"
                hoursAgo < 24 -> "Your progress was last synced ${hoursAgo} hours ago. Your wellness data is safe with us!"
                else -> "Your progress is safely stored locally. We'll sync with the latest data when you're connected!"
            }
            SyncStatus.NEEDS_SYNC -> "Your local progress is ready to sync when you're back online. Nothing is lost!"
            SyncStatus.SYNCING -> "We're updating your progress data right now. Your wellness journey continues!"
            SyncStatus.PENDING_SYNC -> "Your progress updates are queued for synchronization. Everything is safely stored!"
            SyncStatus.SYNC_FAILED -> "We'll keep trying to sync your progress. Your local data is safe and complete!"
            SyncStatus.OFFLINE -> "Working offline - your progress tracking continues seamlessly!"
        }
    }
    
    private fun createCacheStatisticsMessage(total: Int, synced: Int, needsSync: Int): String {
        return when {
            total == 0 -> "Your progress cache is ready to store your wellness journey!"
            needsSync == 0 -> "All $total weeks of your progress data are perfectly synced!"
            needsSync == 1 -> "You have $total weeks of progress data, with 1 week ready to sync when online!"
            else -> "You have $total weeks of progress data, with $needsSync weeks ready to sync when online!"
        }
    }
    
    private fun createMockGoalAchievements(daily: DailyProgressCacheEntity): GoalAchievements {
        // This would integrate with real goal data - for now return mock
        return GoalAchievements(
            stepsGoalAchieved = daily.steps >= 10000,
            caloriesGoalAchieved = daily.calories >= 2000.0,
            heartPointsGoalAchieved = daily.heartPoints >= 30,
            stepsProgress = (daily.steps / 10000f).coerceAtMost(1f),
            caloriesProgress = (daily.calories / 2000.0).coerceAtMost(1.0).toFloat(),
            heartPointsProgress = (daily.heartPoints / 30f).coerceAtMost(1f)
        )
    }
    
    private fun createWeeklyTotals(weeklyCache: WeeklyProgressCacheEntity): WeeklyTotals {
        return WeeklyTotals(
            totalSteps = weeklyCache.totalSteps,
            totalCalories = weeklyCache.totalCalories,
            totalHeartPoints = weeklyCache.totalHeartPoints,
            activeDays = weeklyCache.activeDays,
            averageStepsPerDay = if (weeklyCache.activeDays > 0) weeklyCache.totalSteps / weeklyCache.activeDays else 0,
            averageCaloriesPerDay = if (weeklyCache.activeDays > 0) weeklyCache.totalCalories / weeklyCache.activeDays else 0.0,
            averageHeartPointsPerDay = if (weeklyCache.activeDays > 0) weeklyCache.totalHeartPoints / weeklyCache.activeDays else 0,
            supportiveWeeklySummary = "Your cached progress shows dedication to your wellness journey!"
        )
    }
    
    private fun createSupportiveInsights(weeklyCache: WeeklyProgressCacheEntity): SupportiveInsights {
        // This would create real insights - for now return encouraging placeholder
        return SupportiveInsights(
            weeklyTrends = emptyList(),
            achievements = emptyList(),
            gentleGuidance = emptyList(),
            wellnessJourneyContext = "Your wellness journey continues with every step, online or offline!",
            motivationalMessage = "Your progress is safely stored and ready to inspire your continued wellness journey!"
        )
    }
    
    /**
     * Clears all cached progress data
     */
    suspend fun clearCache() {
        progressDatabase.weeklyProgressDao().deleteAll()
        progressDatabase.dailyProgressDao().deleteAll()
    }
}



/**
 * Data class for cached progress result
 */
data class CachedProgressResult(
    val data: WeeklyProgressData,
    val lastUpdated: Long,
    val syncStatus: SyncStatus,
    val supportiveMessage: String,
    val encouragingContext: String
)

/**
 * Data class for cache statistics
 */
data class CacheStatistics(
    val totalWeeksCached: Int,
    val syncedWeeks: Int,
    val needsSyncWeeks: Int,
    val oldestDataDate: LocalDate?,
    val newestDataDate: LocalDate?,
    val supportiveMessage: String
)