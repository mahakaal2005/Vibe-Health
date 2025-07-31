package com.vibehealth.android.domain.goals

import android.util.Log
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for handling profile updates that trigger goal recalculation.
 * 
 * This use case coordinates profile updates with goal recalculation,
 * ensuring data consistency and handling concurrent updates gracefully.
 */
@Singleton
class ProfileUpdateUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val goalCalculationUseCase: GoalCalculationUseCase,
    private val goalRecalculationTriggerService: GoalRecalculationTriggerService
) {
    
    companion object {
        private const val TAG = "ProfileUpdateUseCase"
    }
    
    // Track ongoing updates to prevent concurrent modifications
    private val ongoingUpdates = ConcurrentHashMap<String, Boolean>()
    
    /**
     * Update user profile and automatically trigger goal recalculation if needed.
     * 
     * This method ensures atomic updates and maintains data consistency
     * across profile and goal data.
     * 
     * @param updatedProfile Updated user profile
     * @param forceGoalRecalculation Force goal recalculation regardless of changes
     * @return Result containing the updated profile or error
     */
    suspend fun updateProfileWithGoalRecalculation(
        updatedProfile: UserProfile,
        forceGoalRecalculation: Boolean = false
    ): ProfileUpdateResult {
        return withContext(Dispatchers.Default) {
            val userId = updatedProfile.userId
            
            // Check for concurrent updates
            if (ongoingUpdates.putIfAbsent(userId, true) != null) {
                Log.w(TAG, "Concurrent update detected for user: $userId")
                return@withContext ProfileUpdateResult.Error(
                    error = ProfileUpdateError.ConcurrentUpdate(userId),
                    message = "Another update is already in progress for this user"
                )
            }
            
            try {
                // Get current profile for change detection
                val currentProfileResult = userProfileRepository.getUserProfile(userId)
                val currentProfile = if (currentProfileResult.isSuccess) {
                    currentProfileResult.getOrNull()
                } else {
                    null
                }
                
                // Detect goal-affecting changes
                val changeDetection = detectGoalAffectingChanges(currentProfile, updatedProfile)
                
                // Update profile in repository
                val updateResult = userProfileRepository.updateUserProfile(updatedProfile)
                if (updateResult.isFailure) {
                    Log.e(TAG, "Failed to update profile for user: $userId", updateResult.exceptionOrNull())
                    return@withContext ProfileUpdateResult.Error(
                        error = ProfileUpdateError.ProfileUpdateFailed(updateResult.exceptionOrNull()),
                        message = "Failed to update user profile: ${updateResult.exceptionOrNull()?.message}"
                    )
                }
                
                val savedProfile = updateResult.getOrThrow()
                
                // Trigger goal recalculation if needed
                var goalRecalculationResult: GoalCalculationResult? = null
                if (forceGoalRecalculation || changeDetection.shouldRecalculate) {
                    Log.d(TAG, "Triggering goal recalculation for user: $userId, reason: ${changeDetection.reason}")
                    
                    goalRecalculationResult = goalCalculationUseCase.recalculateGoalsForProfileUpdate(
                        userId = userId,
                        changedProfile = savedProfile
                    )
                    
                    // Also notify the trigger service for history tracking
                    goalRecalculationTriggerService.onProfileUpdated(currentProfile, savedProfile)
                    
                    when (goalRecalculationResult) {
                        is GoalCalculationResult.Success -> {
                            Log.d(TAG, "Goal recalculation successful for user: $userId")
                        }
                        is GoalCalculationResult.Error -> {
                            Log.w(TAG, "Goal recalculation failed for user: $userId: ${goalRecalculationResult.message}")
                            // Continue with profile update success even if goal calculation fails
                        }
                    }
                } else {
                    Log.d(TAG, "No goal recalculation needed for user: $userId")
                }
                
                return@withContext ProfileUpdateResult.Success(
                    updatedProfile = savedProfile,
                    goalRecalculationResult = goalRecalculationResult,
                    changesSummary = changeDetection
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during profile update for user: $userId", e)
                return@withContext ProfileUpdateResult.Error(
                    error = ProfileUpdateError.UnexpectedError(e),
                    message = "Unexpected error during profile update: ${e.message}"
                )
            } finally {
                ongoingUpdates.remove(userId)
            }
        }
    }
    
    /**
     * Update profile with partial data efficiently.
     * 
     * This method handles partial updates by merging with existing profile data,
     * optimizing for scenarios where only specific fields are changed.
     * 
     * @param userId User ID to update
     * @param partialUpdate Map of field names to new values
     * @return Result containing the updated profile or error
     */
    suspend fun updateProfilePartially(
        userId: String,
        partialUpdate: Map<String, Any>
    ): ProfileUpdateResult {
        return withContext(Dispatchers.Default) {
            try {
                // Get current profile
                val currentProfileResult = userProfileRepository.getUserProfile(userId)
                if (currentProfileResult.isFailure) {
                    return@withContext ProfileUpdateResult.Error(
                        error = ProfileUpdateError.ProfileNotFound(userId),
                        message = "Current profile not found for partial update"
                    )
                }
                
                val currentProfile = currentProfileResult.getOrNull()
                    ?: return@withContext ProfileUpdateResult.Error(
                        error = ProfileUpdateError.ProfileNotFound(userId),
                        message = "Current profile is null"
                    )
                
                // Apply partial updates
                val updatedProfile = applyPartialUpdates(currentProfile, partialUpdate)
                
                // Perform full update with the merged profile
                return@withContext updateProfileWithGoalRecalculation(updatedProfile)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during partial profile update for user: $userId", e)
                return@withContext ProfileUpdateResult.Error(
                    error = ProfileUpdateError.UnexpectedError(e),
                    message = "Error during partial update: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Check if a profile update is currently in progress for a user.
     * 
     * @param userId User ID to check
     * @return True if update is in progress
     */
    fun isUpdateInProgress(userId: String): Boolean {
        return ongoingUpdates.containsKey(userId)
    }
    
    /**
     * Get count of ongoing updates.
     * 
     * @return Number of users with ongoing updates
     */
    fun getOngoingUpdatesCount(): Int {
        return ongoingUpdates.size
    }
    
    // Private helper methods
    
    /**
     * Detect changes that affect goal calculation.
     * 
     * @param currentProfile Current profile (null if new)
     * @param updatedProfile Updated profile
     * @return Change detection result
     */
    private fun detectGoalAffectingChanges(
        currentProfile: UserProfile?,
        updatedProfile: UserProfile
    ): ProfileChangeDetection {
        // If no current profile, this is a new profile
        if (currentProfile == null) {
            return ProfileChangeDetection(
                shouldRecalculate = updatedProfile.isValidForGoalCalculation(),
                reason = "New profile created",
                changedFields = emptyList(),
                wasValidBefore = false,
                isValidAfter = updatedProfile.isValidForGoalCalculation()
            )
        }
        
        // Check if profile validity changed
        val wasValidBefore = currentProfile.isValidForGoalCalculation()
        val isValidAfter = updatedProfile.isValidForGoalCalculation()
        
        if (!wasValidBefore && isValidAfter) {
            return ProfileChangeDetection(
                shouldRecalculate = true,
                reason = "Profile became valid for goal calculation",
                changedFields = getChangedFields(currentProfile, updatedProfile),
                wasValidBefore = false,
                isValidAfter = true
            )
        }
        
        if (wasValidBefore && !isValidAfter) {
            return ProfileChangeDetection(
                shouldRecalculate = false,
                reason = "Profile became invalid for goal calculation",
                changedFields = getChangedFields(currentProfile, updatedProfile),
                wasValidBefore = true,
                isValidAfter = false
            )
        }
        
        // Both profiles are valid, check for goal-affecting changes
        if (isValidAfter) {
            val changedFields = getGoalAffectingChangedFields(currentProfile, updatedProfile)
            
            return ProfileChangeDetection(
                shouldRecalculate = changedFields.isNotEmpty(),
                reason = if (changedFields.isNotEmpty()) {
                    "Goal-affecting fields changed: ${changedFields.joinToString(", ")}"
                } else {
                    "No goal-affecting changes detected"
                },
                changedFields = changedFields,
                wasValidBefore = true,
                isValidAfter = true
            )
        }
        
        // Both profiles are invalid
        return ProfileChangeDetection(
            shouldRecalculate = false,
            reason = "Profile remains invalid for goal calculation",
            changedFields = getChangedFields(currentProfile, updatedProfile),
            wasValidBefore = false,
            isValidAfter = false
        )
    }
    
    /**
     * Get all changed fields between profiles.
     * 
     * @param current Current profile
     * @param updated Updated profile
     * @return List of changed field names
     */
    private fun getChangedFields(current: UserProfile, updated: UserProfile): List<String> {
        val changes = mutableListOf<String>()
        
        if (current.displayName != updated.displayName) changes.add("displayName")
        if (current.firstName != updated.firstName) changes.add("firstName")
        if (current.lastName != updated.lastName) changes.add("lastName")
        if (current.birthday != updated.birthday) changes.add("birthday")
        if (current.gender != updated.gender) changes.add("gender")
        if (current.unitSystem != updated.unitSystem) changes.add("unitSystem")
        if (current.heightInCm != updated.heightInCm) changes.add("heightInCm")
        if (current.weightInKg != updated.weightInKg) changes.add("weightInKg")
        if (current.hasCompletedOnboarding != updated.hasCompletedOnboarding) changes.add("hasCompletedOnboarding")
        
        return changes
    }
    
    /**
     * Get changed fields that affect goal calculation.
     * 
     * @param current Current profile
     * @param updated Updated profile
     * @return List of goal-affecting changed field names
     */
    private fun getGoalAffectingChangedFields(current: UserProfile, updated: UserProfile): List<String> {
        val changes = mutableListOf<String>()
        
        if (current.birthday != updated.birthday) changes.add("birthday")
        if (current.gender != updated.gender) changes.add("gender")
        if (current.heightInCm != updated.heightInCm) changes.add("heightInCm")
        if (current.weightInKg != updated.weightInKg) changes.add("weightInKg")
        
        return changes
    }
    
    /**
     * Apply partial updates to a profile.
     * 
     * @param currentProfile Current profile
     * @param partialUpdate Map of field updates
     * @return Updated profile
     */
    private fun applyPartialUpdates(
        currentProfile: UserProfile,
        partialUpdate: Map<String, Any>
    ): UserProfile {
        var updatedProfile = currentProfile
        
        partialUpdate.forEach { (field, value) ->
            updatedProfile = when (field) {
                "displayName" -> updatedProfile.copy(displayName = value as String)
                "firstName" -> updatedProfile.copy(firstName = value as String)
                "lastName" -> updatedProfile.copy(lastName = value as String)
                "birthday" -> updatedProfile.copy(birthday = value as java.util.Date?)
                "gender" -> updatedProfile.copy(gender = value as com.vibehealth.android.domain.user.Gender)
                "unitSystem" -> updatedProfile.copy(unitSystem = value as com.vibehealth.android.domain.common.UnitSystem)
                "heightInCm" -> updatedProfile.copy(heightInCm = value as Int)
                "weightInKg" -> updatedProfile.copy(weightInKg = value as Double)
                "hasCompletedOnboarding" -> updatedProfile.copy(hasCompletedOnboarding = value as Boolean)
                else -> {
                    Log.w(TAG, "Unknown field in partial update: $field")
                    updatedProfile
                }
            }
        }
        
        // Update the updatedAt timestamp
        return updatedProfile.copy(updatedAt = java.util.Date())
    }
}

/**
 * Result of profile update operation.
 */
sealed class ProfileUpdateResult {
    /**
     * Successful profile update.
     */
    data class Success(
        val updatedProfile: UserProfile,
        val goalRecalculationResult: GoalCalculationResult?,
        val changesSummary: ProfileChangeDetection
    ) : ProfileUpdateResult()
    
    /**
     * Failed profile update.
     */
    data class Error(
        val error: ProfileUpdateError,
        val message: String
    ) : ProfileUpdateResult()
    
    /**
     * Check if result is successful.
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Check if result is an error.
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Get updated profile if successful, null otherwise.
     */
    fun getProfileOrNull(): UserProfile? = (this as? Success)?.updatedProfile
    
    /**
     * Get error if failed, null otherwise.
     */
    fun getErrorOrNull(): ProfileUpdateError? = (this as? Error)?.error
}

/**
 * Types of profile update errors.
 */
sealed class ProfileUpdateError {
    data class ProfileNotFound(val userId: String) : ProfileUpdateError()
    data class ConcurrentUpdate(val userId: String) : ProfileUpdateError()
    data class ProfileUpdateFailed(val exception: Throwable?) : ProfileUpdateError()
    data class UnexpectedError(val exception: Exception) : ProfileUpdateError()
}

/**
 * Result of profile change detection.
 */
data class ProfileChangeDetection(
    val shouldRecalculate: Boolean,
    val reason: String,
    val changedFields: List<String>,
    val wasValidBefore: Boolean,
    val isValidAfter: Boolean
) {
    /**
     * Get a summary of the changes.
     */
    fun getSummary(): String {
        val validityChange = when {
            !wasValidBefore && isValidAfter -> " (became valid)"
            wasValidBefore && !isValidAfter -> " (became invalid)"
            else -> ""
        }
        
        return "$reason$validityChange. Changed fields: ${changedFields.joinToString(", ").ifEmpty { "none" }}"
    }
    
    /**
     * Check if any goal-affecting fields changed.
     */
    fun hasGoalAffectingChanges(): Boolean {
        val goalAffectingFields = setOf("birthday", "gender", "heightInCm", "weightInKg")
        return changedFields.any { it in goalAffectingFields }
    }
}