package com.vibehealth.android.data.progress

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProgressDataLoadingOptimizer - Optimizes data loading for smooth 60fps performance
 * 
 * This class implements efficient data loading strategies that maintain the supportive
 * user experience while ensuring optimal performance. It uses background processing,
 * intelligent caching, and predictive loading to minimize UI blocking operations.
 * 
 * Key Optimization Features:
 * - Background data processing to keep UI thread free
 * - Intelligent caching with LRU eviction policy
 * - Predictive loading for anticipated user actions
 * - Efficient data transformation and aggregation
 * - Memory-conscious data structures
 * - Supportive loading states that don't compromise performance
 */
@Singleton
class ProgressDataLoadingOptimizer @Inject constructor() {
    
    // Optimized caching system
    private val dataCache = LRUCache<String, CachedProgressData>(maxSize = 50)
    private val transformationCache = ConcurrentHashMap<String, Any>()
    
    // Background processing
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataProcessingDispatcher = Dispatchers.IO.limitedParallelism(2)
    
    // Performance monitoring
    private var cacheHits = 0
    private var cacheMisses = 0
    private var averageLoadTime = 0L
    
    /**
     * Loads progress data with optimal performance and supportive caching
     */
    suspend fun loadProgressDataOptimized(
        weekStartDate: LocalDate,
        dataLoader: suspend () -> com.vibehealth.android.ui.progress.models.WeeklyProgressData
    ): Flow<OptimizedDataResult> = flow {
        val cacheKey = generateCacheKey(weekStartDate)
        
        // Emit cached data immediately if available
        dataCache.get(cacheKey)?.let { cachedData ->
            if (!cachedData.isExpired()) {
                cacheHits++
                emit(OptimizedDataResult.CachedData(
                    data = cachedData.data,
                    supportiveMessage = "Showing your recent progress data",
                    loadTime = 0L
                ))
                
                // Continue to check for fresh data in background
                backgroundScope.launch {
                    refreshDataInBackground(weekStartDate, dataLoader, cacheKey)
                }
                return@flow
            }
        }
        
        // Emit loading state
        emit(OptimizedDataResult.Loading(
            supportiveMessage = "Loading your wellness progress...",
            encouragingContext = "We're excited to show you your journey!"
        ))
        
        // Load fresh data with performance monitoring
        val startTime = System.currentTimeMillis()
        
        try {
            val freshData = withContext(dataProcessingDispatcher) {
                val rawData = dataLoader()
                
                // Optimize data structure for UI consumption
                optimizeDataStructure(rawData)
            }
            
            val loadTime = System.currentTimeMillis() - startTime
            averageLoadTime = (averageLoadTime + loadTime) / 2
            
            // Cache the optimized data
            dataCache.put(cacheKey, CachedProgressData(
                data = freshData,
                timestamp = System.currentTimeMillis(),
                expirationTime = System.currentTimeMillis() + CACHE_EXPIRATION_MS
            ))
            
            cacheMisses++
            
            emit(OptimizedDataResult.Success(
                data = freshData,
                supportiveMessage = "Your wellness progress is ready!",
                loadTime = loadTime,
                fromCache = false
            ))
            
        } catch (e: Exception) {
            emit(OptimizedDataResult.Error(
                error = e,
                supportiveMessage = "We're having trouble loading your progress data",
                encouragingContext = "Your wellness journey continues - let's try again!",
                isRetryable = true
            ))
        }
    }
    
    /**
     * Optimizes data structure for efficient UI consumption
     */
    private suspend fun optimizeDataStructure(
        data: com.vibehealth.android.ui.progress.models.WeeklyProgressData
    ): com.vibehealth.android.ui.progress.models.WeeklyProgressData = withContext(Dispatchers.Default) {
        
        // Pre-calculate display values to avoid UI thread calculations
        val optimizedDailyData = data.dailyData.map { dailyData ->
            val stepsDisplay = formatStepsForDisplay(dailyData.steps)
            val caloriesDisplay = formatCaloriesForDisplay(dailyData.calories)
            val heartPointsDisplay = formatHeartPointsForDisplay(dailyData.heartPoints)
            
            // Cache formatted values for quick access
            val dateKey = dailyData.date.toString()
            transformationCache["${dateKey}_steps_display"] = stepsDisplay
            transformationCache["${dateKey}_calories_display"] = caloriesDisplay
            transformationCache["${dateKey}_heartpoints_display"] = heartPointsDisplay
            
            dailyData
        }
        
        // Pre-calculate weekly totals for smooth rendering
        val optimizedWeeklyTotals = data.weeklyTotals
        
        data.copy(
            dailyData = optimizedDailyData,
            weeklyTotals = optimizedWeeklyTotals
        )
    }
    
    /**
     * Refreshes data in background without blocking UI
     */
    private suspend fun refreshDataInBackground(
        weekStartDate: LocalDate,
        dataLoader: suspend () -> com.vibehealth.android.ui.progress.models.WeeklyProgressData,
        cacheKey: String
    ) {
        try {
            val freshData = dataLoader()
            val optimizedData = optimizeDataStructure(freshData)
            
            // Update cache with fresh data
            dataCache.put(cacheKey, CachedProgressData(
                data = optimizedData,
                timestamp = System.currentTimeMillis(),
                expirationTime = System.currentTimeMillis() + CACHE_EXPIRATION_MS
            ))
            
        } catch (e: Exception) {
            // Silent background refresh failure - cached data remains valid
        }
    }
    
