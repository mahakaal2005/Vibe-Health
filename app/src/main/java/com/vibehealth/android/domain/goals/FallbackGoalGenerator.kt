package com.vibehealth.android.domain.goals

import android.util.Log
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UserProfile
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generator for medically-safe fallback goals when WHO-based calculation fails.
 * 
 * This service provides conservative, health-focused default goals based on WHO minimum
 * recommendations when user profile data is incomplete or calculation errors occur.
 * All fallback goals are designed to provide health benefits while being achievable
 * for most users, following the principle of "do no harm" in medical applications.
 * 
 * WHO Sources for Fallback Goals:
 * - WHO Physical Activity Guidelines 2020: Minimum activity recommendations
 * - WHO Global Recommendations on Physical Activity for Health (2010)
 * - WHO Technical Report Series 724 (1985): Energy and protein requirements
 * - American Heart Association Physical Activity Guidelines
 */
@Singleton
class FallbackGoalGenerator @Inject constructor() {
    
    companion object {
        private const val TAG = "FallbackGoalGenerator"
        
        /**
         * Default fallback steps goal based on WHO minimum recommendations.
         * 7,500 steps provides significant health benefits while being achievable
         * for sedentary populations beginning their fitness journey.
         */
        private const val DEFAULT_STEPS_GOAL = 7500
        
        /**
         * Default fallback calories goal based on conservative adult requirements.
         * 1,800 calories supports basic metabolic needs for most adults while
         * being safe across different body types and activity levels.
         */
        private const val DEFAULT_CALORIES_GOAL = 1800
        
        /**
         * Default fallback heart points goal based on WHO 150 min/week recommendation.
         * 150 minutes ÷ 7 days ≈ 21 heart points per day provides cardiovascular benefits.
         */
        private const val DEFAULT_HEART_POINTS_GOAL = 21
        
        /**
         * Minimum age for adult-level goals. Below this age, youth adjustments apply.
         */
        private const val ADULT_AGE_THRESHOLD = 18
        
        /**
         * Age threshold for older adult adjustments per WHO guidelines.
         */
        private const val OLDER_ADULT_AGE_THRESHOLD = 65
    }
    
    /**
     * Generate fallback goals for a user when WHO-based calculation fails.
     * 
     * This method attempts to use available profile data to provide more personalized
     * fallback goals, but will always return safe defaults even with minimal information.
     * 
     * @param userId User ID for the goals
     * @param userProfile Optional user profile for personalization (can be incomplete)
     * @param failureReason Optional reason for fallback to help with logging
     * @return DailyGoals with safe fallback values
     */
    fun generateFallbackGoals(
        userId: String,
        userProfile: UserProfile? = null,
        failureReason: String? = null
    ): DailyGoals {
        Log.i(TAG, "Generating fallback goals for user. Reason: ${failureReason ?: "Unknown"}")
        
        // Attempt to extract basic info from profile if available
        val age = userProfile?.getAge()
        val gender = userProfile?.gender
        
        // Generate age-appropriate fallback goals
        val stepsGoal = generateFallbackStepsGoal(age, gender)
        val caloriesGoal = generateFallbackCaloriesGoal(age, gender)
        val heartPointsGoal = generateFallbackHeartPointsGoal(age)
        
        val goals = DailyGoals(
            userId = userId,
            stepsGoal = stepsGoal,
            caloriesGoal = caloriesGoal,
            heartPointsGoal = heartPointsGoal,
            calculatedAt = LocalDateTime.now(),
            calculationSource = CalculationSource.FALLBACK_DEFAULT
        )
        
        Log.d(TAG, "Generated fallback goals: ${goals.sanitizeForLogging()}")
        return goals
    }
    
    /**
     * Generate fallback goals with specific failure context for monitoring.
     * 
     * @param userId User ID for the goals
     * @param calculationError Exception that caused the calculation failure
     * @param userProfile Optional user profile for context
     * @return DailyGoals with safe fallback values
     */
    fun generateFallbackGoalsForError(
        userId: String,
        calculationError: Exception,
        userProfile: UserProfile? = null
    ): DailyGoals {
        val errorType = when (calculationError) {
            is IllegalArgumentException -> "Invalid input data"
            is ArithmeticException -> "Mathematical calculation error"
            is NullPointerException -> "Missing required data"
            else -> "Unexpected calculation error"
        }
        
        Log.w(TAG, "Generating fallback goals due to error: $errorType", calculationError)
        
        return generateFallbackGoals(
            userId = userId,
            userProfile = userProfile,
            failureReason = errorType
        )
    }
    
    /**
     * Generate age-appropriate fallback steps goal.
     * 
     * @param age User's age (null if unknown)
     * @param gender User's gender (null if unknown)
     * @return Safe steps goal based on available information
     */
    private fun generateFallbackStepsGoal(age: Int?, gender: Gender?): Int {
        val baseGoal = DEFAULT_STEPS_GOAL
        
        // Apply age-based adjustments if age is known
        val ageAdjustment = when {
            age == null -> 1.0 // Unknown age, use default
            age < ADULT_AGE_THRESHOLD -> 1.1 // Youth need slightly more activity
            age >= OLDER_ADULT_AGE_THRESHOLD -> 0.9 // Older adults, more conservative
            else -> 1.0 // Standard adult
        }
        
        // Minimal gender adjustment if known
        val genderAdjustment = when (gender) {
            Gender.MALE -> 1.02 // Slight increase
            Gender.FEMALE -> 0.98 // Slight decrease
            else -> 1.0 // Unknown or inclusive, use neutral
        }
        
        val adjustedGoal = (baseGoal * ageAdjustment * genderAdjustment).toInt()
        
        // Ensure within safe bounds
        return adjustedGoal.coerceIn(6000, 9000)
    }
    
