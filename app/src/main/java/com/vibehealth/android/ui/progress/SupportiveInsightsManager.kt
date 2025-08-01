package com.vibehealth.android.ui.progress

import com.vibehealth.android.ui.progress.models.*
import com.vibehealth.android.ui.progress.models.GuidanceType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SupportiveInsightsManager - Generates encouraging insights for progress data
 * 
 * This class transforms raw wellness data into supportive, encouraging insights
 * that celebrate user achievements and provide gentle guidance following the
 * Companion Principle. All messaging maintains a warm, supportive tone.
 */
@Singleton
class SupportiveInsightsManager @Inject constructor() {
    
    /**
     * Generates supportive insights for weekly progress data
     */
    fun generateSupportiveInsights(weeklyData: WeeklyProgressData): SupportiveInsights {
        val trends = analyzeWeeklyTrends(weeklyData)
        val achievements = extractAchievements(weeklyData)
        val guidance = generateGentleGuidance(weeklyData)
        val journeyContext = createWellnessJourneyContext(weeklyData)
        val motivationalMessage = createMotivationalMessage(weeklyData, achievements)
        
        return SupportiveInsights(
            weeklyTrends = trends,
            achievements = achievements,
            gentleGuidance = guidance,
            wellnessJourneyContext = journeyContext,
            motivationalMessage = motivationalMessage
        )
    }
    
    /**
     * Analyzes weekly trends with encouraging interpretation
     */
    fun analyzeWeeklyTrends(weeklyData: WeeklyProgressData): List<EncouragingTrend> {
        val trends = mutableListOf<EncouragingTrend>()
        
        // Analyze steps trend
        val stepsData = weeklyData.dailyData.map { it.steps.toFloat() }
        val stepsTrend = calculateTrendDirection(stepsData)
        trends.add(
            EncouragingTrend(
                metricName = "Steps",
                trendDirection = stepsTrend,
                changePercentage = calculateChangePercentage(stepsData),
                supportiveInterpretation = createStepsTrendInterpretation(stepsTrend)
            )
        )
        
        // Analyze calories trend
        val caloriesData = weeklyData.dailyData.map { it.calories.toFloat() }
        val caloriesTrend = calculateTrendDirection(caloriesData)
        trends.add(
            EncouragingTrend(
                metricName = "Calories",
                trendDirection = caloriesTrend,
                changePercentage = calculateChangePercentage(caloriesData),
                supportiveInterpretation = createCaloriesTrendInterpretation(caloriesTrend)
            )
        )
        
        // Analyze heart points trend
        val heartPointsData = weeklyData.dailyData.map { it.heartPoints.toFloat() }
        val heartPointsTrend = calculateTrendDirection(heartPointsData)
        trends.add(
            EncouragingTrend(
                metricName = "Heart Points",
                trendDirection = heartPointsTrend,
                changePercentage = calculateChangePercentage(heartPointsData),
                supportiveInterpretation = createHeartPointsTrendInterpretation(heartPointsTrend)
            )
        )
        
        return trends
    }
    
