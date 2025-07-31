package com.vibehealth.android.ui.dashboard.models

import androidx.annotation.ColorInt

/**
 * Represents progress data for a single ring type.
 * Contains current/target values, percentage, and animation state.
 */
data class ProgressData(
    val ringType: RingType,
    val current: Int,
    val target: Int,
    val percentage: Float,
    val isGoalAchieved: Boolean,
    @ColorInt val progressColor: Int,
    val animationProgress: Float = 0f
) {
    companion object {
        fun empty(ringType: RingType): ProgressData {
            return ProgressData(
                ringType = ringType,
                current = 0,
                target = 1, // Avoid division by zero
                percentage = 0f,
                isGoalAchieved = false,
                progressColor = 0xFF6B8E6B.toInt(), // Default sage green
                animationProgress = 0f
            )
        }
    }
    
    /**
     * Returns formatted current value string for display.
     */
    fun getCurrentValueString(): String {
        return when (ringType) {
            RingType.STEPS -> current.toString()
            RingType.CALORIES -> current.toString()
            RingType.HEART_POINTS -> current.toString()
        }
    }
    
    /**
     * Returns formatted target value string for display.
     */
    fun getTargetValueString(): String {
        return when (ringType) {
            RingType.STEPS -> target.toString()
            RingType.CALORIES -> target.toString()
            RingType.HEART_POINTS -> target.toString()
        }
    }
    
    /**
     * Returns percentage as integer for display (0-100).
     */
    fun getPercentageInt(): Int {
        return (percentage * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Returns accessibility description for screen readers.
     */
    fun getAccessibilityDescription(): String {
        return "${ringType.displayName}: ${getCurrentValueString()} of ${getTargetValueString()} ${ringType.unit}, ${getPercentageInt()} percent complete"
    }
}