    /**
     * Generate age and gender-appropriate fallback calories goal.
     * 
     * @param age User's age (null if unknown)
     * @param gender User's gender (null if unknown)
     * @return Safe calories goal based on available information
     */
    private fun generateFallbackCaloriesGoal(age: Int?, gender: Gender?): Int {
        val baseGoal = DEFAULT_CALORIES_GOAL
        
        // Age-based adjustments
        val ageAdjustment = when {
            age == null -> 1.0 // Unknown age, use default
            age < ADULT_AGE_THRESHOLD -> 1.15 // Youth need more calories for growth
            age >= OLDER_ADULT_AGE_THRESHOLD -> 0.9 // Older adults, lower metabolism
            else -> 1.0 // Standard adult
        }
        
        // Gender-based adjustments (more significant for calories due to metabolic differences)
        val genderAdjustment = when (gender) {
            Gender.MALE -> 1.15 // Males typically need more calories
            Gender.FEMALE -> 0.9 // Females typically need fewer calories
            else -> 1.0 // Unknown or inclusive, use neutral
        }
        
        val adjustedGoal = (baseGoal * ageAdjustment * genderAdjustment).toInt()
        
        // Ensure within safe medical bounds
        return adjustedGoal.coerceIn(1400, 2400)
    }
    
    /**
     * Generate age-appropriate fallback heart points goal.
     * 
     * @param age User's age (null if unknown)
     * @return Safe heart points goal based on available information
     */
    private fun generateFallbackHeartPointsGoal(age: Int?): Int {
        val baseGoal = DEFAULT_HEART_POINTS_GOAL
        
        // Age-based adjustments
        val ageAdjustment = when {
            age == null -> 1.0 // Unknown age, use default
            age < ADULT_AGE_THRESHOLD -> 1.1 // Youth benefit from more activity
            age >= OLDER_ADULT_AGE_THRESHOLD -> 0.85 // Older adults, more conservative
            else -> 1.0 // Standard adult
        }
        
        val adjustedGoal = (baseGoal * ageAdjustment).toInt()
        
        // Ensure within safe bounds
        return adjustedGoal.coerceIn(17, 25)
    }
    
    /**
     * Get explanation for why fallback goals were used.
     * 
     * This provides transparency to users about why they received default goals
     * and what they can do to get personalized calculations.
     * 
     * @param failureReason Optional reason for the fallback
     * @return User-friendly explanation
     */
    fun getFallbackExplanation(failureReason: String? = null): String {
        val baseExplanation = """
            We've set safe default wellness goals for you based on WHO health guidelines.
            
            Your current goals:
            • Steps: Encourages daily movement for cardiovascular health
            • Calories: Supports healthy metabolism and energy balance  
            • Heart Points: Meets WHO recommendations for moderate activity
            
            These goals provide proven health benefits and are achievable for most people.
        """.trimIndent()
        
        val reasonExplanation = when (failureReason) {
            "Invalid input data" -> "\n\nTo get personalized goals, please complete your profile with accurate height, weight, and birthday information."
            "Missing required data" -> "\n\nComplete your profile to receive goals calculated specifically for your age, gender, and physical characteristics."
            else -> "\n\nYou can update your profile anytime to receive personalized goal calculations."
        }
        
        return baseExplanation + reasonExplanation
    }
    
    /**
     * Validate that fallback goals are within medical safety bounds.
     * 
     * This provides an additional safety check to ensure fallback goals
     * are always medically appropriate.
     * 
     * @param goals Generated fallback goals
     * @return true if goals are safe, false otherwise
     */
    fun validateFallbackGoals(goals: DailyGoals): Boolean {
        val stepsValid = goals.stepsGoal in 5000..10000
        val caloriesValid = goals.caloriesGoal in 1200..2500
        val heartPointsValid = goals.heartPointsGoal in 15..30
        val sourceValid = goals.calculationSource == CalculationSource.FALLBACK_DEFAULT
        
        val isValid = stepsValid && caloriesValid && heartPointsValid && sourceValid
        
        if (!isValid) {
            Log.e(TAG, "Generated fallback goals failed validation: ${goals.sanitizeForLogging()}")
        }
        
        return isValid
    }
    
    /**
     * Create emergency fallback goals when all other methods fail.
     * 
     * These are the most conservative possible goals that provide health benefits
     * while being safe for virtually any user.
     * 
     * @param userId User ID for the goals
     * @return Ultra-conservative emergency goals
     */
    fun createEmergencyFallbackGoals(userId: String): DailyGoals {
        Log.w(TAG, "Creating emergency fallback goals - all other methods failed")
        
        return DailyGoals(
            userId = userId,
            stepsGoal = 6000, // Conservative but beneficial
            caloriesGoal = 1600, // Safe for most adults
            heartPointsGoal = 18, // Achievable cardiovascular benefit
            calculatedAt = LocalDateTime.now(),
            calculationSource = CalculationSource.FALLBACK_DEFAULT
        )
    }
}

/**
 * Extension functions for DailyGoals to support fallback goal operations.
 */

/**
 * Check if these goals were generated as fallbacks.
 * 
 * @return true if goals are fallback defaults
 */
fun DailyGoals.isFallback(): Boolean {
    return calculationSource == CalculationSource.FALLBACK_DEFAULT
}

/**
 * Get user-friendly explanation for fallback goals.
 * 
 * @return Explanation text for display to users
 */
fun DailyGoals.getFallbackMessage(): String {
    return if (isFallback()) {
        "These are safe default goals based on WHO health guidelines. Complete your profile to get personalized calculations."
    } else {
        "These goals are calculated specifically for you based on WHO standards and your profile."
    }
}