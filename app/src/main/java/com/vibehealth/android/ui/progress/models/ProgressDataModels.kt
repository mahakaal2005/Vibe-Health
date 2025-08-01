package com.vibehealth.android.ui.progress.models

/**
 * Additional data models for progress history with Companion Principle
 * 
 * These models support the progress history functionality with encouraging
 * context and supportive data transformation following the Companion Principle.
 */

/**
 * Achievement - Represents a wellness achievement with celebratory context
 */
data class Achievement(
    val type: AchievementType,
    val title: String,
    val description: String,
    val celebratoryMessage: String,
    val dateAchieved: java.time.LocalDate,
    val metricType: MetricType,
    val value: String
) {
    /**
     * Gets the full celebratory announcement
     */
    val fullCelebration: String
        get() = "$celebratoryMessage $description"
}

/**
 * DashboardData - Represents daily dashboard data from existing repository
 */
data class DashboardData(
    val steps: Int = 0,
    val calories: Double = 0.0,
    val heartPoints: Int = 0,
    val date: java.time.LocalDate = java.time.LocalDate.now()
) {
    /**
     * Indicates if there's any activity data
     */
    val hasActivity: Boolean
        get() = steps > 0 || calories > 0.0 || heartPoints > 0
}

/**
 * DailyGoals - Represents daily goals from existing goal system
 */
data class DailyGoals(
    val stepsGoal: Int = 10000,
    val caloriesGoal: Double = 2000.0,
    val heartPointsGoal: Int = 30
)

/**
 * WeeklyTrends - Analysis of weekly trends with supportive interpretation
 */
data class WeeklyTrends(
    val stepsImprovement: Float = 0f,
    val caloriesImprovement: Float = 0f,
    val heartPointsImprovement: Float = 0f,
    val consistencyScore: Float = 0f,
    val supportiveInterpretation: String = ""
) {
    /**
     * Indicates if overall trends show improvement
     */
    val showsImprovement: Boolean
        get() = stepsImprovement > 0 || caloriesImprovement > 0 || heartPointsImprovement > 0
    
    /**
     * Gets encouraging trend summary
     */
    val encouragingTrendSummary: String
        get() = when {
            showsImprovement -> "Your progress is trending upward! $supportiveInterpretation"
            consistencyScore > 0.7f -> "You're maintaining excellent consistency! $supportiveInterpretation"
            else -> "Every step on your wellness journey matters. $supportiveInterpretation"
        }
}

/**
 * MetricType - Enum for different wellness metrics
 */
enum class MetricType {
    STEPS,
    CALORIES,
    HEART_POINTS;
    
    /**
     * Gets display name for the metric
     */
    val displayName: String
        get() = when (this) {
            STEPS -> "Steps"
            CALORIES -> "Calories"
            HEART_POINTS -> "Heart Points"
        }
    
    /**
     * Gets encouraging description for the metric
     */
    val encouragingDescription: String
        get() = when (this) {
            STEPS -> "Every step moves you toward better health!"
            CALORIES -> "Your energy and effort are making a difference!"
            HEART_POINTS -> "Your cardiovascular health journey is important!"
        }
}

/**
 * AchievementType - Enum for different types of achievements
 */
enum class AchievementType {
    GOAL_ACHIEVED,
    GOAL_REACHED,
    STREAK_MILESTONE,
    CONSISTENCY_MILESTONE,
    IMPROVEMENT_STREAK,
    PERSONAL_BEST,
    CONSISTENCY_AWARD,
    WEEKLY_TARGET
}

/**
 * CelebrationType - Enum for different celebration intensities
 */
enum class CelebrationType {
    GENTLE,
    MODERATE,
    MAJOR
}