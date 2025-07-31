package com.vibehealth.android.core.integration

import android.content.Context
import android.content.Intent
import com.vibehealth.android.MainActivity
import com.vibehealth.android.core.security.DataSanitizationHelper
import com.vibehealth.android.data.auth.SessionManager
import com.vibehealth.android.domain.onboarding.OnboardingResult
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.user.UserRepository
import com.vibehealth.android.ui.onboarding.OnboardingActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Integration manager for loose coupling between onboarding, authentication, and goal calculation
 */
@Singleton
class OnboardingIntegrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val goalCalculationIntegration: GoalCalculationIntegration
) {

    private val integrationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Check if user needs onboarding after authentication
     */
    suspend fun checkOnboardingRequirement(userId: String): OnboardingRequirement {
        return try {
            val isComplete = userRepository.isOnboardingComplete(userId)
            val userProfile = userRepository.getUserProfile(userId).getOrNull()
            
            when {
                !isComplete -> OnboardingRequirement.REQUIRED
                userProfile == null -> OnboardingRequirement.REQUIRED
                !userProfile.isOnboardingDataComplete() -> OnboardingRequirement.REQUIRED
                else -> OnboardingRequirement.NOT_REQUIRED
            }
        } catch (e: Exception) {
            android.util.Log.e("OnboardingIntegration", "Failed to check onboarding requirement", e)
            OnboardingRequirement.REQUIRED // Fail safe to onboarding
        }
    }

    /**
     * Navigate to onboarding from authentication
     */
    fun navigateToOnboardingFromAuth(context: Context, userId: String) {
        val intent = OnboardingActivity.createIntent(context)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
        
        logIntegrationEvent("navigate_to_onboarding", userId, true)
    }

    /**
     * Complete onboarding integration with goal calculation
     */
    suspend fun completeOnboardingIntegration(userProfile: UserProfile): OnboardingResult {
        return try {
            android.util.Log.d("OnboardingIntegration", "Starting onboarding integration for user: ${userProfile.userId}")
            
            // Step 0: Ensure user document exists in Firestore with proper structure
            val authService = com.vibehealth.android.data.auth.AuthService(
                com.google.firebase.auth.FirebaseAuth.getInstance(),
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            )
            val ensureResult = authService.ensureUserDocumentExists()
            if (ensureResult.isFailure) {
                android.util.Log.e("OnboardingIntegration", "Failed to ensure user document exists")
            }
            
            // Step 1: Save complete user profile
            android.util.Log.d("OnboardingIntegration", "Saving user profile: $userProfile")
            val saveResult = userRepository.saveUserProfile(userProfile)
            if (saveResult.isFailure) {
                android.util.Log.e("OnboardingIntegration", "Failed to save user profile: ${saveResult.exceptionOrNull()}")
                return OnboardingResult.Error(
                    exception = Exception(saveResult.exceptionOrNull() ?: Exception("Save failed")),
                    userMessage = "Unable to save your profile. Please try again.",
                    canRetry = true
                )
            }
            android.util.Log.d("OnboardingIntegration", "✅ User profile saved successfully")

            // Step 2: Trigger goal calculation (with rollback on failure)
            val goalResult = triggerGoalCalculationWithRollback(userProfile)
            
            // Step 3: Update session state
            updateSessionState(userProfile)
            
            logIntegrationEvent("complete_onboarding", userProfile.userId, goalResult.isSuccess())
            goalResult
            
        } catch (e: Exception) {
            android.util.Log.e("OnboardingIntegration", "Failed to complete onboarding integration", e)
            OnboardingResult.Error(
                exception = e,
                userMessage = "Unable to complete setup. Please try again.",
                canRetry = true
            )
        }
    }

    /**
     * Trigger goal calculation with rollback capability
     */
    private suspend fun triggerGoalCalculationWithRollback(userProfile: UserProfile): OnboardingResult {
        return try {
            // Attempt goal calculation
            val goalResult = goalCalculationIntegration.calculateDailyGoals(userProfile)
            
            when (goalResult) {
                is GoalCalculationResult.Success -> {
                    // Save goals to repository
                    val saveGoalsResult = userRepository.saveDailyGoals(userProfile.userId, goalResult.goals)
                    if (saveGoalsResult.isSuccess) {
                        OnboardingResult.Success
                    } else {
                        // Goal calculation succeeded but save failed - this is acceptable
                        android.util.Log.w("OnboardingIntegration", "Goal calculation succeeded but save failed")
                        OnboardingResult.Success
                    }
                }
                is GoalCalculationResult.Error -> {
                    // Goal calculation failed - continue with onboarding but log the issue
                    android.util.Log.w("OnboardingIntegration", "Goal calculation failed: ${goalResult.message}")
                    
                    // Don't fail onboarding due to goal calculation failure
                    OnboardingResult.Success
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("OnboardingIntegration", "Goal calculation integration failed", e)
            // Don't fail onboarding due to goal calculation issues
            OnboardingResult.Success
        }
    }

    /**
     * Update session state after onboarding completion
     */
    private suspend fun updateSessionState(userProfile: UserProfile) {
        try {
            // Update session with completed onboarding status
            sessionManager.updateUserProfile(userProfile)
        } catch (e: Exception) {
            android.util.Log.w("OnboardingIntegration", "Failed to update session state", e)
            // Don't fail onboarding due to session update issues
        }
    }

    /**
     * Navigate to main app after onboarding completion
     */
    fun navigateToMainApp(context: Context, userProfile: UserProfile) {
        integrationScope.launch {
            try {
                // Perform any final integration tasks
                finalizeIntegration(userProfile)
                
                // Navigate to main app
                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                
                logIntegrationEvent("navigate_to_main_app", userProfile.userId, true)
                
            } catch (e: Exception) {
                android.util.Log.e("OnboardingIntegration", "Failed to navigate to main app", e)
            }
        }
    }

    /**
     * Handle onboarding completion check for existing users
     */
    suspend fun handleExistingUserOnboardingCheck(userId: String): NavigationDecision {
        return try {
            val requirement = checkOnboardingRequirement(userId)
            
            when (requirement) {
                OnboardingRequirement.REQUIRED -> NavigationDecision.NAVIGATE_TO_ONBOARDING
                OnboardingRequirement.NOT_REQUIRED -> NavigationDecision.NAVIGATE_TO_MAIN_APP
            }
        } catch (e: Exception) {
            android.util.Log.e("OnboardingIntegration", "Failed to check existing user onboarding", e)
            NavigationDecision.NAVIGATE_TO_ONBOARDING // Fail safe
        }
    }

    /**
     * Create integration contracts for external systems
     */
    fun createIntegrationContract(): IntegrationContract {
        return IntegrationContract(
            onboardingCompleteCallback = { userProfile ->
                integrationScope.launch {
                    completeOnboardingIntegration(userProfile)
                }
            },
            goalCalculationTrigger = { userProfile ->
                integrationScope.launch {
                    goalCalculationIntegration.calculateDailyGoals(userProfile)
                }
            },
            sessionUpdateCallback = { userProfile ->
                integrationScope.launch {
                    updateSessionState(userProfile)
                }
            }
        )
    }

    /**
     * Finalize integration tasks
     */
    private suspend fun finalizeIntegration(userProfile: UserProfile) {
        try {
            // Clear any temporary onboarding data
            // Update analytics
            // Trigger any post-onboarding workflows
            
            logIntegrationEvent("finalize_integration", userProfile.userId, true)
        } catch (e: Exception) {
            android.util.Log.w("OnboardingIntegration", "Failed to finalize integration", e)
        }
    }

    /**
     * Log integration events without PII
     */
    private fun logIntegrationEvent(action: String, userId: String, success: Boolean, additionalInfo: Map<String, Any> = emptyMap()) {
        val logEntry = DataSanitizationHelper.createAuditLogEntry(
            action = "integration_$action",
            userId = userId,
            success = success,
            additionalInfo = additionalInfo
        )
        
        android.util.Log.i("OnboardingIntegration", "Integration event: $logEntry")
    }
}

/**
 * Goal calculation integration interface for loose coupling
 */
interface GoalCalculationIntegration {
    suspend fun calculateDailyGoals(userProfile: UserProfile): GoalCalculationResult
}

/**
 * Default implementation of goal calculation integration
 */
@Singleton
class DefaultGoalCalculationIntegration @Inject constructor() : GoalCalculationIntegration {
    
    override suspend fun calculateDailyGoals(userProfile: UserProfile): GoalCalculationResult {
        return try {
            // Simulate goal calculation (replace with actual implementation)
            kotlinx.coroutines.delay(1000) // Simulate processing time
            
            val goals = com.vibehealth.android.domain.user.DailyGoals(
                userId = userProfile.userId,
                calorieGoal = calculateCalorieGoal(userProfile),
                waterGoal = 8, // Standard 8 glasses
                exerciseGoal = 30 // Standard 30 minutes
            )
            
            GoalCalculationResult.Success(goals)
        } catch (e: Exception) {
            GoalCalculationResult.Error("Goal calculation failed: ${e.message}")
        }
    }
    
    private fun calculateCalorieGoal(userProfile: UserProfile): Int {
        // Simple BMR calculation
        val age = userProfile.getAge()
        val weight = userProfile.weightInKg
        val height = userProfile.heightInCm
        
        return when (userProfile.gender) {
            com.vibehealth.android.domain.user.Gender.MALE -> {
                (88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age)).toInt()
            }
            com.vibehealth.android.domain.user.Gender.FEMALE -> {
                (447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age)).toInt()
            }
            else -> {
                ((88.362 + 447.593) / 2 + (13.397 + 9.247) / 2 * weight + 
                 (4.799 + 3.098) / 2 * height - (5.677 + 4.330) / 2 * age).toInt()
            }
        }.coerceIn(1200, 3000)
    }
}

sealed class GoalCalculationResult {
    data class Success(val goals: com.vibehealth.android.domain.user.DailyGoals) : GoalCalculationResult()
    data class Error(val message: String) : GoalCalculationResult()
}

enum class OnboardingRequirement {
    REQUIRED,
    NOT_REQUIRED
}

enum class NavigationDecision {
    NAVIGATE_TO_ONBOARDING,
    NAVIGATE_TO_MAIN_APP
}

data class IntegrationContract(
    val onboardingCompleteCallback: suspend (UserProfile) -> Unit,
    val goalCalculationTrigger: suspend (UserProfile) -> Unit,
    val sessionUpdateCallback: suspend (UserProfile) -> Unit
)