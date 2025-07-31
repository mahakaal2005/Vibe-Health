package com.vibehealth.android.domain.goals

import android.util.Log
import com.vibehealth.android.data.goals.GoalRepository
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Use case for orchestrating goal calculation and storage operations.
 * 
 * This use case coordinates between the goal calculation service and repository,
 * handling error scenarios with retry logic and providing comprehensive
 * validation of calculated goals.
 */
@Singleton
class GoalCalculationUseCase @Inject constructor(
    private val goalCalculationService: GoalCalculationService,
    private val goalRepository: GoalRepository,
    private val userProfileRepository: UserProfileRepository
) {
    
    companion object {
        private const val TAG = "GoalCalculationUseCase"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 500L
        private const val MAX_RETRY_DELAY_MS = 4000L
        private const val CALCULATION_TIMEOUT_HOURS = 24
    }
    
    /**
     * Calculate and store daily goals for a user.
     * 
     * This method orchestrates the complete goal calculation workflow:
     * 1. Retrieves user profile
     * 2. Calculates goals using WHO standards
     * 3. Validates calculated goals
     * 4. Stores goals locally and syncs to cloud
     * 
     * @param userId User ID to calculate goals for
     * @param forceRecalculation Force recalculation even if recent goals exist
     * @return Result containing calculated goals or error
     */
    suspend fun calculateAndStoreGoals(
        userId: String,
        forceRecalculation: Boolean = false
    ): GoalCalculationResult {
        return withContext(Dispatchers.Default) {
            try {
                Log.d(TAG, "Starting goal calculation for user: $userId")
                
                // Check if recalculation is needed
                if (!forceRecalculation && !shouldRecalculateGoals(userId)) {
                    val existingGoals = goalRepository.getCurrentGoalsSync(userId)
                    if (existingGoals != null) {
                        Log.d(TAG, "Using existing goals for user: $userId")
                        return@withContext GoalCalculationResult.Success(
                            goals = existingGoals,
                            wasRecalculated = false,
                            calculationSource = existingGoals.calculationSource
                        )
                    }
                }
                
                // Get user profile
                val userProfile = getUserProfileWithRetry(userId)
                    ?: return@withContext GoalCalculationResult.Error(
                        error = GoalCalculationError.ProfileNotFound(userId),
                        message = "User profile not found or incomplete"
                    )
                
                // Calculate goals with retry logic
                val calculatedGoals = calculateGoalsWithRetry(userProfile)
                    ?: return@withContext GoalCalculationResult.Error(
                        error = GoalCalculationError.CalculationFailed(userId),
                        message = "Goal calculation failed after all retry attempts"
                    )
                
                // Validate calculated goals
                val validationResult = validateCalculatedGoals(calculatedGoals, userProfile)
                if (!validationResult.isValid) {
                    Log.w(TAG, "Goal validation failed: ${validationResult.issues}")
                    return@withContext GoalCalculationResult.Error(
                        error = GoalCalculationError.ValidationFailed(validationResult.issues),
                        message = "Calculated goals failed validation: ${validationResult.issues.joinToString()}"
                    )
                }
                
                // Store goals with retry logic
                val storeResult = storeGoalsWithRetry(calculatedGoals)
                if (storeResult.isFailure) {
                    Log.e(TAG, "Failed to store goals for user: $userId", storeResult.exceptionOrNull())
                    return@withContext GoalCalculationResult.Error(
                        error = GoalCalculationError.StorageFailed(userId),
                        message = "Failed to store calculated goals: ${storeResult.exceptionOrNull()?.message}"
                    )
                }
                
                Log.d(TAG, "Successfully calculated and stored goals for user: $userId")
                return@withContext GoalCalculationResult.Success(
                    goals = calculatedGoals,
                    wasRecalculated = true,
                    calculationSource = calculatedGoals.calculationSource
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during goal calculation for user: $userId", e)
                return@withContext GoalCalculationResult.Error(
                    error = GoalCalculationError.UnexpectedError(e),
                    message = "Unexpected error during goal calculation: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Recalculate goals when user profile changes.
     * 
     * This method is optimized for profile update scenarios and includes
     * change detection to avoid unnecessary recalculations.
     * 
     * @param userId User ID to recalculate goals for
     * @param changedProfile Updated user profile (optional, will fetch if not provided)
     * @return Result containing recalculated goals or error
     */
    suspend fun recalculateGoalsForProfileUpdate(
        userId: String,
        changedProfile: UserProfile? = null
    ): GoalCalculationResult {
        return withContext(Dispatchers.Default) {
            try {
                Log.d(TAG, "Recalculating goals for profile update: $userId")
                
                // Get current and previous goals for comparison
                val previousGoals = goalRepository.getCurrentGoalsSync(userId)
                val userProfile = changedProfile ?: getUserProfileWithRetry(userId)
                    ?: return@withContext GoalCalculationResult.Error(
                        error = GoalCalculationError.ProfileNotFound(userId),
                        message = "Updated user profile not found"
                    )
                
                // Check if recalculation is actually needed
                if (previousGoals != null && !isRecalculationNeeded(userProfile, previousGoals)) {
                    Log.d(TAG, "Profile changes don't affect goals, skipping recalculation")
                    return@withContext GoalCalculationResult.Success(
                        goals = previousGoals.withUpdatedTimestamp(),
                        wasRecalculated = false,
                        calculationSource = previousGoals.calculationSource
                    )
                }
                
                // Perform full recalculation
                return@withContext calculateAndStoreGoals(userId, forceRecalculation = true)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during goal recalculation for user: $userId", e)
                return@withContext GoalCalculationResult.Error(
                    error = GoalCalculationError.UnexpectedError(e),
                    message = "Error during goal recalculation: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get calculation breakdown for transparency and user education.
     * 
     * @param userId User ID to get breakdown for
     * @return Detailed breakdown of goal calculations or null if unavailable
     */
    suspend fun getCalculationBreakdown(userId: String): GoalCalculationBreakdown? {
        return withContext(Dispatchers.Default) {
            try {
                val userProfile = getUserProfileWithRetry(userId) ?: return@withContext null
                goalCalculationService.getCalculationBreakdown(userProfile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get calculation breakdown for user: $userId", e)
                null
            }
        }
    }
    
    /**
     * Check if user has valid goals that don't need recalculation.
     * 
     * @param userId User ID to check
     * @return True if user has fresh, valid goals
     */
    suspend fun hasValidGoals(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val goals = goalRepository.getCurrentGoalsSync(userId)
                goals?.let { 
                    it.isValid() && it.isFresh() && it.calculationSource != CalculationSource.FALLBACK_DEFAULT
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if user has valid goals: $userId", e)
                false
            }
        }
    }
    
    // Private helper methods
    
    /**
     * Determine if goals should be recalculated.
     * 
     * @param userId User ID to check
     * @return True if recalculation is needed
     */
    private suspend fun shouldRecalculateGoals(userId: String): Boolean {
        return try {
            val lastCalculationTime = goalRepository.getLastCalculationTime(userId)
            
            // Always recalculate if no previous calculation
            if (lastCalculationTime == null) {
                return true
            }
            
            // Recalculate if goals are older than threshold
            val hoursSinceCalculation = ChronoUnit.HOURS.between(lastCalculationTime, LocalDateTime.now())
            if (hoursSinceCalculation >= CALCULATION_TIMEOUT_HOURS) {
                Log.d(TAG, "Goals are $hoursSinceCalculation hours old, recalculation needed")
                return true
            }
            
            // Check if current goals are fallback (should be recalculated)
            val currentGoals = goalRepository.getCurrentGoalsSync(userId)
            if (currentGoals?.calculationSource == CalculationSource.FALLBACK_DEFAULT) {
                Log.d(TAG, "Current goals are fallback, recalculation needed")
                return true
            }
            
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking if recalculation needed, defaulting to true", e)
            true
        }
    }
    
    /**
     * Check if recalculation is needed based on profile changes.
     * 
     * @param userProfile Current user profile
     * @param previousGoals Previous calculated goals
     * @return True if recalculation is needed
     */
    private fun isRecalculationNeeded(userProfile: UserProfile, previousGoals: DailyGoals): Boolean {
        // Always recalculate if goals are old
        if (!previousGoals.isFresh()) {
            return true
        }
        
        // Always recalculate if previous goals were fallback
        if (previousGoals.calculationSource == CalculationSource.FALLBACK_DEFAULT) {
            return true
        }
        
        // For now, always recalculate to ensure accuracy
        // Future optimization: track specific field changes
        return true
    }
    
    /**
     * Get user profile with retry logic.
     * 
     * @param userId User ID to get profile for
     * @return UserProfile or null if not found/invalid
     */
    private suspend fun getUserProfileWithRetry(userId: String): UserProfile? {
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                val result = userProfileRepository.getUserProfile(userId)
                if (result.isSuccess) {
                    val profile = result.getOrNull()
                    if (profile?.isValidForGoalCalculation() == true) {
                        return profile
                    }
                }
                
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(calculateRetryDelay(attempt))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1} to get user profile failed", e)
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(calculateRetryDelay(attempt))
                }
            }
        }
        return null
    }
    
    /**
     * Calculate goals with retry logic.
     * 
     * @param userProfile User profile to calculate goals for
     * @return Calculated goals or null if all attempts failed
     */
    private suspend fun calculateGoalsWithRetry(userProfile: UserProfile): DailyGoals? {
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                return goalCalculationService.calculateGoals(userProfile)
            } catch (e: Exception) {
                Log.w(TAG, "Goal calculation attempt ${attempt + 1} failed", e)
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(calculateRetryDelay(attempt))
                }
            }
        }
        return null
    }
    
    /**
     * Store goals with retry logic.
     * 
     * @param goals Goals to store
     * @return Result of storage operation
     */
    private suspend fun storeGoalsWithRetry(goals: DailyGoals): Result<DailyGoals> {
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            val result = goalRepository.saveAndSyncGoals(goals)
            if (result.isSuccess) {
                return result
            }
            
            Log.w(TAG, "Storage attempt ${attempt + 1} failed: ${result.exceptionOrNull()?.message}")
            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                delay(calculateRetryDelay(attempt))
            }
        }
        
        // Return the last failed result
        return goalRepository.saveAndSyncGoals(goals)
    }
    
    /**
     * Validate calculated goals against business rules.
     * 
     * @param goals Calculated goals to validate
     * @param userProfile User profile used for calculation
     * @return Validation result with issues if any
     */
    private fun validateCalculatedGoals(goals: DailyGoals, userProfile: UserProfile): GoalValidationResult {
        val issues = mutableListOf<String>()
        
        // Basic range validation (already done in DailyGoals.isValid())
        if (!goals.isValid()) {
            issues.add("Goals are outside acceptable ranges")
        }
        
        // Age-specific validation
        val userAge = userProfile.getAge()
        when {
            userAge < 18 -> {
                // Youth should have higher activity goals
                if (goals.stepsGoal < 8000) {
                    issues.add("Steps goal too low for youth (${goals.stepsGoal} < 8000)")
                }
            }
            userAge >= 65 -> {
                // Older adults should have more conservative goals
                if (goals.stepsGoal > 15000) {
                    issues.add("Steps goal too high for older adults (${goals.stepsGoal} > 15000)")
                }
            }
        }
        
        // BMI-based validation
        userProfile.getBMI()?.let { bmi ->
            when {
                bmi < 18.5 -> {
                    // Underweight users might need higher calorie goals
                    if (goals.caloriesGoal < 1500) {
                        issues.add("Calorie goal too low for underweight user (${goals.caloriesGoal} < 1500)")
                    }
                }
                bmi > 30.0 -> {
                    // Obese users might benefit from more conservative calorie goals
                    if (goals.caloriesGoal > 3000) {
                        issues.add("Calorie goal too high for obese user (${goals.caloriesGoal} > 3000)")
                    }
                }
            }
        }
        
        // Consistency validation
        val stepsToCaloriesRatio = goals.caloriesGoal.toDouble() / goals.stepsGoal
        if (stepsToCaloriesRatio < 0.1 || stepsToCaloriesRatio > 0.5) {
            issues.add("Inconsistent steps to calories ratio: $stepsToCaloriesRatio")
        }
        
        return GoalValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }
    
    /**
     * Calculate retry delay using exponential backoff.
     * 
     * @param attempt Current attempt number (0-based)
     * @return Delay in milliseconds
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val exponentialDelay = INITIAL_RETRY_DELAY_MS * (2.0.pow(attempt)).toLong()
        return min(exponentialDelay, MAX_RETRY_DELAY_MS)
    }
}

/**
 * Result of goal calculation operation.
 */
sealed class GoalCalculationResult {
    /**
     * Successful goal calculation.
     */
    data class Success(
        val goals: DailyGoals,
        val wasRecalculated: Boolean,
        val calculationSource: CalculationSource
    ) : GoalCalculationResult()
    
    /**
     * Failed goal calculation.
     */
    data class Error(
        val error: GoalCalculationError,
        val message: String
    ) : GoalCalculationResult()
    
    /**
     * Check if result is successful.
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Check if result is an error.
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Get goals if successful, null otherwise.
     */
    fun getGoalsOrNull(): DailyGoals? = (this as? Success)?.goals
    
    /**
     * Get error if failed, null otherwise.
     */
    fun getErrorOrNull(): GoalCalculationError? = (this as? Error)?.error
}

/**
 * Types of goal calculation errors.
 */
sealed class GoalCalculationError {
    data class ProfileNotFound(val userId: String) : GoalCalculationError()
    data class CalculationFailed(val userId: String) : GoalCalculationError()
    data class ValidationFailed(val issues: List<String>) : GoalCalculationError()
    data class StorageFailed(val userId: String) : GoalCalculationError()
    data class UnexpectedError(val exception: Exception) : GoalCalculationError()
}

/**
 * Result of goal validation.
 */
data class GoalValidationResult(
    val isValid: Boolean,
    val issues: List<String>
)