    /**
     * Extracts achievements with celebratory messaging
     */
    fun extractAchievements(weeklyData: WeeklyProgressData): List<CelebratoryAchievement> {
        val achievements = mutableListOf<CelebratoryAchievement>()
        
        // Check for goal achievements
        weeklyData.dailyData.forEach { dailyData ->
            if (dailyData.goalAchievements.stepsGoalAchieved) {
                achievements.add(
                    CelebratoryAchievement(
                        achievementType = AchievementType.GOAL_ACHIEVED,
                        metricName = "Steps",
                        achievementValue = dailyData.steps.toString(),
                        celebratoryText = "🎉 Amazing! You reached your step goal!",
                        encouragingContext = "Your commitment to movement is inspiring!"
                    )
                )
            }
            
            if (dailyData.goalAchievements.caloriesGoalAchieved) {
                achievements.add(
                    CelebratoryAchievement(
                        achievementType = AchievementType.GOAL_ACHIEVED,
                        metricName = "Calories",
                        achievementValue = dailyData.calories.toInt().toString(),
                        celebratoryText = "🔥 Fantastic! You hit your calorie burn target!",
                        encouragingContext = "Your energy and effort are making a real difference!"
                    )
                )
            }
            
            if (dailyData.goalAchievements.heartPointsGoalAchieved) {
                achievements.add(
                    CelebratoryAchievement(
                        achievementType = AchievementType.GOAL_ACHIEVED,
                        metricName = "Heart Points",
                        achievementValue = dailyData.heartPoints.toString(),
                        celebratoryText = "❤️ Outstanding! You earned your heart points goal!",
                        encouragingContext = "Your cardiovascular health journey is remarkable!"
                    )
                )
            }
        }
        
        // Check for consistency achievements
        val activeDays = weeklyData.weeklyTotals.activeDays
        if (activeDays >= 5) {
            achievements.add(
                CelebratoryAchievement(
                    achievementType = AchievementType.CONSISTENCY_MILESTONE,
                    metricName = "Consistency",
                    achievementValue = "$activeDays days",
                    celebratoryText = "🌟 Incredible consistency!",
                    encouragingContext = "You were active on $activeDays days this week - you're building lasting healthy habits!"
                )
            )
        }
        
        return achievements
    }
    
    /**
     * Generates gentle guidance for continued progress
     */
    private fun generateGentleGuidance(weeklyData: WeeklyProgressData): List<SupportiveGuidance> {
        val guidance = mutableListOf<SupportiveGuidance>()
        
        val activeDays = weeklyData.weeklyTotals.activeDays
        val totalSteps = weeklyData.weeklyTotals.totalSteps
        
        when {
            activeDays == 0 -> {
                guidance.add(
                    SupportiveGuidance(
                        guidanceType = GuidanceType.GENTLE_ENCOURAGEMENT,
                        message = "Every wellness journey starts with a single step.",
                        actionSuggestion = "Consider taking a short walk today - even 5 minutes counts!",
                        encouragingContext = "You're at the perfect starting point for building healthy habits."
                    )
                )
            }
            activeDays < 3 -> {
                guidance.add(
                    SupportiveGuidance(
                        guidanceType = GuidanceType.CONSISTENCY_TIP,
                        message = "You've taken great first steps toward wellness!",
                        actionSuggestion = "Try adding one more active day this week.",
                        encouragingContext = "Small, consistent steps lead to lasting changes."
                    )
                )
            }
            totalSteps < 50000 -> {
                guidance.add(
                    SupportiveGuidance(
                        guidanceType = GuidanceType.ACTIVITY_SUGGESTION,
                        message = "Your movement is building momentum!",
                        actionSuggestion = "Consider taking the stairs or parking a bit farther away.",
                        encouragingContext = "Every extra step contributes to your wellness journey."
                    )
                )
            }
            else -> {
                guidance.add(
                    SupportiveGuidance(
                        guidanceType = GuidanceType.MOTIVATIONAL_BOOST,
                        message = "Your dedication to wellness is truly inspiring!",
                        actionSuggestion = "Keep up this amazing momentum - you're doing great!",
                        encouragingContext = "You're building habits that will serve you for life."
                    )
                )
            }
        }
        
        return guidance
    }
    
    /**
     * Creates wellness journey context with supportive messaging
     */
    private fun createWellnessJourneyContext(weeklyData: WeeklyProgressData): String {
        val activeDays = weeklyData.weeklyTotals.activeDays
        val achievements = weeklyData.celebratoryMessages.size
        
        return when {
            achievements > 0 -> "This week you achieved $achievements wellness milestones! Your commitment to health is creating positive changes in your life."
            activeDays >= 5 -> "Your consistency this week shows real dedication to your wellness journey. You're building habits that will benefit you for years to come."
            activeDays >= 3 -> "You maintained good activity levels this week. Every day you choose to move is a victory worth celebrating."
            activeDays >= 1 -> "You took steps toward wellness this week. Remember, every journey begins with a single step, and you're moving forward."
            else -> "This week is a fresh start for your wellness journey. Every day offers new opportunities to care for your health and well-being."
        }
    }
    
