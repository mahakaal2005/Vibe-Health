package com.vibehealth.android.core.offline

import android.content.Context
import com.vibehealth.android.core.network.NetworkMonitor
import com.vibehealth.android.data.user.local.UserProfileDao
import com.vibehealth.android.data.user.remote.UserProfileService
import com.vibehealth.android.domain.onboarding.OnboardingResult
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for offline support and intelligent sync
 */
@Singleton
class OfflineSupportManager @Inject constructor(
    private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val userProfileDao: UserProfileDao,
    private val userProfileService: UserProfileService
) {

    /**
     * Flow that emits offline support status
     */
    val offlineStatus: Flow<OfflineStatus> = combine(
        networkMonitor.isOnline,
        getDirtyProfilesFlow()
    ) { isOnline, dirtyCount ->
        when {
            isOnline && dirtyCount == 0 -> OfflineStatus.ONLINE_SYNCED
            isOnline && dirtyCount > 0 -> OfflineStatus.ONLINE_PENDING_SYNC
            !isOnline && dirtyCount == 0 -> OfflineStatus.OFFLINE_NO_CHANGES
            !isOnline && dirtyCount > 0 -> OfflineStatus.OFFLINE_WITH_CHANGES
            else -> OfflineStatus.UNKNOWN
        }
    }.distinctUntilChanged()

    /**
     * Save user profile with offline support
     */
    suspend fun saveUserProfileOffline(userProfile: UserProfile): OnboardingResult {
        return try {
            // Always save locally first
            val entity = com.vibehealth.android.data.user.local.UserProfileEntity.fromDomainModel(
                userProfile, 
                isDirty = true
            )
            userProfileDao.upsertUserProfile(entity)

            // Try to sync if online
            if (networkMonitor.isCurrentlyOnline()) {
                val syncResult = syncUserProfile(userProfile)
                if (syncResult.isSuccess()) {
                    userProfileDao.markAsSynced(userProfile.userId, System.currentTimeMillis())
                }
            }

            OnboardingResult.Success
        } catch (e: Exception) {
            OnboardingResult.Error(
                exception = e,
                userMessage = "Unable to save your information offline. Please try again.",
                canRetry = true
            )
        }
    }

    /**
     * Sync user profile when online
     */
    private suspend fun syncUserProfile(userProfile: UserProfile): OnboardingResult {
        return try {
            val result = userProfileService.saveUserProfile(userProfile)
            if (result.isSuccess) {
                OnboardingResult.Success
            } else {
                OnboardingResult.Error(
                    exception = Exception(result.exceptionOrNull() ?: Exception("Sync failed")),
                    userMessage = "Unable to sync your data. It will be synced when connection improves.",
                    canRetry = true
                )
            }
        } catch (e: Exception) {
            OnboardingResult.Error(
                exception = e,
                userMessage = "Sync failed. Your data is saved locally and will sync automatically.",
                canRetry = true
            )
        }
    }

    /**
     * Sync all pending changes when coming back online
     */
    suspend fun syncPendingChanges(): OnboardingResult {
        return try {
            if (!networkMonitor.isCurrentlyOnline()) {
                return OnboardingResult.Error(
                    exception = Exception("No network connection"),
                    userMessage = "No internet connection available for sync.",
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
                val result = userProfileService.saveUserProfile(profile)
                
                if (result.isSuccess) {
                    userProfileDao.markAsSynced(profile.userId, System.currentTimeMillis())
                    syncedCount++
                } else {
                    failedCount++
                }
            }

            if (failedCount == 0) {
                OnboardingResult.Success
            } else {
                OnboardingResult.Error(
                    exception = Exception("Partial sync failure"),
                    userMessage = "Some data couldn't be synced. Will retry automatically.",
                    canRetry = true
                )
            }
        } catch (e: Exception) {
            OnboardingResult.Error(
                exception = e,
                userMessage = "Sync failed. Will retry when connection improves.",
                canRetry = true
            )
        }
    }

    /**
     * Get user profile with offline support
     */
    suspend fun getUserProfileOffline(uid: String): UserProfile? {
        return try {
            val localEntity = userProfileDao.getUserProfile(uid)
            localEntity?.toDomainModel()
        } catch (e: Exception) {
            android.util.Log.e("OfflineSupportManager", "Failed to get user profile offline", e)
            null
        }
    }

    /**
     * Check if user has unsaved changes
     */
    suspend fun hasUnsavedChanges(uid: String): Boolean {
        return try {
            val entity = userProfileDao.getUserProfile(uid)
            entity?.isDirty ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get count of profiles pending sync
     */
    suspend fun getPendingSyncCount(): Int {
        return try {
            userProfileDao.getDirtyUserProfiles().size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Flow of dirty profiles count
     */
    private fun getDirtyProfilesFlow(): Flow<Int> {
        return flow {
            val profiles = userProfileDao.getUserProfilesNeedingSync(System.currentTimeMillis() - 60000) // 1 minute ago
            emit(profiles.size)
        }
    }

    /**
     * Get offline support message for user
     */
    fun getOfflineMessage(status: OfflineStatus): String {
        return when (status) {
            OfflineStatus.ONLINE_SYNCED -> "All data is synced"
            OfflineStatus.ONLINE_PENDING_SYNC -> "Syncing your data..."
            OfflineStatus.OFFLINE_NO_CHANGES -> "You're offline, but your data is saved"
            OfflineStatus.OFFLINE_WITH_CHANGES -> "You're offline. Changes will sync when connection returns"
            OfflineStatus.UNKNOWN -> "Checking connection status..."
        }
    }

    /**
     * Check if operation can proceed offline
     */
    fun canProceedOffline(operation: OfflineOperation): Boolean {
        return when (operation) {
            OfflineOperation.SAVE_PROFILE -> true
            OfflineOperation.VALIDATE_DATA -> true
            OfflineOperation.NAVIGATE -> true
            OfflineOperation.SYNC_DATA -> networkMonitor.isCurrentlyOnline()
            OfflineOperation.COMPLETE_ONBOARDING -> true // Can complete offline, sync later
        }
    }
}

enum class OfflineStatus {
    ONLINE_SYNCED,
    ONLINE_PENDING_SYNC,
    OFFLINE_NO_CHANGES,
    OFFLINE_WITH_CHANGES,
    UNKNOWN
}

enum class OfflineOperation {
    SAVE_PROFILE,
    VALIDATE_DATA,
    NAVIGATE,
    SYNC_DATA,
    COMPLETE_ONBOARDING
}