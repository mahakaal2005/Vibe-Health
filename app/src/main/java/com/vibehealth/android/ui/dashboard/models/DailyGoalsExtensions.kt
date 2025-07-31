package com.vibehealth.android.ui.dashboard.models

import com.vibehealth.android.domain.goals.DailyGoals

/**
 * Extension functions to convert Story 1.3 DailyGoals to dashboard format.
 * Provides seamless integration between goal calculation service and dashboard display.
 */

/**
 * Converts DailyGoals to DashboardGoals format for UI display.
 * Maps the three wellness metrics to dashboard-specific data structure.
 */
fun DailyGoals.toDashboardGoals(): DashboardGoals {
    return DashboardGoals(
        stepsTarget = this.stepsGoal,
        caloriesTarget = this.caloriesGoal,
        heartPointsTarget = this.heartPointsGoal,
        calculatedAt = this.calculatedAt,
        source = this.calculationSource.getDisplayName()
    )
}

/**
 * Creates empty progress data based on DailyGoals targets.
 * Useful for initializing dashboard state with zero progress.
 */
fun DailyGoals.toEmptyProgress(): DailyProgress {
    return DailyProgress(
        stepsProgress = ProgressData(
            ringType = RingType.STEPS,
            current = 0,
            target = this.stepsGoal,
            percentage = 0f,
            isGoalAchieved = false,
            progressColor = RingType.STEPS.getDefaultColor()
        ),
        caloriesProgress = ProgressData(
            ringType = RingType.CALORIES,
            current = 0,
            target = this.caloriesGoal,
            percentage = 0f,
            isGoalAchieved = false,
            progressColor = RingType.CALORIES.getDefaultColor()
        ),
        heartPointsProgress = ProgressData(
            ringType = RingType.HEART_POINTS,
            current = 0,
            target = this.heartPointsGoal,
            percentage = 0f,
            isGoalAchieved = false,
            progressColor = RingType.HEART_POINTS.getDefaultColor()
        )
    )
}

/**
 * Creates progress data with specified current values.
 * Used to update dashboard with real activity data.
 */
fun DailyGoals.toProgressWithValues(
    currentSteps: Int,
    currentCalories: Int,
    currentHeartPoints: Int
): DailyProgress {
    return DailyProgress(
        stepsProgress = ProgressData(
            ringType = RingType.STEPS,
            current = currentSteps,
            target = this.stepsGoal,
            percentage = (currentSteps.toFloat() / this.stepsGoal).coerceAtMost(1f),
            isGoalAchieved = currentSteps >= this.stepsGoal,
            progressColor = RingType.STEPS.getDefaultColor()
        ),
        caloriesProgress = ProgressData(
            ringType = RingType.CALORIES,
            current = currentCalories,
            target = this.caloriesGoal,
            percentage = (currentCalories.toFloat() / this.caloriesGoal).coerceAtMost(1f),
            isGoalAchieved = currentCalories >= this.caloriesGoal,
            progressColor = RingType.CALORIES.getDefaultColor()
        ),
        heartPointsProgress = ProgressData(
            ringType = RingType.HEART_POINTS,
            current = currentHeartPoints,
            target = this.heartPointsGoal,
            percentage = (currentHeartPoints.toFloat() / this.heartPointsGoal).coerceAtMost(1f),
            isGoalAchieved = currentHeartPoints >= this.heartPointsGoal,
            progressColor = RingType.HEART_POINTS.getDefaultColor()
        )
    )
}

/**
 * Dashboard-specific goals data structure.
 * Simplified version of DailyGoals for UI display purposes.
 */
data class DashboardGoals(
    val stepsTarget: Int,
    val caloriesTarget: Int,
    val heartPointsTarget: Int,
    val calculatedAt: java.time.LocalDateTime,
    val source: String
)