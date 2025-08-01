package com.vibehealth.android.ui.progress.models

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

/**
 * WeeklyProgressData - Core data model for weekly progress with Companion Principle
 * 
 * This data class represents a week's worth of wellness progress data,
 * transformed with supportive insights and encouraging context following
 * the Companion Principle. Designed to celebrate user achievements and
 * provide gentle guidance for continued wellness engagement.
 */
data class WeeklyProgressData(
    val weekStartDate: LocalDate,
    val dailyData: List<DailyProgressData>,
    val weeklyTotals: WeeklyTotals,
    val supportiveInsights: SupportiveInsights,
    val celebratoryMessages: List<String> = emptyList()
) {
    /**
     * Indicates if the week has any recorded activity data
     */
    val hasAnyData: Boolean
        get() = dailyData.any { it.hasActivity }
    
    /**
     * Gets encouraging weekly summary with supportive tone
     */
    val encouragingWeekSummary: String
        get() = supportiveInsights.generateWeeklySummary()
    
    /**
     * Gets the week date range in user-friendly format
     */
    val weekDateRange: String
        get() {
            val endDate = weekStartDate.plusDays(6)
            return "${weekStartDate.monthValue}/${weekStartDate.dayOfMonth} - ${endDate.monthValue}/${endDate.dayOfMonth}"
        }
    
    /**
     * Gets steps data with supportive context for graph display
     */
    fun getStepsData(): List<DailyMetricData> {
        return dailyData.map { daily ->
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
     * Gets calories data with supportive context for graph display
     */
    fun getCaloriesData(): List<DailyMetricData> {
        return dailyData.map { daily ->
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
     * Gets heart points data with supportive context for graph display
     */
    fun getHeartPointsData(): List<DailyMetricData> {
        return dailyData.map { daily ->
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
    
    /**
     * Gets supportive message for steps metric
     */
    fun getStepsSupportiveMessage(): String {
        val totalSteps = weeklyTotals.totalSteps
        val activeDays = weeklyTotals.activeDays
        
        return when {
            totalSteps >= 70000 -> "Outstanding! You've taken over 70,000 steps this week. Your commitment to movement is inspiring!"
            totalSteps >= 50000 -> "Excellent work! Over 50,000 steps shows your dedication to staying active."
            totalSteps >= 30000 -> "Great progress! You've been consistently moving with over 30,000 steps this week."
            totalSteps >= 10000 -> "Good job staying active! Every step contributes to your wellness journey."
            totalSteps > 0 -> "Every step counts! You're building healthy movement habits."
            else -> "Ready to start stepping toward better health? Every journey begins with a single step!"
        }
    }
    
    /**
     * Gets supportive message for calories metric
     */
    fun getCaloriesSupportiveMessage(): String {
        val totalCalories = weeklyTotals.totalCalories
        val activeDays = weeklyTotals.activeDays
        
        return when {
            totalCalories >= 14000 -> "Amazing energy expenditure! You've burned over 14,000 calories this week through your activities."
            totalCalories >= 10000 -> "Fantastic effort! Your consistent activity has burned over 10,000 calories this week."
            totalCalories >= 7000 -> "Great work! You've maintained good energy balance with over 7,000 calories burned."
            totalCalories >= 3500 -> "Good progress! Your activities are contributing to your overall wellness goals."
            totalCalories > 0 -> "Every calorie burned through activity is progress toward better health!"
            else -> "Ready to energize your wellness journey? Every bit of movement makes a difference!"
        }
    }
    
    /**
     * Gets supportive message for heart points metric
     */
    fun getHeartPointsSupportiveMessage(): String {
        val totalHeartPoints = weeklyTotals.totalHeartPoints
        val activeDays = weeklyTotals.activeDays
        
        return when {
            totalHeartPoints >= 150 -> "Exceptional cardiovascular commitment! You've earned over 150 heart points this week."
            totalHeartPoints >= 100 -> "Outstanding heart health focus! Over 100 heart points shows excellent cardiovascular activity."
            totalHeartPoints >= 50 -> "Great cardiovascular progress! Your heart is getting stronger with each activity."
            totalHeartPoints >= 20 -> "Good heart-healthy activities! You're building cardiovascular fitness."
            totalHeartPoints > 0 -> "Every heart point earned strengthens your cardiovascular health!"
            else -> "Ready to give your heart some love? Heart-healthy activities await!"
        }
    }
    
    /**
     * Gets the most active day of the week with celebratory context
     */
    fun getMostActiveDay(): DailyProgressData? {
        return dailyData.maxByOrNull { it.getTotalActivityScore() }
    }
    
    /**
     * Gets achievement summary with encouraging tone
     */
    fun getAchievementSummary(): String {
        val goalAchievementDays = dailyData.count { day ->
            day.goalAchievements.stepsGoalAchieved || 
            day.goalAchievements.caloriesGoalAchieved || 
            day.goalAchievements.heartPointsGoalAchieved
        }
        
        return when {
            goalAchievementDays >= 6 -> "Incredible! You achieved wellness goals on $goalAchievementDays days this week!"
            goalAchievementDays >= 4 -> "Excellent! You reached your goals on $goalAchievementDays days this week!"
            goalAchievementDays >= 2 -> "Great work! You achieved goals on $goalAchievementDays days this week!"
            goalAchievementDays == 1 -> "Nice! You reached your wellness goals on 1 day this week!"
            else -> "Every day is a new opportunity to reach your wellness goals!"
        }
    }
    
    /**
     * Gets consistency message with supportive encouragement
     */
    fun getConsistencyMessage(): String {
        val activeDays = weeklyTotals.activeDays
        
        return when {
            activeDays == 7 -> "Perfect consistency! You were active every single day this week. You're building incredible healthy habits!"
            activeDays >= 5 -> "Outstanding consistency! You were active on $activeDays days this week. Your dedication shows!"
            activeDays >= 3 -> "Good consistency! You maintained activity on $activeDays days this week. Keep building those healthy habits!"
            activeDays >= 1 -> "You took steps toward wellness on $activeDays days this week. Every bit of activity matters!"
            else -> "This week is a fresh start! Every day offers new opportunities for wellness activities."
        }
    }
}

/**
 * DailyProgressData - Individual day's progress with supportive context
 */
data class DailyProgressData(
    val date: LocalDate,
    val steps: Int = 0,
    val calories: Double = 0.0,
    val heartPoints: Int = 0,
    val goalAchievements: GoalAchievements = GoalAchievements(),
    val supportiveContext: String = ""
) {
    /**
     * Indicates if the day has any recorded activity
     */
    val hasActivity: Boolean
        get() = steps > 0 || calories > 0.0 || heartPoints > 0
    
    /**
     * Gets encouraging day summary with supportive tone
     */
    val encouragingDaySummary: String
        get() = generateSupportiveDaySummary()
    
    /**
     * Gets day of week in user-friendly format
     */
    val dayOfWeekDisplay: String
        get() = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    
    /**
     * Gets total activity score for comparison
     */
    fun getTotalActivityScore(): Float {
        return (steps * 0.001f) + (calories * 0.1f).toFloat() + (heartPoints * 10f)
    }
    
    /**
     * Generates supportive day summary with encouraging tone
     */
    private fun generateSupportiveDaySummary(): String {
        return when {
            goalAchievements.allGoalsAchieved -> "Perfect day! You achieved all your wellness goals on ${dayOfWeekDisplay}!"
            goalAchievements.anyGoalAchieved -> "Great day! You reached some of your wellness goals on ${dayOfWeekDisplay}!"
            hasActivity -> "Good activity on ${dayOfWeekDisplay}! Every bit of movement contributes to your wellness journey."
            else -> "A rest day on ${dayOfWeekDisplay} - important for recovery and balance in your wellness journey!"
        }
    }
    
    /**
     * Generates supportive label for steps metric
     */
    fun generateStepsSupportiveLabel(): String {
        return when {
            steps >= 15000 -> "Amazing! $steps steps"
            steps >= 10000 -> "Excellent! $steps steps"
            steps >= 7500 -> "Great! $steps steps"
            steps >= 5000 -> "Good! $steps steps"
            steps > 0 -> "$steps steps"
            else -> "Rest day"
        }
    }
    
    /**
     * Generates supportive label for calories metric
     */
    fun generateCaloriesSupportiveLabel(): String {
        val caloriesInt = calories.toInt()
        return when {
            calories >= 3000 -> "Outstanding! $caloriesInt cal"
            calories >= 2000 -> "Excellent! $caloriesInt cal"
            calories >= 1500 -> "Great! $caloriesInt cal"
            calories >= 1000 -> "Good! $caloriesInt cal"
            calories > 0 -> "$caloriesInt cal"
            else -> "Rest day"
        }
    }
    
    /**
     * Generates supportive label for heart points metric
     */
    fun generateHeartPointsSupportiveLabel(): String {
        return when {
            heartPoints >= 50 -> "Amazing! $heartPoints pts"
            heartPoints >= 30 -> "Excellent! $heartPoints pts"
            heartPoints >= 20 -> "Great! $heartPoints pts"
            heartPoints >= 10 -> "Good! $heartPoints pts"
            heartPoints > 0 -> "$heartPoints pts"
            else -> "Rest day"
        }
    }
}

/**
 * DailyMetricData - Formatted data for graph display with supportive context
 */
data class DailyMetricData(
    val date: LocalDate,
    val value: Float,
    val displayValue: String,
    val supportiveLabel: String,
    val isGoalAchieved: Boolean,
    val progressPercentage: Float
) {
    /**
     * Gets accessibility description for the data point
     */
    fun getAccessibilityDescription(): String {
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val achievementText = if (isGoalAchieved) "Goal achieved!" else "Progress: ${(progressPercentage * 100).toInt()}%"
        return "$dayName: $supportiveLabel. $achievementText"
    }
}

/**
 * GoalAchievements - Goal achievement status with progress tracking
 */
data class GoalAchievements(
    val stepsGoalAchieved: Boolean = false,
    val caloriesGoalAchieved: Boolean = false,
    val heartPointsGoalAchieved: Boolean = false,
    val stepsProgress: Float = 0f,
    val caloriesProgress: Float = 0f,
    val heartPointsProgress: Float = 0f
) {
    /**
     * Indicates if any goal was achieved
     */
    val anyGoalAchieved: Boolean
        get() = stepsGoalAchieved || caloriesGoalAchieved || heartPointsGoalAchieved
    
    /**
     * Indicates if all goals were achieved
     */
    val allGoalsAchieved: Boolean
        get() = stepsGoalAchieved && caloriesGoalAchieved && heartPointsGoalAchieved
    
    /**
     * Gets count of achieved goals
     */
    val achievedGoalsCount: Int
        get() = listOf(stepsGoalAchieved, caloriesGoalAchieved, heartPointsGoalAchieved).count { it }
    
    /**
     * Gets overall progress percentage
     */
    val overallProgress: Float
        get() = (stepsProgress + caloriesProgress + heartPointsProgress) / 3f
}

/**
 * WeeklyTotals - Aggregated weekly data with supportive summaries
 */
data class WeeklyTotals(
    val totalSteps: Int,
    val totalCalories: Double,
    val totalHeartPoints: Int,
    val activeDays: Int,
    val averageStepsPerDay: Int,
    val averageCaloriesPerDay: Double,
    val averageHeartPointsPerDay: Int,
    val supportiveWeeklySummary: String
) {
    /**
     * Gets formatted total steps with supportive context
     */
    val formattedTotalSteps: String
        get() = when {
            totalSteps >= 100000 -> "${totalSteps / 1000}K+ steps - Incredible!"
            totalSteps >= 10000 -> "${String.format("%,d", totalSteps)} steps - Excellent!"
            totalSteps > 0 -> "${String.format("%,d", totalSteps)} steps - Great progress!"
            else -> "Ready to start stepping!"
        }
    
    /**
     * Gets formatted total calories with supportive context
     */
    val formattedTotalCalories: String
        get() = when {
            totalCalories >= 20000 -> "${totalCalories.toInt() / 1000}K+ cal - Outstanding!"
            totalCalories >= 5000 -> "${String.format("%,d", totalCalories.toInt())} cal - Excellent!"
            totalCalories > 0 -> "${totalCalories.toInt()} cal - Good work!"
            else -> "Ready to energize!"
        }
    
    /**
     * Gets formatted total heart points with supportive context
     */
    val formattedTotalHeartPoints: String
        get() = when {
            totalHeartPoints >= 200 -> "$totalHeartPoints pts - Amazing!"
            totalHeartPoints >= 100 -> "$totalHeartPoints pts - Excellent!"
            totalHeartPoints > 0 -> "$totalHeartPoints pts - Great!"
            else -> "Ready for heart health!"
        }
}