    /**
     * Creates motivational message based on progress patterns
     */
    private fun createMotivationalMessage(
        weeklyData: WeeklyProgressData,
        achievements: List<CelebratoryAchievement>
    ): String {
        return when {
            achievements.size >= 3 -> "Your dedication to wellness is truly exceptional! You're not just reaching goals - you're exceeding them and inspiring others."
            achievements.size >= 1 -> "Congratulations on your wellness achievements! Your consistency and effort are building a healthier, happier you."
            weeklyData.weeklyTotals.activeDays >= 4 -> "Your commitment to staying active is admirable! You're proving that wellness is a priority in your life."
            weeklyData.weeklyTotals.totalSteps > 0 -> "Every step you take is progress toward better health. Your wellness journey is unique and valuable."
            else -> "Welcome to your wellness journey! This is where you'll see your progress unfold as you build healthy habits one day at a time."
        }
    }
    
    /**
     * Gets metric-specific encouragement
     */
    fun getMetricEncouragement(weeklyData: WeeklyProgressData, metricType: MetricType): String {
        return when (metricType) {
            MetricType.STEPS -> weeklyData.getStepsSupportiveMessage()
            MetricType.CALORIES -> weeklyData.getCaloriesSupportiveMessage()
            MetricType.HEART_POINTS -> weeklyData.getHeartPointsSupportiveMessage()
        }
    }
    
    /**
     * Calculates trend direction from data points
     */
    private fun calculateTrendDirection(data: List<Float>): TrendDirection {
        if (data.size < 2) return TrendDirection.STABLE
        
        val firstHalf = data.take(data.size / 2).average()
        val secondHalf = data.drop(data.size / 2).average()
        
        return when {
            secondHalf > firstHalf * 1.1 -> TrendDirection.IMPROVING
            secondHalf < firstHalf * 0.9 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
    }
    
    /**
     * Calculates percentage change in data
     */
    private fun calculateChangePercentage(data: List<Float>): Float {
        if (data.size < 2) return 0f
        
        val firstHalf = data.take(data.size / 2).average()
        val secondHalf = data.drop(data.size / 2).average()
        
        return if (firstHalf > 0) {
            ((secondHalf - firstHalf) / firstHalf * 100).toFloat()
        } else 0f
    }
    
    /**
     * Creates supportive interpretation for steps trend
     */
    private fun createStepsTrendInterpretation(trend: TrendDirection): String {
        return when (trend) {
            TrendDirection.IMPROVING -> "Your movement is increasing! You're building excellent momentum in your wellness journey."
            TrendDirection.STABLE -> "You're maintaining consistent movement patterns. Consistency is key to building lasting healthy habits."
            TrendDirection.DECLINING -> "Every step counts, and you're still moving forward. Consider this a gentle reminder to prioritize movement when you can."
        }
    }
    
    /**
     * Creates supportive interpretation for calories trend
     */
    private fun createCaloriesTrendInterpretation(trend: TrendDirection): String {
        return when (trend) {
            TrendDirection.IMPROVING -> "Your energy expenditure is increasing! Your efforts are creating positive changes in your fitness."
            TrendDirection.STABLE -> "You're maintaining steady energy expenditure. Your consistent effort is building a strong foundation for health."
            TrendDirection.DECLINING -> "Your body needs rest too. Balance is important - listen to your body and move when it feels right."
        }
    }
    
    /**
     * Creates supportive interpretation for heart points trend
     */
    private fun createHeartPointsTrendInterpretation(trend: TrendDirection): String {
        return when (trend) {
            TrendDirection.IMPROVING -> "Your cardiovascular activity is increasing! Your heart is getting stronger with each workout."
            TrendDirection.STABLE -> "You're maintaining good cardiovascular activity. Your heart health is benefiting from your consistent efforts."
            TrendDirection.DECLINING -> "Heart health is a journey, not a destination. Every bit of cardiovascular activity contributes to your overall wellness."
        }
    }
}