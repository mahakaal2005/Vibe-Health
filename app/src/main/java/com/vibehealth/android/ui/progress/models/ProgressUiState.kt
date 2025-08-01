package com.vibehealth.android.ui.progress.models

/**
 * ProgressUiState - UI state model for progress view with Companion Principle
 * 
 * This data class represents the complete UI state for the progress history view,
 * including supportive messaging, celebratory feedback, and encouraging guidance
 * following the Companion Principle. Designed to maintain user confidence and
 * provide gentle, supportive feedback throughout all interaction states.
 */
data class ProgressUiState(
    val weeklyData: WeeklyProgressData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val supportiveMessage: String? = null,
    val celebratoryFeedback: String? = null,
    val showEmptyState: Boolean = false,
    val hasError: Boolean = false,
    val offlineMode: Boolean = false,
    val lastUpdated: Long? = null
) {
    /**
     * Indicates if supportive guidance should be shown
     */
    val showSupportiveGuidance: Boolean
        get() = supportiveMessage != null || celebratoryFeedback != null
    
    /**
     * Indicates if there's encouraging content to display
     */
    val hasEncouragingContent: Boolean
        get() = weeklyData?.hasAnyData == true || showSupportiveGuidance
    
    /**
     * Indicates if the view should show progress data
     */
    val showProgressData: Boolean
        get() = !isLoading && errorMessage == null && !showEmptyState && weeklyData != null
    
    /**
     * Indicates if there are achievements to celebrate
     */
    val hasAchievements: Boolean
        get() = weeklyData?.celebratoryMessages?.isNotEmpty() == true || celebratoryFeedback != null
    
    /**
     * Gets the primary supportive message to display
     */
    val primarySupportiveMessage: String?
        get() = celebratoryFeedback ?: supportiveMessage
    
    /**
     * Gets encouraging loading message with supportive tone
     */
    val encouragingLoadingMessage: String
        get() = "Preparing your wellness journey insights... We're excited to show you your progress!"
    
    /**
     * Gets supportive empty state message
     */
    val supportiveEmptyStateMessage: String
        get() = "Your wellness journey is just beginning! Every step counts, and we're here to celebrate your progress."
    
    /**
     * Gets encouraging empty state guidance
     */
    val encouragingEmptyStateGuidance: String
        get() = "Start tracking your daily activities to see your progress here. Even small steps lead to big changes over time."
    
    /**
     * Gets offline indicator message with supportive tone
     */
    val supportiveOfflineMessage: String
        get() = if (offlineMode) {
            "You can still view your progress while offline. We'll sync everything when you're connected again."
        } else ""
    
    /**
     * Gets last updated message with encouraging context
     */
    val lastUpdatedMessage: String
        get() = lastUpdated?.let { timestamp ->
            val timeAgo = getTimeAgoString(timestamp)
            "Your progress was last updated $timeAgo. Your wellness journey continues!"
        } ?: ""
    
    /**
     * Creates a copy with supportive loading state
     */
    fun withSupportiveLoading(message: String = encouragingLoadingMessage): ProgressUiState {
        return copy(
            isLoading = true,
            errorMessage = null,
            supportiveMessage = message,
            showEmptyState = false
        )
    }
    
    /**
     * Creates a copy with encouraging success state
     */
    fun withEncouragingSuccess(
        data: WeeklyProgressData,
        supportiveMessage: String? = null,
        celebratoryFeedback: String? = null
    ): ProgressUiState {
        return copy(
            weeklyData = data,
            isLoading = false,
            errorMessage = null,
            supportiveMessage = supportiveMessage,
            celebratoryFeedback = celebratoryFeedback,
            showEmptyState = !data.hasAnyData,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Creates a copy with supportive error state
     */
    fun withSupportiveError(
        errorMessage: String,
        supportiveGuidance: String? = null
    ): ProgressUiState {
        return copy(
            isLoading = false,
            errorMessage = errorMessage,
            supportiveMessage = supportiveGuidance,
            celebratoryFeedback = null,
            showEmptyState = false
        )
    }
    
    /**
     * Creates a copy with encouraging empty state
     */
    fun withEncouragingEmptyState(
        supportiveMessage: String = supportiveEmptyStateMessage
    ): ProgressUiState {
        return copy(
            weeklyData = null,
            isLoading = false,
            errorMessage = null,
            supportiveMessage = supportiveMessage,
            celebratoryFeedback = null,
            showEmptyState = true
        )
    }
    
    /**
     * Creates a copy with offline mode and supportive messaging
     */
    fun withSupportiveOfflineMode(
        cachedData: WeeklyProgressData? = null
    ): ProgressUiState {
        return copy(
            weeklyData = cachedData,
            isLoading = false,
            errorMessage = null,
            supportiveMessage = supportiveOfflineMessage,
            offlineMode = true,
            showEmptyState = cachedData?.hasAnyData != true
        )
    }
    
    /**
     * Gets time ago string in user-friendly format
     */
    private fun getTimeAgoString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            else -> "${diff / 86400_000} days ago"
        }
    }
}

