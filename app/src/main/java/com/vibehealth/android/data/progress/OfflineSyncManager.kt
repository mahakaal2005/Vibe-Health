package com.vibehealth.android.data.progress

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OfflineSyncManager - Manages offline-first progress data with supportive sync messaging
 * 
 * This class provides seamless offline functionality for progress tracking while
 * maintaining the supportive, encouraging user experience. It handles background
 * synchronization, conflict resolution, and provides reassuring feedback about
 * data availability and sync status.
 * 
 * Key Features:
 * - Offline-first architecture with local data priority
 * - Background synchronization with supportive status updates
 * - Graceful network failure handling with encouraging messaging
 * - Data consistency management between local and cloud storage
 * - Supportive offline indicators that don't create anxiety
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    private val context: Context,
    private val progressDatabase: ProgressDatabase,
    private val progressSyncService: ProgressSyncService
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Network state management
    private val _networkState = MutableStateFlow(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    // Sync status management
    private val _syncStatus = MutableStateFlow<OfflineSyncStatus>(OfflineSyncStatus.Idle)
    val syncStatus: StateFlow<OfflineSyncStatus> = _syncStatus.asStateFlow()
    
    // Pending sync operations
    private val pendingSyncOperations = mutableSetOf<String>()
    
    init {
        setupNetworkMonitoring()
        startPeriodicSync()
    }
    
    /**
     * Sets up network connectivity monitoring
     */
    private fun setupNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkState.value = NetworkState.Connected
                triggerBackgroundSync()
            }
            
            override fun onLost(network: Network) {
                _networkState.value = NetworkState.Disconnected
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                _networkState.value = if (hasInternet && hasValidated) {
                    NetworkState.Connected
                } else {
                    NetworkState.Limited
                }
            }
        })
    }
    
    /**
     * Starts periodic background synchronization
     */
    private fun startPeriodicSync() {
        syncScope.launch {
            while (true) {
                delay(SYNC_INTERVAL_MS)
                if (_networkState.value == NetworkState.Connected) {
                    performBackgroundSync()
                }
            }
        }
    }
    
    /**
     * Loads progress data with offline-first approach
     */
    suspend fun loadProgressDataOfflineFirst(
        weekStartDate: java.time.LocalDate
    ): Flow<OfflineDataResult> = flow {
        // Always emit local data first for immediate display
        val localData = progressDatabase.weeklyProgressDao().getWeeklyProgress(weekStartDate)
        
        if (localData != null) {
            emit(OfflineDataResult.LocalData(
                data = convertToWeeklyProgressData(localData),
                supportiveMessage = "Showing your saved progress data",
                lastSyncTime = localData.lastUpdated,
                isOffline = _networkState.value != NetworkState.Connected
            ))
        } else {
            emit(OfflineDataResult.NoLocalData(
                supportiveMessage = "Preparing to load your progress data",
                encouragingContext = "Your wellness journey is being retrieved!"
            ))
        }
        
        // Attempt to fetch fresh data if connected
        if (_networkState.value == NetworkState.Connected) {
            try {
                _syncStatus.value = OfflineSyncStatus.Syncing(
                    message = "Updating your progress data...",
                    encouragingContext = "Getting the latest from your wellness journey!"
                )
                
                val freshData = progressSyncService.fetchWeeklyProgressData(weekStartDate)
                
                if (freshData != null) {
                    // Save fresh data locally
                    val cacheEntity = convertToWeeklyProgressCacheEntity(freshData, weekStartDate)
                    progressDatabase.weeklyProgressDao().insertOrUpdate(cacheEntity)
                    
                    emit(OfflineDataResult.FreshData(
                        data = freshData,
                        supportiveMessage = "Your progress data is up to date!",
                        syncTime = System.currentTimeMillis()
                    ))
                }
                
                _syncStatus.value = OfflineSyncStatus.Success(
                    message = "Progress data updated successfully",
                    encouragingContext = "Your wellness journey is current!"
                )
                
            } catch (e: Exception) {
                handleSyncError(e, weekStartDate)
            }
        } else {
            emit(OfflineDataResult.OfflineMode(
                supportiveMessage = "Working offline - your progress is safely stored locally",
                encouragingContext = "Everything will sync automatically when you're back online",
                hasLocalData = localData != null
            ))
        }
    }
    
    /**
     * Saves progress data with offline support
     */
    suspend fun saveProgressDataOffline(
        data: com.vibehealth.android.ui.progress.models.WeeklyProgressData
    ): OfflineSaveResult {
        return try {
            // Always save locally first
            val cacheEntity = convertToWeeklyProgressCacheEntity(data, data.weekStartDate)
            progressDatabase.weeklyProgressDao().insertOrUpdate(cacheEntity)
            
            if (_networkState.value == NetworkState.Connected) {
                // Attempt immediate sync
                try {
                    progressSyncService.uploadWeeklyProgressData(data)
                    progressDatabase.weeklyProgressDao().updateSyncStatus(data.weekStartDate, com.vibehealth.android.domain.user.SyncStatus.SYNCED, null)
                    
                    OfflineSaveResult.SyncedSave(
                        supportiveMessage = "Progress saved and synced successfully!",
                        encouragingContext = "Your wellness data is safely stored everywhere"
                    )
                } catch (e: Exception) {
                    // Local save succeeded, sync failed - queue for later
                    queueForSync(data.weekStartDate.toString())
                    
                    OfflineSaveResult.LocalSave(
                        supportiveMessage = "Progress saved locally - will sync when connected",
                        encouragingContext = "Your wellness data is safe and will update automatically"
                    )
                }
            } else {
                // Offline mode - queue for sync
                queueForSync(data.weekStartDate.toString())
                
                OfflineSaveResult.OfflineSave(
                    supportiveMessage = "Progress saved offline - ready to sync later",
                    encouragingContext = "Your wellness journey continues even offline!"
                )
            }
        } catch (e: Exception) {
            OfflineSaveResult.SaveError(
                error = e,
                supportiveMessage = "Having trouble saving your progress",
                encouragingContext = "Let's try again - your wellness data is important to us"
            )
        }
    }
    
    /**
     * Triggers background synchronization
     */
    private fun triggerBackgroundSync() {
        syncScope.launch {
            performBackgroundSync()
        }
    }
    
    /**
     * Performs background synchronization of pending data
     */
    private suspend fun performBackgroundSync() {
        if (pendingSyncOperations.isEmpty()) return
        
        _syncStatus.value = OfflineSyncStatus.BackgroundSync(
            message = "Syncing your wellness data in the background",
            itemCount = pendingSyncOperations.size
        )
        
        val operationsToSync = pendingSyncOperations.toList()
        var successCount = 0
        
        for (operation in operationsToSync) {
            try {
                val weekStartDate = java.time.LocalDate.parse(operation)
                val localData = progressDatabase.weeklyProgressDao().getWeeklyProgress(weekStartDate)
                
                if (localData != null) {
                    val weeklyProgressData = convertToWeeklyProgressData(localData)
                    progressSyncService.uploadWeeklyProgressData(weeklyProgressData)
                    progressDatabase.weeklyProgressDao().updateSyncStatus(weekStartDate, com.vibehealth.android.domain.user.SyncStatus.SYNCED, null)
                    pendingSyncOperations.remove(operation)
                    successCount++
                }
            } catch (e: Exception) {
                // Continue with other operations
                continue
            }
        }
        
        if (successCount > 0) {
            _syncStatus.value = OfflineSyncStatus.Success(
                message = "Synced $successCount wellness data updates",
                encouragingContext = "Your progress is now up to date everywhere!"
            )
        } else if (operationsToSync.isNotEmpty()) {
            _syncStatus.value = OfflineSyncStatus.PartialSync(
                message = "Some data is still waiting to sync",
                encouragingContext = "We'll keep trying automatically - no action needed"
            )
        }
        
        // Reset to idle after a delay
        delay(3000)
        _syncStatus.value = OfflineSyncStatus.Idle
    }
    
    /**
     * Handles synchronization errors with supportive messaging
     */
    private suspend fun handleSyncError(error: Exception, weekStartDate: java.time.LocalDate) {
        queueForSync(weekStartDate.toString())
        
        val supportiveError = when (error) {
            is java.net.UnknownHostException -> OfflineSyncStatus.NetworkError(
                message = "Connection issue - using your saved progress data",
                encouragingContext = "Your wellness journey continues offline!",
                isRetryable = true
            )
            is java.net.SocketTimeoutException -> OfflineSyncStatus.TimeoutError(
                message = "Taking longer than expected - showing saved data",
                encouragingContext = "Your progress is safely stored locally",
                isRetryable = true
            )
            else -> OfflineSyncStatus.SyncError(
                message = "Sync issue - your local data is current",
                encouragingContext = "We'll keep trying automatically in the background",
                error = error,
                isRetryable = true
            )
        }
        
        _syncStatus.value = supportiveError
        
        // Auto-retry after delay
        delay(RETRY_DELAY_MS)
        if (_networkState.value == NetworkState.Connected) {
            triggerBackgroundSync()
        }
    }
    
    /**
     * Queues data for synchronization when connection is available
     */
    private fun queueForSync(identifier: String) {
        pendingSyncOperations.add(identifier)
    }
    
    /**
     * Converts WeeklyProgressCacheEntity to WeeklyProgressData
     */
    private fun convertToWeeklyProgressData(entity: WeeklyProgressCacheEntity): com.vibehealth.android.ui.progress.models.WeeklyProgressData {
        return com.vibehealth.android.ui.progress.models.WeeklyProgressData(
            weekStartDate = entity.weekStartDate,
            dailyData = emptyList(), // Would be populated from daily cache
            weeklyTotals = com.vibehealth.android.ui.progress.models.WeeklyTotals(
                totalSteps = entity.totalSteps,
                totalCalories = entity.totalCalories,
                totalHeartPoints = entity.totalHeartPoints,
                activeDays = entity.activeDays,
                averageStepsPerDay = entity.totalSteps / 7,
                averageCaloriesPerDay = entity.totalCalories / 7,
                averageHeartPointsPerDay = entity.totalHeartPoints / 7,
                supportiveWeeklySummary = "Your weekly progress summary"
            ),
            supportiveInsights = com.vibehealth.android.ui.progress.models.SupportiveInsights(
                weeklyTrends = emptyList(),
                achievements = emptyList(),
                gentleGuidance = emptyList(),
                wellnessJourneyContext = "Your wellness journey continues!",
                motivationalMessage = "Keep up the great work!"
            ),
            celebratoryMessages = entity.celebratoryMessages
        )
    }
    
    /**
     * Converts WeeklyProgressData to WeeklyProgressCacheEntity
     */
    private fun convertToWeeklyProgressCacheEntity(
        data: com.vibehealth.android.ui.progress.models.WeeklyProgressData,
        weekStartDate: java.time.LocalDate
    ): WeeklyProgressCacheEntity {
        return WeeklyProgressCacheEntity(
            weekStartDate = weekStartDate,
            totalSteps = data.weeklyTotals.totalSteps,
            totalCalories = data.weeklyTotals.totalCalories,
            totalHeartPoints = data.weeklyTotals.totalHeartPoints,
            activeDays = data.weeklyTotals.activeDays,
            celebratoryMessages = data.celebratoryMessages,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = com.vibehealth.android.domain.user.SyncStatus.SYNCED
        )
    }
    
    /**
     * Gets current offline status with supportive messaging
     */
    fun getOfflineStatus(): SyncOfflineStatus {
        return when (_networkState.value) {
            NetworkState.Connected -> SyncOfflineStatus.Online(
                supportiveMessage = "Connected - your progress syncs automatically",
                encouragingContext = "Your wellness data is always up to date"
            )
            NetworkState.Limited -> SyncOfflineStatus.LimitedConnection(
                supportiveMessage = "Limited connection - using saved progress data",
                encouragingContext = "Your wellness journey continues with local data"
            )
            NetworkState.Disconnected -> SyncOfflineStatus.Offline(
                supportiveMessage = "Working offline - your progress is safely stored locally",
                encouragingContext = "Everything will sync automatically when you're back online"
            )
            NetworkState.Unknown -> SyncOfflineStatus.Unknown(
                supportiveMessage = "Checking connection - your saved data is ready",
                encouragingContext = "Your wellness progress is always available"
            )
        }
    }
    
    /**
     * Forces a manual sync attempt
     */
    suspend fun forceSyncAttempt(): SyncAttemptResult {
        return if (_networkState.value == NetworkState.Connected) {
            try {
                performBackgroundSync()
                SyncAttemptResult.Success(
                    supportiveMessage = "Sync completed successfully!",
                    encouragingContext = "Your wellness data is now current everywhere"
                )
            } catch (e: Exception) {
                SyncAttemptResult.Failed(
                    supportiveMessage = "Sync didn't complete this time",
                    encouragingContext = "Your local data is safe - we'll keep trying automatically",
                    error = e
                )
            }
        } else {
            SyncAttemptResult.NoConnection(
                supportiveMessage = "No connection available for sync",
                encouragingContext = "Your progress is safely stored locally and will sync when connected"
            )
        }
    }
    
    companion object {
        private const val SYNC_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
        private const val RETRY_DELAY_MS = 30 * 1000L // 30 seconds
    }
}