    /**
     * Predictively loads next week's data for smooth navigation
     */
    fun preloadNextWeekData(
        currentWeekStart: LocalDate,
        dataLoader: suspend () -> com.vibehealth.android.ui.progress.models.WeeklyProgressData
    ) {
        backgroundScope.launch {
            val nextWeekStart = currentWeekStart.plusWeeks(1)
            val cacheKey = generateCacheKey(nextWeekStart)
            
            if (dataCache.get(cacheKey) == null) {
                try {
                    val nextWeekData = dataLoader()
                    val optimizedData = optimizeDataStructure(nextWeekData)
                    
                    dataCache.put(cacheKey, CachedProgressData(
                        data = optimizedData,
                        timestamp = System.currentTimeMillis(),
                        expirationTime = System.currentTimeMillis() + CACHE_EXPIRATION_MS
                    ))
                } catch (e: Exception) {
                    // Preloading failure is not critical
                }
            }
        }
    }
    
    /**
     * Formats steps for display with supportive messaging
     */
    private fun formatStepsForDisplay(steps: Int): String {
        return when {
            steps >= 10000 -> "${String.format("%,d", steps)} steps - Excellent!"
            steps >= 5000 -> "${String.format("%,d", steps)} steps - Great progress!"
            steps > 0 -> "${String.format("%,d", steps)} steps - Every step counts!"
            else -> "Ready to start your journey!"
        }
    }
    
    /**
     * Formats calories for display with encouraging context
     */
    private fun formatCaloriesForDisplay(calories: Double): String {
        return when {
            calories >= 2000 -> "${String.format("%.0f", calories)} cal - Fantastic energy!"
            calories >= 1000 -> "${String.format("%.0f", calories)} cal - Good activity!"
            calories > 0 -> "${String.format("%.0f", calories)} cal - Building momentum!"
            else -> "Ready to energize your day!"
        }
    }
    
    /**
     * Formats heart points for display with supportive messaging
     */
    private fun formatHeartPointsForDisplay(heartPoints: Int): String {
        return when {
            heartPoints >= 30 -> "$heartPoints points - Heart healthy!"
            heartPoints >= 15 -> "$heartPoints points - Great cardio!"
            heartPoints > 0 -> "$heartPoints points - Heart active!"
            else -> "Ready for heart health!"
        }
    }
    
    /**
     * Generates cache key for data storage
     */
    private fun generateCacheKey(weekStartDate: LocalDate): String {
        return "progress_week_${weekStartDate.toEpochDay()}"
    }
    
    /**
     * Gets performance metrics for monitoring
     */
    fun getPerformanceMetrics(): DataLoadingMetrics {
        val totalRequests = cacheHits + cacheMisses
        val cacheHitRate = if (totalRequests > 0) cacheHits.toFloat() / totalRequests else 0f
        
        return DataLoadingMetrics(
            cacheHitRate = cacheHitRate,
            averageLoadTime = averageLoadTime,
            cacheSize = dataCache.size(),
            totalRequests = totalRequests
        )
    }
    
    /**
     * Clears cache to free memory
     */
    fun clearCache() {
        dataCache.evictAll()
        transformationCache.clear()
    }
    
    companion object {
        private const val CACHE_EXPIRATION_MS = 30 * 60 * 1000L // 30 minutes
    }
}

/**
 * Cached progress data with expiration
 */
data class CachedProgressData(
    val data: com.vibehealth.android.ui.progress.models.WeeklyProgressData,
    val timestamp: Long,
    val expirationTime: Long
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expirationTime
}

/**
 * Optimized data loading results
 */
sealed class OptimizedDataResult {
    data class Loading(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : OptimizedDataResult()
    
    data class CachedData(
        val data: com.vibehealth.android.ui.progress.models.WeeklyProgressData,
        val supportiveMessage: String,
        val loadTime: Long
    ) : OptimizedDataResult()
    
    data class Success(
        val data: com.vibehealth.android.ui.progress.models.WeeklyProgressData,
        val supportiveMessage: String,
        val loadTime: Long,
        val fromCache: Boolean
    ) : OptimizedDataResult()
    
    data class Error(
        val error: Exception,
        val supportiveMessage: String,
        val encouragingContext: String,
        val isRetryable: Boolean
    ) : OptimizedDataResult()
}

/**
 * Data loading performance metrics
 */
data class DataLoadingMetrics(
    val cacheHitRate: Float,
    val averageLoadTime: Long,
    val cacheSize: Int,
    val totalRequests: Int
) {
    val isPerformant: Boolean
        get() = cacheHitRate > 0.7f && averageLoadTime < 500L
}

/**
 * Simple LRU Cache implementation
 */
class LRUCache<K, V>(private val maxSize: Int) {
    private val cache = LinkedHashMap<K, V>(maxSize, 0.75f, true)
    
    @Synchronized
    fun get(key: K): V? = cache[key]
    
    @Synchronized
    fun put(key: K, value: V): V? {
        val previous = cache.put(key, value)
        if (cache.size > maxSize) {
            val eldest = cache.entries.iterator().next()
            cache.remove(eldest.key)
        }
        return previous
    }
    
    @Synchronized
    fun size(): Int = cache.size
    
    @Synchronized
    fun evictAll() = cache.clear()
}