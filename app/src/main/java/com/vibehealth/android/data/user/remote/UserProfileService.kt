package com.vibehealth.android.data.user.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.vibehealth.android.core.security.DataSanitizationHelper
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Firebase Firestore service for UserProfile operations in asia-south1 region with retry logic
 */
@Singleton
class UserProfileService @Inject constructor() {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 10000L
        private const val TIMEOUT_MS = 30000L
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            // Configure for asia-south1 (Mumbai) region
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }
    }

    /**
     * Save user profile to Firestore with retry logic
     */
    suspend fun saveUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return executeWithRetry("saveUserProfile") {
            try {
                // Sanitize data before saving to cloud
                val sanitizedProfile = userProfile.copy(
                    displayName = DataSanitizationHelper.sanitizeName(userProfile.displayName),
                    firstName = DataSanitizationHelper.sanitizeName(userProfile.firstName),
                    lastName = DataSanitizationHelper.sanitizeName(userProfile.lastName),
                    updatedAt = java.util.Date()
                )

                firestore.collection(USERS_COLLECTION)
                    .document(userProfile.userId)
                    .set(sanitizedProfile)
                    .await()

                Result.success(sanitizedProfile)
            } catch (e: Exception) {
                android.util.Log.e("UserProfileService", "Failed to save user profile", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get user profile from Firestore with retry logic
     */
    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return executeWithRetry("getUserProfile") {
            try {
                val document = firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .get()
                    .await()

                if (document.exists()) {
                    val userProfile = document.toObject(UserProfile::class.java)
                    Result.success(userProfile)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                android.util.Log.e("UserProfileService", "Failed to get user profile", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update user profile in Firestore with retry logic
     */
    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return executeWithRetry("updateUserProfile") {
            try {
                // Sanitize data before updating
                val sanitizedProfile = userProfile.copy(
                    displayName = DataSanitizationHelper.sanitizeName(userProfile.displayName),
                    firstName = DataSanitizationHelper.sanitizeName(userProfile.firstName),
                    lastName = DataSanitizationHelper.sanitizeName(userProfile.lastName),
                    updatedAt = java.util.Date()
                )

                firestore.collection(USERS_COLLECTION)
                    .document(userProfile.userId)
                    .set(sanitizedProfile)
                    .await()

                Result.success(sanitizedProfile)
            } catch (e: Exception) {
                android.util.Log.e("UserProfileService", "Failed to update user profile", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Check onboarding status in Firestore
     */
    suspend fun checkOnboardingStatus(uid: String): Result<Boolean> {
        return executeWithRetry("checkOnboardingStatus") {
            try {
                val document = firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .get()
                    .await()

                if (document.exists()) {
                    val hasCompleted = document.getBoolean("hasCompletedOnboarding") ?: false
                    Result.success(hasCompleted)
                } else {
                    Result.success(false)
                }
            } catch (e: Exception) {
                android.util.Log.e("UserProfileService", "Failed to check onboarding status", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete user profile from Firestore
     */
    suspend fun deleteUserProfile(uid: String): Result<Unit> {
        return executeWithRetry("deleteUserProfile") {
            try {
                firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .delete()
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("UserProfileService", "Failed to delete user profile", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Batch update multiple user profiles
     */
    suspend fun batchUpdateUserProfiles(profiles: List<UserProfile>): Result<List<UserProfile>> {
        return executeWithRetry("batchUpdateUserProfiles") {
            try {
                val batch = firestore.batch()
                val sanitizedProfiles = mutableListOf<UserProfile>()

                profiles.forEach { profile ->
                    val sanitizedProfile = profile.copy(
                        displayName = DataSanitizationHelper.sanitizeName(profile.displayName),
                        firstName = DataSanitizationHelper.sanitizeName(profile.firstName),
                        lastName = DataSanitizationHelper.sanitizeName(profile.lastName),
                        updatedAt = java.util.Date()
                    )
                    
                    val docRef = firestore.collection(USERS_COLLECTION).document(profile.userId)
                    batch.set(docRef, sanitizedProfile)
                    sanitizedProfiles.add(sanitizedProfile)
                }

                batch.commit().await()
                Result.success(sanitizedProfiles)
            } catch (e: Exception) {
                android.util.Log.e("UserProfileService", "Failed to batch update user profiles", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get user profiles updated after a specific timestamp
     */
    suspend fun getUserProfilesUpdatedAfter(timestamp: Long): Result<List<UserProfile>> {
        return executeWithRetry("getUserProfilesUpdatedAfter") {
            try {
                val query = firestore.collection(USERS_COLLECTION)
                    .whereGreaterThan("updatedAt", java.util.Date(timestamp))
                    .get()
                    .await()

                val profiles = query.documents.mapNotNull { document ->
                    document.toObject(UserProfile::class.java)
                }

                Result.success(profiles)
            } catch (e: Exception) {
                android.util.Log.e("UserProfileService", "Failed to get updated user profiles", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Check if Firestore is available
     */
    suspend fun isFirestoreAvailable(): Boolean {
        return try {
            // Perform a simple read operation to check connectivity
            firestore.collection(USERS_COLLECTION)
                .limit(1)
                .get()
                .await()
            true
        } catch (e: Exception) {
            android.util.Log.w("UserProfileService", "Firestore not available", e)
            false
        }
    }

    /**
     * Execute operation with exponential backoff retry logic
     */
    private suspend fun <T> executeWithRetry(
        operation: String,
        block: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null
        
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                val result = block()
                if (result.isSuccess) {
                    if (attempt > 0) {
                        android.util.Log.i("UserProfileService", "$operation succeeded after ${attempt + 1} attempts")
                    }
                    return result
                }
                lastException = result.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w("UserProfileService", "$operation attempt ${attempt + 1} failed", e)
            }

            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                val delayMs = calculateBackoffDelay(attempt)
                android.util.Log.d("UserProfileService", "Retrying $operation in ${delayMs}ms")
                delay(delayMs)
            }
        }

        val finalException = lastException ?: Exception("Unknown error in $operation")
        android.util.Log.e("UserProfileService", "$operation failed after $MAX_RETRY_ATTEMPTS attempts", finalException)
        return Result.failure(finalException)
    }

    /**
     * Calculate exponential backoff delay with jitter
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = BASE_DELAY_MS * (2.0.pow(attempt)).toLong()
        val jitter = (Math.random() * 0.1 * exponentialDelay).toLong()
        return min(exponentialDelay + jitter, MAX_DELAY_MS)
    }

    /**
     * Create audit log for operations (without PII)
     */
    private fun logOperation(operation: String, userId: String, success: Boolean, error: String? = null) {
        val logEntry = DataSanitizationHelper.createAuditLogEntry(
            action = operation,
            userId = userId,
            success = success,
            additionalInfo = if (error != null) mapOf("error" to error) else emptyMap()
        )
        
        android.util.Log.i("UserProfileService", "Operation: $operation, Success: $success")
        if (error != null) {
            android.util.Log.w("UserProfileService", "Error details: ${DataSanitizationHelper.sanitizeForLogging(error)}")
        }
    }
}