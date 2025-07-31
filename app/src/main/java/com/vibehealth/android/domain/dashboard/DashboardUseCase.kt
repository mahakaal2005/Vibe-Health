package com.vibehealth.android.domain.dashboard

import com.vibehealth.android.ui.dashboard.models.DashboardState
import kotlinx.coroutines.flow.Flow

/**
 * Use case interface for dashboard data operations.
 * Orchestrates data from GoalRepository and ActivityRepository to provide
 * complete dashboard state information.
 */
interface DashboardUseCase {
    
    /**
     * Gets real-time dashboard data for the specified user.
     * Combines goal data from Story 1.3 with current activity progress.
     * 
     * @param userId The user ID to get dashboard data for
     * @return Flow of DashboardState with real-time updates
     */
    fun getDashboardData(userId: String): Flow<DashboardState>
    
    /**
     * Refreshes dashboard data by clearing cache and fetching fresh data.
     * 
     * @param userId The user ID to refresh data for
     */
    suspend fun refreshDashboardData(userId: String)
}