package com.vibehealth.android.ui.progress

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehealth.android.data.progress.BasicProgressRepository
import com.vibehealth.android.ui.progress.models.ProgressUiState
import com.vibehealth.android.ui.progress.models.WeeklyProgressData
import com.vibehealth.android.ui.progress.models.MonthlyProgressData
import com.vibehealth.android.ui.progress.models.MetricType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * BasicProgressViewModel - Manages progress history state with Companion Principle
 * 
 * This ViewModel provides supportive state management for the progress history view,
 * transforming raw wellness data into encouraging insights and celebratory feedback.
 * Follows MVVM pattern with reactive programming using Kotlin Coroutines and Flow.
 * 
 * Features:
 * - Supportive state management with encouraging feedback
 * - Offline-first data loading with gentle messaging
 * - Celebratory achievement recognition
 * - Error handling with supportive recovery guidance
 * - Accessibility-friendly state announcements
 */
@HiltViewModel
class BasicProgressViewModel @Inject constructor(
    private val progressRepository: BasicProgressRepository
) : ViewModel() {
    
    // Internal mutable state
    private val _uiState = MutableStateFlow(ProgressUiState())
    private val _supportiveMessages = MutableSharedFlow<String>()
    private val _celebratoryFeedback = MutableSharedFlow<String>()
    
    // NEW MONTHLY EXTENSIONS
    private val _viewMode = MutableStateFlow(ViewMode.WEEKLY)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()
    
    private val _monthlyProgressData = MutableStateFlow<MonthlyProgressData?>(null)
    val monthlyProgressData: StateFlow<MonthlyProgressData?> = _monthlyProgressData.asStateFlow()
    
    // Public read-only state
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    val supportiveMessages: SharedFlow<String> = _supportiveMessages.asSharedFlow()
    val celebratoryFeedback: SharedFlow<String> = _celebratoryFeedback.asSharedFlow()
    
    enum class ViewMode { WEEKLY, MONTHLY }
    
    companion object {
        private const val TAG = "VIBE_FIX_VIEWMODEL"
    }
    
    init {
        Log.d(TAG, "VIBE_FIX: BasicProgressViewModel created successfully")
        // Load initial progress data with supportive context
        Log.d(TAG, "VIBE_FIX: About to call loadProgressWithEncouragement() from init")
        loadProgressWithEncouragement()
        Log.d(TAG, "VIBE_FIX: loadProgressWithEncouragement() called from init")
    }
    
    /**
     * Loads progress data with encouraging context and supportive messaging
     */
    fun loadProgressWithEncouragement(celebrationContext: String? = null) {
        Log.d(TAG, "VIBE_FIX: loadProgressWithEncouragement() called with context: $celebrationContext")
        viewModelScope.launch {
            Log.d(TAG, "VIBE_FIX: Inside viewModelScope.launch")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                supportiveMessage = "Preparing your wellness journey insights..."
            )
            Log.d(TAG, "VIBE_FIX: UI state set to loading")
            
            try {
                Log.d(TAG, "VIBE_FIX: About to call progressRepository.getWeeklyProgressWithSupportiveContext()")
                progressRepository.getWeeklyProgressWithSupportiveContext()
                    .catch { exception ->
                        Log.e(TAG, "VIBE_FIX: Exception caught in flow", exception)
                        handleProgressErrorWithSupport(exception)
                    }
                    .collect { progressResult ->
                        Log.d(TAG, "VIBE_FIX: Progress result received: ${progressResult::class.simpleName}")
                        handleProgressResultWithCelebration(progressResult, celebrationContext)
                    }
                Log.d(TAG, "VIBE_FIX: Flow collection completed")
                    
            } catch (exception: Exception) {
                Log.e(TAG, "VIBE_FIX: Exception in loadProgressWithEncouragement()", exception)
                handleProgressErrorWithSupport(exception)
            }
        }
        Log.d(TAG, "VIBE_FIX: loadProgressWithEncouragement() method completed")
    }
    
    /**
     * Retries loading progress data with encouraging feedback
     */
    fun retryLoadingWithEncouragement() {
        viewModelScope.launch {
            _supportiveMessages.emit("Let's try loading your progress again. Your wellness journey is worth celebrating!")
            loadProgressWithEncouragement("We're excited to show you your progress!")
        }
    }
    
    /**
     * Refreshes progress data with supportive messaging
     */
    fun refreshProgressWithSupport() {
        viewModelScope.launch {
            if (!_uiState.value.isLoading) {
                _supportiveMessages.emit("Refreshing your latest wellness achievements...")
                loadProgressWithEncouragement()
            }
        }
    }
    
    /**
     * Handles progress result with celebratory feedback and supportive context
     */
    private suspend fun handleProgressResultWithCelebration(
        result: ProgressDataResult,
        celebrationContext: String?
    ) {
        when (result) {
            is ProgressDataResult.Success -> {
                val weeklyData = result.data
                
                _uiState.value = ProgressUiState(
                    weeklyData = weeklyData,
                    isLoading = false,
                    supportiveMessage = "🎉 Your wellness journey is inspiring!",
                    celebratoryFeedback = "Great progress this week!",
                    showEmptyState = !weeklyData.hasAnyData
                )
                
                // Emit supportive messages
                _supportiveMessages.emit("Your progress data has been loaded successfully!")
                
                // Emit celebratory feedback
                _celebratoryFeedback.emit("Keep up the amazing work!")
                
                // Add celebration context if provided
                celebrationContext?.let { context ->
                    _supportiveMessages.emit(context)
                }
            }
            
            is ProgressDataResult.CachedSuccess -> {
                val weeklyData = result.data
                
                _uiState.value = ProgressUiState(
                    weeklyData = weeklyData,
                    isLoading = false,
                    supportiveMessage = "📊 Your cached progress data is ready!",
                    showEmptyState = !weeklyData.hasAnyData
                )
                
                // Show supportive offline message
                _supportiveMessages.emit(result.supportiveMessage)
            }
            
            is ProgressDataResult.EmptyState -> {
                _uiState.value = ProgressUiState(
                    isLoading = false,
                    showEmptyState = true,
                    supportiveMessage = result.encouragingMessage
                )
                
                _supportiveMessages.emit(result.encouragingMessage)
            }
            
            is ProgressDataResult.Error -> {
                _uiState.value = ProgressUiState(
                    isLoading = false,
                    hasError = true,
                    supportiveMessage = result.supportiveMessage
                )
                
                _supportiveMessages.emit(result.supportiveMessage)
            }
        }
    }
    
    /**
     * Handles progress errors with supportive recovery guidance
     */
    private suspend fun handleProgressErrorWithSupport(exception: Throwable) {
        _uiState.value = ProgressUiState(
            isLoading = false,
            hasError = true,
            supportiveMessage = "We're having trouble loading your progress, but your data is safe. Let's try again!"
        )
        
        _supportiveMessages.emit("Don't worry, we'll get your progress data loaded. Your wellness journey is important to us!")
    }
    
    /**
     * Detects achievements and generates celebratory feedback
     */
    private fun detectAchievementsWithCelebration(weeklyData: WeeklyProgressData): AchievementCelebration {
        val achievements = mutableListOf<String>()
        
        // Simple achievement detection based on weekly totals
        if (weeklyData.weeklyTotals.totalSteps > 50000) {
            achievements.add("Amazing! You walked over 50,000 steps this week!")
        }
        if (weeklyData.weeklyTotals.totalCalories > 14000) {
            achievements.add("Fantastic! You burned over 14,000 calories this week!")
        }
        if (weeklyData.weeklyTotals.totalHeartPoints > 150) {
            achievements.add("Outstanding! You earned over 150 heart points this week!")
        }
        
        // Check for consistency
        if (weeklyData.weeklyTotals.activeDays >= 5) {
            achievements.add("Incredible consistency! You were active on ${weeklyData.weeklyTotals.activeDays} days this week!")
        }
        
        return AchievementCelebration(
            hasAchievements = achievements.isNotEmpty(),
            celebratoryMessage = if (achievements.isNotEmpty()) {
                "🎉 " + achievements.joinToString(" ") + " Your dedication to wellness is inspiring!"
            } else {
                "Every step on your wellness journey matters. You're building healthy habits!"
            }
        )
    }
    
    /**
     * Gets weekly achievements for accessibility announcements
     */
    fun getWeeklyAchievements(): List<Achievement> {
        val currentData = _uiState.value.weeklyData ?: return emptyList()
        
        // Simple achievement list based on weekly totals
        val achievements = mutableListOf<Achievement>()
        
        if (currentData.weeklyTotals.totalSteps > 50000) {
            achievements.add(Achievement(
                type = "STEPS_MILESTONE",
                title = "Step Champion",
                description = "Over 50,000 steps this week!"
            ))
        }
        
        if (currentData.weeklyTotals.activeDays >= 5) {
            achievements.add(Achievement(
                type = "CONSISTENCY",
                title = "Consistency Star",
                description = "Active on ${currentData.weeklyTotals.activeDays} days!"
            ))
        }
        
        return achievements
    }
    
    /**
     * Simple Achievement data class
     */
    data class Achievement(
        val type: String,
        val title: String,
        val description: String
    )
    
    /**
     * MONTHLY EXTENSION: Sets view mode and loads appropriate data
     */
    fun setViewMode(mode: ViewMode) {
        Log.d("MONTHLY_VIEWMODEL", "Setting view mode to: $mode")
        _viewMode.value = mode
        
        when (mode) {
            ViewMode.WEEKLY -> loadProgressWithEncouragement("Showing your weekly progress") // EXISTING METHOD
            ViewMode.MONTHLY -> loadMonthlyData() // NEW EXTENSION
        }
    }
    
    /**
     * MONTHLY EXTENSION: Loads monthly progress data using existing repository patterns
     */
    fun loadMonthlyData() {
        Log.d("MONTHLY_VIEWMODEL", "Loading monthly progress data using existing repository")
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    supportiveMessage = "Preparing your monthly wellness insights..."
                )
                
                // Use existing repository patterns - extend with monthly support
                val monthlyData = progressRepository.getMonthlyProgress() // EXTEND EXISTING
                _monthlyProgressData.value = monthlyData
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    supportiveMessage = "🎉 Your monthly progress is inspiring!",
                    showEmptyState = !monthlyData.hasAnyData
                )
                
                Log.d("MONTHLY_VIEWMODEL", "Monthly data loaded successfully: ${monthlyData.totalSteps} total steps")
                
                // Emit supportive message
                _supportiveMessages.emit("Your monthly progress shows amazing consistency!")
                
            } catch (e: Exception) {
                Log.e("MONTHLY_ERRORS", "Failed to load monthly data", e)
                // Use existing error handling patterns
                handleProgressErrorWithSupport(e) // EXISTING METHOD
            }
        }
    }
    
    /**
     * Provides supportive context for specific metrics
     */
    fun getMetricSupportiveContext(metricType: MetricType): String {
        return when (metricType) {
            MetricType.STEPS -> "Every step you take builds stronger habits and better health!"
            MetricType.CALORIES -> "Your body is efficiently burning energy and building a healthy metabolism!"
            MetricType.HEART_POINTS -> "Your heart is getting stronger with each activity session!"
        }
    }
    
    /**
     * Handles user interaction with progress data (for analytics)
     */
    fun onProgressDataInteraction(interactionType: ProgressInteractionType) {
        viewModelScope.launch {
            // Track supportive interaction without PII
            when (interactionType) {
                ProgressInteractionType.GRAPH_EXPLORED -> {
                    _supportiveMessages.emit("Great job exploring your progress! Understanding your patterns helps build healthy habits.")
                }
                ProgressInteractionType.ACHIEVEMENT_CELEBRATED -> {
                    _celebratoryFeedback.emit("We love celebrating your wellness wins with you!")
                }
                ProgressInteractionType.INSIGHTS_VIEWED -> {
                    _supportiveMessages.emit("Your commitment to understanding your wellness journey is admirable!")
                }
            }
        }
    }
    
    /**
     * Data classes for supportive state management
     */
    data class AchievementCelebration(
        val hasAchievements: Boolean,
        val celebratoryMessage: String
    )
    
    /**
     * Sealed class for progress data results with supportive messaging
     */
    sealed class ProgressDataResult {
        data class Success(
            val data: WeeklyProgressData,
            val supportiveMessage: String = "Your progress data is ready! Let's celebrate your wellness journey."
        ) : ProgressDataResult()
        
        data class CachedSuccess(
            val data: WeeklyProgressData,
            val supportiveMessage: String = "Here's your recent progress! We'll refresh with the latest data in just a moment."
        ) : ProgressDataResult()
        
        data class EmptyState(
            val encouragingMessage: String = "Your wellness journey is just beginning! Every step counts, and we're here to celebrate your progress."
        ) : ProgressDataResult()
        
        data class Error(
            val exception: Throwable,
            val supportiveMessage: String = "We're having trouble loading your progress right now, but your data is safe. Please try again in a moment."
        ) : ProgressDataResult()
    }
    
    /**
     * Enum for tracking user interactions with supportive context
     */
    enum class ProgressInteractionType {
        GRAPH_EXPLORED,
        ACHIEVEMENT_CELEBRATED,
        INSIGHTS_VIEWED
    }
    
    /**
     * Error types for supportive error handling
     */
    sealed class ProgressError {
        object NetworkError : ProgressError()
        object DataNotFound : ProgressError()
        object CacheError : ProgressError()
        data class UnknownError(val exception: Throwable) : ProgressError()
        
        companion object {
            fun fromException(exception: Throwable): ProgressError {
                return when (exception) {
                    is java.net.UnknownHostException,
                    is java.net.SocketTimeoutException -> NetworkError
                    is NoSuchElementException -> DataNotFound
                    else -> UnknownError(exception)
                }
            }
        }
    }
}