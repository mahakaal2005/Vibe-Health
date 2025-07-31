package com.vibehealth.android.data.dashboard

import com.vibehealth.android.ui.dashboard.models.DailyProgress
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for dashboard-specific data management.
 * Handles caching, offline support, and data freshness tracking.
 */
interface DashboardRepository {
    
    /**
     * Gets current daily progress for the user.
     * Combines data from various activity sources.
     * 
     * @param userId The user ID to get progress for
     * @return Flow of DailyProgress with real-time updates
     */
    fun getCurrentDayProgress(userId: String): Flow<DailyProgress>
    
    /**
     * Caches dashboard data locally with encryption.
     * 
     * @param userId The user ID
     * @param progress The progress data to cache
     */
    suspend fun cacheDashboardData(userId: String, progress: DailyProgress)
    
    /**
     * Gets cached dashboard data for offline support.
     * 
     * @param userId The user ID
     * @return Cached DailyProgress or null if not available
     */
    suspend fun getCachedDashboardData(userId: String): DailyProgress?
    
    /**
     * Clears cached data for the user.
     * 
     * @param userId The user ID
     */
    suspend fun clearCachedData(userId: String)
}