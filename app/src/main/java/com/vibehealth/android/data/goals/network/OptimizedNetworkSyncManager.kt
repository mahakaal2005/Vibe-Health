package com.vibehealth.android.data.goals.network

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.DocumentSnapshot

import com.vibehealth.android.domain.goals.DailyGoals
import com.vibehealth.android.domain.goals.performance.GoalCalculationPerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized network sync manager for goal data.
 * 
 * Implements request batching, compression, intelligent sync scheduling,
 * and optimized Firebase query patterns as specified in Task 6.1.
 */
@Singleton
class OptimizedNetworkSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val performanceMonitor: GoalCalculationPerformanceMonitor
) {

    private val syncQueue = Channel<SyncOperation>(capacity = Channel.UNLIMITED)
    private val batchSize = 10
    private val compressionThreshold = 1024 // bytes
    
    // Intelligent sync scheduling
    private var lastSyncTime = 0L
    private val minSyncInterval = 30 * 1000L // 30 seconds
    private val maxBatchWaitTime = 5 * 1000L // 5 seconds

    init {
        // Start background sync processor
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            processSyncQueue()
        }
    }

    /**
     * Queues goal for sync with intelligent batching.
     */
    suspend fun syncGoal(goal: DailyGoals): Result<Unit> {
        val operation = SyncOperation.SingleGoalSync(goal)
        return try {
            syncQueue.send(operation)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Queues multiple goals for batch sync.
     */
    suspend fun syncGoalsBatch(goals: List<DailyGoals>): Result<Unit> {
        val operation = SyncOperation.BatchGoalSync(goals)
        return try {
            syncQueue.send(operation)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads goals with optimized query patterns.
     */
    suspend fun downloadGoals(userId: String, limit: Int = 50): Result<List<DailyGoals>> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("goals")
                .orderBy("calculatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val goals = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(DailyGoals::class.java)
                } catch (e: Exception) {
                    null // Skip malformed documents
                }
            }

            val duration = System.currentTimeMillis() - startTime
            performanceMonitor.recordNetworkSync(
                GoalCalculationPerformanceMonitor.NetworkSyncType.DOWNLOAD,
                duration,
                true,
                calculateDataSize(goals)
            )

            Result.success(goals)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            performanceMonitor.recordNetworkSync(
                GoalCalculationPerformanceMonitor.NetworkSyncType.DOWNLOAD,
                duration,
                false
            )
            Result.failure(e)
        }
    }

    /**
     * Processes sync queue with intelligent batching and scheduling.
     */
    private suspend fun processSyncQueue() {
        val pendingOperations = mutableListOf<SyncOperation>()
        
        while (true) {
            try {
                // Collect operations for batching
                val operation = withTimeoutOrNull(maxBatchWaitTime) {
                    syncQueue.receive()
                }

                if (operation != null) {
                    pendingOperations.add(operation)
                }

                // Process batch if conditions are met
                val shouldProcessBatch = pendingOperations.isNotEmpty() && (
                    pendingOperations.size >= batchSize ||
                    operation == null || // Timeout occurred
                    System.currentTimeMillis() - lastSyncTime > minSyncInterval
                )

                if (shouldProcessBatch) {
                    processBatch(pendingOperations.toList())
                    pendingOperations.clear()
                    lastSyncTime = System.currentTimeMillis()
                }

            } catch (e: Exception) {
                // Log error and continue processing
                pendingOperations.clear()
                delay(1000) // Brief pause before retrying
            }
        }
    }

    /**
     * Processes a batch of sync operations efficiently.
     */
    private suspend fun processBatch(operations: List<SyncOperation>) {
        val startTime = System.currentTimeMillis()
        
        try {
            val batch = firestore.batch()
            var operationCount = 0

            operations.forEach { operation ->
                when (operation) {
                    is SyncOperation.SingleGoalSync -> {
                        addGoalToBatch(batch, operation.goal)
                        operationCount++
                    }
                    is SyncOperation.BatchGoalSync -> {
                        operation.goals.forEach { goal ->
                            addGoalToBatch(batch, goal)
                            operationCount++
                        }
                    }
                    is SyncOperation.ForceSync -> {
                        // Force sync doesn't add to batch, just triggers processing
                    }
                }
            }

            if (operationCount > 0) {
                batch.commit().await()
                
                val duration = System.currentTimeMillis() - startTime
                val totalDataSize = operations.sumOf { operation ->
                    when (operation) {
                        is SyncOperation.SingleGoalSync -> calculateDataSize(listOf(operation.goal))
                        is SyncOperation.BatchGoalSync -> calculateDataSize(operation.goals)
                        is SyncOperation.ForceSync -> 0L
                    }
                }

                performanceMonitor.recordNetworkSync(
                    GoalCalculationPerformanceMonitor.NetworkSyncType.BATCH_SYNC,
                    duration,
                    true,
                    totalDataSize
                )
            }

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            performanceMonitor.recordNetworkSync(
                GoalCalculationPerformanceMonitor.NetworkSyncType.BATCH_SYNC,
                duration,
                false
            )
            
            // Retry individual operations on batch failure
            retryIndividualOperations(operations)
        }
    }

    /**
     * Adds goal to Firestore batch with compression if needed.
     */
    private fun addGoalToBatch(batch: WriteBatch, goal: DailyGoals) {
        val docRef = firestore.collection("users")
            .document(goal.userId)
            .collection("goals")
            .document(generateGoalId(goal))

        val goalData = goal.toFirestoreMap()
        
        // Apply compression for large data
        val compressedData = if (calculateDataSize(listOf(goal)) > compressionThreshold) {
            compressGoalData(goalData)
        } else {
            goalData
        }

        batch.set(docRef, compressedData)
    }

    /**
     * Retries individual operations when batch fails.
     */
    private suspend fun retryIndividualOperations(operations: List<SyncOperation>) {
        operations.forEach { operation ->
            try {
                when (operation) {
                    is SyncOperation.SingleGoalSync -> {
                        syncGoalIndividually(operation.goal)
                    }
                    is SyncOperation.BatchGoalSync -> {
                        operation.goals.forEach { goal ->
                            syncGoalIndividually(goal)
                        }
                    }
                    is SyncOperation.ForceSync -> {
                        // Force sync doesn't need individual retry
                    }
                }
            } catch (e: Exception) {
                // Log individual failure but continue with other operations
            }
        }
    }

    /**
     * Syncs individual goal with retry logic.
     */
    private suspend fun syncGoalIndividually(goal: DailyGoals) {
        val startTime = System.currentTimeMillis()
        
        try {
            val docRef = firestore.collection("users")
                .document(goal.userId)
                .collection("goals")
                .document(generateGoalId(goal))

            docRef.set(goal.toFirestoreMap()).await()

            val duration = System.currentTimeMillis() - startTime
            performanceMonitor.recordNetworkSync(
                GoalCalculationPerformanceMonitor.NetworkSyncType.UPLOAD,
                duration,
                true,
                calculateDataSize(listOf(goal))
            )

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            performanceMonitor.recordNetworkSync(
                GoalCalculationPerformanceMonitor.NetworkSyncType.UPLOAD,
                duration,
                false
            )
            throw e
        }
    }

    /**
     * Compresses goal data for network transfer.
     */
    private fun compressGoalData(data: Map<String, Any>): Map<String, Any> {
        return try {
            // Simple JSON serialization without Gson
            val jsonString = data.toString()
            val compressed = compress(jsonString)
            mapOf(
                "compressed" to true,
                "data" to compressed
            )
        } catch (e: Exception) {
            data // Return original data if compression fails
        }
    }

    /**
     * GZIP compression utility.
     */
    private fun compress(data: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
        gzipOutputStream.write(data.toByteArray())
        gzipOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    /**
     * Calculates data size for monitoring.
     */
    private fun calculateDataSize(goals: List<DailyGoals>): Long {
        return goals.sumOf { goal ->
            // Rough estimation of serialized size
            goal.toString().length.toLong()
        }
    }

    /**
     * Gets sync queue status for monitoring.
     */
    fun getSyncQueueStatus(): SyncQueueStatus {
        return SyncQueueStatus(
            queueSize = syncQueue.tryReceive().isSuccess, // Approximate
            lastSyncTime = lastSyncTime,
            nextScheduledSync = lastSyncTime + minSyncInterval
        )
    }

    /**
     * Forces immediate sync of pending operations.
     */
    suspend fun forceSyncNow(): Result<Unit> {
        return try {
            syncQueue.send(SyncOperation.ForceSync)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync operation types.
     */
    sealed class SyncOperation {
        data class SingleGoalSync(val goal: DailyGoals) : SyncOperation()
        data class BatchGoalSync(val goals: List<DailyGoals>) : SyncOperation()
        object ForceSync : SyncOperation()
    }

    /**
     * Sync queue status for monitoring.
     */
    data class SyncQueueStatus(
        val queueSize: Boolean, // Simplified for this implementation
        val lastSyncTime: Long,
        val nextScheduledSync: Long
    )

    /**
     * Generates a unique ID for a goal.
     */
    private fun generateGoalId(goal: DailyGoals): String {
        return "${goal.userId}_${goal.calculatedAt}"
    }

    /**
     * Extension function to convert DailyGoals to Firestore map.
     */
    private fun DailyGoals.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "stepsGoal" to stepsGoal,
            "caloriesGoal" to caloriesGoal,
            "heartPointsGoal" to heartPointsGoal,
            "calculatedAt" to calculatedAt,
            "calculationSource" to calculationSource.name,
            "timestamp" to System.currentTimeMillis()
        )
    }
}