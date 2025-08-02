package com.vibehealth.android.data.goals

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.core.security.EncryptionResult
import com.vibehealth.android.data.goals.local.DailyGoalsEntity
import com.vibehealth.android.data.goals.local.GoalDao
import com.vibehealth.android.domain.goals.DailyGoals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Repository for managing daily goals data with offline-first approach.
 * 
 * This repository implements the offline-first pattern, prioritizing local storage
 * and syncing to cloud when available. It handles encryption for sensitive data
 * and provides robust error handling with retry mechanisms.
 */
@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val firestore: FirebaseFirestore,
    private val encryptionHelper: EncryptionHelper
) {
    
    companion object {
        private const val TAG = "GoalRepository"
        private const val FIRESTORE_COLLECTION = "users"
        private const val GOALS_FIELD = "dailyGoals"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 8000L
    }
    
    /**
     * Save goals locally with encryption.
     * This is the primary storage method following offline-first principle.
     * 
     * @param goals DailyGoals to save
     * @param markAsDirty Whether to mark for cloud sync
     * @return Result indicating success or failure
     */
    suspend fun saveGoalsLocally(goals: DailyGoals, markAsDirty: Boolean = true): Result<DailyGoals> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = DailyGoalsEntity.fromDomainModel(goals, isDirty = markAsDirty)
                val encryptedEntity = encryptGoalsEntity(entity)
                
                goalDao.upsertGoals(encryptedEntity)
                
                Log.d(TAG, "Successfully saved goals locally for user: ${goals.userId}")
                Result.success(goals)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save goals locally for user: ${goals.userId}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get current goals for a user from local storage.
     * Returns Flow for reactive updates.
     * 
     * @param userId User ID to get goals for
     * @return Flow of DailyGoals or null if not found
     */
    fun getCurrentGoals(userId: String): Flow<DailyGoals?> {
        return goalDao.getCurrentGoalsForUser(userId).map { entity ->
            entity?.let { 
                try {
                    val decryptedEntity = decryptGoalsEntity(it)
                    decryptedEntity.toDomainModel()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decrypt goals for user: $userId", e)
                    null
                }
            }
        }
    }
    
    /**
     * Get current goals for a user synchronously.
     * Useful for one-time operations.
     * 
     * @param userId User ID to get goals for
     * @return DailyGoals or null if not found
     */
    suspend fun getCurrentGoalsSync(userId: String): DailyGoals? {
        return withContext(Dispatchers.IO) {
            try {
                val entity = goalDao.getCurrentGoalsForUserSync(userId)
                entity?.let { 
                    val decryptedEntity = decryptGoalsEntity(it)
                    decryptedEntity.toDomainModel()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get current goals for user: $userId", e)
                null
            }
        }
    }
    
    /**
     * Sync goals to Firebase Firestore with retry logic.
     * Uses exponential backoff for failed attempts.
     * 
     * @param goals DailyGoals to sync
     * @return Result indicating sync success or failure
     */
    suspend fun syncGoalsToCloud(goals: DailyGoals): Result<Unit> {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            
            repeat(MAX_RETRY_ATTEMPTS) { attempt ->
                try {
                    val goalsData = mapOf(
                        GOALS_FIELD to mapOf(
                            "stepsGoal" to goals.stepsGoal,
                            "caloriesGoal" to goals.caloriesGoal,
                            "heartPointsGoal" to goals.heartPointsGoal,
                            "calculatedAt" to goals.calculatedAt.toString(),
                            "calculationSource" to goals.calculationSource.name,
                            "lastUpdated" to Date()
                        )
                    )
                    
                    firestore.collection(FIRESTORE_COLLECTION)
                        .document(goals.userId)
                        .set(goalsData, SetOptions.merge())
                        .await()
                    
                    // Mark as synced in local database
                    markGoalsAsSynced(listOf(goals.userId))
                    
                    Log.d(TAG, "Successfully synced goals to cloud for user: ${goals.userId}")
                    return@withContext Result.success(Unit)
                    
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Sync attempt ${attempt + 1} failed for user: ${goals.userId}", e)
                    
                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        val delayMs = calculateRetryDelay(attempt)
                        Log.d(TAG, "Retrying sync in ${delayMs}ms")
                        delay(delayMs)
                    }
                }
            }
            
            Log.e(TAG, "All sync attempts failed for user: ${goals.userId}", lastException)
            Result.failure(lastException ?: Exception("Sync failed after $MAX_RETRY_ATTEMPTS attempts"))
        }
    }
    
    /**
     * Sync all dirty goals to cloud.
     * Used by background sync service.
     * 
     * @return Result with count of successfully synced goals
     */
    suspend fun syncAllDirtyGoals(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val dirtyGoals = goalDao.getDirtyGoals()
                var successCount = 0
                
                dirtyGoals.forEach { entity ->
                    try {
                        val decryptedEntity = decryptGoalsEntity(entity)
                        val domainGoals = decryptedEntity.toDomainModel()
                        
                        val syncResult = syncGoalsToCloud(domainGoals)
                        if (syncResult.isSuccess) {
                            successCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync dirty goal: ${entity.id}", e)
                    }
                }
                
                Log.d(TAG, "Synced $successCount out of ${dirtyGoals.size} dirty goals")
                Result.success(successCount)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync dirty goals", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Save and sync goals in one operation.
     * Saves locally first, then attempts cloud sync.
     * 
     * @param goals DailyGoals to save and sync
     * @return Result indicating overall success
     */
    suspend fun saveAndSyncGoals(goals: DailyGoals): Result<DailyGoals> {
        return withContext(Dispatchers.IO) {
            // Save locally first (offline-first approach)
            val localResult = saveGoalsLocally(goals, markAsDirty = true)
            
            if (localResult.isFailure) {
                return@withContext localResult
            }
            
            // Attempt cloud sync (non-blocking for user experience)
            val syncResult = syncGoalsToCloud(goals)
            if (syncResult.isFailure) {
                Log.w(TAG, "Cloud sync failed, but local save succeeded. Will retry sync later.")
            }
            
            // Return success if local save succeeded, regardless of sync result
            localResult
        }
    }
    
    /**
     * Delete goals for a user from both local and cloud storage.
     * 
     * @param userId User ID to delete goals for
     * @return Result indicating deletion success
     */
    suspend fun deleteGoalsForUser(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Delete from local storage
                val deletedCount = goalDao.deleteGoalsForUser(userId)
                
                // Delete from cloud storage
                try {
                    firestore.collection(FIRESTORE_COLLECTION)
                        .document(userId)
                        .update(mapOf(GOALS_FIELD to null))
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete goals from cloud for user: $userId", e)
                    // Continue with local deletion success
                }
                
                Log.d(TAG, "Deleted $deletedCount goal records for user: $userId")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete goals for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if user has any goals stored.
     * 
     * @param userId User ID to check
     * @return True if user has goals
     */
    suspend fun hasGoalsForUser(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                goalDao.hasGoalsForUser(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check if user has goals: $userId", e)
                false
            }
        }
    }
    
    /**
     * Get the last calculation time for a user.
     * 
     * @param userId User ID to check
     * @return LocalDateTime of last calculation or null
     */
    suspend fun getLastCalculationTime(userId: String): LocalDateTime? {
        return withContext(Dispatchers.IO) {
            try {
                goalDao.getLastCalculationTime(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get last calculation time for user: $userId", e)
                null
            }
        }
    }
    
    /**
     * Get all goals for a user (for history/analytics).
     * 
     * @param userId User ID to get goals for
     * @return Flow of list of DailyGoals
     */
    fun getAllGoalsForUser(userId: String): Flow<List<DailyGoals>> {
        return goalDao.getAllGoalsForUser(userId).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    val decryptedEntity = decryptGoalsEntity(entity)
                    decryptedEntity.toDomainModel()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decrypt goal entity: ${entity.id}", e)
                    null
                }
            }
        }
    }
    
    /**
     * Clean up old goals beyond retention period.
     * 
     * @param beforeDate Delete goals calculated before this date
     * @return Number of deleted records
     */
    suspend fun cleanupOldGoals(beforeDate: LocalDateTime): Int {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = goalDao.deleteOldGoals(beforeDate)
                Log.d(TAG, "Cleaned up $deletedCount old goal records")
                deletedCount
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup old goals", e)
                0
            }
        }
    }
    
    // Private helper methods
    
    /**
     * Encrypt goals entity for secure storage.
     * 
     * @param entity DailyGoalsEntity to encrypt
     * @return Encrypted entity
     */
    private fun encryptGoalsEntity(entity: DailyGoalsEntity): DailyGoalsEntity {
        return try {
            // For now, we'll encrypt sensitive calculation data
            // In a full implementation, you might encrypt more fields
            val encryptedUserId = when (val result = encryptionHelper.encrypt(entity.userId)) {
                is EncryptionResult.Success -> result.data
                is EncryptionResult.Error -> {
                    Log.w(TAG, "Failed to encrypt userId, using original: ${result.message}")
                    entity.userId
                }
            }
            
            entity.copy(userId = encryptedUserId)
        } catch (e: Exception) {
            Log.w(TAG, "Encryption failed, using original entity", e)
            entity
        }
    }
    
    /**
     * Decrypt goals entity from secure storage.
     * 
     * @param entity Encrypted DailyGoalsEntity
     * @return Decrypted entity
     */
    private fun decryptGoalsEntity(entity: DailyGoalsEntity): DailyGoalsEntity {
        return try {
            val decryptedUserId = when (val result = encryptionHelper.decrypt(entity.userId)) {
                is EncryptionResult.Success -> result.data
                is EncryptionResult.Error -> {
                    Log.w(TAG, "Failed to decrypt userId, using original: ${result.message}")
                    entity.userId
                }
            }
            
            entity.copy(userId = decryptedUserId)
        } catch (e: Exception) {
            Log.w(TAG, "Decryption failed, using original entity", e)
            entity
        }
    }
    
    /**
     * Mark goals as successfully synced.
     * 
     * @param userIds List of user IDs that were synced
     */
    private suspend fun markGoalsAsSynced(userIds: List<String>) {
        try {
            goalDao.markGoalsAsSynced(userIds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark goals as synced", e)
        }
    }
    
    /**
     * Calculate retry delay using exponential backoff.
     * 
     * @param attempt Current attempt number (0-based)
     * @return Delay in milliseconds
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val exponentialDelay = INITIAL_RETRY_DELAY_MS * (2.0.pow(attempt)).toLong()
        return min(exponentialDelay, MAX_RETRY_DELAY_MS)
    }
}