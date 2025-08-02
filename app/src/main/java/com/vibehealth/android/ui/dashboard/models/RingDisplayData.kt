package com.vibehealth.android.ui.dashboard.models

/**
 * VIBE_FIX: Phase 3 - Display data for individual rings in the TripleRingView
 */
data class RingDisplayData(
    val ringType: RingType,
    val progress: Float,
    val currentValue: String,
    val targetValue: String,
    val color: Int,
    val isAnimating: Boolean = false,
    val accessibilityLabel: String
) {
    fun getRingPosition(): Int {
        return when (ringType) {
            RingType.STEPS -> 0
            RingType.CALORIES -> 1
            RingType.HEART_POINTS -> 2
        }
    }
    
    companion object {
        fun fromProgressData(progressData: ProgressData): RingDisplayData {
            return RingDisplayData(
                ringType = progressData.ringType,
                progress = progressData.percentage,
                currentValue = progressData.getCurrentValueString(),
                targetValue = progressData.getTargetValueString(),
                color = progressData.progressColor,
                accessibilityLabel = progressData.getAccessibilityDescription()
            )
        }
    }
}