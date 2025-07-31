package com.vibehealth.android.ui.messaging

import com.vibehealth.android.ui.dashboard.models.DailyProgress
import com.vibehealth.android.ui.dashboard.models.RingType
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Motivational messaging system following the Companion Principle.
 * Creates contextual messaging that feels like "a calm, supportive, and intelligent companion."
 * 
 * Philosophy: "What would a calm, supportive, and intelligent companion do?"
 * Goal: Make the app feel like "a moment of calm clarity in a busy day" and "a deep breath, not another task"
 */
@Singleton
class CompanionMessagingSystem @Inject constructor() {
    
    /**
     * Gets contextual greeting message based on time and progress.
     */
    fun getGreetingMessage(progress: DailyProgress): String {
        val hour = LocalTime.now().hour
        val overallProgress = calculateOverallProgress(progress)
        
        return when (hour) {
            in 5..11 -> getMorningGreeting(overallProgress)
            in 12..16 -> getAfternoonGreeting(overallProgress)
            in 17..20 -> getEveningGreeting(overallProgress)
            else -> getNightGreeting(overallProgress)
        }
    }
    
    /**
     * Gets encouraging feedback for progress updates.
     * Feels like "a moment of calm clarity" rather than demanding.
     */
    fun getProgressUpdateMessage(
        ringType: RingType,
        previousProgress: Float,
        currentProgress: Float
    ): String {
        val progressIncrease = currentProgress - previousProgress
        
        return when {
            progressIncrease > 0.1f -> getSignificantProgressMessage(ringType, currentProgress)
            progressIncrease > 0.05f -> getModerateProgressMessage(ringType, currentProgress)
            progressIncrease > 0f -> getGentleProgressMessage(ringType, currentProgress)
            else -> getEncouragingMessage(ringType, currentProgress)
        }
    }
    
    /**
     * Gets supportive messaging for low progress without being demanding.
     * Maintains gentle nudge approach per Companion Principle.
     */
    fun getLowProgressMessage(progress: DailyProgress): String {
        val lowestProgressRing = findLowestProgressRing(progress)
        val timeOfDay = LocalTime.now().hour
        
        return when {
            timeOfDay < 12 -> getGentleMorningNudge(lowestProgressRing)
            timeOfDay < 17 -> getAfternoonEncouragement(lowestProgressRing)
            else -> getEveningSupport(lowestProgressRing)
        }
    }
    
    /**
     * Gets celebration messages for milestone achievements.
     * Feels purposeful and meaningful per UI/UX motion design principles.
     */
    fun getCelebrationMessage(achievedRings: List<RingType>): String {
        return when (achievedRings.size) {
            3 -> getAllGoalsAchievedMessage()
            2 -> getTwoGoalsAchievedMessage(achievedRings)
            1 -> getSingleGoalAchievedMessage(achievedRings.first())
            else -> ""
        }
    }
    
    /**
     * Gets contextual messages that support the aspirational goal.
     * Ensures messaging feels like "a deep breath, not another task."
     */
    fun getContextualMessage(progress: DailyProgress, context: MessageContext): String {
        return when (context) {
            MessageContext.DASHBOARD_LOAD -> getDashboardWelcomeMessage(progress)
            MessageContext.GOAL_EXPLANATION -> getGoalExplanationMessage()
            MessageContext.DATA_REFRESH -> getDataRefreshMessage()
            MessageContext.OFFLINE_MODE -> getOfflineModeMessage()
            MessageContext.ERROR_RECOVERY -> getErrorRecoveryMessage()
        }
    }
    
    // Morning greetings based on progress
    private fun getMorningGreeting(overallProgress: Float): String {
        return when {
            overallProgress > 0.3f -> "Good morning! You're off to a wonderful start today."
            overallProgress > 0.1f -> "Good morning! Ready to embrace today's wellness journey?"
            else -> "Good morning! Today is full of possibilities for your wellbeing."
        }
    }
    
    // Afternoon greetings
    private fun getAfternoonGreeting(overallProgress: Float): String {
        return when {
            overallProgress > 0.7f -> "Good afternoon! You're having an amazing day."
            overallProgress > 0.4f -> "Good afternoon! You're making steady progress today."
            else -> "Good afternoon! There's still time to nurture your wellness."
        }
    }
    
    // Evening greetings
    private fun getEveningGreeting(overallProgress: Float): String {
        return when {
            overallProgress > 0.8f -> "Good evening! What an accomplished day you've had."
            overallProgress > 0.5f -> "Good evening! You've made meaningful progress today."
            else -> "Good evening! Every step toward wellness matters."
        }
    }
    
    // Night greetings
    private fun getNightGreeting(overallProgress: Float): String {
        return when {
            overallProgress > 0.6f -> "Your wellness journey continues, even in quiet moments."
            else -> "Rest well, knowing tomorrow brings new opportunities for wellness."
        }
    }
    
