package com.vibehealth.android.ui.progress

import com.vibehealth.android.data.goals.GoalRepository
import com.vibehealth.android.domain.goals.DailyGoals
import com.vibehealth.android.ui.progress.models.WeeklyProgressData
import com.vibehealth.android.ui.progress.models.DailyProgressData
import com.vibehealth.android.ui.progress.models.MetricType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GoalProgressIntegration - Integrates progress data with goal context for celebratory feedback
 * 
 * This class connects progress history with existing goal information from Story 1.3,
 * providing celebratory messaging for reached milestones, supportive goal progress
 * indicators, and encouraging insights about progress patterns relative to personalized goals.
 * 
 * Features:
 * - Goal achievement detection with celebratory messaging
 * - Supportive goal progress indicators showing advancement
 * - Encouraging insights about progress patterns relative to goals
 * - Gentle guidance for improvement areas without pressure
 * - Celebratory feedback that reinforces positive engagement
 */
@Singleton
class GoalProgressIntegration @Inject constructor(
    private val goalRepository: GoalRepository
) {
    
    /**
     * Integrates weekly progress data with goal context for celebratory feedback
     */
    suspend fun integrateProgressWithGoals(
        weeklyProgressData: WeeklyProgressData,
        weekStartDate: LocalDate
    ): GoalIntegratedProgressData {
        // Get daily goals for the week
        val dailyGoals = goalRepository.getCurrentGoalsSync("current_user") ?: return GoalIntegratedProgressData(
            originalProgressData = weeklyProgressData,
            dailyGoals = com.vibehealth.android.domain.goals.DailyGoals(
                userId = "current_user",
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30,
                calculatedAt = java.time.LocalDateTime.now(),
                calculationSource = com.vibehealth.android.domain.goals.CalculationSource.WHO_STANDARD
            ),
            goalAchievementAnalysis = WeeklyGoalAchievementAnalysis(
                dailyAchievements = emptyList(),
                totalGoalDays = 0,
                perfectDays = 0,
                stepsGoalDays = 0,
                caloriesGoalDays = 0,
                heartPointsGoalDays = 0,
                weeklyGoalConsistency = 0f
            ),
            celebratoryMessages = emptyList(),
            goalProgressIndicators = GoalProgressIndicators(
                stepsIndicator = ProgressIndicator(
                    metricType = com.vibehealth.android.ui.progress.models.MetricType.STEPS,
                    currentValue = 0,
                    goalValue = 10000,
                    progressPercentage = 0f,
                    supportiveMessage = "Set up your goals to track progress!",
                    encouragingContext = "Your wellness journey starts with every step!"
                ),
                caloriesIndicator = ProgressIndicator(
                    metricType = com.vibehealth.android.ui.progress.models.MetricType.CALORIES,
                    currentValue = 0,
                    goalValue = 2000,
                    progressPercentage = 0f,
                    supportiveMessage = "Set up your goals to track progress!",
                    encouragingContext = "Every calorie burned contributes to your health!"
                ),
                heartPointsIndicator = ProgressIndicator(
                    metricType = com.vibehealth.android.ui.progress.models.MetricType.HEART_POINTS,
                    currentValue = 0,
                    goalValue = 30,
                    progressPercentage = 0f,
                    supportiveMessage = "Set up your goals to track progress!",
                    encouragingContext = "Your heart health is worth celebrating!"
                )
            ),
            progressInsights = emptyList(),
            gentleGuidance = emptyList()
        )
        
        // Analyze goal achievements for each day
        val goalAchievementAnalysis = analyzeWeeklyGoalAchievements(
            weeklyProgressData,
            dailyGoals
        )
        
        // Generate celebratory messages for achievements
        val celebratoryMessages = generateCelebratoryAchievementMessages(goalAchievementAnalysis)
        
        // Create supportive goal progress indicators
        val goalProgressIndicators = createSupportiveGoalProgressIndicators(
            weeklyProgressData,
            dailyGoals
        )
        
        // Generate encouraging insights about progress patterns
        val progressInsights = generateEncouragingProgressInsights(
            weeklyProgressData,
            dailyGoals,
            goalAchievementAnalysis
        )
        
        // Provide gentle guidance for improvement areas
        val gentleGuidance = generateGentleImprovementGuidance(
            weeklyProgressData,
            dailyGoals,
            goalAchievementAnalysis
        )
        
        return GoalIntegratedProgressData(
            originalProgressData = weeklyProgressData,
            dailyGoals = dailyGoals,
            goalAchievementAnalysis = goalAchievementAnalysis,
            celebratoryMessages = celebratoryMessages,
            goalProgressIndicators = goalProgressIndicators,
            progressInsights = progressInsights,
            gentleGuidance = gentleGuidance
        )
    }
    
    /**
     * Analyzes weekly goal achievements with encouraging recognition
     */
    private fun analyzeWeeklyGoalAchievements(
        weeklyProgressData: WeeklyProgressData,
        dailyGoals: DailyGoals
    ): WeeklyGoalAchievementAnalysis {
        val dailyAchievements = weeklyProgressData.dailyData.map { dailyData ->
            DailyGoalAchievement(
                date = dailyData.date,
                stepsAchieved = dailyData.steps >= dailyGoals.stepsGoal,
                caloriesAchieved = dailyData.calories >= dailyGoals.caloriesGoal,
                heartPointsAchieved = dailyData.heartPoints >= dailyGoals.heartPointsGoal,
                stepsProgress = if (dailyGoals.stepsGoal > 0) {
                    (dailyData.steps.toFloat() / dailyGoals.stepsGoal).coerceAtMost(1f)
                } else 0f,
                caloriesProgress = if (dailyGoals.caloriesGoal > 0) {
                    (dailyData.calories / dailyGoals.caloriesGoal).coerceAtMost(1.0).toFloat()
                } else 0f,
                heartPointsProgress = if (dailyGoals.heartPointsGoal > 0) {
                    (dailyData.heartPoints.toFloat() / dailyGoals.heartPointsGoal).coerceAtMost(1f)
                } else 0f
            )
        }
        
        val totalGoalDays = dailyAchievements.count { it.hasAnyGoalAchieved }
        val perfectDays = dailyAchievements.count { it.hasAllGoalsAchieved }
        val stepsGoalDays = dailyAchievements.count { it.stepsAchieved }
        val caloriesGoalDays = dailyAchievements.count { it.caloriesAchieved }
        val heartPointsGoalDays = dailyAchievements.count { it.heartPointsAchieved }
        
        return WeeklyGoalAchievementAnalysis(
            dailyAchievements = dailyAchievements,
            totalGoalDays = totalGoalDays,
            perfectDays = perfectDays,
            stepsGoalDays = stepsGoalDays,
            caloriesGoalDays = caloriesGoalDays,
            heartPointsGoalDays = heartPointsGoalDays,
            weeklyGoalConsistency = calculateWeeklyConsistency(dailyAchievements)
        )
    }
    
    /**
     * Generates celebratory messages for goal achievements
     */
    private fun generateCelebratoryAchievementMessages(
        analysis: WeeklyGoalAchievementAnalysis
    ): List<CelebratoryMessage> {
        val messages = mutableListOf<CelebratoryMessage>()
        
        // Celebrate perfect days
        if (analysis.perfectDays > 0) {
            messages.add(CelebratoryMessage(
                type = CelebratoryMessageType.PERFECT_DAYS,
                title = "Perfect Days! 🌟",
                message = when (analysis.perfectDays) {
                    1 -> "You had 1 perfect day this week - achieving all your wellness goals! That's incredible dedication!"
                    else -> "Amazing! You had ${analysis.perfectDays} perfect days this week, achieving all your wellness goals each time!"
                },
                encouragingContext = "Perfect days show your commitment to comprehensive wellness. You're building amazing habits!",
                celebrationLevel = CelebrationLevel.MAJOR
            ))
        }
        
        // Celebrate goal consistency
        if (analysis.totalGoalDays >= 4) {
            messages.add(CelebratoryMessage(
                type = CelebratoryMessageType.CONSISTENCY,
                title = "Outstanding Consistency! 💪",
                message = "You achieved wellness goals on ${analysis.totalGoalDays} days this week! Your consistency is truly inspiring.",
                encouragingContext = "Consistent goal achievement is the foundation of lasting wellness habits. You're doing amazingly!",
                celebrationLevel = CelebrationLevel.MAJOR
            ))
        } else if (analysis.totalGoalDays >= 2) {
            messages.add(CelebratoryMessage(
                type = CelebratoryMessageType.CONSISTENCY,
                title = "Great Progress! 🎉",
                message = "You reached your wellness goals on ${analysis.totalGoalDays} days this week! Every achievement counts.",
                encouragingContext = "Building consistency takes time, and you're making excellent progress on your wellness journey!",
                celebrationLevel = CelebrationLevel.MODERATE
            ))
        }
        
        // Celebrate specific metric achievements
        if (analysis.stepsGoalDays >= 5) {
            messages.add(CelebratoryMessage(
                type = CelebratoryMessageType.STEPS_EXCELLENCE,
                title = "Steps Champion! 👟",
                message = "You crushed your step goal on ${analysis.stepsGoalDays} days! Your commitment to movement is fantastic.",
                encouragingContext = "Regular step goals build cardiovascular health and energy. Keep up this excellent momentum!",
                celebrationLevel = CelebrationLevel.MAJOR
            ))
        }
        
        if (analysis.caloriesGoalDays >= 5) {
            messages.add(CelebratoryMessage(
                type = CelebratoryMessageType.CALORIES_EXCELLENCE,
                title = "Energy Powerhouse! 🔥",
                message = "You hit your calorie burn target on ${analysis.caloriesGoalDays} days! Your energy and effort are paying off.",
                encouragingContext = "Consistent calorie goals support your fitness journey and overall health. You're doing great!",
                celebrationLevel = CelebrationLevel.MAJOR
            ))
        }
        
        if (analysis.heartPointsGoalDays >= 5) {
            messages.add(CelebratoryMessage(
                type = CelebratoryMessageType.HEART_POINTS_EXCELLENCE,
                title = "Heart Health Hero! ❤️",
                message = "You earned your heart points goal on ${analysis.heartPointsGoalDays} days! Your heart is getting stronger.",
                encouragingContext = "Heart points represent cardiovascular fitness improvements. Your dedication to heart health is admirable!",
                celebrationLevel = CelebrationLevel.MAJOR
            ))
        }
        
        // Always include an encouraging message if no specific achievements
        if (messages.isEmpty()) {
            messages.add(CelebratoryMessage(
                type = CelebratoryMessageType.GENTLE_ENCOURAGEMENT,
                title = "Your Progress Matters! 💚",
                message = "Every step toward your wellness goals is meaningful progress worth celebrating!",
                encouragingContext = "Wellness is a journey, not a destination. You're moving forward at your own perfect pace!",
                celebrationLevel = CelebrationLevel.GENTLE
            ))
        }
        
        return messages
    }
    
    /**
     * Creates supportive goal progress indicators
     */
    private fun createSupportiveGoalProgressIndicators(
        weeklyProgressData: WeeklyProgressData,
        dailyGoals: DailyGoals
    ): GoalProgressIndicators {
        val weeklyTotals = weeklyProgressData.weeklyTotals
        
        // Calculate weekly progress toward goals
        val weeklyStepsProgress = if (dailyGoals.stepsGoal > 0) {
            (weeklyTotals.totalSteps.toFloat() / (dailyGoals.stepsGoal * 7)).coerceAtMost(1f)
        } else 0f
        
        val weeklyCaloriesProgress = if (dailyGoals.caloriesGoal > 0) {
            (weeklyTotals.totalCalories / (dailyGoals.caloriesGoal * 7)).coerceAtMost(1.0).toFloat()
        } else 0f
        
        val weeklyHeartPointsProgress = if (dailyGoals.heartPointsGoal > 0) {
            (weeklyTotals.totalHeartPoints.toFloat() / (dailyGoals.heartPointsGoal * 7)).coerceAtMost(1f)
        } else 0f
        
        return GoalProgressIndicators(
            stepsIndicator = ProgressIndicator(
                metricType = MetricType.STEPS,
                currentValue = weeklyTotals.totalSteps,
                goalValue = dailyGoals.stepsGoal * 7,
                progressPercentage = weeklyStepsProgress,
                supportiveMessage = generateProgressIndicatorMessage(
                    MetricType.STEPS,
                    weeklyStepsProgress,
                    weeklyTotals.totalSteps,
                    dailyGoals.stepsGoal * 7
                ),
                encouragingContext = "Every step contributes to your cardiovascular health and energy levels!"
            ),
            caloriesIndicator = ProgressIndicator(
                metricType = MetricType.CALORIES,
                currentValue = weeklyTotals.totalCalories.toInt(),
                goalValue = (dailyGoals.caloriesGoal * 7).toInt(),
                progressPercentage = weeklyCaloriesProgress,
                supportiveMessage = generateProgressIndicatorMessage(
                    MetricType.CALORIES,
                    weeklyCaloriesProgress,
                    weeklyTotals.totalCalories.toInt(),
                    (dailyGoals.caloriesGoal * 7).toInt()
                ),
                encouragingContext = "Your energy expenditure supports your fitness goals and overall wellness!"
            ),
            heartPointsIndicator = ProgressIndicator(
                metricType = MetricType.HEART_POINTS,
                currentValue = weeklyTotals.totalHeartPoints,
                goalValue = dailyGoals.heartPointsGoal * 7,
                progressPercentage = weeklyHeartPointsProgress,
                supportiveMessage = generateProgressIndicatorMessage(
                    MetricType.HEART_POINTS,
                    weeklyHeartPointsProgress,
                    weeklyTotals.totalHeartPoints,
                    dailyGoals.heartPointsGoal * 7
                ),
                encouragingContext = "Heart points represent your cardiovascular fitness improvements!"
            )
        )
    }
    
    /**
     * Generates encouraging progress insights about patterns relative to goals
     */
    private fun generateEncouragingProgressInsights(
        weeklyProgressData: WeeklyProgressData,
        dailyGoals: DailyGoals,
        analysis: WeeklyGoalAchievementAnalysis
    ): List<ProgressInsight> {
        val insights = mutableListOf<ProgressInsight>()
        
        // Analyze consistency patterns
        if (analysis.weeklyGoalConsistency >= 0.7f) {
            insights.add(ProgressInsight(
                type = InsightType.CONSISTENCY_STRENGTH,
                title = "Excellent Consistency Pattern",
                message = "You're maintaining great consistency with your wellness goals! This steady approach builds lasting healthy habits.",
                encouragingContext = "Consistency is more valuable than perfection. You're creating sustainable wellness patterns!",
                actionableGuidance = "Keep up this excellent rhythm - your body and mind thrive on consistent wellness habits."
            ))
        }
        
        // Analyze improvement trends
        val improvingMetrics = analyzeImprovementTrends(weeklyProgressData, dailyGoals)
        if (improvingMetrics.isNotEmpty()) {
            insights.add(ProgressInsight(
                type = InsightType.IMPROVEMENT_TREND,
                title = "Positive Progress Trends",
                message = "Your ${improvingMetrics.joinToString(" and ")} are showing improvement relative to your goals!",
                encouragingContext = "Upward trends indicate your efforts are paying off. Your dedication is creating real results!",
                actionableGuidance = "Continue your current approach - these positive trends show you're on the right path."
            ))
        }
        
        // Analyze goal achievement patterns
        if (analysis.perfectDays > 0) {
            insights.add(ProgressInsight(
                type = InsightType.ACHIEVEMENT_PATTERN,
                title = "Goal Achievement Success",
                message = "You've demonstrated the ability to achieve all your wellness goals! This shows excellent planning and execution.",
                encouragingContext = "Perfect days prove you have the skills and dedication for comprehensive wellness success!",
                actionableGuidance = "Reflect on what made those perfect days successful and apply those strategies more often."
            ))
        }
        
        return insights
    }
    
    /**
     * Generates gentle guidance for improvement areas without pressure
     */
    private fun generateGentleImprovementGuidance(
        weeklyProgressData: WeeklyProgressData,
        dailyGoals: DailyGoals,
        analysis: WeeklyGoalAchievementAnalysis
    ): List<GentleGuidance> {
        val guidance = mutableListOf<GentleGuidance>()
        
        // Identify areas with room for growth
        val areasForGrowth = identifyGrowthOpportunities(analysis, dailyGoals)
        
        areasForGrowth.forEach { area ->
            when (area.metricType) {
                MetricType.STEPS -> {
                    guidance.add(GentleGuidance(
                        metricType = MetricType.STEPS,
                        guidanceType = GuidanceType.GENTLE_ENCOURAGEMENT,
                        title = "Steps Growth Opportunity",
                        message = "Your step count has room to grow toward your goal. Every additional step counts!",
                        encouragingContext = "Building step habits gradually is the most sustainable approach. You're on the right track!",
                        gentleSuggestion = "Consider adding a short walk to your routine or taking the stairs when possible.",
                        supportiveFraming = "Small increases in daily movement create big long-term health benefits."
                    ))
                }
                MetricType.CALORIES -> {
                    guidance.add(GentleGuidance(
                        metricType = MetricType.CALORIES,
                        guidanceType = GuidanceType.GENTLE_ENCOURAGEMENT,
                        title = "Energy Expenditure Opportunity",
                        message = "There's potential to increase your calorie burn toward your wellness goal.",
                        encouragingContext = "Gradual increases in activity help build sustainable fitness habits without overwhelming your schedule!",
                        gentleSuggestion = "Try adding 5-10 minutes of activity you enjoy to your day.",
                        supportiveFraming = "Consistent, moderate increases in activity are more effective than dramatic changes."
                    ))
                }
                MetricType.HEART_POINTS -> {
                    guidance.add(GentleGuidance(
                        metricType = MetricType.HEART_POINTS,
                        guidanceType = GuidanceType.GENTLE_ENCOURAGEMENT,
                        title = "Heart Health Opportunity",
                        message = "Your heart points show potential for growth toward your cardiovascular goal.",
                        encouragingContext = "Heart health improvements happen gradually. Every bit of cardiovascular activity benefits your heart!",
                        gentleSuggestion = "Consider adding brief periods of moderate activity like brisk walking or dancing.",
                        supportiveFraming = "Your heart gets stronger with each cardiovascular activity, no matter how small."
                    ))
                }
            }
        }
        
        // Always include supportive overall guidance
        guidance.add(GentleGuidance(
            metricType = null,
            guidanceType = GuidanceType.OVERALL_SUPPORT,
            title = "Your Wellness Journey",
            message = "Remember that wellness is a personal journey, and you're making progress at your own perfect pace.",
            encouragingContext = "Every step forward, no matter how small, is meaningful progress worth celebrating!",
            gentleSuggestion = "Be kind to yourself and celebrate the progress you're making.",
            supportiveFraming = "Your commitment to wellness, regardless of the pace, is admirable and valuable."
        ))
        
        return guidance
    }
    
    /**
     * Helper methods for analysis
     */
    private fun calculateWeeklyConsistency(dailyAchievements: List<DailyGoalAchievement>): Float {
        if (dailyAchievements.isEmpty()) return 0f
        val achievementDays = dailyAchievements.count { it.hasAnyGoalAchieved }
        return achievementDays.toFloat() / dailyAchievements.size
    }
    
    private fun analyzeImprovementTrends(
        weeklyProgressData: WeeklyProgressData,
        dailyGoals: DailyGoals
    ): List<String> {
        // This would analyze trends compared to previous weeks
        // For now, return placeholder based on current performance
        val improvements = mutableListOf<String>()
        
        val weeklyTotals = weeklyProgressData.weeklyTotals
        if (weeklyTotals.totalSteps > dailyGoals.stepsGoal * 5) {
            improvements.add("steps")
        }
        if (weeklyTotals.totalCalories > dailyGoals.caloriesGoal * 5) {
            improvements.add("calories")
        }
        if (weeklyTotals.totalHeartPoints > dailyGoals.heartPointsGoal * 5) {
            improvements.add("heart points")
        }
        
        return improvements
    }
    
    private fun identifyGrowthOpportunities(
        analysis: WeeklyGoalAchievementAnalysis,
        dailyGoals: DailyGoals
    ): List<GrowthOpportunity> {
        val opportunities = mutableListOf<GrowthOpportunity>()
        
        if (analysis.stepsGoalDays < 4) {
            opportunities.add(GrowthOpportunity(MetricType.STEPS, analysis.stepsGoalDays))
        }
        if (analysis.caloriesGoalDays < 4) {
            opportunities.add(GrowthOpportunity(MetricType.CALORIES, analysis.caloriesGoalDays))
        }
        if (analysis.heartPointsGoalDays < 4) {
            opportunities.add(GrowthOpportunity(MetricType.HEART_POINTS, analysis.heartPointsGoalDays))
        }
        
        return opportunities
    }
    
    private fun generateProgressIndicatorMessage(
        metricType: MetricType,
        progressPercentage: Float,
        currentValue: Int,
        goalValue: Int
    ): String {
        val percentage = (progressPercentage * 100).toInt()
        return when {
            progressPercentage >= 1.0f -> "🎉 You exceeded your weekly ${metricType.displayName.lowercase()} goal! Amazing work!"
            progressPercentage >= 0.8f -> "⭐ You're at $percentage% of your weekly ${metricType.displayName.lowercase()} goal! So close to achieving it!"
            progressPercentage >= 0.5f -> "💪 You're halfway to your weekly ${metricType.displayName.lowercase()} goal! Great progress!"
            progressPercentage >= 0.25f -> "🌱 You're making good progress toward your weekly ${metricType.displayName.lowercase()} goal!"
            else -> "🚀 Every bit of ${metricType.displayName.lowercase()} counts toward your wellness goal!"
        }
    }
}
/**
 *
 Data class for goal-integrated progress data
 */