/**
 * SupportiveInsights - Encouraging insights and guidance for progress data
 */
data class SupportiveInsights(
    val weeklyTrends: List<EncouragingTrend>,
    val achievements: List<CelebratoryAchievement>,
    val gentleGuidance: List<SupportiveGuidance>,
    val wellnessJourneyContext: String,
    val motivationalMessage: String = ""
) {
    /**
     * Generates encouraging weekly summary with supportive tone
     */
    fun generateWeeklySummary(): String {
        return when {
            achievements.isNotEmpty() -> {
                val achievementCount = achievements.size
                "🎉 You achieved $achievementCount wellness milestones this week! " + motivationalMessage
            }
            weeklyTrends.any { it.showsImprovement } -> {
                "📈 Your progress is trending upward! " + motivationalMessage
            }
            else -> motivationalMessage.ifEmpty {
                "Every step on your wellness journey matters. You're building healthier habits!"
            }
        }
    }
    
    /**
     * Gets contextual motivational message based on progress patterns
     */
    fun getContextualMotivationalMessage(): String {
        return when {
            achievements.size >= 3 -> "Your dedication to wellness is truly inspiring! Keep up this amazing momentum."
            achievements.size >= 1 -> "Great job reaching your wellness goals! Your consistency is building healthy habits."
            weeklyTrends.any { it.showsImprovement } -> "Your progress shows real improvement! Every step forward counts."
            else -> "Your wellness journey is unique and valuable. Every bit of progress matters!"
        }
    }
    
    /**
     * Gets encouraging trend message
     */
    fun getTrendMessage(): String {
        val improvingTrends = weeklyTrends.filter { it.showsImprovement }
        return when {
            improvingTrends.size >= 2 -> "Multiple metrics show improvement! Your efforts are paying off beautifully."
            improvingTrends.size == 1 -> "Your ${improvingTrends.first().metricName} is trending upward! Excellent progress."
            else -> "Progress isn't always linear - you're building lasting healthy habits!"
        }
    }
    
    /**
     * Gets supportive guidance message
     */
    fun getGuidanceMessage(): String {
        return gentleGuidance.firstOrNull()?.message ?: 
               "Continue being kind to yourself on this wellness journey. Every step matters!"
    }
    
    /**
     * Gets celebration-worthy achievements
     */
    fun getCelebrationMessage(): String {
        return achievements.joinToString(" ") { it.celebratoryText }
            .ifEmpty { "Your commitment to wellness deserves celebration!" }
    }
}

/**
 * EncouragingTrend - Trend analysis with supportive interpretation
 */
data class EncouragingTrend(
    val metricName: String,
    val trendDirection: TrendDirection,
    val changePercentage: Float,
    val supportiveInterpretation: String
) {
    /**
     * Indicates if the trend shows improvement
     */
    val showsImprovement: Boolean
        get() = trendDirection == TrendDirection.IMPROVING || 
                (trendDirection == TrendDirection.STABLE && changePercentage >= 0)
    
    /**
     * Gets encouraging trend description
     */
    val encouragingDescription: String
        get() = when (trendDirection) {
            TrendDirection.IMPROVING -> "📈 Your $metricName is improving! $supportiveInterpretation"
            TrendDirection.STABLE -> "📊 Your $metricName is consistent! $supportiveInterpretation"
            TrendDirection.DECLINING -> "💚 Your $metricName shows room for growth. $supportiveInterpretation"
        }
}

/**
 * TrendDirection - Direction of metric trends
 */
enum class TrendDirection {
    IMPROVING,
    STABLE,
    DECLINING
}

/**
 * CelebratoryAchievement - Achievement with celebratory messaging
 */
data class CelebratoryAchievement(
    val achievementType: AchievementType,
    val metricName: String,
    val achievementValue: String,
    val celebratoryText: String,
    val encouragingContext: String
) {
    /**
     * Gets full celebration message
     */
    val fullCelebrationMessage: String
        get() = "$celebratoryText $encouragingContext"
}



/**
 * SupportiveGuidance - Gentle guidance for continued progress
 */
data class SupportiveGuidance(
    val guidanceType: GuidanceType,
    val message: String,
    val actionSuggestion: String? = null,
    val encouragingContext: String
) {
    /**
     * Gets complete guidance message with encouraging context
     */
    val completeGuidanceMessage: String
        get() = "$message $encouragingContext" + 
                (actionSuggestion?.let { " $it" } ?: "")
}

/**
 * GuidanceType - Types of supportive guidance
 */
enum class GuidanceType {
    GENTLE_ENCOURAGEMENT,
    ACTIVITY_SUGGESTION,
    CONSISTENCY_TIP,
    WELLNESS_INSIGHT,
    MOTIVATIONAL_BOOST
}

