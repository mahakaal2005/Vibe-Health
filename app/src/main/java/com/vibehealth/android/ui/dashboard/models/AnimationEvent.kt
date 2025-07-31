package com.vibehealth.android.ui.dashboard.models

/**
 * Represents animation events that should be triggered in the dashboard UI.
 * Used to coordinate animations between ViewModel and View components.
 */
sealed class AnimationEvent {
    /**
     * Progress has updated and rings should animate to new values.
     */
    data class ProgressUpdate(val changes: List<ProgressChange>) : AnimationEvent()
    
    /**
     * One or more goals have been achieved and should be celebrated.
     */
    data class GoalAchieved(val achievedRings: List<RingType>) : AnimationEvent()
    
    /**
     * Dashboard data has been refreshed and should show refresh animation.
     */
    object DataRefreshed : AnimationEvent()
}

/**
 * Represents a change in progress for a specific ring type.
 */
data class ProgressChange(
    val ringType: RingType,
    val fromProgress: Float,
    val toProgress: Float
) {
    /**
     * Returns true if this represents a significant progress change worth animating.
     */
    fun isSignificantChange(): Boolean {
        return kotlin.math.abs(toProgress - fromProgress) >= 0.01f // 1% threshold
    }
}