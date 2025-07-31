package com.vibehealth.android.domain.goals.performance

import com.vibehealth.android.domain.goals.*
import com.vibehealth.android.domain.user.Gender
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Performance-optimized goal calculation service.
 * 
 * Implements calculation result caching, minimizes object creation,
 * and optimizes mathematical operations as specified in Task 6.1.
 */
@Singleton
class OptimizedGoalCalculationService @Inject constructor(
    private val stepsCalculator: StepsGoalCalculator,
    private val caloriesCalculator: CaloriesGoalCalculator,
    private val heartPointsCalculator: HeartPointsGoalCalculator,
    private val fallbackGenerator: FallbackGoalGenerator,
    private val performanceMonitor: GoalCalculationPerformanceMonitor
) {

    // Cache for calculation results to avoid redundant calculations
    private val calculationCache = mutableMapOf<String, CachedCalculationResult>()
    private val maxCacheSize = 100
    private val cacheExpirationMs = 5 * 60 * 1000L // 5 minutes

    /**
     * Optimized goal calculation with caching and performance monitoring.
     */
    suspend fun calculateGoals(input: GoalCalculationInput): Result<DailyGoals> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Check cache first
            val cacheKey = generateCacheKey(input)
            val cachedResult = getCachedResult(cacheKey)
            if (cachedResult != null) {
                performanceMonitor.recordCacheHit(System.currentTimeMillis() - startTime)
                return@withContext Result.success(cachedResult)
            }

            // Perform optimized calculations
            val goals = performOptimizedCalculations(input)
            
            // Cache the result
            cacheResult(cacheKey, goals)
            
            val duration = System.currentTimeMillis() - startTime
            performanceMonitor.recordCalculationSuccess(duration)
            
            Result.success(goals)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            performanceMonitor.recordCalculationFailure(duration, e)
            
            // Try fallback
            try {
                val fallbackGoals = fallbackGenerator.generateFallbackGoals(
                    userId = "", // Will be set by caller
                    userProfile = null
                )
                performanceMonitor.recordFallbackUsage()
                Result.success(fallbackGoals)
            } catch (fallbackException: Exception) {
                Result.failure(fallbackException)
            }
        }
    }

    /**
     * Performs optimized calculations with minimal object allocation.
     */
    private suspend fun performOptimizedCalculations(input: GoalCalculationInput): DailyGoals = coroutineScope {
        // Pre-calculate common values to avoid redundant calculations
        val ageDouble = input.age.toDouble()
        val heightDouble = input.heightInCm.toDouble()
        val weightDouble = input.weightInKg
        
        // Calculate all goals in parallel for better performance
        val stepsDeferred = this@coroutineScope.async { 
            calculateStepsOptimized(ageDouble, input.gender, input.activityLevel) 
        }
        val caloriesDeferred = this@coroutineScope.async { 
            calculateCaloriesOptimized(ageDouble, input.gender, heightDouble, weightDouble, input.activityLevel) 
        }
        val heartPointsDeferred = this@coroutineScope.async { 
            calculateHeartPointsOptimized(ageDouble, input.activityLevel) 
        }

        val stepsGoal = stepsDeferred.await()
        val caloriesGoal = caloriesDeferred.await()
        val heartPointsGoal = heartPointsDeferred.await()

        DailyGoals(
            userId = "", // Will be set by caller
            stepsGoal = stepsGoal,
            caloriesGoal = caloriesGoal,
            heartPointsGoal = heartPointsGoal,
            calculatedAt = LocalDateTime.ofEpochSecond(System.currentTimeMillis() / 1000, 0, ZoneOffset.UTC),
            calculationSource = CalculationSource.WHO_STANDARD
        )
    }

    /**
     * Optimized steps calculation with minimal allocations.
     */
    private fun calculateStepsOptimized(age: Double, gender: Gender, activityLevel: ActivityLevel): Int {
        // Base WHO recommendation
        var steps = 10000.0

        // Age adjustment (optimized calculation)
        when {
            age < 18 -> steps *= 1.2
            age > 65 -> steps *= 0.8
            age > 50 -> steps *= 0.9
        }

        // Gender adjustment (minimal)
        if (gender == Gender.FEMALE) {
            steps *= 0.95
        }

        // Activity level adjustment
        steps *= when (activityLevel) {
            ActivityLevel.SEDENTARY -> 0.8
            ActivityLevel.LIGHT -> 1.0
            ActivityLevel.MODERATE -> 1.1
            ActivityLevel.ACTIVE -> 1.2
            ActivityLevel.VERY_ACTIVE -> 1.3
        }

        // Apply bounds and return
        return steps.toInt().coerceIn(5000, 20000)
    }

    /**
     * Optimized calories calculation using pre-computed constants.
     */
    private fun calculateCaloriesOptimized(
        age: Double, 
        gender: Gender, 
        height: Double, 
        weight: Double, 
        activityLevel: ActivityLevel
    ): Int {
        // Harris-Benedict equation optimized for performance
        val bmr = when (gender) {
            Gender.MALE -> 88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age)
            Gender.FEMALE -> 447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age)
            else -> {
                // Mifflin-St Jeor for other genders (more accurate)
                10 * weight + 6.25 * height - 5 * age + if (gender == Gender.MALE) 5 else -161
            }
        }

        // Activity multiplier (pre-computed constants)
        val activityMultiplier = when (activityLevel) {
            ActivityLevel.SEDENTARY -> 1.2
            ActivityLevel.LIGHT -> 1.375
            ActivityLevel.MODERATE -> 1.55
            ActivityLevel.ACTIVE -> 1.725
            ActivityLevel.VERY_ACTIVE -> 1.9
        }

        val totalCalories = bmr * activityMultiplier
        return totalCalories.toInt().coerceIn(1200, 4000)
    }

    /**
     * Optimized heart points calculation.
     */
    private fun calculateHeartPointsOptimized(age: Double, activityLevel: ActivityLevel): Int {
        // WHO 150 minutes moderate activity per week = ~21 heart points per day
        var heartPoints = 21.0

        // Age adjustment
        when {
            age < 30 -> heartPoints *= 1.1
            age > 60 -> heartPoints *= 0.9
        }

        // Activity level adjustment
        heartPoints *= when (activityLevel) {
            ActivityLevel.SEDENTARY -> 0.8
            ActivityLevel.LIGHT -> 1.0
            ActivityLevel.MODERATE -> 1.1
            ActivityLevel.ACTIVE -> 1.3
            ActivityLevel.VERY_ACTIVE -> 1.5
        }

        return heartPoints.toInt().coerceIn(15, 50)
    }

    /**
     * Generates cache key for calculation input.
     */
    private fun generateCacheKey(input: GoalCalculationInput): String {
        return "${input.age}_${input.gender}_${input.heightInCm}_${input.weightInKg}_${input.activityLevel}"
    }

    /**
     * Retrieves cached result if valid.
     */
    private fun getCachedResult(cacheKey: String): DailyGoals? {
        val cached = calculationCache[cacheKey]
        return if (cached != null && !cached.isExpired()) {
            cached.goals
        } else {
            calculationCache.remove(cacheKey)
            null
        }
    }

    /**
     * Caches calculation result with expiration.
     */
    private fun cacheResult(cacheKey: String, goals: DailyGoals) {
        // Implement LRU eviction if cache is full
        if (calculationCache.size >= maxCacheSize) {
            val oldestKey = calculationCache.keys.first()
            calculationCache.remove(oldestKey)
        }

        calculationCache[cacheKey] = CachedCalculationResult(
            goals = goals,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Clears expired cache entries.
     */
    fun clearExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = calculationCache.filter { (_, cached) ->
            currentTime - cached.timestamp > cacheExpirationMs
        }.keys
        
        expiredKeys.forEach { calculationCache.remove(it) }
    }

    /**
     * Gets cache statistics for monitoring.
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = calculationCache.size,
            maxSize = maxCacheSize,
            hitRate = performanceMonitor.getCacheHitRate()
        )
    }

    /**
     * Cached calculation result with expiration.
     */
    private data class CachedCalculationResult(
        val goals: DailyGoals,
        val timestamp: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > 5 * 60 * 1000L // 5 minutes
        }
    }

    /**
     * Cache statistics for monitoring.
     */
    data class CacheStats(
        val size: Int,
        val maxSize: Int,
        val hitRate: Double
    )
}