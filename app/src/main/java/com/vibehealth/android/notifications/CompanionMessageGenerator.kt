package com.vibehealth.android.notifications

import android.util.Log
import com.vibehealth.android.domain.reminders.ReminderPreferences
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TASK 3 ANALYSIS: Companion Principle message generator for activity reminders
 * 
 * COMPANION PRINCIPLE IMPLEMENTATION:
 * - Encouraging, supportive language that follows UI/UX specifications
 * - Avoids guilt-inducing or pressure-creating messaging
 * - Includes supportive context about benefits of movement
 * - Rotates through multiple encouraging messages to prevent repetition
 * - Maintains calm, supportive aesthetic of overall app design
 * 
 * PERSONALIZATION FEATURES:
 * - Time-aware messaging based on current hour
 * - Duration-sensitive content based on inactivity period
 * - Context-aware suggestions for different times of day
 * - Wellness-focused imagery and language integration
 * 
 * REQUIREMENTS INTEGRATION:
 * - Requirement 3.1: Encouraging, positive language following Companion Principle
 * - Requirement 3.2: Avoids guilt-inducing or pressure-creating language
 * - Requirement 3.3: Includes supportive context about benefits of movement
 * - Requirement 3.5: Rotates through multiple encouraging messages
 * - Requirement 3.6: Gentle, wellness-focused messaging
 */
@Singleton
class CompanionMessageGenerator @Inject constructor() {
    
    companion object {
        private const val TAG = "CompanionMessageGenerator"
        private const val TAG_NOTIFICATIONS = "REMINDER_NOTIFICATIONS"
        private const val TAG_PERFORMANCE = "REMINDER_PERFORMANCE"
        
        // Message categories for different contexts
        private const val MORNING_HOUR_START = 6
        private const val AFTERNOON_HOUR_START = 12
        private const val EVENING_HOUR_START = 18
        private const val NIGHT_HOUR_START = 21
    }
    
