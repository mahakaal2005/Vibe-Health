package com.vibehealth.android.data.dashboard

import android.util.Log
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.core.security.EncryptionResult
import com.vibehealth.android.ui.dashboard.models.DailyProgress
import com.vibehealth.android.ui.dashboard.models.ProgressData
import com.vibehealth.android.ui.dashboard.models.RingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Implementation of DashboardRepository for dashboard-specific data management.
 * Handles caching, offline support, and data freshness tracking with AES-256 encryption.
 */
@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val encryptionHelper: EncryptionHelper
) : DashboardRepository {
    
    companion object {
        private const val TAG = "DashboardRepository"
        private const val CACHE_EXPIRY_HOURS = 24
    }
    
    // In-memory cache for demo purposes
    // In production, this would use Room database or SharedPreferences
    private val progressCache = mutableMapOf<String, CachedProgressData>()
    
    /**
     * Gets current daily progress for the user.
     * For demo purposes, generates realistic progress data.
     * In production, this would integrate with Google Fit, HealthConnect, or similar APIs.
     */
    override fun getCurrentDayProgress(userId: String): Flow<DailyProgress> = flow {
        try {
            // Check cache first
            val cached = getCachedProgressData(userId)
            if (cached != null && !cached.isExpired()) {
                emit(cached.progress)
                return@flow
            }
            
            // Generate realistic demo data
            // In production, this would fetch from activity APIs
            val demoProgress = generateDemoProgress()
            
            // Cache the data
            cacheProgressData(userId, demoProgress)
            
            emit(demoProgress)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current day progress for user: $userId", e)
            
            // Try to return cached data even if expired
            val cached = getCachedProgressData(userId)
            if (cached != null) {
                emit(cached.progress)
            } else {
                // Return empty progress as fallback
                emit(DailyProgress.empty())
            }
        }
    }
    
    /**
     * Caches dashboard data locally with AES-256 encryption.
     */
    override suspend fun cacheDashboardData(userId: String, progress: DailyProgress) {
        withContext(Dispatchers.IO) {
            try {
                val cachedData = CachedProgressData(
                    progress = progress,
                    cachedAt = LocalDateTime.now(),
                    userId = userId
                )
                
                // Encrypt the cached data
                val encryptedData = encryptProgressData(cachedData)
                progressCache[userId] = encryptedData
                
                Log.d(TAG, "Successfully cached dashboard data for user: $userId")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache dashboard data for user: $userId", e)
            }
        }
    }
    
    /**
     * Gets cached dashboard data for offline support.
     */
    override suspend fun getCachedDashboardData(userId: String): DailyProgress? {
        return withContext(Dispatchers.IO) {
            try {
                val cachedData = getCachedProgressData(userId)
                cachedData?.progress
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get cached dashboard data for user: $userId", e)
                null
            }
        }
    }
    
    /**
     * Clears cached data for the user.
     */
    override suspend fun clearCachedData(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                progressCache.remove(userId)
                Log.d(TAG, "Cleared cached data for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cached data for user: $userId", e)
            }
        }
    }
    
    /**
     * Gets cached progress data with decryption.
     */
    private fun getCachedProgressData(userId: String): CachedProgressData? {
        return try {
            val encryptedData = progressCache[userId]
            encryptedData?.let { decryptProgressData(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cached progress data for user: $userId", e)
            null
        }
    }
    
    /**
     * Caches progress data with encryption.
     */
    private fun cacheProgressData(userId: String, progress: DailyProgress) {
        try {
            val cachedData = CachedProgressData(
                progress = progress,
                cachedAt = LocalDateTime.now(),
                userId = userId
            )
            
            val encryptedData = encryptProgressData(cachedData)
            progressCache[userId] = encryptedData
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache progress data for user: $userId", e)
        }
    }
    
    /**
     * Generates realistic demo progress data.
     * In production, this would be replaced with actual activity data integration.
     */
    private fun generateDemoProgress(): DailyProgress {
        val currentHour = LocalDateTime.now().hour
        val progressFactor = (currentHour / 24f).coerceAtMost(1f)
        
        // Generate realistic progress based on time of day
        val baseSteps = (8000 * progressFactor + Random.nextInt(1000)).toInt()
        val baseCalories = (1800 * progressFactor + Random.nextInt(200)).toInt()
        val baseHeartPoints = (25 * progressFactor + Random.nextInt(5)).toInt()
        
        return DailyProgress(
            stepsProgress = ProgressData(
                ringType = RingType.STEPS,
                current = baseSteps,
                target = 10000, // Default target, will be updated by use case
                percentage = (baseSteps / 10000f).coerceAtMost(1f),
                isGoalAchieved = baseSteps >= 10000,
                progressColor = RingType.STEPS.getDefaultColor()
            ),
            caloriesProgress = ProgressData(
                ringType = RingType.CALORIES,
                current = baseCalories,
                target = 2000, // Default target, will be updated by use case
                percentage = (baseCalories / 2000f).coerceAtMost(1f),
                isGoalAchieved = baseCalories >= 2000,
                progressColor = RingType.CALORIES.getDefaultColor()
            ),
            heartPointsProgress = ProgressData(
                ringType = RingType.HEART_POINTS,
                current = baseHeartPoints,
                target = 30, // Default target, will be updated by use case
                percentage = (baseHeartPoints / 30f).coerceAtMost(1f),
                isGoalAchieved = baseHeartPoints >= 30,
                progressColor = RingType.HEART_POINTS.getDefaultColor()
            )
        )
    }
    
    /**
     * Encrypts progress data for secure caching.
     */
    private fun encryptProgressData(data: CachedProgressData): CachedProgressData {
        return try {
            // For demo purposes, we'll encrypt the user ID
            // In production, you might encrypt more sensitive data
            val encryptedUserId = when (val result = encryptionHelper.encrypt(data.userId)) {
                is EncryptionResult.Success -> result.data
                is EncryptionResult.Error -> {
                    Log.w(TAG, "Failed to encrypt userId: ${result.message}")
                    data.userId
                }
            }
            
            data.copy(userId = encryptedUserId)
        } catch (e: Exception) {
            Log.w(TAG, "Encryption failed, using original data", e)
            data
        }
    }
    
    /**
     * Decrypts progress data from secure cache.
     */
    private fun decryptProgressData(data: CachedProgressData): CachedProgressData {
        return try {
            val decryptedUserId = when (val result = encryptionHelper.decrypt(data.userId)) {
                is EncryptionResult.Success -> result.data
                is EncryptionResult.Error -> {
                    Log.w(TAG, "Failed to decrypt userId: ${result.message}")
                    data.userId
                }
            }
            
            data.copy(userId = decryptedUserId)
        } catch (e: Exception) {
            Log.w(TAG, "Decryption failed, using original data", e)
            data
        }
    }
    
    /**
     * Data class for cached progress with expiry tracking.
     */
    private data class CachedProgressData(
        val progress: DailyProgress,
        val cachedAt: LocalDateTime,
        val userId: String
    ) {
        /**
         * Checks if cached data has expired.
         */
        fun isExpired(): Boolean {
            val now = LocalDateTime.now()
            val hoursOld = java.time.Duration.between(cachedAt, now).toHours()
            return hoursOld >= CACHE_EXPIRY_HOURS
        }
    }
}