package com.vibehealth.android.ui.dashboard.models

/**
 * Contains progress data for all three ring types in the dashboard.
 * Represents the current daily progress toward wellness goals.
 */
data class DailyProgress(
    val stepsProgress: ProgressData,
    val caloriesProgress: ProgressData,
    val heartPointsProgress: ProgressData
) {
    companion object {
        fun empty(): DailyProgress {
            return DailyProgress(
                stepsProgress = ProgressData.empty(RingType.STEPS),
                caloriesProgress = ProgressData.empty(RingType.CALORIES),
                heartPointsProgress = ProgressData.empty(RingType.HEART_POINTS)
            )
        }
    }
    
    /**
     * Returns progress data for a specific ring type.
     */
    fun getProgressForRing(ringType: RingType): ProgressData {
        return when (ringType) {
            RingType.STEPS -> stepsProgress
            RingType.CALORIES -> caloriesProgress
            RingType.HEART_POINTS -> heartPointsProgress
        }
    }
    
    /**
     * Returns all progress data as a list for iteration.
     */
    fun getAllProgress(): List<ProgressData> {
        return listOf(stepsProgress, caloriesProgress, heartPointsProgress)
    }
}