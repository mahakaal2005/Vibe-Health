package com.vibehealth.android.data.user.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for UserProfile with comprehensive CRUD operations
 */
@Dao
interface UserProfileDao {

    /**
     * Insert a new user profile
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfileEntity): Long

    /**
     * Update an existing user profile
     */
    @Update
    suspend fun updateUserProfile(userProfile: UserProfileEntity): Int

    /**
     * Delete a user profile
     */
    @Delete
    suspend fun deleteUserProfile(userProfile: UserProfileEntity): Int

    /**
     * Get user profile by user ID
     */
    @Query("SELECT * FROM user_profiles WHERE user_id = :userId")
    suspend fun getUserProfile(userId: String): UserProfileEntity?

    /**
     * Get user profile as Flow for reactive updates
     */
    @Query("SELECT * FROM user_profiles WHERE user_id = :userId")
    fun getUserProfileFlow(userId: String): Flow<UserProfileEntity?>

    /**
     * Check if user has completed onboarding
     */
    @Query("SELECT has_completed_onboarding FROM user_profiles WHERE user_id = :userId")
    suspend fun isOnboardingComplete(userId: String): Boolean?

    /**
     * Update onboarding completion status
     */
    @Query("UPDATE user_profiles SET has_completed_onboarding = :isComplete, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateOnboardingStatus(userId: String, isComplete: Boolean, updatedAt: Long): Int

    /**
     * Get all user profiles (for admin/debug purposes)
     */
    @Query("SELECT * FROM user_profiles ORDER BY created_at DESC")
    suspend fun getAllUserProfiles(): List<UserProfileEntity>

    /**
     * Get user profiles that need syncing (dirty flag)
     */
    @Query("SELECT * FROM user_profiles WHERE is_dirty = 1")
    suspend fun getDirtyUserProfiles(): List<UserProfileEntity>

    /**
     * Mark user profile as synced
     */
    @Query("UPDATE user_profiles SET is_dirty = 0, last_sync_at = :syncTime WHERE user_id = :userId")
    suspend fun markAsSynced(userId: String, syncTime: Long): Int

    /**
     * Mark user profile as dirty (needs sync)
     */
    @Query("UPDATE user_profiles SET is_dirty = 1 WHERE user_id = :userId")
    suspend fun markAsDirty(userId: String): Int

    /**
     * Delete all user profiles (for testing/reset)
     */
    @Query("DELETE FROM user_profiles")
    suspend fun deleteAllUserProfiles(): Int

    /**
     * Get user profile count
     */
    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getUserProfileCount(): Int

    /**
     * Check if user profile exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM user_profiles WHERE user_id = :userId)")
    suspend fun userProfileExists(userId: String): Boolean

    /**
     * Get user profiles created after a specific date
     */
    @Query("SELECT * FROM user_profiles WHERE created_at > :afterDate ORDER BY created_at DESC")
    suspend fun getUserProfilesCreatedAfter(afterDate: Long): List<UserProfileEntity>

    /**
     * Get user profiles updated after a specific date
     */
    @Query("SELECT * FROM user_profiles WHERE updated_at > :afterDate ORDER BY updated_at DESC")
    suspend fun getUserProfilesUpdatedAfter(afterDate: Long): List<UserProfileEntity>

    /**
     * Update user profile's last sync time
     */
    @Query("UPDATE user_profiles SET last_sync_at = :syncTime WHERE user_id = :userId")
    suspend fun updateLastSyncTime(userId: String, syncTime: Long): Int

    /**
     * Get user profiles that haven't been synced for a while
     */
    @Query("SELECT * FROM user_profiles WHERE last_sync_at IS NULL OR last_sync_at < :beforeDate")
    suspend fun getUserProfilesNeedingSync(beforeDate: Long): List<UserProfileEntity>

    /**
     * Upsert operation (insert or update)
     */
    @Transaction
    suspend fun upsertUserProfile(userProfile: UserProfileEntity) {
        val existingProfile = getUserProfile(userProfile.userId)
        if (existingProfile != null) {
            updateUserProfile(userProfile.copy(isDirty = true))
        } else {
            insertUserProfile(userProfile.copy(isDirty = true))
        }
    }

    /**
     * Batch insert user profiles
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfiles(userProfiles: List<UserProfileEntity>): List<Long>

    /**
     * Get database statistics for monitoring
     */
    @Query("""
        SELECT 
            COUNT(*) as total_profiles,
            COUNT(CASE WHEN has_completed_onboarding = 1 THEN 1 END) as completed_onboarding,
            COUNT(CASE WHEN is_dirty = 1 THEN 1 END) as dirty_profiles,
            MIN(created_at) as oldest_profile,
            MAX(updated_at) as latest_update
        FROM user_profiles
    """)
    suspend fun getDatabaseStats(): DatabaseStats?
}

/**
 * Data class for database statistics
 */
data class DatabaseStats(
    @ColumnInfo(name = "total_profiles")
    val totalProfiles: Int,
    @ColumnInfo(name = "completed_onboarding")
    val completedOnboarding: Int,
    @ColumnInfo(name = "dirty_profiles")
    val dirtyProfiles: Int,
    @ColumnInfo(name = "oldest_profile")
    val oldestProfile: Long?,
    @ColumnInfo(name = "latest_update")
    val latestUpdate: Long?
)