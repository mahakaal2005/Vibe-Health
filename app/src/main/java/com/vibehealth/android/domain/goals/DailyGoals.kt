package com.vibehealth.android.domain.goals

import java.time.LocalDateTime

/**
 * Domain model representing a user's calculated daily wellness goals.
 * 
 * This immutable data class encapsulates the three core wellness metrics
 * calculated based on WHO standards and user profile data.
 */
data class DailyGoals(
    /**
     * User ID this goal set belongs to.
     * Links goals to the authenticated user for data isolation.
     */
    val userId: String,
    
    /**
     * Daily steps goal based on WHO 10,000 steps baseline.
     * Adjusted for age, gender, and activity level with bounds checking (5,000-20,000).
     */
    val stepsGoal: Int,
    
    /**
     * Daily calories goal based on Harris-Benedict/Mifflin-St Jeor equations.
     * Calculated from BMR and activity level with bounds checking (1,200-4,000).
     */
    val caloriesGoal: Int,
    
    /**
     * Daily heart points goal based on WHO 150 min/week moderate activity.
     * Converted to Google Fit heart points standard with bounds checking (15-50).
     */
    val heartPointsGoal: Int,
    
    /**
     * Timestamp when these goals were calculated.
     * Used for cache invalidation and user information.
     */
    val calculatedAt: LocalDateTime,
    
    /**
     * Source of the calculation for transparency and debugging.
     * Indicates whether goals are WHO-based, fallback defaults, or user-adjusted.
     */
    val calculationSource: CalculationSource
) {
    
    /**
     * Validate that all goal values are within expected ranges.
     * 
     * @return true if all goals are within medical safety bounds
     */
    fun isValid(): Boolean {
        return stepsGoal in 5000..20000 &&
               caloriesGoal in 1200..4000 &&
               heartPointsGoal in 15..50
    }
    
    /**
     * Get a summary of the goals for display purposes.
     * 
     * @return Human-readable summary of all three goals
     */
    fun getSummary(): String {
        return "Daily Goals: ${stepsGoal} steps, ${caloriesGoal} calories, ${heartPointsGoal} heart points"
    }
    
    /**
     * Check if goals were calculated recently (within last 24 hours).
     * 
     * @return true if goals are fresh and don't need recalculation
     */
    fun isFresh(): Boolean {
        val now = LocalDateTime.now()
        val hoursOld = java.time.Duration.between(calculatedAt, now).toHours()
        return hoursOld < 24
    }
    
    /**
     * Create a copy with updated calculation timestamp.
     * Useful when goals are recalculated with same values.
     * 
     * @return Copy with current timestamp
     */
    fun withUpdatedTimestamp(): DailyGoals {
        return copy(calculatedAt = LocalDateTime.now())
    }
    
    /**
     * Create a sanitized copy for logging purposes.
     * Removes user ID while preserving goal values for debugging.
     * 
     * @return Copy with anonymized user ID
     */
    fun sanitizeForLogging(): DailyGoals {
        return copy(userId = "[USER_ID_REDACTED]")
    }
}

/**
 * Enumeration of possible sources for goal calculations.
 * 
 * This provides transparency about how goals were determined and
 * helps with debugging and user education.
 */
enum class CalculationSource {
    /**
     * Goals calculated using WHO standards and user profile data.
     * This is the preferred source indicating successful calculation.
     */
    WHO_STANDARD,
    
    /**
     * Fallback goals used when calculation fails or profile data is incomplete.
     * Based on WHO minimum recommendations for health benefits.
     */
    FALLBACK_DEFAULT,
    
    /**
     * Goals that have been manually adjusted by the user.
     * Note: Current requirements specify goals should not be user-editable,
     * but this enum value is included for future extensibility.
     */
    USER_ADJUSTED;
    
    /**
     * Get human-readable description of the calculation source.
     * 
     * @return User-friendly description for display
     */
    fun getDisplayName(): String {
        return when (this) {
            WHO_STANDARD -> "Calculated based on WHO standards"
            FALLBACK_DEFAULT -> "Default goals for health benefits"
            USER_ADJUSTED -> "Manually adjusted goals"
        }
    }
    
    /**
     * Check if this source represents successfully calculated goals.
     * 
     * @return true if goals were calculated (not fallback)
     */
    fun isCalculated(): Boolean {
        return this == WHO_STANDARD || this == USER_ADJUSTED
    }
}