/**
 * Network connectivity states
 */
enum class NetworkState {
    Connected,
    Limited,
    Disconnected,
    Unknown
}

/**
 * Synchronization status with supportive messaging
 */
sealed class OfflineSyncStatus {
    object Idle : OfflineSyncStatus()
    
    data class Syncing(
        val message: String,
        val encouragingContext: String
    ) : OfflineSyncStatus()
    
    data class BackgroundSync(
        val message: String,
        val itemCount: Int
    ) : OfflineSyncStatus()
    
    data class Success(
        val message: String,
        val encouragingContext: String
    ) : OfflineSyncStatus()
    
    data class PartialSync(
        val message: String,
        val encouragingContext: String
    ) : OfflineSyncStatus()
    
    data class NetworkError(
        val message: String,
        val encouragingContext: String,
        val isRetryable: Boolean
    ) : OfflineSyncStatus()
    
    data class TimeoutError(
        val message: String,
        val encouragingContext: String,
        val isRetryable: Boolean
    ) : OfflineSyncStatus()
    
    data class SyncError(
        val message: String,
        val encouragingContext: String,
        val error: Exception,
        val isRetryable: Boolean
    ) : OfflineSyncStatus()
}

/**
 * Offline data loading results
 */
sealed class OfflineDataResult {
    data class LocalData(
        val data: com.vibehealth.android.ui.progress.models.WeeklyProgressData,
        val supportiveMessage: String,
        val lastSyncTime: Long?,
        val isOffline: Boolean
    ) : OfflineDataResult()
    
