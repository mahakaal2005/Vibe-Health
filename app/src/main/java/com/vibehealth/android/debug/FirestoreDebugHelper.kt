package com.vibehealth.android.debug

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug helper for testing Firestore user profile integration
 */
@Singleton
class FirestoreDebugHelper @Inject constructor(
    private val userRepository: UserRepository,
    private val goalCalculationUseCase: com.vibehealth.android.domain.goals.GoalCalculationUseCase
) {
    
    companion object {
        private const val TAG = "FirestoreDebugHelper"
    }
    
    /**
     * Create and save a test user profile to verify Firestore integration
     */
    fun createTestUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found")
            return
        }
        
        val testProfile = UserProfile(
            userId = currentUser.uid,
            email = currentUser.email ?: "test@example.com",
            displayName = "Test User",
            firstName = "Test",
            lastName = "User",
            birthday = Date(System.currentTimeMillis() - (25 * 365 * 24 * 60 * 60 * 1000L)), // 25 years ago
            gender = Gender.PREFER_NOT_TO_SAY,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 175,
            weightInKg = 70.0,
            hasCompletedOnboarding = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        Log.d(TAG, "Creating test user profile: $testProfile")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = userRepository.saveUserProfile(testProfile)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Test user profile saved successfully")
                    
                    // Verify by reading it back
                    val readResult = userRepository.getUserProfile(currentUser.uid)
                    if (readResult.isSuccess) {
                        val savedProfile = readResult.getOrNull()
                        Log.d(TAG, "✅ Test user profile read back: $savedProfile")
                        
                        if (savedProfile != null && savedProfile.isOnboardingDataComplete()) {
                            Log.d(TAG, "✅ All onboarding data is complete in Firestore")
                        } else {
                            Log.e(TAG, "❌ Onboarding data is incomplete in Firestore")
                        }
                    } else {
                        Log.e(TAG, "❌ Failed to read back test profile: ${readResult.exceptionOrNull()}")
                    }
                } else {
                    Log.e(TAG, "❌ Failed to save test user profile: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during test profile creation", e)
            }
        }
    }
    
    /**
     * Verify current user's profile data in Firestore
     */
    fun verifyCurrentUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found")
            return
        }
        
        Log.d(TAG, "Verifying profile for user: ${currentUser.uid}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = userRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val profile = result.getOrNull()
                    if (profile != null) {
                        Log.d(TAG, "✅ User profile found in Firestore:")
                        Log.d(TAG, "   - userId: ${profile.userId}")
                        Log.d(TAG, "   - email: ${profile.email}")
                        Log.d(TAG, "   - displayName: ${profile.displayName}")
                        Log.d(TAG, "   - birthday: ${profile.birthday}")
                        Log.d(TAG, "   - gender: ${profile.gender}")
                        Log.d(TAG, "   - height: ${profile.heightInCm} cm")
                        Log.d(TAG, "   - weight: ${profile.weightInKg} kg")
                        Log.d(TAG, "   - hasCompletedOnboarding: ${profile.hasCompletedOnboarding}")
                        Log.d(TAG, "   - isOnboardingDataComplete: ${profile.isOnboardingDataComplete()}")
                        Log.d(TAG, "   - isValidForGoalCalculation: ${profile.isValidForGoalCalculation()}")
                    } else {
                        Log.e(TAG, "❌ User profile is null")
                    }
                } else {
                    Log.e(TAG, "❌ Failed to get user profile: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during profile verification", e)
            }
        }
    }
    
    /**
     * Check onboarding completion status
     */
    fun checkOnboardingStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found")
            return
        }
        
        Log.d(TAG, "Checking onboarding status for user: ${currentUser.uid}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isComplete = userRepository.isOnboardingComplete(currentUser.uid)
                Log.d(TAG, "Onboarding complete: $isComplete")
                
                val hasCompleted = userRepository.hasCompletedOnboarding(currentUser.uid)
                if (hasCompleted.isSuccess) {
                    Log.d(TAG, "Has completed onboarding: ${hasCompleted.getOrNull()}")
                } else {
                    Log.e(TAG, "Failed to check onboarding completion: ${hasCompleted.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during onboarding status check", e)
            }
        }
    }
    
    /**
     * Test AuthService to ensure user document creation works
     */
    fun testAuthService() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found")
            return
        }
        
        Log.d(TAG, "Testing AuthService for user: ${currentUser.uid}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = com.vibehealth.android.data.auth.AuthService(
                    FirebaseAuth.getInstance(),
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                )
                
                val result = authService.ensureUserDocumentExists()
                if (result.isSuccess) {
                    val profile = result.getOrNull()
                    Log.d(TAG, "✅ AuthService test successful. Profile: $profile")
                } else {
                    Log.e(TAG, "❌ AuthService test failed: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during AuthService test", e)
            }
        }
    }
    
    /**
     * Force update the current user's profile with complete onboarding data
     * This is a debug function to manually fix incomplete profiles
     */
    fun forceUpdateUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found")
            return
        }
        
        Log.d(TAG, "🔧 Force updating user profile for: ${currentUser.uid}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create a complete test profile with all required fields
                val completeProfile = UserProfile(
                    userId = currentUser.uid,
                    email = currentUser.email ?: "test@example.com",
                    displayName = currentUser.displayName ?: "Test User",
                    firstName = "Test",
                    lastName = "User",
                    birthday = Date(System.currentTimeMillis() - (25 * 365 * 24 * 60 * 60 * 1000L)), // 25 years ago
                    gender = com.vibehealth.android.domain.user.Gender.PREFER_NOT_TO_SAY,
                    unitSystem = com.vibehealth.android.domain.common.UnitSystem.METRIC,
                    heightInCm = 175,
                    weightInKg = 70.0,
                    hasCompletedOnboarding = true,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                Log.d(TAG, "🔧 Forcing profile update with complete data: $completeProfile")
                
                val result = userRepository.saveUserProfile(completeProfile)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Force profile update successful!")
                    
                    // Verify the update worked
                    val verifyResult = userRepository.getUserProfile(currentUser.uid)
                    if (verifyResult.isSuccess) {
                        val savedProfile = verifyResult.getOrNull()
                        Log.d(TAG, "✅ Verification successful. Updated profile: $savedProfile")
                        
                        if (savedProfile != null && savedProfile.isOnboardingDataComplete()) {
                            Log.d(TAG, "✅ Profile is now complete with all onboarding data!")
                        } else {
                            Log.e(TAG, "❌ Profile is still incomplete after force update")
                        }
                    } else {
                        Log.e(TAG, "❌ Failed to verify profile update: ${verifyResult.exceptionOrNull()}")
                    }
                } else {
                    Log.e(TAG, "❌ Force profile update failed: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during force profile update", e)
            }
        }
    }
    
    /**
     * Direct Firestore test - bypasses repository layer
     */
    fun directFirestoreTest() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found")
            return
        }
        
        Log.d(TAG, "🔥 Direct Firestore test for user: ${currentUser.uid}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                // Create complete profile data map
                val profileData = mapOf(
                    "userId" to currentUser.uid,
                    "email" to (currentUser.email ?: "test@example.com"),
                    "displayName" to (currentUser.displayName ?: "Test User"),
                    "firstName" to "Test",
                    "lastName" to "User",
                    "birthday" to Date(System.currentTimeMillis() - (25 * 365 * 24 * 60 * 60 * 1000L)),
                    "gender" to "PREFER_NOT_TO_SAY",
                    "unitSystem" to "METRIC",
                    "heightInCm" to 175,
                    "weightInKg" to 70.0,
                    "hasCompletedOnboarding" to true,
                    "createdAt" to Date(),
                    "updatedAt" to Date()
                )
                
                Log.d(TAG, "🔥 Writing directly to Firestore: $profileData")
                
                firestore.collection("users")
                    .document(currentUser.uid)
                    .set(profileData)
                    .await()
                
                Log.d(TAG, "✅ Direct Firestore write successful!")
                
                // Read it back to verify
                val document = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                if (document.exists()) {
                    val data = document.data
                    Log.d(TAG, "✅ Direct Firestore read successful: $data")
                } else {
                    Log.e(TAG, "❌ Document doesn't exist after direct write")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during direct Firestore test", e)
            }
        }
    }
    
    /**
     * Trigger goal calculation using the injected use case
     */
    fun triggerGoalCalculation() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found")
            return
        }
        
        Log.d(TAG, "🎯 Triggering goal calculation for user: ${currentUser.uid}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = goalCalculationUseCase.calculateAndStoreGoals(
                    userId = currentUser.uid,
                    forceRecalculation = true
                )
                
                when (result) {
                    is com.vibehealth.android.domain.goals.GoalCalculationResult.Success -> {
                        Log.d(TAG, "✅ Goal calculation successful!")
                        Log.d(TAG, "   - Steps Goal: ${result.goals.stepsGoal}")
                        Log.d(TAG, "   - Calories Goal: ${result.goals.caloriesGoal}")
                        Log.d(TAG, "   - Heart Points Goal: ${result.goals.heartPointsGoal}")
                        Log.d(TAG, "   - Source: ${result.goals.calculationSource}")
                    }
                    is com.vibehealth.android.domain.goals.GoalCalculationResult.Error -> {
                        Log.e(TAG, "❌ Goal calculation failed: ${result.message}")
                        Log.e(TAG, "   - Error: ${result.error}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during goal calculation", e)
            }
        }
    }
}