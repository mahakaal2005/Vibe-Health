package com.vibehealth.android.domain.goals.performance

import android.os.Debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Performance monitoring for goal calculation operations.
 * 
 * Instruments calculation timing, success rates, memory usage,
 * and provides actionable insights as specified in Task 6.1.
 */
@Singleton
class GoalCalculationPerformanceMonitor @Inject constructor() {

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    private val calculationTimes = mutableListOf<Long>()
    private val maxHistorySize = 100
    
    private var totalCalculations = 0L
    private var successfulCalculations = 0L
    private var failedCalculations = 0L
    private var cacheHits = 0L
    private var fallbackUsages = 0L

    /**
     * Records successful calculation timing.
     */
    fun recordCalculationSuccess(durationMs: Long) {
        synchronized(this) {
            totalCalculations++
            successfulCalculations++
            recordCalculationTime(durationMs)
            updateMetrics()
        }
    }

    /**
     * Records failed calculation timing.
     */
    fun recordCalculationFailure(durationMs: Long, exception: Exception) {
        synchronized(this) {
            totalCalculations++
            failedCalculations++
            recordCalculationTime(durationMs)
            updateMetrics()
        }
    }

    /**
     * Records cache hit timing.
     */
    fun recordCacheHit(durationMs: Long) {
        synchronized(this) {
            cacheHits++
            recordCalculationTime(durationMs)
            updateMetrics()
        }
    }

    /**
     * Records fallback usage.
     */
    fun recordFallbackUsage() {
        synchronized(this) {
            fallbackUsages++
            updateMetrics()
        }
    }

    /**
     * Records database operation performance.
     */
    fun recordDatabaseOperation(operationType: DatabaseOperationType, durationMs: Long, success: Boolean) {
        synchronized(this) {
            val currentMetrics = _performanceMetrics.value
            val dbMetrics = currentMetrics.databaseMetrics.toMutableMap()
            
            val operationMetrics = dbMetrics[operationType] ?: DatabaseOperationMetrics()
            val updatedMetrics = operationMetrics.copy(
                totalOperations = operationMetrics.totalOperations + 1,
                successfulOperations = if (success) operationMetrics.successfulOperations + 1 else operationMetrics.successfulOperations,
                averageDurationMs = calculateNewAverage(operationMetrics.averageDurationMs, operationMetrics.totalOperations, durationMs),
                minDurationMs = min(operationMetrics.minDurationMs, durationMs),
                maxDurationMs = max(operationMetrics.maxDurationMs, durationMs)
            )
            
            dbMetrics[operationType] = updatedMetrics
            
            _performanceMetrics.value = currentMetrics.copy(
                databaseMetrics = dbMetrics
            )
        }
    }

    /**
     * Records network sync performance.
     */
    fun recordNetworkSync(syncType: NetworkSyncType, durationMs: Long, success: Boolean, bytesTransferred: Long = 0) {
        synchronized(this) {
            val currentMetrics = _performanceMetrics.value
            val networkMetrics = currentMetrics.networkMetrics.toMutableMap()
            
            val syncMetrics = networkMetrics[syncType] ?: NetworkSyncMetrics()
            val updatedMetrics = syncMetrics.copy(
                totalSyncs = syncMetrics.totalSyncs + 1,
                successfulSyncs = if (success) syncMetrics.successfulSyncs + 1 else syncMetrics.successfulSyncs,
                averageDurationMs = calculateNewAverage(syncMetrics.averageDurationMs, syncMetrics.totalSyncs, durationMs),
                totalBytesTransferred = syncMetrics.totalBytesTransferred + bytesTransferred
            )
            
            networkMetrics[syncType] = updatedMetrics
            
            _performanceMetrics.value = currentMetrics.copy(
                networkMetrics = networkMetrics
            )
        }
    }

