package com.vibehealth.android.ui.dashboard.models

import androidx.annotation.ColorInt

/**
 * UI-specific data model for rendering rings in the TripleRingView.
 * Contains all information needed for visual display and accessibility.
 */
data class RingDisplayData(
    val ringType: RingType,
    val progress: Float, // 0.0 to 1.0
    val currentValue: String,
    val targetValue: String,
    @ColorInt val color: Int,
    val isAnimating: Boolean,
    val accessibilityLabel: String
) {
    companion object {
        /**
         * Creates RingDisplayData from ProgressData for UI rendering.
         */
        fun fromProgressData(progressData: ProgressData, isAnimating: Boolean = false): RingDisplayData {
            return RingDisplayData(
                ringType = progressData.ringType,
                progress = progressData.percentage,
                currentValue = progressData.getCurrentValueString(),
                targetValue = progressData.getTargetValueString(),
                color = progressData.progressColor,
                isAnimating = isAnimating,
                accessibilityLabel = progressData.getAccessibilityDescription()
            )
        }
    }
    
    /**
     * Returns the ring position index for drawing (0, 1, 2).
     */
    fun getRingPosition(): Int {
        return when (ringType) {
            RingType.STEPS -> 0      // Outermost ring
            RingType.CALORIES -> 1   // Middle ring
            RingType.HEART_POINTS -> 2 // Innermost ring
        }
    }
    
    /**
     * Returns progress as percentage integer for display.
     */
    fun getProgressPercentage(): Int {
        return (progress * 100).toInt().coerceIn(0, 100)
    }
}