    data class FreshData(
        val data: com.vibehealth.android.ui.progress.models.WeeklyProgressData,
        val supportiveMessage: String,
        val syncTime: Long
    ) : OfflineDataResult()
    
    data class NoLocalData(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : OfflineDataResult()
    
    data class OfflineMode(
        val supportiveMessage: String,
        val encouragingContext: String,
        val hasLocalData: Boolean
    ) : OfflineDataResult()
}

/**
 * Offline save results
 */
sealed class OfflineSaveResult {
    data class SyncedSave(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : OfflineSaveResult()
    
    data class LocalSave(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : OfflineSaveResult()
    
    data class OfflineSave(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : OfflineSaveResult()
    
    data class SaveError(
        val error: Exception,
        val supportiveMessage: String,
        val encouragingContext: String
    ) : OfflineSaveResult()
}

/**
 * Offline status with supportive messaging
 */
sealed class SyncOfflineStatus {
    data class Online(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : SyncOfflineStatus()
    
    data class LimitedConnection(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : SyncOfflineStatus()
    
    data class Offline(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : SyncOfflineStatus()
    
    data class Unknown(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : SyncOfflineStatus()
}

/**
 * Manual sync attempt results
 */
sealed class SyncAttemptResult {
    data class Success(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : SyncAttemptResult()
    
    data class Failed(
        val supportiveMessage: String,
        val encouragingContext: String,
        val error: Exception
    ) : SyncAttemptResult()
    
    data class NoConnection(
        val supportiveMessage: String,
        val encouragingContext: String
    ) : SyncAttemptResult()
}