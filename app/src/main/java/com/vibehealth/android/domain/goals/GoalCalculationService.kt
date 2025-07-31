package com.vibehealth.android.domain.goals

import android.util.Log
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.Period
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for orchestrating daily goal calculations using WHO-based algorithms.
 * 
 * This service coordinates the three goal calculators (steps, calories, heart points)
 * and handles error scenarios with appropriate fallback mechanisms. All calculations
 * run on background threads using Kotlin Coroutines.
 * 
 * The service follows the offline-first principle and provides medically-safe
 * fallback goals when calculation fails due to invalid data or system errors.
 */
@Singleton
class GoalCalculationService @Inject constructor(
    private val stepsGoalCalculator: StepsGoalCalculator,
    private val caloriesGoalCalculator: CaloriesGoalCalculator,
    private val heartPointsGoalCalculator: HeartPointsGoalCalculator,
    private val fallbackGoalGenerator: FallbackGoalGenerator
) {
    
    companion object {
        private const val TAG = "GoalCalculationService"
    }
    
    /**
     * Calculate daily goals for a user based on their profile.
     * 
     * This method orchestrates all three goal calculations and handles errors
     * gracefully by providing medically-safe fallback goals when needed.
     * All calculations run on a background thread to avoid blocking the UI.
     * 
     * @param userProfile Complete user profile with required data
     * @return DailyGoals with calculated or fallback values
     */
    suspend fun calculateGoals(userProfile: UserProfile): DailyGoals {
        return withContext(Dispatchers.Default) {
            try {
                // Validate user profile has required data
                if (!userProfile.isValidForGoalCalculation()) {
                    Log.w(TAG, "User profile incomplete for goal calculation, using fallback goals")
                    return@withContext fallbackGoalGenerator.generateFallbackGoals(
                        userId = userProfile.userId,
                        userProfile = userProfile,
                        failureReason = "Missing required data"
                    )
                }
                
                // Convert user profile to calculation input
                val input = userProfile.toGoalCalculationInput()
                    ?: return@withContext fallbackGoalGenerator.generateFallbackGoals(
                        userId = userProfile.userId,
                        userProfile = userProfile,
                        failureReason = "Invalid input data"
                    )
                
                // Calculate each goal type
                val stepsGoal = stepsGoalCalculator.calculateStepsGoal(input)
                val caloriesGoal = caloriesGoalCalculator.calculateCaloriesGoal(input)
                val heartPointsGoal = heartPointsGoalCalculator.calculateHeartPointsGoal(input)
                
                // Create successful result
                val goals = DailyGoals(
                    userId = userProfile.userId,
                    stepsGoal = stepsGoal,
                    caloriesGoal = caloriesGoal,
                    heartPointsGoal = heartPointsGoal,
                    calculatedAt = LocalDateTime.now(),
                    calculationSource = CalculationSource.WHO_STANDARD
                )
                
                // Validate calculated goals are reasonable
                if (!goals.isValid()) {
                    Log.w(TAG, "Calculated goals failed validation, using fallback goals: ${goals.sanitizeForLogging()}")
                    return@withContext fallbackGoalGenerator.generateFallbackGoals(
                        userId = userProfile.userId,
                        userProfile = userProfile,
                        failureReason = "Calculated goals failed validation"
                    )
                }
                
                Log.d(TAG, "Successfully calculated goals: ${goals.sanitizeForLogging()}")
                return@withContext goals
                
            } catch (e: Exception) {
                // Log error with sanitized context for debugging
                Log.e(TAG, "Goal calculation failed for user, using fallback goals", e)
                return@withContext fallbackGoalGenerator.generateFallbackGoalsForError(
                    userId = userProfile.userId,
                    calculationError = e,
                    userProfile = userProfile
                )
            }
        }
    }
    
    /**
     * Recalculate goals when user profile changes.
     * 
     * This method is optimized for profile update scenarios and includes
     * additional validation to ensure recalculation is necessary.
     * 
     * @param userProfile Updated user profile
     * @param previousGoals Previous goals for comparison (optional)
     * @return New DailyGoals or previous goals if recalculation not needed
     */
    suspend fun recalculateGoals(
        userProfile: UserProfile,
        previousGoals: DailyGoals? = null
    ): DailyGoals {
        return withContext(Dispatchers.Default) {
            try {
                // Check if recalculation is actually needed
                if (previousGoals != null && !shouldRecalculate(userProfile, previousGoals)) {
                    Log.d(TAG, "Recalculation not needed, returning previous goals with updated timestamp")
                    return@withContext previousGoals.withUpdatedTimestamp()
                }
                
                // Perform full calculation
                return@withContext calculateGoals(userProfile)
                
            } catch (e: Exception) {
                Log.e(TAG, "Goal recalculation failed", e)
                // Return previous goals if available, otherwise fallback
                return@withContext previousGoals ?: fallbackGoalGenerator.generateFallbackGoalsForError(
                    userId = userProfile.userId,
                    calculationError = e,
                    userProfile = userProfile
                )
            }
        }
    }
    
    /**
     * Get calculation breakdown for all three goal types.
     * 
     * This method provides detailed insight into how goals were calculated,
     * useful for user education and debugging purposes.
     * 
     * @param userProfile User profile for calculation
     * @return Detailed breakdown of all calculations
     */
    suspend fun getCalculationBreakdown(userProfile: UserProfile): GoalCalculationBreakdown? {
        return withContext(Dispatchers.Default) {
            try {
                val input = userProfile.toGoalCalculationInput() ?: return@withContext null
                
                val stepsBreakdown = GoalCalculationBreakdown.StepsBreakdown(
                    baseGoal = 10000,
                    ageAdjustment = when {
                        input.age < 18 -> 1.2
                        input.age >= 65 -> 0.8
                        else -> 1.0
                    },
                    genderAdjustment = when (input.gender) {
                        com.vibehealth.android.domain.user.Gender.MALE -> 1.05
                        com.vibehealth.android.domain.user.Gender.FEMALE -> 0.95
                        else -> 1.0
                    },
                    finalGoal = stepsGoalCalculator.calculateStepsGoal(input)
                )
                
                val caloriesBreakdown = caloriesGoalCalculator.getCalculationBreakdown(input)
                val heartPointsBreakdown = heartPointsGoalCalculator.getCalculationBreakdown(input)
                
                return@withContext GoalCalculationBreakdown(
                    stepsBreakdown = stepsBreakdown,
                    caloriesBreakdown = caloriesBreakdown,
                    heartPointsBreakdown = heartPointsBreakdown,
                    userAge = input.age,
                    userGender = input.gender,
                    activityLevel = input.activityLevel
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate calculation breakdown", e)
                return@withContext null
            }
        }
    }
    
    /**
     * Determine if goals should be recalculated based on profile changes.
     * 
     * This optimization avoids unnecessary recalculations when profile changes
     * don't affect goal calculation (e.g., name changes).
     * 
     * @param userProfile Current user profile
     * @param previousGoals Previous calculated goals
     * @return true if recalculation is needed
     */
    private fun shouldRecalculate(userProfile: UserProfile, previousGoals: DailyGoals): Boolean {
        // Always recalculate if goals are old (more than 7 days)
        val daysOld = Period.between(previousGoals.calculatedAt.toLocalDate(), LocalDateTime.now().toLocalDate()).days
        if (daysOld > 7) {
            return true
        }
        
        // Always recalculate if previous goals were fallback
        if (previousGoals.calculationSource == CalculationSource.FALLBACK_DEFAULT) {
            return true
        }
        
        // Recalculate if profile is now complete and wasn't before
        if (userProfile.isValidForGoalCalculation()) {
            return true
        }
        
        // For now, always recalculate to ensure accuracy
        // Future optimization: track which profile fields changed
        return true
    }
}

/**
 * Comprehensive breakdown of goal calculations for transparency and education.
 * 
 * This data class provides detailed insight into how all three goal types
 * were calculated, useful for user education and debugging.
 */
data class GoalCalculationBreakdown(
    /**
     * Steps calculation breakdown.
     */
    val stepsBreakdown: StepsBreakdown,
    
    /**
     * Calories calculation breakdown.
     */
    val caloriesBreakdown: CalorieCalculationBreakdown,
    
    /**
     * Heart points calculation breakdown.
     */
    val heartPointsBreakdown: HeartPointsCalculationBreakdown,
    
    /**
     * User's age used in calculations.
     */
    val userAge: Int,
    
    /**
     * User's gender used in calculations.
     */
    val userGender: com.vibehealth.android.domain.user.Gender,
    
    /**
     * Activity level used in calculations.
     */
    val activityLevel: ActivityLevel
) {
    
    /**
     * Steps calculation breakdown.
     */
    data class StepsBreakdown(
        val baseGoal: Int,
        val ageAdjustment: Double,
        val genderAdjustment: Double,
        val finalGoal: Int
    )
    
    /**
     * Get a comprehensive explanation of all calculations.
     * 
     * @return Formatted string explaining all three goal calculations
     */
    fun getComprehensiveExplanation(): String {
        return """
            Daily Goals Calculation Summary
            User Profile: Age $userAge, Gender ${userGender.getDisplayName()}, Activity Level ${activityLevel.description}
            
            ${stepsBreakdown.getExplanation()}
            
            ${caloriesBreakdown.getExplanation()}
            
            ${heartPointsBreakdown.getExplanation()}
            
            All calculations follow WHO Physical Activity Guidelines 2020 and established medical formulas.
            Goals are automatically adjusted based on your age, gender, and activity level for optimal health benefits.
        """.trimIndent()
    }
    
    private fun StepsBreakdown.getExplanation(): String {
        return """
            Steps Goal: $finalGoal steps/day
            - WHO Baseline: $baseGoal steps
            - Age Adjustment: ${ageAdjustment}x
            - Gender Adjustment: ${genderAdjustment}x
            - Final Goal: ${(baseGoal * ageAdjustment * genderAdjustment).toInt()} steps
        """.trimIndent()
    }
}