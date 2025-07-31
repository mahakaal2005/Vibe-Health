package com.vibehealth.android.debug

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.vibehealth.android.domain.goals.GoalCalculationService
import com.vibehealth.android.domain.goals.DailyGoals
import com.vibehealth.android.domain.goals.CalculationSource
import com.vibehealth.android.data.goals.GoalRepository
import com.vibehealth.android.data.user.remote.UserProfileService
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.goals.ActivityLevel
import com.vibehealth.android.domain.common.UnitSystem
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.Date
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Personalized goal creator that uses real user profile data
 * to calculate customized daily goals based on WHO standards
 */
@Singleton
class PersonalizedGoalCreator @Inject constructor(
    private val goalCalculationService: GoalCalculationService,
    private val goalRepository: GoalRepository,
    private val userProfileService: UserProfileService,
    private val userProfileDao: com.vibehealth.android.data.user.local.UserProfileDao
) {
    
    companion object {
        private const val TAG = "HomeFragment" // Using HomeFragment tag for unified logging
    }
    
    /**
     * Create personalized goals based on user profile
     */
    fun createPersonalizedGoals(): Boolean {
        Log.d(TAG, "🎯 Creating personalized goals...")
        android.util.Log.d("HomeFragment", "PersonalizedGoalCreator: 🎯 Creating personalized goals...")
        
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "❌ No authenticated user found")
                android.util.Log.e("HomeFragment", "PersonalizedGoalCreator: ❌ No authenticated user found")
                return false
            }
            
            Log.d(TAG, "👤 Creating personalized goals for user: ${currentUser.uid}")
            android.util.Log.d("HomeFragment", "PersonalizedGoalCreator: 👤 Creating personalized goals for user: ${currentUser.uid}")
            
            // Get user profile
            android.util.Log.d("HomeFragment", "PersonalizedGoalCreator: 📋 About to fetch user profile...")
            val userProfile = runBlocking {
                getUserProfile(currentUser.uid)
            }
            
            if (userProfile == null) {
                Log.w(TAG, "⚠️ No user profile found, creating with fallback profile")
                android.util.Log.w("HomeFragment", "PersonalizedGoalCreator: ⚠️ No user profile found, creating with fallback profile")
                return createWithFallbackProfile(currentUser.uid)
            }
            
            Log.d(TAG, "📋 User profile found: Age=${userProfile.getAge()}, Gender=${userProfile.gender}, Weight=${userProfile.weightInKg}kg, Height=${userProfile.heightInCm}cm")
            
            // Calculate personalized goals
            val personalizedGoals = runBlocking {
                goalCalculationService.calculateGoals(userProfile)
            }
            
            Log.d(TAG, "🧮 Calculated personalized goals:")
            Log.d(TAG, "   - Steps Goal: ${personalizedGoals.stepsGoal} (vs standard 10,000)")
            Log.d(TAG, "   - Calories Goal: ${personalizedGoals.caloriesGoal} (vs standard 2,000)")
            Log.d(TAG, "   - Heart Points Goal: ${personalizedGoals.heartPointsGoal} (vs standard 30)")
            Log.d(TAG, "   - Source: ${personalizedGoals.calculationSource}")
            
            // Ensure user profile exists in local database before saving goals
            Log.d(TAG, "💾 Ensuring user profile exists in local database...")
            val profileSaved = runBlocking {
                ensureUserProfileInDatabase(userProfile)
            }
            
            if (!profileSaved) {
                Log.e(TAG, "❌ Failed to save user profile to local database")
                return false
            }
            
            // Save personalized goals
            Log.d(TAG, "💾 Saving personalized goals to database...")
            Log.d(TAG, "💾 Goals user ID: ${personalizedGoals.userId}")
            Log.d(TAG, "💾 Profile user ID: ${userProfile.userId}")
            val result = runBlocking {
                goalRepository.saveGoalsLocally(personalizedGoals, markAsDirty = false)
            }
            
            if (result.isSuccess) {
                Log.d(TAG, "✅ Personalized goals saved successfully!")
                
                // Verify the goals were saved
                val verification = verifyGoals(currentUser.uid)
                Log.d(TAG, "🔍 Verification result: $verification")
                
                return verification
            } else {
                Log.e(TAG, "❌ Failed to save personalized goals: ${result.exceptionOrNull()}")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in createPersonalizedGoals", e)
            false
        }
    }
    
    /**
     * Get user profile from service
     */
    private suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            Log.d(TAG, "📋 Fetching user profile for: $userId")
            val result = userProfileService.getUserProfile(userId)
            if (result.isSuccess) {
                result.getOrNull()
            } else {
                Log.e(TAG, "❌ Failed to fetch user profile: ${result.exceptionOrNull()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception fetching user profile", e)
            null
        }
    }
    
    /**
     * Create goals with a fallback profile when user profile is incomplete
     */
    private fun createWithFallbackProfile(userId: String): Boolean {
        Log.d(TAG, "🔄 Creating goals with fallback profile...")
        
        return try {
            // Create a reasonable fallback profile for goal calculation
            val currentUser = FirebaseAuth.getInstance().currentUser
            val fallbackBirthday = Calendar.getInstance().apply {
                add(Calendar.YEAR, -30) // 30 years old
            }.time
            
            val fallbackProfile = UserProfile(
                userId = userId,
                email = currentUser?.email ?: "",
                displayName = currentUser?.displayName ?: "User",
                birthday = fallbackBirthday,        // 30 years old
                gender = Gender.OTHER,              // Neutral gender for inclusive calculation
                unitSystem = UnitSystem.METRIC,     // Default to metric
                heightInCm = 170,                   // Average adult height (170cm)
                weightInKg = 70.0,                  // Average adult weight (70kg)
                hasCompletedOnboarding = true,      // Mark as complete for calculation
                createdAt = Date(),
                updatedAt = Date()
            )
            
            Log.d(TAG, "📋 Using fallback profile: Age=${fallbackProfile.getAge()}, Weight=${fallbackProfile.weightInKg}kg, Height=${fallbackProfile.heightInCm}cm")
            
            // Ensure fallback profile exists in local database
            Log.d(TAG, "💾 Ensuring fallback profile exists in local database...")
            val profileSaved = runBlocking {
                ensureUserProfileInDatabase(fallbackProfile)
            }
            
            if (!profileSaved) {
                Log.e(TAG, "❌ Failed to save fallback profile to local database")
                return false
            }
            
            // Calculate goals with fallback profile
            val personalizedGoals = runBlocking {
                goalCalculationService.calculateGoals(fallbackProfile)
            }
            
            Log.d(TAG, "🧮 Calculated goals with fallback profile:")
            Log.d(TAG, "   - Steps Goal: ${personalizedGoals.stepsGoal}")
            Log.d(TAG, "   - Calories Goal: ${personalizedGoals.caloriesGoal}")
            Log.d(TAG, "   - Heart Points Goal: ${personalizedGoals.heartPointsGoal}")
            
            // Save goals
            Log.d(TAG, "💾 Saving fallback goals to database...")
            Log.d(TAG, "💾 Fallback goals user ID: '${personalizedGoals.userId}'")
            Log.d(TAG, "💾 Fallback profile user ID: '${fallbackProfile.userId}'")
            
            // Double-check profile exists right before saving goals
            val profileCheck = runBlocking {
                userProfileDao.getUserProfile(personalizedGoals.userId)
            }
            
            if (profileCheck != null) {
                Log.d(TAG, "✅ Profile confirmed exists right before goal save: '${profileCheck.userId}'")
                
                // Check if foreign keys are enabled in the database
                val foreignKeysEnabled = runBlocking {
                    try {
                        // This is a hack to check foreign key status
                        userProfileDao.getUserProfileCount()
                        Log.d(TAG, "🔧 Database connection is working")
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Database connection issue", e)
                        false
                    }
                }
                
                Log.d(TAG, "🔧 Database status check: $foreignKeysEnabled")
            } else {
                Log.e(TAG, "❌ Profile NOT found right before goal save!")
                return false
            }
            
            val result = runBlocking {
                goalRepository.saveGoalsLocally(personalizedGoals, markAsDirty = false)
            }
            
            if (result.isSuccess) {
                Log.d(TAG, "✅ Fallback personalized goals saved successfully!")
                return true
            } else {
                Log.e(TAG, "❌ Failed to save fallback goals: ${result.exceptionOrNull()}")
                Log.d(TAG, "💡 Database constraint issue - personalized calculation worked but save failed")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception creating fallback goals", e)
            false
        }
    }
    
    /**
     * Verify goals were saved correctly
     */
    private fun verifyGoals(userId: String): Boolean {
        return try {
            Log.d(TAG, "🔍 Verifying personalized goals for user: $userId")
            
            val savedGoals = runBlocking {
                goalRepository.getCurrentGoalsSync(userId)
            }
            
            if (savedGoals != null) {
                Log.d(TAG, "✅ Goals verification successful:")
                Log.d(TAG, "   - Steps: ${savedGoals.stepsGoal}")
                Log.d(TAG, "   - Calories: ${savedGoals.caloriesGoal}")
                Log.d(TAG, "   - Heart Points: ${savedGoals.heartPointsGoal}")
                Log.d(TAG, "   - Source: ${savedGoals.calculationSource}")
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
     * Check if personalized goals exist for user
     */
    fun checkPersonalizedGoalsExist(userId: String): Boolean {
        return try {
            Log.d(TAG, "🔍 Checking if personalized goals exist for user: $userId")
            
            val existingGoals = runBlocking {
                goalRepository.getCurrentGoalsSync(userId)
            }
            
            val exists = existingGoals != null
            Log.d(TAG, "📊 Personalized goals exist: $exists")
            
            if (exists && existingGoals != null) {
                Log.d(TAG, "📊 Current goals: Steps=${existingGoals.stepsGoal}, Calories=${existingGoals.caloriesGoal}, HeartPoints=${existingGoals.heartPointsGoal}")
            }
            
            exists
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception checking personalized goals", e)
            false
        }
    }
    
    /**
     * Ensure user profile exists in local database before saving goals
     */
    private suspend fun ensureUserProfileInDatabase(userProfile: UserProfile): Boolean {
        return try {
            // Validate user ID is not empty
            if (userProfile.userId.isBlank()) {
                Log.e(TAG, "❌ Cannot save profile with empty user ID")
                return false
            }
            
            Log.d(TAG, "🔍 Checking if user profile exists in local database...")
            Log.d(TAG, "🔍 Looking for user ID: '${userProfile.userId}'")
            
            val exists = userProfileDao.userProfileExists(userProfile.userId)
            if (exists) {
                Log.d(TAG, "✅ User profile already exists in local database")
                
                // Double-check by fetching the profile
                val existingProfile = userProfileDao.getUserProfile(userProfile.userId)
                if (existingProfile != null) {
                    Log.d(TAG, "✅ Confirmed: Profile found with ID: ${existingProfile.userId}")
                } else {
                    Log.w(TAG, "⚠️ Profile exists check returned true but fetch returned null")
                }
                
                // Debug: Check all profiles in database
                val allProfiles = userProfileDao.getAllUserProfiles()
                Log.d(TAG, "🔍 Total profiles in database: ${allProfiles.size}")
                allProfiles.forEach { profile ->
                    Log.d(TAG, "🔍 Profile in DB: '${profile.userId}'")
                }
                
                // Clean up any profiles with empty user IDs
                val emptyProfiles = allProfiles.filter { it.userId.isBlank() }
                if (emptyProfiles.isNotEmpty()) {
                    Log.w(TAG, "🧹 Found ${emptyProfiles.size} profiles with empty user IDs, cleaning up...")
                    emptyProfiles.forEach { emptyProfile ->
                        userProfileDao.deleteUserProfile(emptyProfile)
                        Log.d(TAG, "🧹 Deleted empty profile")
                    }
                }
                
                return true
            }
            
            Log.d(TAG, "💾 User profile not found, saving to local database...")
            val entity = com.vibehealth.android.data.user.local.UserProfileEntity.fromDomainModel(
                userProfile, 
                isDirty = false
            )
            
            val result = userProfileDao.insertUserProfile(entity)
            if (result > 0) {
                Log.d(TAG, "✅ User profile saved to local database successfully")
                true
            } else {
                Log.e(TAG, "❌ Failed to save user profile to local database")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception ensuring user profile in database", e)
            false
        }
    }
}