    init {
        Log.d(TAG_NOTIFICATIONS, "=== COMPANION MESSAGE GENERATOR INITIALIZATION ===")
        Log.d(TAG_NOTIFICATIONS, "Companion Principle messaging patterns:")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Encouraging, supportive language implementation")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Time-aware messaging for different periods")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Duration-sensitive content generation")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Message rotation to prevent repetition")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Wellness-focused imagery and language")
        Log.d(TAG_NOTIFICATIONS, "=== MESSAGE GENERATOR INITIALIZATION COMPLETE ===")
    }
    
    /**
     * Generate encouraging notification title
     * Uses time-aware and context-sensitive messaging
     */
    fun generateNotificationTitle(
        inactivityDuration: Duration,
        preferences: ReminderPreferences
    ): String {
        Log.d(TAG_NOTIFICATIONS, "Generating Companion Principle notification title")
        
        val currentHour = LocalDateTime.now().hour
        val minutes = inactivityDuration.toMinutes()
        
        return when {
            currentHour in MORNING_HOUR_START until AFTERNOON_HOUR_START -> {
                getMorningTitle(minutes)
            }
            currentHour in AFTERNOON_HOUR_START until EVENING_HOUR_START -> {
                getAfternoonTitle(minutes)
            }
            currentHour in EVENING_HOUR_START until NIGHT_HOUR_START -> {
                getEveningTitle(minutes)
            }
            else -> {
                getGeneralTitle(minutes)
            }
        }
    }
    
    /**
     * Generate encouraging notification message
     * Follows Companion Principle with supportive, non-pressuring language
     */
    fun generateNotificationMessage(
        inactivityDuration: Duration,
        preferences: ReminderPreferences
    ): String {
        Log.d(TAG_NOTIFICATIONS, "Generating Companion Principle notification message")
        
        val currentHour = LocalDateTime.now().hour
        val minutes = inactivityDuration.toMinutes()
        
        val baseMessage = when {
            currentHour in MORNING_HOUR_START until AFTERNOON_HOUR_START -> {
                getMorningMessage(minutes)
            }
            currentHour in AFTERNOON_HOUR_START until EVENING_HOUR_START -> {
                getAfternoonMessage(minutes)
            }
            currentHour in EVENING_HOUR_START until NIGHT_HOUR_START -> {
                getEveningMessage(minutes)
            }
            else -> {
                getGeneralMessage(minutes)
            }
        }
        
        // Add supportive context about benefits
        val benefitContext = getBenefitContext(currentHour)
        
        Log.d(TAG_NOTIFICATIONS, "Generated message with Companion Principle patterns")
        return "$baseMessage $benefitContext"
    }
    
    /**
     * Generate morning-specific encouraging titles
     * Energizing and positive for the start of the day
     */
    private fun getMorningTitle(minutes: Long): String {
        val morningTitles = listOf(
            "Good morning! Time for a gentle movement break 🌅",
            "Start your day with a refreshing stretch! ☀️",
            "Your morning wellness companion is here! 🌱",
            "Rise and move! Your body will thank you 💚",
            "Morning energy boost time! 🌿"
        )
        
        return getRotatedMessage(morningTitles, minutes)
    }
    
    /**
     * Generate afternoon-specific encouraging titles
     * Energizing for the productive part of the day
     */
    private fun getAfternoonTitle(minutes: Long): String {
        val afternoonTitles = listOf(
            "Midday movement break! Keep your energy flowing ⚡",
            "Time to recharge with a gentle walk! 🚶‍♀️",
            "Your afternoon wellness check-in! 🌞",
            "Productivity boost: movement time! 💪",
            "Refresh your focus with a quick stretch! ✨"
        )
        
        return getRotatedMessage(afternoonTitles, minutes)
    }
    
    /**
     * Generate evening-specific encouraging titles
     * Calming and restorative for the end of the day
     */
    private fun getEveningTitle(minutes: Long): String {
        val eveningTitles = listOf(
            "Evening wellness moment! Time to unwind 🌙",
            "Gentle movement to end your day well 🌆",
            "Your evening self-care reminder! 💜",
            "Wind down with a peaceful stretch 🕯️",
            "Evening energy restoration time! 🌸"
        )
        
        return getRotatedMessage(eveningTitles, minutes)
    }
    
    /**
     * Generate general encouraging titles
     * Suitable for any time of day
     */
    private fun getGeneralTitle(minutes: Long): String {
        val generalTitles = listOf(
            "Time for a gentle movement break! 🌿",
            "Your wellness companion is here! 💚",
            "Movement moment: your body will love this! ✨",
            "Gentle reminder to care for yourself 🌱",
            "Time to refresh and recharge! 💫"
        )
        
        return getRotatedMessage(generalTitles, minutes)
    }
    
    /**
     * Generate morning-specific encouraging messages
     * Supportive and energizing for morning hours
     */
    private fun getMorningMessage(minutes: Long): String {
        val morningMessages = listOf(
            "You've been focused for $minutes minutes this morning! A gentle stretch or short walk can energize your entire day.",
            "Starting your day with mindful movement sets a positive tone. How about a brief walk to awaken your body?",
            "Your morning productivity is inspiring! A quick movement break can boost your energy and clarity.",
            "Good morning focus! Taking a moment to move helps maintain your energy throughout the busy day ahead.",
            "You're off to a great start today! A gentle movement break can enhance your morning momentum."
        )
        
        return getRotatedMessage(morningMessages, minutes)
    }
    
    /**
     * Generate afternoon-specific encouraging messages
     * Supportive and re-energizing for productive hours
     */
    private fun getAfternoonMessage(minutes: Long): String {
        val afternoonMessages = listOf(
            "You've been productive for $minutes minutes! A midday movement break can refresh your mind and body.",
            "Your afternoon focus is impressive! A brief walk or stretch can boost your energy for the rest of the day.",
            "Time flies when you're in the zone! A gentle movement break helps maintain your peak performance.",
            "Your dedication is admirable! Taking a moment to move can enhance your afternoon productivity.",
            "You're doing great work! A quick stretch or walk can recharge your energy and focus."
        )
        
        return getRotatedMessage(afternoonMessages, minutes)
    }
    
    /**
     * Generate evening-specific encouraging messages
     * Calming and restorative for evening hours
     */
    private fun getEveningMessage(minutes: Long): String {
        val eveningMessages = listOf(
            "You've accomplished so much today! A gentle evening stretch can help you unwind and relax.",
            "Your evening focus is wonderful! A peaceful walk can help transition you into a restful evening.",
            "You've been dedicated for $minutes minutes! A calming movement break can help you wind down.",
            "Your hard work today is appreciated! A gentle stretch can help release the day's tension.",
            "You've earned a peaceful moment! A brief walk can help you transition into a relaxing evening."
        )
        
        return getRotatedMessage(eveningMessages, minutes)
    }
    
    /**
     * Generate general encouraging messages
     * Suitable for any time of day with universal appeal
     */
    private fun getGeneralMessage(minutes: Long): String {
        val generalMessages = listOf(
            "You've been focused for $minutes minutes! A gentle stretch or short walk can refresh your mind and body.",
            "Your dedication is inspiring! Taking a moment to move helps maintain your energy and well-being.",
            "Time for a mindful movement break! Your future self will appreciate this moment of self-care.",
            "You've been working steadily! A brief walk or stretch can boost your energy and mood.",
            "Your wellness companion here! Even a few minutes of gentle movement can make a wonderful difference."
        )
        
        return getRotatedMessage(generalMessages, minutes)
    }
    
    /**
     * Get supportive context about movement benefits
     * Provides educational and encouraging information
     */
    private fun getBenefitContext(currentHour: Int): String {
        val benefitContexts = listOf(
            "Movement helps improve circulation and mental clarity! 🧠",
            "Gentle activity boosts mood and reduces stress naturally 😌",
            "Your body loves regular movement - it's a gift to yourself! 💝",
            "Even brief activity can enhance focus and creativity ✨",
            "Movement is medicine for both body and mind 🌿",
            "Taking care of yourself through movement is an act of self-love 💚"
        )
        
        // Rotate based on hour to provide variety throughout the day
        val contextIndex = currentHour % benefitContexts.size
        return benefitContexts[contextIndex]
    }
    
    /**
     * Get rotated message from list to prevent repetition
     * Uses time-based rotation for variety
     */
    private fun getRotatedMessage(messages: List<String>, minutes: Long): String {
        // Use combination of time and duration for rotation
        val rotationFactor = (System.currentTimeMillis() / (1000 * 60 * 60)) + minutes
        val messageIndex = (rotationFactor % messages.size).toInt()
        return messages[messageIndex]
    }
    
    /**
     * Generate contextual action suggestions
     * Provides specific, actionable movement suggestions
     */
    fun generateActionSuggestions(
        inactivityDuration: Duration,
        preferences: ReminderPreferences
    ): List<String> {
        Log.d(TAG_NOTIFICATIONS, "Generating contextual action suggestions")
        
        val currentHour = LocalDateTime.now().hour
        val minutes = inactivityDuration.toMinutes()
        
        return when {
            minutes < 60 -> getQuickActionSuggestions(currentHour)
            minutes < 120 -> getMediumActionSuggestions(currentHour)
            else -> getExtendedActionSuggestions(currentHour)
        }
    }
    
    /**
     * Quick action suggestions for shorter inactivity periods
     */
    private fun getQuickActionSuggestions(currentHour: Int): List<String> {
        return listOf(
            "Take 5 deep breaths and stretch your arms",
            "Stand up and do gentle neck rolls",
            "Walk to the window and back",
            "Do some shoulder shrugs and arm circles",
            "Take a moment to stretch your legs"
        )
    }
    
    /**
     * Medium action suggestions for moderate inactivity periods
     */
    private fun getMediumActionSuggestions(currentHour: Int): List<String> {
        return listOf(
            "Take a 5-minute walk around your space",
            "Do some gentle stretching exercises",
            "Walk up and down stairs if available",
            "Step outside for fresh air and movement",
            "Do some light calisthenics or yoga poses"
        )
    }
    
    /**
     * Extended action suggestions for longer inactivity periods
     */
    private fun getExtendedActionSuggestions(currentHour: Int): List<String> {
        return listOf(
            "Take a 10-15 minute walk outdoors",
            "Do a full body stretching routine",
            "Take a walking break to a nearby location",
            "Engage in light exercise or yoga",
            "Take a mindful walking meditation"
        )
    }
}