data class GoalIntegratedProgressData(
    val originalProgressData: WeeklyProgressData,
    val dailyGoals: DailyGoals,
    val goalAchievementAnalysis: WeeklyGoalAchievementAnalysis,
    val celebratoryMessages: List<CelebratoryMessage>,
    val goalProgressIndicators: GoalProgressIndicators,
    val progressInsights: List<ProgressInsight>,
    val gentleGuidance: List<GentleGuidance>
)

/**
 * Data class for weekly goal achievement analysis
 */
data class WeeklyGoalAchievementAnalysis(
    val dailyAchievements: List<DailyGoalAchievement>,
    val totalGoalDays: Int,
    val perfectDays: Int,
    val stepsGoalDays: Int,
    val caloriesGoalDays: Int,
    val heartPointsGoalDays: Int,
    val weeklyGoalConsistency: Float
)

/**
 * Data class for daily goal achievement
 */
data class DailyGoalAchievement(
    val date: LocalDate,
    val stepsAchieved: Boolean,
    val caloriesAchieved: Boolean,
    val heartPointsAchieved: Boolean,
    val stepsProgress: Float,
    val caloriesProgress: Float,
    val heartPointsProgress: Float
) {
    val hasAnyGoalAchieved: Boolean
        get() = stepsAchieved || caloriesAchieved || heartPointsAchieved
    
    val hasAllGoalsAchieved: Boolean
        get() = stepsAchieved && caloriesAchieved && heartPointsAchieved
    
    val achievementCount: Int
        get() = listOf(stepsAchieved, caloriesAchieved, heartPointsAchieved).count { it }
}

