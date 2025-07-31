package com.vibehealth.android.debug

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.vibehealth.android.domain.goals.DailyGoals
import com.vibehealth.android.domain.goals.CalculationSource
import com.vibehealth.android.data.goals.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct goal injector for debugging dashboard issues
 * This bypasses complex goal calculation and directly injects goals into the repository
 */
@Singleton
class DirectGoalInjector @Inject constructor(
    private val goalRepository: GoalRepository
) {
    
    companion object {
        private const val TAG = "DirectGoalInjector"
    }
    
    /**
     * Directly inject test goals into the goal repository
     * This ensures the dashboard can find goals in the local database
     */
    fun injectTestGoals() {
        Log.d(TAG, "🚀 DirectGoalInjector.injectTestGoals() called")
        
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "❌ No authenticated user found")
                return
            }
            
            Log.d(TAG, "💉 Injecting test goals directly into repository for user: ${currentUser.uid}")
            
            // Launch coroutine with better error handling
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "🔄 Inside coroutine - starting goal creation...")
                    
                    // Create test daily goals
                    val testGoals = DailyGoals(
                        userId = currentUser.uid,
                        stepsGoal = 10000,
                        caloriesGoal = 2000,
                        heartPointsGoal = 30,
                        calculatedAt = LocalDateTime.now(),
                        calculationSource = CalculationSource.WHO_STANDARD
                    )
                    
                    Log.d(TAG, "📝 Created test goals: $testGoals")
                    
                    // Save directly to the goal repository (this will save to local database)
                    Log.d(TAG, "💾 Saving goals to repository...")
                    val result = goalRepository.saveGoalsLocally(testGoals, markAsDirty = false)
                    
                    if (result.isSuccess) {
                        Log.d(TAG, "✅ Test goals injected successfully!")
                        Log.d(TAG, "   - Steps Goal: ${testGoals.stepsGoal}")
                        Log.d(TAG, "   - Calories Goal: ${testGoals.caloriesGoal}")
                        Log.d(TAG, "   - Heart Points Goal: ${testGoals.heartPointsGoal}")
                        
                        // Verify the goals were saved
                        verifyGoalsInjection(currentUser.uid)
                    } else {
                        Log.e(TAG, "❌ Failed to inject test goals: ${result.exceptionOrNull()}")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception during goal injection in coroutine", e)
                }
            }
            
            Log.d(TAG, "🚀 Coroutine launched successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in injectTestGoals method", e)
        }
    }
    
    /**
     * Verify that goals were successfully injected
     */
    private suspend fun verifyGoalsInjection(userId: String) {
        try {
            Log.d(TAG, "🔍 Verifying goal injection...")
            
            val savedGoals = goalRepository.getCurrentGoalsSync(userId)
            if (savedGoals != null) {
                Log.d(TAG, "✅ Goals verification successful: $savedGoals")
            } else {
                Log.e(TAG, "❌ Goals verification failed - no goals found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during goal verification", e)
        }
    }
    
    /**
     * Check if goals exist for the current user
     */
    fun checkGoalsExist() {
        Log.d(TAG, "🚀 DirectGoalInjector.checkGoalsExist() called")
        
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "❌ No authenticated user found")
                return
            }
            
            Log.d(TAG, "🔍 Checking if goals exist for user: ${currentUser.uid}")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "🔄 Inside coroutine - checking goals...")
                    val existingGoals = goalRepository.getCurrentGoalsSync(currentUser.uid)
                    if (existingGoals != null) {
                        Log.d(TAG, "✅ Goals found: $existingGoals")
                    } else {
                        Log.d(TAG, "❌ No goals found for user")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception checking goals in coroutine", e)
                }
            }
            
            Log.d(TAG, "🚀 Check goals coroutine launched successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in checkGoalsExist method", e)
        }
    }
}