package com.vibehealth.android.debug

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.vibehealth.android.domain.goals.DailyGoals
import com.vibehealth.android.domain.goals.CalculationSource
import com.vibehealth.android.data.goals.GoalRepository
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronous goal creator that bypasses coroutine issues
 * This creates goals immediately without async complications
 */
@Singleton
class SynchronousGoalCreator @Inject constructor(
    private val goalRepository: GoalRepository
) {
    
    companion object {
        private const val TAG = "SynchronousGoalCreator"
    }
    
    /**
     * Create goals synchronously using runBlocking
     * This ensures goals are created immediately
     */
    fun createGoalsNow(): Boolean {
        Log.d(TAG, "🚀 SynchronousGoalCreator.createGoalsNow() called")
        
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "❌ No authenticated user found")
                return false
            }
            
            Log.d(TAG, "👤 Creating goals for user: ${currentUser.uid}")
            
            // Create test daily goals
            val testGoals = DailyGoals(
                userId = currentUser.uid,
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30,
                calculatedAt = LocalDateTime.now(),
                calculationSource = CalculationSource.WHO_STANDARD
            )
            
            Log.d(TAG, "📝 Created goals object: $testGoals")
            
            // Use runBlocking to make it synchronous
            val result = runBlocking {
                Log.d(TAG, "💾 Saving goals synchronously...")
                goalRepository.saveGoalsLocally(testGoals, markAsDirty = false)
            }
            
            if (result.isSuccess) {
                Log.d(TAG, "✅ Goals saved successfully!")
                Log.d(TAG, "   - Steps Goal: ${testGoals.stepsGoal}")
                Log.d(TAG, "   - Calories Goal: ${testGoals.caloriesGoal}")
                Log.d(TAG, "   - Heart Points Goal: ${testGoals.heartPointsGoal}")
                
                // Verify the goals were saved
                val verification = verifyGoals(currentUser.uid)
                Log.d(TAG, "🔍 Verification result: $verification")
                
                return verification
            } else {
                Log.e(TAG, "❌ Failed to save goals: ${result.exceptionOrNull()}")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in createGoalsNow", e)
            false
        }
    }
    
    /**
     * Verify goals were saved correctly
     */
    private fun verifyGoals(userId: String): Boolean {
        return try {
            Log.d(TAG, "🔍 Verifying goals for user: $userId")
            
            val savedGoals = runBlocking {
                goalRepository.getCurrentGoalsSync(userId)
            }
            
            if (savedGoals != null) {
                Log.d(TAG, "✅ Goals verification successful: $savedGoals")
                true
            } else {
                Log.e(TAG, "❌ Goals verification failed - no goals found")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during verification", e)
            false
        }
    }
    
    /**
     * Check if goals exist for user
     */
    fun checkGoalsExist(userId: String): Boolean {
        return try {
            Log.d(TAG, "🔍 Checking if goals exist for user: $userId")
            
            val existingGoals = runBlocking {
                goalRepository.getCurrentGoalsSync(userId)
            }
            
            val exists = existingGoals != null
            Log.d(TAG, "📊 Goals exist: $exists")
            
            exists
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception checking goals", e)
            false
        }
    }
}