/**
 * Data class for celebratory messages
 */
data class CelebratoryMessage(
    val type: CelebratoryMessageType,
    val title: String,
    val message: String,
    val encouragingContext: String,
    val celebrationLevel: CelebrationLevel
)

/**
 * Enum for celebratory message types
 */
enum class CelebratoryMessageType {
    PERFECT_DAYS,
    CONSISTENCY,
    STEPS_EXCELLENCE,
    CALORIES_EXCELLENCE,
    HEART_POINTS_EXCELLENCE,
    GENTLE_ENCOURAGEMENT
}

/**
 * Enum for celebration levels
 */
enum class CelebrationLevel {
    MAJOR,      // Confetti, major celebration
    MODERATE,   // Stars, moderate celebration
    GENTLE      // Gentle positive feedback
}

/**
 * Data class for goal progress indicators
 */
data class GoalProgressIndicators(
    val stepsIndicator: ProgressIndicator,
    val caloriesIndicator: ProgressIndicator,
    val heartPointsIndicator: ProgressIndicator
)

/**
 * Data class for individual progress indicator
 */
data class ProgressIndicator(
    val metricType: MetricType,
    val currentValue: Int,
    val goalValue: Int,
    val progressPercentage: Float,
    val supportiveMessage: String,
    val encouragingContext: String
) {
    val isGoalAchieved: Boolean
        get() = currentValue >= goalValue
    
    val progressDisplayText: String
        get() = "${String.format("%,d", currentValue)} / ${String.format("%,d", goalValue)}"
    
    val percentageDisplayText: String
        get() = "${(progressPercentage * 100).toInt()}%"
}

/**
 * Data class for progress insights
 */
data class ProgressInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val encouragingContext: String,
    val actionableGuidance: String
)

/**
 * Enum for insight types
 */
enum class InsightType {
    CONSISTENCY_STRENGTH,
    IMPROVEMENT_TREND,
    ACHIEVEMENT_PATTERN,
    WELLNESS_MILESTONE
}

/**
 * Data class for gentle guidance
 */
data class GentleGuidance(
    val metricType: MetricType?,
    val guidanceType: GuidanceType,
    val title: String,
    val message: String,
    val encouragingContext: String,
    val gentleSuggestion: String,
    val supportiveFraming: String
)

/**
 * Enum for guidance types
 */
enum class GuidanceType {
    GENTLE_ENCOURAGEMENT,
    ACTIVITY_SUGGESTION,
    CONSISTENCY_TIP,
    WELLNESS_INSIGHT,
    MOTIVATIONAL_BOOST,
    OVERALL_SUPPORT
}

/**
 * Data class for growth opportunities
 */
data class GrowthOpportunity(
    val metricType: MetricType,
    val currentAchievementDays: Int
)