package com.vibehealth.android.ui.dashboard.models

import com.vibehealth.android.domain.goals.DailyGoals
import java.time.LocalDateTime

/**
 * Represents the complete state of the dashboard UI.
 * Contains goals, progress, loading state, and error information.
 */
data class DashboardState(
    val goals: DailyGoals?,
    val progress: DailyProgress,
    val loadingState: LoadingState,
    val errorState: ErrorState?,
    val lastUpdated: LocalDateTime
) {
    companion object {
        fun loading(): DashboardState {
            return DashboardState(
                goals = null,
                progress = DailyProgress.empty(),
                loadingState = LoadingState.LOADING,
                errorState = null,
                lastUpdated = LocalDateTime.now()
            )
        }
        
        fun loaded(goals: DailyGoals, progress: DailyProgress): DashboardState {
            return DashboardState(
                goals = goals,
                progress = progress,
                loadingState = LoadingState.LOADED,
                errorState = null,
                lastUpdated = LocalDateTime.now()
            )
        }
        
        fun empty(): DashboardState {
            return DashboardState(
                goals = null,
                progress = DailyProgress.empty(),
                loadingState = LoadingState.EMPTY,
                errorState = ErrorState.NoGoals("Complete your profile to see personalized goals"),
                lastUpdated = LocalDateTime.now()
            )
        }
        
        fun error(error: Throwable): DashboardState {
            return DashboardState(
                goals = null,
                progress = DailyProgress.empty(),
                loadingState = LoadingState.ERROR,
                errorState = ErrorState.Unknown(error.message ?: "Unknown error occurred"),
                lastUpdated = LocalDateTime.now()
            )
        }
    }
}