    /**
     * Records memory usage snapshot.
     */
    fun recordMemoryUsage() {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        
        val currentMetrics = _performanceMetrics.value
        _performanceMetrics.value = currentMetrics.copy(
            memoryMetrics = MemoryMetrics(
                heapUsedKB = (memoryInfo.dalvikPrivateDirty + memoryInfo.nativePrivateDirty).toLong(),
                heapMaxKB = (Runtime.getRuntime().maxMemory() / 1024).toLong(),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Gets cache hit rate.
     */
    fun getCacheHitRate(): Double {
        return if (totalCalculations > 0) {
            cacheHits.toDouble() / totalCalculations.toDouble()
        } else {
            0.0
        }
    }

    /**
     * Gets performance insights and recommendations.
     */
    fun getPerformanceInsights(): List<PerformanceInsight> {
        val insights = mutableListOf<PerformanceInsight>()
        val metrics = _performanceMetrics.value

        // Calculation performance insights
        if (metrics.averageCalculationTimeMs > 500) {
            insights.add(
                PerformanceInsight(
                    type = InsightType.WARNING,
                    category = "Calculation Performance",
                    message = "Average calculation time (${metrics.averageCalculationTimeMs}ms) exceeds 500ms target",
                    recommendation = "Consider optimizing calculation algorithms or increasing cache size"
                )
            )
        }

        // Success rate insights
        if (metrics.successRate < 0.95) {
            insights.add(
                PerformanceInsight(
                    type = InsightType.ERROR,
                    category = "Reliability",
                    message = "Success rate (${(metrics.successRate * 100).toInt()}%) is below 95% target",
                    recommendation = "Investigate calculation failures and improve error handling"
                )
            )
        }

        // Cache performance insights
        if (getCacheHitRate() < 0.3) {
            insights.add(
                PerformanceInsight(
                    type = InsightType.INFO,
                    category = "Cache Performance",
                    message = "Cache hit rate (${(getCacheHitRate() * 100).toInt()}%) could be improved",
                    recommendation = "Consider increasing cache size or adjusting cache expiration time"
                )
            )
        }

        // Memory usage insights
        val memoryUsagePercent = if (metrics.memoryMetrics.heapMaxKB > 0) {
            (metrics.memoryMetrics.heapUsedKB.toDouble() / metrics.memoryMetrics.heapMaxKB.toDouble()) * 100
        } else 0.0

        if (memoryUsagePercent > 80) {
            insights.add(
                PerformanceInsight(
                    type = InsightType.WARNING,
                    category = "Memory Usage",
                    message = "Memory usage (${memoryUsagePercent.toInt()}%) is high",
                    recommendation = "Consider reducing cache size or optimizing object allocation"
                )
            )
        }

        // Database performance insights
        metrics.databaseMetrics.forEach { (operationType, dbMetrics) ->
            if (dbMetrics.averageDurationMs > 100) {
                insights.add(
                    PerformanceInsight(
                        type = InsightType.WARNING,
                        category = "Database Performance",
                        message = "$operationType operations averaging ${dbMetrics.averageDurationMs}ms",
                        recommendation = "Consider adding database indexes or optimizing queries"
                    )
                )
            }
        }

        return insights
    }

    /**
     * Resets all performance metrics.
     */
    fun resetMetrics() {
        synchronized(this) {
            calculationTimes.clear()
            totalCalculations = 0
            successfulCalculations = 0
            failedCalculations = 0
            cacheHits = 0
            fallbackUsages = 0
            _performanceMetrics.value = PerformanceMetrics()
        }
    }

    private fun recordCalculationTime(durationMs: Long) {
        calculationTimes.add(durationMs)
        if (calculationTimes.size > maxHistorySize) {
            calculationTimes.removeAt(0)
        }
    }

    private fun updateMetrics() {
        val avgTime = if (calculationTimes.isNotEmpty()) {
            calculationTimes.average().toLong()
        } else 0L

        val minTime = calculationTimes.minOrNull() ?: 0L
        val maxTime = calculationTimes.maxOrNull() ?: 0L

        _performanceMetrics.value = _performanceMetrics.value.copy(
            totalCalculations = totalCalculations,
            successfulCalculations = successfulCalculations,
            failedCalculations = failedCalculations,
            successRate = if (totalCalculations > 0) successfulCalculations.toDouble() / totalCalculations.toDouble() else 0.0,
            averageCalculationTimeMs = avgTime,
            minCalculationTimeMs = minTime,
            maxCalculationTimeMs = maxTime,
            cacheHits = cacheHits,
            fallbackUsages = fallbackUsages
        )
    }

    private fun calculateNewAverage(currentAvg: Long, count: Long, newValue: Long): Long {
        return if (count == 0L) {
            newValue
        } else {
            ((currentAvg * count) + newValue) / (count + 1)
        }
    }

    /**
     * Performance metrics data class.
     */
    data class PerformanceMetrics(
        val totalCalculations: Long = 0,
        val successfulCalculations: Long = 0,
        val failedCalculations: Long = 0,
        val successRate: Double = 0.0,
        val averageCalculationTimeMs: Long = 0,
        val minCalculationTimeMs: Long = 0,
        val maxCalculationTimeMs: Long = 0,
        val cacheHits: Long = 0,
        val fallbackUsages: Long = 0,
        val databaseMetrics: Map<DatabaseOperationType, DatabaseOperationMetrics> = emptyMap(),
        val networkMetrics: Map<NetworkSyncType, NetworkSyncMetrics> = emptyMap(),
        val memoryMetrics: MemoryMetrics = MemoryMetrics()
    )

    data class DatabaseOperationMetrics(
        val totalOperations: Long = 0,
        val successfulOperations: Long = 0,
        val averageDurationMs: Long = 0,
        val minDurationMs: Long = Long.MAX_VALUE,
        val maxDurationMs: Long = 0
    )

    data class NetworkSyncMetrics(
        val totalSyncs: Long = 0,
        val successfulSyncs: Long = 0,
        val averageDurationMs: Long = 0,
        val totalBytesTransferred: Long = 0
    )

    data class MemoryMetrics(
        val heapUsedKB: Long = 0,
        val heapMaxKB: Long = 0,
        val timestamp: Long = 0
    )

    data class PerformanceInsight(
        val type: InsightType,
        val category: String,
        val message: String,
        val recommendation: String
    )

    enum class InsightType {
        INFO, WARNING, ERROR
    }

    enum class DatabaseOperationType {
        INSERT, UPDATE, DELETE, QUERY, SYNC
    }

    enum class NetworkSyncType {
        UPLOAD, DOWNLOAD, BATCH_SYNC
    }
}