package com.vibehealth.android.ui.progress.models

import java.time.LocalDate

/**
 * MONTHLY EXTENSION: Monthly progress data model extending existing patterns
 * Created in existing data/progress/models/ directory
 */
data class MonthlyProgressData(
    val month: String,
    val year: Int,
    val dailyEntries: List<DailyProgressData>, // REUSE EXISTING MODEL
    val totalSteps: Int,
    val averageSteps: Int,
    val trendDirection: TrendDirection, // REUSE EXISTING ENUM
    val achievements: List<Achievement>, // REUSE EXISTING MODEL
    val insights: SupportiveInsights, // REUSE EXISTING MODEL
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        // Use existing expiration logic patterns
        return System.currentTimeMillis() - lastUpdated > CACHE_EXPIRY_TIME
    }
    
    val hasAnyData: Boolean
        get() = dailyEntries.isNotEmpty() && dailyEntries.any { it.steps > 0 }
    
    /**
     * MONTHLY EXTENSION: Converts monthly data to graph-compatible format
     */
    fun getStepsData(): List<DailyMetricData> {
        return dailyEntries.map { daily ->
            DailyMetricData(
                date = daily.date,
                value = daily.steps.toFloat(),
                displayValue = daily.steps.toString(),
                supportiveLabel = daily.generateStepsSupportiveLabel(),
                isGoalAchieved = daily.goalAchievements.stepsGoalAchieved,
                progressPercentage = daily.goalAchievements.stepsProgress
            )
        }
    }
    
    /**
     * MONTHLY EXTENSION: Gets calories data for graph display
     */
    fun getCaloriesData(): List<DailyMetricData> {
        return dailyEntries.map { daily ->
            DailyMetricData(
                date = daily.date,
                value = daily.calories.toFloat(),
                displayValue = "${daily.calories.toInt()} cal",
                supportiveLabel = daily.generateCaloriesSupportiveLabel(),
                isGoalAchieved = daily.goalAchievements.caloriesGoalAchieved,
                progressPercentage = daily.goalAchievements.caloriesProgress
            )
        }
    }
    
    /**
     * MONTHLY EXTENSION: Gets heart points data for graph display
     */
    fun getHeartPointsData(): List<DailyMetricData> {
        return dailyEntries.map { daily ->
            DailyMetricData(
                date = daily.date,
                value = daily.heartPoints.toFloat(),
                displayValue = "${daily.heartPoints} pts",
                supportiveLabel = daily.generateHeartPointsSupportiveLabel(),
                isGoalAchieved = daily.goalAchievements.heartPointsGoalAchieved,
                progressPercentage = daily.goalAchievements.heartPointsProgress
            )
        }
    }
    
    companion object {
        private const val CACHE_EXPIRY_TIME = 5 * 60 * 1000L // 5 minutes
    }
}