    // Significant progress messages
    private fun getSignificantProgressMessage(ringType: RingType, progress: Float): String {
        val messages = when (ringType) {
            RingType.STEPS -> listOf(
                "Your steps are adding up beautifully today.",
                "Each step is a gift to your future self.",
                "You're moving with purpose and grace."
            )
            RingType.CALORIES -> listOf(
                "Your energy is flowing wonderfully today.",
                "You're nurturing your body with mindful movement.",
                "Your metabolism is thanking you for this care."
            )
            RingType.HEART_POINTS -> listOf(
                "Your heart is celebrating this activity.",
                "You're building strength with every heartbeat.",
                "Your cardiovascular health is flourishing."
            )
        }
        return messages.random()
    }
    
    // Moderate progress messages
    private fun getModerateProgressMessage(ringType: RingType, progress: Float): String {
        val messages = when (ringType) {
            RingType.STEPS -> listOf(
                "You're finding your rhythm today.",
                "Steady progress, just as it should be.",
                "Your consistency is your strength."
            )
            RingType.CALORIES -> listOf(
                "You're honoring your body's needs.",
                "Gentle progress is still progress.",
                "Your body appreciates this mindful care."
            )
            RingType.HEART_POINTS -> listOf(
                "Your heart is getting stronger.",
                "Building health, one beat at a time.",
                "You're investing in your heart's wellbeing."
            )
        }
        return messages.random()
    }
    
    // Gentle progress messages
    private fun getGentleProgressMessage(ringType: RingType, progress: Float): String {
        return "Every moment of movement matters. You're doing well."
    }
    
    // Encouraging messages for no progress
    private fun getEncouragingMessage(ringType: RingType, progress: Float): String {
        return "Your wellness journey is unique to you. There's no rush."
    }
    
    // Gentle morning nudges
    private fun getGentleMorningNudge(ringType: RingType): String {
        val messages = when (ringType) {
            RingType.STEPS -> listOf(
                "Perhaps a gentle walk could start your day beautifully.",
                "Your body might enjoy some mindful movement this morning.",
                "A few steps could be a lovely way to greet the day."
            )
            RingType.CALORIES -> listOf(
                "Your body is ready for some nurturing activity.",
                "A little movement could energize your morning.",
                "Your metabolism would welcome some gentle activity."
            )
            RingType.HEART_POINTS -> listOf(
                "Your heart might enjoy a bit more activity today.",
                "Some heart-healthy movement could brighten your morning.",
                "Your cardiovascular system would appreciate some care."
            )
        }
        return messages.random()
    }
    
    // Afternoon encouragement
    private fun getAfternoonEncouragement(ringType: RingType): String {
        return "The afternoon offers a perfect opportunity for some gentle movement."
    }
    
    // Evening support
    private fun getEveningSupport(ringType: RingType): String {
        return "Even small movements in the evening contribute to your wellbeing."
    }
    
    // Celebration messages
    private fun getAllGoalsAchievedMessage(): String {
        val messages = listOf(
            "What a beautiful day of wellness you've created!",
            "You've honored your body in every way today.",
            "Your commitment to yourself is truly inspiring.",
            "Today you've been your own best companion."
        )
        return messages.random()
    }
    
    private fun getTwoGoalsAchievedMessage(achievedRings: List<RingType>): String {
        return "You're nurturing your wellbeing so thoughtfully today."
    }
    
    private fun getSingleGoalAchievedMessage(ringType: RingType): String {
        val messages = when (ringType) {
            RingType.STEPS -> listOf(
                "Your steps have carried you to your goal beautifully.",
                "You've moved with intention and reached your steps goal."
            )
            RingType.CALORIES -> listOf(
                "Your energy goal is complete - well done.",
                "You've nurtured your metabolism perfectly today."
            )
            RingType.HEART_POINTS -> listOf(
                "Your heart is celebrating this achievement.",
                "You've given your cardiovascular health wonderful care."
            )
        }
        return messages.random()
    }
    
    // Contextual messages
    private fun getDashboardWelcomeMessage(progress: DailyProgress): String {
        return "Welcome to your wellness sanctuary. Take a moment to breathe and see how you're doing."
    }
    
    private fun getGoalExplanationMessage(): String {
        return "Your goals are thoughtfully calculated to support your unique wellness journey."
    }
    
    private fun getDataRefreshMessage(): String {
        return "Your wellness data is now refreshed and ready."
    }
    
    private fun getOfflineModeMessage(): String {
        return "Your wellness companion is here, even when you're offline."
    }
    
    private fun getErrorRecoveryMessage(): String {
        return "We're working to restore your wellness data. Your progress is safe."
    }
    
    // Helper methods
    private fun calculateOverallProgress(progress: DailyProgress): Float {
        val allProgress = progress.getAllProgress()
        return allProgress.map { it.percentage }.average().toFloat()
    }
    
    private fun findLowestProgressRing(progress: DailyProgress): RingType {
        val allProgress = progress.getAllProgress()
        return allProgress.minByOrNull { it.percentage }?.ringType ?: RingType.STEPS
    }
}

/**
 * Enum for different message contexts.
 */
enum class MessageContext {
    DASHBOARD_LOAD,
    GOAL_EXPLANATION,
    DATA_REFRESH,
    OFFLINE_MODE,
    ERROR_RECOVERY
}