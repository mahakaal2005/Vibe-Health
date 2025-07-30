package com.vibehealth.android.domain.user

import com.vibehealth.android.domain.onboarding.OnboardingResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile operations with dual storage support
 */
interface UserRepository {
    
    /**
     * Save user profile to both local and cloud storage
     */
    suspend fun saveUserProfile(userProfile: UserProfile): Result<UserProfile>
    
    /**
     * Get user profile by user ID
     */
    suspend fun getUserProfile(uid: String): Result<UserProfile?>
    
    /**
     * Update user profile with conflict resolution
     */
    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile>
    
    /**
     * Check if user has completed onboarding
     */
    suspend fun isOnboardingComplete(uid: String): Boolean
    
    /**
     * Check if user has completed onboarding (alternative method name for compatibility)
     */
    suspend fun hasCompletedOnboarding(uid: String): Result<Boolean>
    
    /**
     * Save daily goals (for integration with goal calculation)
     */
    suspend fun saveDailyGoals(uid: String, goals: DailyGoals): Result<DailyGoals>
    
    /**
     * Get user profile as Flow for reactive updates
     */
    fun getUserProfileFlow(uid: String): Flow<UserProfile?>
    
    /**
     * Sync local data with cloud storage
     */
    suspend fun syncUserProfile(uid: String): OnboardingResult
    
    /**
     * Force sync all dirty profiles
     */
    suspend fun syncAllDirtyProfiles(): OnboardingResult
    
    /**
     * Clear local cache for user
     */
    suspend fun clearLocalCache(uid: String): Result<Unit>
    
    /**
     * Get sync status for user
     */
    suspend fun getSyncStatus(uid: String): SyncStatus
}

/**
 * Data class for daily goals (placeholder for goal calculation integration)
 */
data class DailyGoals(
    val userId: String = "",
    val calorieGoal: Int = 0,
    val waterGoal: Int = 0,
    val exerciseGoal: Int = 0,
    val createdAt: java.util.Date = java.util.Date(),
    val updatedAt: java.util.Date = java.util.Date()
)

/**
 * Enum representing sync status
 */
enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    SYNC_FAILED,
    OFFLINE
}