package com.vibehealth.android.data.user

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.vibehealth.android.core.security.DataSanitizationHelper
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.data.user.local.UserProfileDao
import com.vibehealth.android.data.user.local.UserProfileEntity
import com.vibehealth.android.data.user.remote.UserProfileService
import com.vibehealth.android.domain.onboarding.OnboardingResult
import com.vibehealth.android.domain.user.DailyGoals
import com.vibehealth.android.domain.user.SyncStatus
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation with secure dual storage (Room + Firebase) and conflict resolution
 */
@Singleton
class UserProfileRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val userProfileService: UserProfileService,
    private val encryptionHelper: EncryptionHelper,
    private val context: Context
) : UserRepository {

    companion object {
        private const val TAG = "UserProfileRepository"
        private const val SYNC_TIMEOUT_MS = 30000L
    }

    /**
     * Save user profile with offline-first approach and automatic sync
     */
    override suspend fun saveUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            // Always save to local storage first (offline-first)
            val entity = UserProfileEntity.fromDomainModel(userProfile, isDirty = true)
            userProfileDao.upsertUserProfile(entity)

            // Attempt cloud sync if network is available
            if (isNetworkAvailable()) {
                val cloudResult = userProfileService.saveUserProfile(userProfile)
                if (cloudResult.isSuccess) {
                    // Mark as synced if cloud save successful
                    userProfileDao.markAsSynced(userProfile.userId, System.currentTimeMillis())
                    logOperation("saveUserProfile", userProfile.userId, true)
                } else {
                    logOperation("saveUserProfile", userProfile.userId, false, "Cloud sync failed")
                }
            }

            Result.success(userProfile)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save user profile", e)
            logOperation("saveUserProfile", userProfile.userId, false, e.message)
            Result.failure(e)
        }
    }

    /**
     * Get user profile with offline-first approach
     */
    override suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            android.util.Log.d("UserProfileRepository", "🔍 getUserProfile called for: $uid")
            
            // Try local storage first
            val localEntity = userProfileDao.getUserProfile(uid)
            val localProfile = localEntity?.toDomainModel()
            android.util.Log.d("UserProfileRepository", "💾 Local profile found: ${localProfile != null}")

            // If network is available, try to sync with cloud
            val networkAvailable = isNetworkAvailable()
            android.util.Log.d("UserProfileRepository", "🌐 Network available: $networkAvailable")
            
            if (networkAvailable) {
                android.util.Log.d("UserProfileRepository", "☁️ Fetching from Firestore...")
                val cloudResult = userProfileService.getUserProfile(uid)
                android.util.Log.d("UserProfileRepository", "☁️ Firestore result success: ${cloudResult.isSuccess}")
                
                if (cloudResult.isSuccess) {
                    val cloudProfile = cloudResult.getOrNull()
                    android.util.Log.d("UserProfileRepository", "☁️ Cloud profile found: ${cloudProfile != null}")
                    
                    // Resolve conflicts if both local and cloud data exist
                    val resolvedProfile = resolveConflicts(localProfile, cloudProfile)
                    
                    // Update local storage with resolved data
                    if (resolvedProfile != null && resolvedProfile != localProfile) {
                        val entity = UserProfileEntity.fromDomainModel(resolvedProfile, isDirty = false)
                        userProfileDao.upsertUserProfile(entity)
                        userProfileDao.markAsSynced(uid, System.currentTimeMillis())
                    }
                    
                    logOperation("getUserProfile", uid, true)
                    return Result.success(resolvedProfile)
                }
            }

            // Return local data if cloud sync fails or network unavailable
            logOperation("getUserProfile", uid, localProfile != null, 
                if (localProfile == null) "No local data found" else null)
            Result.success(localProfile)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get user profile", e)
            logOperation("getUserProfile", uid, false, e.message)
            Result.failure(e)
        }
    }

    /**
     * Update user profile with conflict resolution
     */
    override suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            val updatedProfile = userProfile.copy(updatedAt = java.util.Date())
            
            // Save to local storage first
            val entity = UserProfileEntity.fromDomainModel(updatedProfile, isDirty = true)
            userProfileDao.upsertUserProfile(entity)

            // Attempt cloud sync
            if (isNetworkAvailable()) {
                val cloudResult = userProfileService.updateUserProfile(updatedProfile)
                if (cloudResult.isSuccess) {
                    userProfileDao.markAsSynced(updatedProfile.userId, System.currentTimeMillis())
                    logOperation("updateUserProfile", updatedProfile.userId, true)
                } else {
                    logOperation("updateUserProfile", updatedProfile.userId, false, "Cloud sync failed")
                }
            }

            Result.success(updatedProfile)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to update user profile", e)
            logOperation("updateUserProfile", userProfile.userId, false, e.message)
            Result.failure(e)
        }
    }

    /**
     * Check if user has completed onboarding
     */
    override suspend fun isOnboardingComplete(uid: String): Boolean {
        return try {
            // Check local storage first
            val localStatus = userProfileDao.isOnboardingComplete(uid)
            if (localStatus == true) {
                return true
            }

            // Check cloud if local is false or null and network is available
            if (isNetworkAvailable()) {
                val cloudResult = userProfileService.checkOnboardingStatus(uid)
                if (cloudResult.isSuccess) {
                    val cloudStatus = cloudResult.getOrNull() ?: false
                    
                    // Update local storage if cloud has different status
                    if (cloudStatus && localStatus != true) {
                        userProfileDao.updateOnboardingStatus(uid, true, System.currentTimeMillis())
                    }
                    
                    return cloudStatus
                }
            }

            localStatus ?: false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to check onboarding status", e)
            false
        }
    }

    /**
     * Check if user has completed onboarding (alternative method name for compatibility)
     */
    override suspend fun hasCompletedOnboarding(uid: String): Result<Boolean> {
        return try {
            val completed = isOnboardingComplete(uid)
            Result.success(completed)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to check onboarding completion", e)
            Result.failure(e)
        }
    }

    /**
     * Save daily goals (placeholder for goal calculation integration)
     */
    override suspend fun saveDailyGoals(uid: String, goals: DailyGoals): Result<DailyGoals> {
        return try {
            // This would integrate with the goal calculation system
            // For now, just return success
            logOperation("saveDailyGoals", uid, true)
            Result.success(goals)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save daily goals", e)
            logOperation("saveDailyGoals", uid, false, e.message)
            Result.failure(e)
        }
    }

    /**
     * Get user profile as reactive Flow
     */
    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> {
        return userProfileDao.getUserProfileFlow(uid)
            .map { entity -> entity?.toDomainModel() }
            .catch { e ->
                android.util.Log.e(TAG, "Error in user profile flow", e)
                emit(null)
            }
    }

    /**
     * Sync user profile with cloud storage
     */
    override suspend fun syncUserProfile(uid: String): OnboardingResult {
        return try {
            if (!isNetworkAvailable()) {
                return OnboardingResult.Error(
                    Exception("Network not available"),
                    "Please check your internet connection",
                    canRetry = true
                )
            }

            val localEntity = userProfileDao.getUserProfile(uid)
            if (localEntity == null) {
                return OnboardingResult.Error(
                    Exception("No local data to sync"),
                    "No user data found locally",
                    canRetry = false
                )
            }

            val localProfile = localEntity.toDomainModel()
            val cloudResult = userProfileService.getUserProfile(uid)
            
            if (cloudResult.isSuccess) {
                val cloudProfile = cloudResult.getOrNull()
                val resolvedProfile = resolveConflicts(localProfile, cloudProfile)
                
                if (resolvedProfile != null) {
                    // Update cloud with resolved data
                    val updateResult = userProfileService.updateUserProfile(resolvedProfile)
                    if (updateResult.isSuccess) {
                        // Mark as synced
                        userProfileDao.markAsSynced(uid, System.currentTimeMillis())
                        logOperation("syncUserProfile", uid, true)
                        return OnboardingResult.Success
                    }
                }
            }

            OnboardingResult.Error(
                Exception("Sync failed"),
                "Unable to sync your data. Please try again.",
                canRetry = true
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to sync user profile", e)
            logOperation("syncUserProfile", uid, false, e.message)
            OnboardingResult.Error(e, "Sync failed. Please try again.", canRetry = true)
        }
    }

    /**
     * Sync all dirty profiles
     */
    override suspend fun syncAllDirtyProfiles(): OnboardingResult {
        return try {
            if (!isNetworkAvailable()) {
                return OnboardingResult.Error(
                    Exception("Network not available"),
                    "Please check your internet connection",
                    canRetry = true
                )
            }

            val dirtyProfiles = userProfileDao.getDirtyUserProfiles()
            if (dirtyProfiles.isEmpty()) {
                return OnboardingResult.Success
            }

            var syncedCount = 0
            var failedCount = 0

            for (entity in dirtyProfiles) {
                val profile = entity.toDomainModel()
                val result = userProfileService.updateUserProfile(profile)
                
                if (result.isSuccess) {
                    userProfileDao.markAsSynced(profile.userId, System.currentTimeMillis())
                    syncedCount++
                } else {
                    failedCount++
                }
            }

            android.util.Log.i(TAG, "Sync completed: $syncedCount synced, $failedCount failed")
            
            if (failedCount == 0) {
                OnboardingResult.Success
            } else {
                OnboardingResult.Error(
                    Exception("Partial sync failure"),
                    "Some data couldn't be synced. Will retry automatically.",
                    canRetry = true
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to sync all dirty profiles", e)
            OnboardingResult.Error(e, "Sync failed. Please try again.", canRetry = true)
        }
    }

    /**
     * Clear local cache for user
     */
    override suspend fun clearLocalCache(uid: String): Result<Unit> {
        return try {
            val entity = userProfileDao.getUserProfile(uid)
            if (entity != null) {
                userProfileDao.deleteUserProfile(entity)
            }
            logOperation("clearLocalCache", uid, true)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to clear local cache", e)
            logOperation("clearLocalCache", uid, false, e.message)
            Result.failure(e)
        }
    }

    /**
     * Get sync status for user
     */
    override suspend fun getSyncStatus(uid: String): SyncStatus {
        return try {
            if (!isNetworkAvailable()) {
                return SyncStatus.OFFLINE
            }

            val entity = userProfileDao.getUserProfile(uid)
            when {
                entity == null -> SyncStatus.OFFLINE
                entity.isDirty -> SyncStatus.PENDING_SYNC
                entity.lastSyncAt != null -> SyncStatus.SYNCED
                else -> SyncStatus.SYNC_FAILED
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get sync status", e)
            SyncStatus.SYNC_FAILED
        }
    }

    /**
     * Resolve conflicts between local and cloud data using last-write-wins strategy
     */
    private fun resolveConflicts(localProfile: UserProfile?, cloudProfile: UserProfile?): UserProfile? {
        return when {
            localProfile == null && cloudProfile == null -> null
            localProfile == null -> cloudProfile
            cloudProfile == null -> localProfile
            else -> {
                // Last-write-wins conflict resolution
                if (localProfile.updatedAt.after(cloudProfile.updatedAt)) {
                    android.util.Log.d(TAG, "Using local profile (newer)")
                    localProfile
                } else {
                    android.util.Log.d(TAG, "Using cloud profile (newer)")
                    cloudProfile
                }
            }
        }
    }

    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to check network availability", e)
            false
        }
    }

    /**
     * Log operations without PII
     */
    private fun logOperation(operation: String, userId: String, success: Boolean, error: String? = null) {
        val logEntry = DataSanitizationHelper.createAuditLogEntry(
            action = operation,
            userId = userId,
            success = success,
            additionalInfo = if (error != null) mapOf("error" to error) else emptyMap()
        )
        
        android.util.Log.i(TAG, "Operation: $operation, Success: $success")
        if (error != null) {
            android.util.Log.w(TAG, "Error: ${DataSanitizationHelper.sanitizeForLogging(error)}")
        }
    }
}