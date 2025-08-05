package com.vibehealth.android.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.vibehealth.android.domain.reminders.ReminderPreferences
import com.vibehealth.android.ui.dashboard.models.RingType
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TASK 3 ANALYSIS: Daily limit manager for intelligent notification spacing
 * 
 * INTELLIGENT SPACING IMPLEMENTATION:
 * - Maximum 8 notifications per day with automatic reset at midnight
 * - Intelligent spacing of remaining reminders throughout the day
 * - Priority system for longest periods of inactivity
 * - Automatic daily count reset in user's local timezone
 * - Graceful handling when daily limit is reached
 * 
 * PERFORMANCE OPTIMIZATION:
 * - Efficient SharedPreferences usage for daily tracking
 * - Minimal memory footprint with automatic cleanup
 * - Fast daily reset detection with timezone awareness
 * - Optimized notification spacing algorithms
 * 
 * REQUIREMENTS INTEGRATION:
 * - Requirement 6.1: Maximum 8 notifications per day
 * - Requirement 6.2: Daily count reset at midnight in user's timezone
 * - Requirement 6.4: Prioritize most important inactivity periods
 * - Requirement 6.5: Intelligent spacing of remaining reminders
 * - Requirement 6.6: Continue monitoring when daily limit reached
 */
@Singleton
class DailyLimitManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "DailyLimitManager"
        private const val TAG_NOTIFICATIONS = "REMINDER_NOTIFICATIONS"
        private const val TAG_PERFORMANCE = "REMINDER_PERFORMANCE"
        private const val TAG_SECURITY = "REMINDER_SECURITY"
        private const val TAG_INTEGRATION = "REMINDER_INTEGRATION"
        
        // SharedPreferences configuration
        private const val PREFS_NAME = "daily_limit_manager"
        private const val PREF_DAILY_COUNT_PREFIX = "daily_count_"
        private const val PREF_LAST_RESET_DATE = "last_reset_date"
        private const val PREF_LAST_NOTIFICATION_TIME_PREFIX = "last_notification_time_"
        private const val PREF_PRIORITY_SCORE_PREFIX = "priority_score_"
        private const val PREF_LAST_FOCUS_PREFIX = "last_focus_"
        private const val PREF_LAST_PRIORITY_PREFIX = "last_priority_"
        private const val PREF_LAST_PROGRESS_PREFIX = "last_progress_"
        
        // Intelligent spacing constants
        private const val MIN_NOTIFICATION_INTERVAL_MINUTES = 30
        private const val PRIORITY_BOOST_MULTIPLIER = 1.5
        private const val SPACING_CALCULATION_HOURS = 16 // Waking hours for spacing
    }
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        Log.d(TAG_NOTIFICATIONS, "=== DAILY LIMIT MANAGER INITIALIZATION ===")
        Log.d(TAG_NOTIFICATIONS, "Intelligent notification spacing system:")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Maximum 8 notifications per day with automatic reset")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Intelligent spacing throughout remaining day")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Priority system for longest inactivity periods")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Timezone-aware daily reset at midnight")
        Log.d(TAG_NOTIFICATIONS, "  ✓ Graceful handling when daily limit reached")
        Log.d(TAG_PERFORMANCE, "Daily limit performance optimization enabled")
        Log.d(TAG_NOTIFICATIONS, "=== DAILY LIMIT MANAGER INITIALIZATION COMPLETE ===")
    }
    
    /**
     * Check if daily notification limit has been reached
     * Automatically resets count at midnight in user's timezone
     */
    fun isDailyLimitReached(userId: String, preferences: ReminderPreferences): Boolean {
        return try {
            Log.d(TAG_NOTIFICATIONS, "Checking daily notification limit for user: $userId")
            
            resetDailyCountIfNeeded()
            
            val currentCount = getDailyCount(userId)
            val maxDaily = preferences.maxDailyReminders
            val limitReached = currentCount >= maxDaily
            
            Log.d(TAG_NOTIFICATIONS, "Daily notification status:")
            Log.d(TAG_NOTIFICATIONS, "  Current count: $currentCount")
            Log.d(TAG_NOTIFICATIONS, "  Maximum daily: $maxDaily")
            Log.d(TAG_NOTIFICATIONS, "  Limit reached: $limitReached")
            
            limitReached
            
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error checking daily limit", e)
            false // Default to allowing notifications on error
        }
    }
    
    /**
     * Check if notification should be sent based on intelligent spacing
     * Considers daily limits, timing, and priority scoring
     */
    fun shouldSendNotification(
        userId: String,
        preferences: ReminderPreferences,
        inactivityDurationMinutes: Long
    ): Boolean {
        return try {
            Log.d(TAG_NOTIFICATIONS, "Evaluating notification with intelligent spacing")
            
            // Check daily limit first
            if (isDailyLimitReached(userId, preferences)) {
                Log.d(TAG_NOTIFICATIONS, "Daily limit reached - notification suppressed")
                return false
            }
            
            // Check minimum interval between notifications
            if (!hasMinimumIntervalPassed(userId)) {
                Log.d(TAG_NOTIFICATIONS, "Minimum interval not met - notification suppressed")
                return false
            }
            
            // Calculate priority score for this notification
            val priorityScore = calculatePriorityScore(userId, inactivityDurationMinutes, preferences)
            val shouldSend = shouldSendBasedOnPriority(userId, priorityScore, preferences)
            
            Log.d(TAG_NOTIFICATIONS, "Intelligent spacing evaluation:")
            Log.d(TAG_NOTIFICATIONS, "  Inactivity duration: $inactivityDurationMinutes minutes")
            Log.d(TAG_NOTIFICATIONS, "  Priority score: $priorityScore")
            Log.d(TAG_NOTIFICATIONS, "  Should send: $shouldSend")
            
            shouldSend
            
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error in intelligent spacing evaluation", e)
            true // Default to allowing notifications on error
        }
    }
    
    /**
     * Record notification sent for daily tracking and spacing
     * Updates counters and timing for intelligent spacing
     */
    fun recordNotificationSent(
        userId: String,
        inactivityDurationMinutes: Long,
        preferences: ReminderPreferences
    ) {
        try {
            Log.d(TAG_NOTIFICATIONS, "Recording notification sent for intelligent tracking")
            
            resetDailyCountIfNeeded()
            
            val currentCount = getDailyCount(userId)
            val newCount = currentCount + 1
            
            // Update daily count
            sharedPrefs.edit()
                .putInt(PREF_DAILY_COUNT_PREFIX + userId, newCount)
                .putLong(PREF_LAST_NOTIFICATION_TIME_PREFIX + userId, System.currentTimeMillis())
                .apply()
            
            Log.d(TAG_NOTIFICATIONS, "Notification recorded:")
            Log.d(TAG_NOTIFICATIONS, "  User: $userId")
            Log.d(TAG_NOTIFICATIONS, "  New daily count: $newCount")
            Log.d(TAG_NOTIFICATIONS, "  Inactivity duration: $inactivityDurationMinutes minutes")
            Log.d(TAG_PERFORMANCE, "Daily tracking updated efficiently")
            
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error recording notification", e)
        }
    }
    
    /**
     * TASK 7 ANALYSIS: Record contextual notification with goal integration
     */
    fun recordContextualNotificationSent(
        userId: String,
        primaryFocus: RingType,
        priority: Int,
        goalProgress: Int,
        preferences: ReminderPreferences
    ) {
        try {
            Log.d(TAG_NOTIFICATIONS, "Recording contextual notification with goal integration")
            Log.d(TAG_INTEGRATION, "  Primary focus: ${primaryFocus.displayName}")
            Log.d(TAG_INTEGRATION, "  Priority: $priority")
            Log.d(TAG_INTEGRATION, "  Goal progress: $goalProgress%")
            
            // Use existing notification recording with enhanced metadata
            val currentTime = System.currentTimeMillis()
            
            val currentCount = sharedPrefs.getInt("${PREF_DAILY_COUNT_PREFIX}$userId", 0)
            val newCount = currentCount + 1
            
            // Store contextual notification data
            sharedPrefs.edit()
                .putInt("${PREF_DAILY_COUNT_PREFIX}$userId", newCount)
                .putLong("${PREF_LAST_NOTIFICATION_TIME_PREFIX}$userId", currentTime)
                .putString("${PREF_LAST_FOCUS_PREFIX}$userId", primaryFocus.name)
                .putInt("${PREF_LAST_PRIORITY_PREFIX}$userId", priority)
                .putInt("${PREF_LAST_PROGRESS_PREFIX}$userId", goalProgress)
                .apply()
            
            Log.d(TAG_NOTIFICATIONS, "✅ Contextual notification recorded successfully")
            Log.d(TAG_INTEGRATION, "  Daily count: $newCount/${preferences.maxDailyReminders}")
            Log.d(TAG_INTEGRATION, "  Focus area: ${primaryFocus.displayName}")
            
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error recording contextual notification", e)
            Log.e(TAG_INTEGRATION, "Error in goal integration recording", e)
        }
    }
    
    /**
     * TASK 7 ANALYSIS: Check if low priority reminder can be sent
     */
    fun canSendLowPriorityReminder(userId: String, preferences: ReminderPreferences): Boolean {
        return try {
            val currentCount = sharedPrefs.getInt("${PREF_DAILY_COUNT_PREFIX}$userId", 0)
            val lowPriorityLimit = (preferences.maxDailyReminders * 0.7).toInt() // 70% of daily limit for low priority
            
            val canSend = currentCount < lowPriorityLimit
            
            Log.d(TAG_NOTIFICATIONS, "Low priority reminder check:")
            Log.d(TAG_NOTIFICATIONS, "  Current count: $currentCount")
            Log.d(TAG_NOTIFICATIONS, "  Low priority limit: $lowPriorityLimit")
            Log.d(TAG_NOTIFICATIONS, "  Can send: $canSend")
            
            canSend
            
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error checking low priority reminder eligibility", e)
            false
        }
    }
    
    /**
     * Get current daily notification count for user
     * Automatically handles daily reset
     */
    fun getDailyCount(userId: String): Int {
        resetDailyCountIfNeeded()
        return sharedPrefs.getInt(PREF_DAILY_COUNT_PREFIX + userId, 0)
    }
    
    /**
     * Get remaining notifications for the day
     * Useful for intelligent spacing calculations
     */
    fun getRemainingNotifications(userId: String, preferences: ReminderPreferences): Int {
        val currentCount = getDailyCount(userId)
        val maxDaily = preferences.maxDailyReminders
        return maxOf(0, maxDaily - currentCount)
    }
    
    /**
     * Calculate priority score for notification
     * Higher scores indicate more important notifications
     */
    private fun calculatePriorityScore(
        userId: String,
        inactivityDurationMinutes: Long,
        preferences: ReminderPreferences
    ): Double {
        return try {
            Log.d(TAG_NOTIFICATIONS, "Calculating priority score for intelligent spacing")
            
            // Base score from inactivity duration (longer = higher priority)
            val durationScore = inactivityDurationMinutes / 60.0 // Convert to hours
            
            // Time of day factor (midday gets slight boost)
            val currentHour = LocalDateTime.now().hour
            val timeOfDayFactor = when (currentHour) {
                in 10..14 -> PRIORITY_BOOST_MULTIPLIER // Midday boost
                in 15..17 -> 1.2 // Afternoon boost
                else -> 1.0
            }
            
            // Remaining notifications factor (fewer remaining = higher priority)
            val remaining = getRemainingNotifications(userId, preferences)
            val remainingFactor = if (remaining <= 2) PRIORITY_BOOST_MULTIPLIER else 1.0
            
            val priorityScore = durationScore * timeOfDayFactor * remainingFactor
            
            Log.d(TAG_NOTIFICATIONS, "Priority score calculation:")
            Log.d(TAG_NOTIFICATIONS, "  Duration score: $durationScore")
            Log.d(TAG_NOTIFICATIONS, "  Time of day factor: $timeOfDayFactor")
            Log.d(TAG_NOTIFICATIONS, "  Remaining factor: $remainingFactor")
            Log.d(TAG_NOTIFICATIONS, "  Final priority score: $priorityScore")
            
            priorityScore
            
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error calculating priority score", e)
            1.0 // Default priority score
        }
    }
    
    /**
     * Determine if notification should be sent based on priority
     * Implements intelligent spacing algorithm
     */
    private fun shouldSendBasedOnPriority(
        userId: String,
        priorityScore: Double,
        preferences: ReminderPreferences
    ): Boolean {
        return try {
            Log.d(TAG_NOTIFICATIONS, "Evaluating priority-based notification decision")
            
            val remaining = getRemainingNotifications(userId, preferences)
            val currentHour = LocalDateTime.now().hour
            val wakingHoursRemaining = maxOf(0, preferences.wakingHoursEnd - currentHour)
            
            // If we have plenty of notifications left, be more permissive
            if (remaining >= 4) {
                Log.d(TAG_NOTIFICATIONS, "Plenty of notifications remaining ($remaining) - allowing")
                return true
            }
            
            // If we're running low on notifications, be more selective
            if (remaining <= 2) {
                val minimumPriorityThreshold = 1.5 // Require higher priority when running low
                val shouldSend = priorityScore >= minimumPriorityThreshold
                Log.d(TAG_NOTIFICATIONS, "Low notifications remaining ($remaining) - priority threshold: $minimumPriorityThreshold, should send: $shouldSend")
                return shouldSend
            }
            
            // For moderate remaining notifications, use balanced approach
            val balancedThreshold = 1.0
            val shouldSend = priorityScore >= balancedThreshold
            Log.d(TAG_NOTIFICATIONS, "Moderate notifications remaining ($remaining) - balanced threshold: $balancedThreshold, should send: $shouldSend")
            
            shouldSend
            
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error in priority-based decision", e)
            true // Default to allowing notifications
        }
    }
    
    /**
     * Check if minimum interval has passed since last notification
     * Prevents notification spam
     */
    private fun hasMinimumIntervalPassed(userId: String): Boolean {
        return try {
            val lastNotificationTime = sharedPrefs.getLong(PREF_LAST_NOTIFICATION_TIME_PREFIX + userId, 0)
            
            if (lastNotificationTime == 0L) {
                Log.d(TAG_NOTIFICATIONS, "No previous notification - minimum interval satisfied")
                return true
            }
            
            val currentTime = System.currentTimeMillis()
            val timeSinceLastMinutes = (currentTime - lastNotificationTime) / (1000 * 60)
            val intervalPassed = timeSinceLastMinutes >= MIN_NOTIFICATION_INTERVAL_MINUTES
            
            Log.d(TAG_NOTIFICATIONS, "Minimum interval check:")
            Log.d(TAG_NOTIFICATIONS, "  Time since last: $timeSinceLastMinutes minutes")
            Log.d(TAG_NOTIFICATIONS, "  Minimum required: $MIN_NOTIFICATION_INTERVAL_MINUTES minutes")
            Log.d(TAG_NOTIFICATIONS, "  Interval passed: $intervalPassed")
            
            intervalPassed
            
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error checking minimum interval", e)
            true // Default to allowing notifications
        }
    }
    
    /**
     * Reset daily counts if a new day has started
     * Handles timezone changes and ensures accurate daily tracking
     */
    private fun resetDailyCountIfNeeded() {
        try {
            val today = LocalDate.now().toString()
            val lastResetDate = sharedPrefs.getString(PREF_LAST_RESET_DATE, "")
            
            if (today != lastResetDate) {
                Log.d(TAG_NOTIFICATIONS, "New day detected - resetting daily notification counts")
                Log.d(TAG_NOTIFICATIONS, "  Previous reset date: $lastResetDate")
                Log.d(TAG_NOTIFICATIONS, "  Current date: $today")
                
                // Clear all daily counts and timing data
                val editor = sharedPrefs.edit()
                val allPrefs = sharedPrefs.all
                
                for (key in allPrefs.keys) {
                    if (key.startsWith(PREF_DAILY_COUNT_PREFIX) || 
                        key.startsWith(PREF_LAST_NOTIFICATION_TIME_PREFIX) ||
                        key.startsWith(PREF_PRIORITY_SCORE_PREFIX)) {
                        editor.remove(key)
                    }
                }
                
                editor.putString(PREF_LAST_RESET_DATE, today)
                editor.apply()
                
                Log.d(TAG_NOTIFICATIONS, "Daily counts reset completed for date: $today")
                Log.d(TAG_PERFORMANCE, "Daily reset completed efficiently")
            }
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error resetting daily counts", e)
        }
    }
    
    /**
     * Get user-friendly status of daily notification limits
     * Useful for debugging and user feedback
     */
    fun getDailyLimitStatus(userId: String, preferences: ReminderPreferences): String {
        return try {
            val currentCount = getDailyCount(userId)
            val maxDaily = preferences.maxDailyReminders
            val remaining = getRemainingNotifications(userId, preferences)
            val currentDate = LocalDate.now().toString()
            
            when {
                remaining == 0 -> {
                    "Daily limit reached ($currentCount/$maxDaily) for $currentDate. Monitoring continues without notifications."
                }
                remaining <= 2 -> {
                    "Approaching daily limit ($currentCount/$maxDaily) for $currentDate. $remaining notifications remaining - using intelligent spacing."
                }
                else -> {
                    "Daily notifications: $currentCount/$maxDaily used for $currentDate. $remaining notifications remaining."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error getting daily limit status", e)
            "Daily limit status unavailable"
        }
    }
    
    /**
     * Clear all daily tracking data for user
     * Used when user disables reminders or logs out
     */
    fun clearUserData(userId: String) {
        try {
            Log.d(TAG_NOTIFICATIONS, "Clearing daily tracking data for user: $userId")
            
            val editor = sharedPrefs.edit()
            val allPrefs = sharedPrefs.all
            
            for (key in allPrefs.keys) {
                if (key.endsWith(userId)) {
                    editor.remove(key)
                }
            }
            
            editor.apply()
            
            Log.d(TAG_NOTIFICATIONS, "User daily tracking data cleared")
            Log.d(TAG_PERFORMANCE, "User data cleanup completed efficiently")
            
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATIONS, "Error clearing user data", e)
        }
    }
}