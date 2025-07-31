package com.vibehealth.android.domain.dashboard

import android.util.Log
import com.vibehealth.android.data.dashboard.DashboardRepository
import com.vibehealth.android.data.goals.GoalRepository
import com.vibehealth.android.ui.dashboard.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DashboardUseCase that orchestrates data from multiple sources.
 * Combines goal data from Story 1.3 with current activity progress to provide
 * complete dashboard state information.
 */
@Singleton
class DashboardUseCaseImpl @Inject constructor(
    private val goalRepository: GoalRepository,
    private val dashboardRepository: DashboardRepository
) : DashboardUseCase {
    
    companion object {
        private const val TAG = "DashboardUseCase"
    }
    
    /**
     * Gets real-time dashboard data for the specified user.
     * Combines goal data from Story 1.3 with current activity progress.
     */
    override fun getDashboardData(userId: String): Flow<DashboardState> = flow {
        emit(DashboardState.loading())
        
        try {
            // Get goals from Story 1.3 GoalRepository
            goalRepository.getCurrentGoals(userId).collect { goals ->
                if (goals == null) {
                    emit(DashboardState.empty())
                    return@collect
                }
                
                // Get current activity progress
                dashboardRepository.getCurrentDayProgress(userId).collect { progress ->
                    // Calculate progress based on goals
                    val calculatedProgress = calculateProgress(goals, progress)
                    
                    // Create loaded dashboard state
                    val dashboardState = DashboardState.loaded(goals, calculatedProgress)
                    emit(dashboardState)
                    
                    // Cache the data for offline support
                    dashboardRepository.cacheDashboardData(userId, calculatedProgress)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get dashboard data for user: $userId", e)
            
            // Try to get cached data for offline support
            val cachedProgress = dashboardRepository.getCachedDashboardData(userId)
            if (cachedProgress != null) {
                val cachedGoals = goalRepository.getCurrentGoalsSync(userId)
                if (cachedGoals != null) {
                    emit(DashboardState.loaded(cachedGoals, cachedProgress))
                } else {
                    emit(DashboardState.error(e))
                }
            } else {
                emit(DashboardState.error(e))
            }
        }
    }
    
    /**
     * Refreshes dashboard data by clearing cache and fetching fresh data.
     */
    override suspend fun refreshDashboardData(userId: String) {
        try {
            // Clear cached data to force refresh
            dashboardRepository.clearCachedData(userId)
            Log.d(TAG, "Refreshed dashboard data for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh dashboard data for user: $userId", e)
            throw e
        }
    }
    
    /**
     * Calculates progress data by combining goals with current activity data.
     * Maps the three wellness metrics to their respective ring types.
     */
    private fun calculateProgress(
        goals: com.vibehealth.android.domain.goals.DailyGoals,
        currentProgress: DailyProgress
    ): DailyProgress {
        
        // Get current values from the progress (these would come from activity data)
        val currentSteps = currentProgress.stepsProgress.current
        val currentCalories = currentProgress.caloriesProgress.current
        val currentHeartPoints = currentProgress.heartPointsProgress.current
        
        // Calculate progress data for each ring type
        val stepsProgress = ProgressData(
            ringType = RingType.STEPS,
            current = currentSteps,
            target = goals.stepsGoal,
            percentage = (currentSteps.toFloat() / goals.stepsGoal).coerceAtMost(1f),
            isGoalAchieved = currentSteps >= goals.stepsGoal,
            progressColor = RingType.STEPS.getDefaultColor()
        )
        
        val caloriesProgress = ProgressData(
            ringType = RingType.CALORIES,
            current = currentCalories,
            target = goals.caloriesGoal,
            percentage = (currentCalories.toFloat() / goals.caloriesGoal).coerceAtMost(1f),
            isGoalAchieved = currentCalories >= goals.caloriesGoal,
            progressColor = RingType.CALORIES.getDefaultColor()
        )
        
        val heartPointsProgress = ProgressData(
            ringType = RingType.HEART_POINTS,
            current = currentHeartPoints,
            target = goals.heartPointsGoal,
            percentage = (currentHeartPoints.toFloat() / goals.heartPointsGoal).coerceAtMost(1f),
            isGoalAchieved = currentHeartPoints >= goals.heartPointsGoal,
            progressColor = RingType.HEART_POINTS.getDefaultColor()
        )
        
        return DailyProgress(
            stepsProgress = stepsProgress,
            caloriesProgress = caloriesProgress,
            heartPointsProgress = heartPointsProgress
        )
    }
}