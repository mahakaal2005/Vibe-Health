package com.vibehealth.android.core.abandonment

import android.content.Context
import android.content.SharedPreferences
import com.vibehealth.android.core.security.DataSanitizationHelper
import com.vibehealth.android.domain.onboarding.OnboardingStep
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for preventing user abandonment during onboarding
 */
@Singleton
class AbandonmentPreventionManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "abandonment_prevention"
        private const val KEY_ABANDONMENT_COUNT = "abandonment_count"
        private const val KEY_LAST_ABANDONMENT_TIME = "last_abandonment_time"
        private const val KEY_CURRENT_SESSION_START = "current_session_start"
        private const val KEY_TOTAL_TIME_SPENT = "total_time_spent"
        private const val KEY_FURTHEST_STEP = "furthest_step"
        private const val KEY_ABANDONMENT_POINTS = "abandonment_points"
        
        private const val ABANDONMENT_THRESHOLD_MS = 30000L // 30 seconds of inactivity
        private const val MAX_ABANDONMENT_COUNT = 3
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _abandonmentRisk = MutableStateFlow(AbandonmentRisk.LOW)
    val abandonmentRisk: StateFlow<AbandonmentRisk> = _abandonmentRisk.asStateFlow()
    
    private var sessionStartTime = System.currentTimeMillis()
    private var lastActivityTime = System.currentTimeMillis()
    private var currentStep = OnboardingStep.WELCOME

    init {
        sessionStartTime = System.currentTimeMillis()
        prefs.edit().putLong(KEY_CURRENT_SESSION_START, sessionStartTime).apply()
        updateAbandonmentRisk()
    }

    /**
     * Track user activity to reset abandonment timer
     */
    fun trackActivity() {
        lastActivityTime = System.currentTimeMillis()
        updateAbandonmentRisk()
    }

    /**
     * Track step progression
     */
    fun trackStepProgression(step: OnboardingStep) {
        currentStep = step
        trackActivity()
        
        // Update furthest step reached
        val currentFurthest = prefs.getInt(KEY_FURTHEST_STEP, 0)
        if (step.stepNumber > currentFurthest) {
            prefs.edit().putInt(KEY_FURTHEST_STEP, step.stepNumber).apply()
        }
        
        updateAbandonmentRisk()
    }

    /**
     * Track form field interactions
     */
    fun trackFieldInteraction(fieldName: String) {
        trackActivity()
        
        // Log field interaction for analytics (without PII)
        val logEntry = DataSanitizationHelper.createAuditLogEntry(
            action = "field_interaction",
            userId = "[USER_ID_REDACTED]",
            success = true,
            additionalInfo = mapOf(
                "field" to fieldName,
                "step" to currentStep.name,
                "session_time" to (System.currentTimeMillis() - sessionStartTime)
            )
        )
        android.util.Log.d("AbandonmentPrevention", "Field interaction: $logEntry")
    }

    /**
     * Track validation errors
     */
    fun trackValidationError(fieldName: String, errorType: String) {
        trackActivity()
        
        // Validation errors can indicate frustration
        updateAbandonmentRisk(riskIncrease = true)
        
        val logEntry = DataSanitizationHelper.createAuditLogEntry(
            action = "validation_error",
            userId = "[USER_ID_REDACTED]",
            success = false,
            additionalInfo = mapOf(
                "field" to fieldName,
                "error_type" to errorType,
                "step" to currentStep.name
            )
        )
        android.util.Log.d("AbandonmentPrevention", "Validation error: $logEntry")
    }

    /**
     * Record abandonment event
     */
    fun recordAbandonment(reason: AbandonmentReason) {
        val currentCount = prefs.getInt(KEY_ABANDONMENT_COUNT, 0)
        val newCount = currentCount + 1
        
        prefs.edit()
            .putInt(KEY_ABANDONMENT_COUNT, newCount)
            .putLong(KEY_LAST_ABANDONMENT_TIME, System.currentTimeMillis())
            .putString(KEY_ABANDONMENT_POINTS, "${getAbandonmentPoints()},${currentStep.name}")
            .apply()
        
        // Log abandonment (without PII)
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        val logEntry = DataSanitizationHelper.createAuditLogEntry(
            action = "onboarding_abandonment",
            userId = "[USER_ID_REDACTED]",
            success = false,
            additionalInfo = mapOf(
                "reason" to reason.name,
                "step" to currentStep.name,
                "session_duration" to sessionDuration,
                "abandonment_count" to newCount
            )
        )
        android.util.Log.i("AbandonmentPrevention", "Abandonment recorded: $logEntry")
    }

    /**
     * Get abandonment prevention message
     */
    fun getPreventionMessage(): String? {
        return when (_abandonmentRisk.value) {
            AbandonmentRisk.HIGH -> {
                "We're here to help! Your progress is saved and you can continue anytime."
            }
            AbandonmentRisk.MEDIUM -> {
                "Almost there! Just a few more steps to personalize your wellness journey."
            }
            AbandonmentRisk.LOW -> null
        }
    }

    /**
     * Get encouragement message based on progress
     */
    fun getEncouragementMessage(): String {
        val progressPercentage = (currentStep.stepNumber.toFloat() / currentStep.totalSteps) * 100
        
        return when {
            progressPercentage < 25 -> "Great start! Let's get your wellness journey personalized."
            progressPercentage < 50 -> "You're making good progress! Keep going."
            progressPercentage < 75 -> "Almost halfway there! Your personalized goals are waiting."
            progressPercentage < 100 -> "You're so close! Just one more step to complete your setup."
            else -> "Congratulations! Your wellness journey is ready to begin."
        }
    }

    /**
     * Check if user should see recovery message
     */
    fun shouldShowRecoveryMessage(): Boolean {
        val abandonmentCount = prefs.getInt(KEY_ABANDONMENT_COUNT, 0)
        val lastAbandonmentTime = prefs.getLong(KEY_LAST_ABANDONMENT_TIME, 0)
        val timeSinceLastAbandonment = System.currentTimeMillis() - lastAbandonmentTime
        
        return abandonmentCount > 0 && timeSinceLastAbandonment < 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Get recovery message for returning users
     */
    fun getRecoveryMessage(): String {
        val furthestStep = prefs.getInt(KEY_FURTHEST_STEP, 0)
        val stepName = OnboardingStep.values().find { it.stepNumber == furthestStep }?.name ?: "beginning"
        
        return "Welcome back! We saved your progress from $stepName. Ready to continue?"
    }

    /**
     * Update abandonment risk based on current conditions
     */
    private fun updateAbandonmentRisk(riskIncrease: Boolean = false) {
        val timeSinceActivity = System.currentTimeMillis() - lastActivityTime
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        val abandonmentCount = prefs.getInt(KEY_ABANDONMENT_COUNT, 0)
        
        val risk = when {
            riskIncrease || abandonmentCount >= MAX_ABANDONMENT_COUNT -> AbandonmentRisk.HIGH
            timeSinceActivity > ABANDONMENT_THRESHOLD_MS || sessionDuration > 5 * 60 * 1000L -> AbandonmentRisk.MEDIUM
            else -> AbandonmentRisk.LOW
        }
        
        _abandonmentRisk.value = risk
    }

    /**
     * Get abandonment points (steps where users commonly abandon)
     */
    private fun getAbandonmentPoints(): String {
        return prefs.getString(KEY_ABANDONMENT_POINTS, "") ?: ""
    }

    /**
     * Get session analytics
     */
    fun getSessionAnalytics(): SessionAnalytics {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        val totalTimeSpent = prefs.getLong(KEY_TOTAL_TIME_SPENT, 0) + sessionDuration
        val abandonmentCount = prefs.getInt(KEY_ABANDONMENT_COUNT, 0)
        val furthestStep = prefs.getInt(KEY_FURTHEST_STEP, 0)
        
        return SessionAnalytics(
            sessionDuration = sessionDuration,
            totalTimeSpent = totalTimeSpent,
            abandonmentCount = abandonmentCount,
            furthestStepReached = furthestStep,
            currentStep = currentStep.stepNumber,
            abandonmentRisk = _abandonmentRisk.value
        )
    }

    /**
     * Save progress for recovery
     */
    fun saveProgressForRecovery(userProfile: UserProfile) {
        // Update total time spent
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        val totalTimeSpent = prefs.getLong(KEY_TOTAL_TIME_SPENT, 0) + sessionDuration
        
        prefs.edit()
            .putLong(KEY_TOTAL_TIME_SPENT, totalTimeSpent)
            .putInt(KEY_FURTHEST_STEP, currentStep.stepNumber)
            .apply()
    }

    /**
     * Clear abandonment data on successful completion
     */
    fun clearAbandonmentData() {
        prefs.edit()
            .remove(KEY_ABANDONMENT_COUNT)
            .remove(KEY_LAST_ABANDONMENT_TIME)
            .remove(KEY_ABANDONMENT_POINTS)
            .apply()
        
        _abandonmentRisk.value = AbandonmentRisk.LOW
    }
}

enum class AbandonmentRisk {
    LOW, MEDIUM, HIGH
}

enum class AbandonmentReason {
    BACK_BUTTON,
    APP_SWITCH,
    VALIDATION_FRUSTRATION,
    NETWORK_ISSUES,
    UNKNOWN
}

data class SessionAnalytics(
    val sessionDuration: Long,
    val totalTimeSpent: Long,
    val abandonmentCount: Int,
    val furthestStepReached: Int,
    val currentStep: Int,
    val abandonmentRisk: AbandonmentRisk
)