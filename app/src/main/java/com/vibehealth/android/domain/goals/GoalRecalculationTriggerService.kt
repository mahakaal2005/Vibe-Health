package com.vibehealth.android.domain.goals

import android.util.Log
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing automatic goal recalculation triggers.
 * 
 * This service listens for user profile changes and intelligently triggers
 * goal recalculation when relevant fields change, with debouncing to avoid
 * excessive calculations and history tracking for debugging.
 */
@Singleton
class GoalRecalculationTriggerService @Inject constructor(
    private val goalCalculationUseCase: GoalCalculationUseCase
) {
    
    companion object {
        private const val TAG = "GoalRecalculationTrigger"
        private const val DEBOUNCE_DELAY_MS = 2000L // 2 seconds
        private const val MAX_HISTORY_SIZE = 100
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Track pending recalculations to enable debouncing
    private val pendingRecalculations = ConcurrentHashMap<String, Job>()
    
    // Track calculation history for debugging
    private val calculationHistory = mutableListOf<CalculationTriggerEvent>()
    
    // Track profile change listeners
    private val profileChangeListeners = mutableSetOf<ProfileChangeListener>()
    
    /**
     * Register a profile change listener.
     * 
     * @param listener Listener to register
     */
    fun registerProfileChangeListener(listener: ProfileChangeListener) {
        profileChangeListeners.add(listener)
        Log.d(TAG, "Registered profile change listener: ${listener::class.simpleName}")
    }
    
    /**
     * Unregister a profile change listener.
     * 
     * @param listener Listener to unregister
     */
    fun unregisterProfileChangeListener(listener: ProfileChangeListener) {
        profileChangeListeners.remove(listener)
        Log.d(TAG, "Unregistered profile change listener: ${listener::class.simpleName}")
    }
    
    /**
     * Handle profile update and trigger recalculation if needed.
     * 
     * This method implements debouncing to avoid excessive calculations
     * when users make rapid profile changes.
     * 
     * @param oldProfile Previous profile state (null for new profiles)
     * @param newProfile Updated profile state
     */
    fun onProfileUpdated(oldProfile: UserProfile?, newProfile: UserProfile) {
        scope.launch {
            try {
                // Check if recalculation is needed
                val triggerReason = shouldTriggerRecalculation(oldProfile, newProfile)
                if (triggerReason == null) {
                    Log.d(TAG, "Profile update doesn't require goal recalculation for user: ${newProfile.userId}")
                    return@launch
                }
                
                Log.d(TAG, "Profile update triggers goal recalculation for user: ${newProfile.userId}, reason: $triggerReason")
                
                // Cancel any pending recalculation for this user
                pendingRecalculations[newProfile.userId]?.cancel()
                
                // Schedule debounced recalculation
                val recalculationJob = launch {
                    delay(DEBOUNCE_DELAY_MS)
                    
                    try {
                        performRecalculation(newProfile, triggerReason)
                    } catch (e: CancellationException) {
                        Log.d(TAG, "Recalculation cancelled for user: ${newProfile.userId}")
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Recalculation failed for user: ${newProfile.userId}", e)
                        recordCalculationEvent(
                            userId = newProfile.userId,
                            triggerReason = triggerReason,
                            success = false,
                            errorMessage = e.message
                        )
                    } finally {
                        pendingRecalculations.remove(newProfile.userId)
                    }
                }
                
                pendingRecalculations[newProfile.userId] = recalculationJob
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling profile update for user: ${newProfile.userId}", e)
            }
        }
    }
    
    /**
     * Force immediate recalculation for a user (bypasses debouncing).
     * 
     * @param userProfile User profile to recalculate goals for
     * @param reason Reason for forced recalculation
     */
    fun forceRecalculation(userProfile: UserProfile, reason: String) {
        scope.launch {
            try {
                // Cancel any pending recalculation
                pendingRecalculations[userProfile.userId]?.cancel()
                pendingRecalculations.remove(userProfile.userId)
                
                Log.d(TAG, "Forcing immediate recalculation for user: ${userProfile.userId}, reason: $reason")
                performRecalculation(userProfile, reason)
                
            } catch (e: Exception) {
                Log.e(TAG, "Forced recalculation failed for user: ${userProfile.userId}", e)
                recordCalculationEvent(
                    userId = userProfile.userId,
                    triggerReason = reason,
                    success = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    /**
     * Get calculation history for debugging purposes.
     * 
     * @param userId Optional user ID to filter history
     * @return List of calculation trigger events
     */
    fun getCalculationHistory(userId: String? = null): List<CalculationTriggerEvent> {
        return synchronized(calculationHistory) {
            if (userId != null) {
                calculationHistory.filter { it.userId == userId }
            } else {
                calculationHistory.toList()
            }
        }
    }
    
    /**
     * Clear calculation history.
     */
    fun clearCalculationHistory() {
        synchronized(calculationHistory) {
            calculationHistory.clear()
        }
        Log.d(TAG, "Calculation history cleared")
    }
    
    /**
     * Get pending recalculations count.
     * 
     * @return Number of users with pending recalculations
     */
    fun getPendingRecalculationsCount(): Int {
        return pendingRecalculations.size
    }
    
    /**
     * Cancel all pending recalculations.
     */
    fun cancelAllPendingRecalculations() {
        pendingRecalculations.values.forEach { it.cancel() }
        pendingRecalculations.clear()
        Log.d(TAG, "All pending recalculations cancelled")
    }
    
    // Private helper methods
    
    /**
     * Determine if profile changes should trigger goal recalculation.
     * 
     * @param oldProfile Previous profile state
     * @param newProfile Updated profile state
     * @return Reason for recalculation or null if not needed
     */
    private fun shouldTriggerRecalculation(oldProfile: UserProfile?, newProfile: UserProfile): String? {
        // Always trigger for new profiles
        if (oldProfile == null) {
            return "New profile created"
        }
        
        // Check if profile is now valid for calculation when it wasn't before
        if (!oldProfile.isValidForGoalCalculation() && newProfile.isValidForGoalCalculation()) {
            return "Profile became valid for calculation"
        }
        
        // Check if profile became invalid
        if (oldProfile.isValidForGoalCalculation() && !newProfile.isValidForGoalCalculation()) {
            return "Profile became invalid for calculation"
        }
        
        // Only check specific fields if both profiles are valid
        if (!newProfile.isValidForGoalCalculation()) {
            return null
        }
        
        // Check goal-affecting fields
        val changes = mutableListOf<String>()
        
        if (oldProfile.birthday != newProfile.birthday) {
            changes.add("birthday")
        }
        
        if (oldProfile.gender != newProfile.gender) {
            changes.add("gender")
        }
        
        if (oldProfile.heightInCm != newProfile.heightInCm) {
            changes.add("height")
        }
        
        if (oldProfile.weightInKg != newProfile.weightInKg) {
            changes.add("weight")
        }
        
        return if (changes.isNotEmpty()) {
            "Profile fields changed: ${changes.joinToString(", ")}"
        } else {
            null
        }
    }
    
    /**
     * Perform the actual goal recalculation.
     * 
     * @param userProfile User profile to recalculate goals for
     * @param triggerReason Reason for the recalculation
     */
    private suspend fun performRecalculation(userProfile: UserProfile, triggerReason: String) {
        val startTime = System.currentTimeMillis()
        
        try {
            val result = goalCalculationUseCase.recalculateGoalsForProfileUpdate(
                userId = userProfile.userId,
                changedProfile = userProfile
            )
            
            val duration = System.currentTimeMillis() - startTime
            
            when (result) {
                is GoalCalculationResult.Success -> {
                    Log.d(TAG, "Goal recalculation successful for user: ${userProfile.userId} in ${duration}ms")
                    
                    recordCalculationEvent(
                        userId = userProfile.userId,
                        triggerReason = triggerReason,
                        success = true,
                        duration = duration,
                        wasRecalculated = result.wasRecalculated
                    )
                    
                    // Notify listeners
                    notifyListeners(userProfile.userId, result.goals, triggerReason)
                }
                
                is GoalCalculationResult.Error -> {
                    Log.w(TAG, "Goal recalculation failed for user: ${userProfile.userId}: ${result.message}")
                    
                    recordCalculationEvent(
                        userId = userProfile.userId,
                        triggerReason = triggerReason,
                        success = false,
                        duration = duration,
                        errorMessage = result.message
                    )
                }
            }
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Exception during goal recalculation for user: ${userProfile.userId}", e)
            
            recordCalculationEvent(
                userId = userProfile.userId,
                triggerReason = triggerReason,
                success = false,
                duration = duration,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Record a calculation trigger event for debugging.
     * 
     * @param userId User ID
     * @param triggerReason Reason for the calculation
     * @param success Whether the calculation was successful
     * @param duration Duration in milliseconds
     * @param wasRecalculated Whether goals were actually recalculated
     * @param errorMessage Error message if failed
     */
    private fun recordCalculationEvent(
        userId: String,
        triggerReason: String,
        success: Boolean,
        duration: Long? = null,
        wasRecalculated: Boolean? = null,
        errorMessage: String? = null
    ) {
        val event = CalculationTriggerEvent(
            userId = userId,
            timestamp = LocalDateTime.now(),
            triggerReason = triggerReason,
            success = success,
            duration = duration,
            wasRecalculated = wasRecalculated,
            errorMessage = errorMessage
        )
        
        synchronized(calculationHistory) {
            calculationHistory.add(event)
            
            // Keep history size manageable
            if (calculationHistory.size > MAX_HISTORY_SIZE) {
                calculationHistory.removeAt(0)
            }
        }
    }
    
    /**
     * Notify registered listeners about goal recalculation.
     * 
     * @param userId User ID
     * @param newGoals Newly calculated goals
     * @param triggerReason Reason for the recalculation
     */
    private fun notifyListeners(userId: String, newGoals: DailyGoals, triggerReason: String) {
        profileChangeListeners.forEach { listener ->
            try {
                listener.onGoalsRecalculated(userId, newGoals, triggerReason)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying listener: ${listener::class.simpleName}", e)
            }
        }
    }
    
    /**
     * Clean up resources when service is destroyed.
     */
    fun destroy() {
        cancelAllPendingRecalculations()
        scope.cancel()
        profileChangeListeners.clear()
        clearCalculationHistory()
        Log.d(TAG, "GoalRecalculationTriggerService destroyed")
    }
}

/**
 * Interface for listening to profile changes and goal recalculations.
 */
interface ProfileChangeListener {
    /**
     * Called when goals are recalculated due to profile changes.
     * 
     * @param userId User ID
     * @param newGoals Newly calculated goals
     * @param triggerReason Reason for the recalculation
     */
    fun onGoalsRecalculated(userId: String, newGoals: DailyGoals, triggerReason: String)
}

/**
 * Event representing a goal calculation trigger for debugging.
 */
data class CalculationTriggerEvent(
    val userId: String,
    val timestamp: LocalDateTime,
    val triggerReason: String,
    val success: Boolean,
    val duration: Long? = null,
    val wasRecalculated: Boolean? = null,
    val errorMessage: String? = null
) {
    /**
     * Get a human-readable summary of the event.
     */
    fun getSummary(): String {
        val status = if (success) "SUCCESS" else "FAILED"
        val durationText = duration?.let { " (${it}ms)" } ?: ""
        val recalcText = wasRecalculated?.let { if (it) " [RECALCULATED]" else " [CACHED]" } ?: ""
        val errorText = errorMessage?.let { " - $it" } ?: ""
        
        return "[$timestamp] $status: $triggerReason$durationText$recalcText$errorText"
    }
}