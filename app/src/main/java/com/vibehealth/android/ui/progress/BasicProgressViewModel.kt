package com.vibehealth.android.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehealth.android.data.progress.BasicProgressRepository
import com.vibehealth.android.data.progress.OfflineProgressManager
import com.vibehealth.android.data.progress.ProgressDataResult
import com.vibehealth.android.ui.progress.models.ProgressUiState
import com.vibehealth.android.ui.progress.models.WeeklyProgressData
import com.vibehealth.android.ui.progress.models.Achievement
import com.vibehealth.android.ui.progress.models.MetricType
import com.vibehealth.android.ui.progress.ProgressError
import com.vibehealth.android.ui.progress.ProgressErrorResponse
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
    private val progressRepository: BasicProgressRepository,
    private val supportiveInsightsManager: SupportiveInsightsManager,
    private val progressErrorHandler: ProgressErrorHandler,
    private val offlineProgressManager: OfflineProgressManager
) : ViewModel() {
    
    // Internal mutable state
    private val _uiState = MutableStateFlow(ProgressUiState())
    private val _supportiveMessages = MutableSharedFlow<String>()
    private val _celebratoryFeedback = MutableSharedFlow<String>()
    
    // Public read-only state
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    val supportiveMessages: SharedFlow<String> = _supportiveMessages.asSharedFlow()
    val celebratoryFeedback: SharedFlow<String> = _celebratoryFeedback.asSharedFlow()
    
    init {
        // Load initial progress data with supportive context
        loadProgressWithEncouragement()
    }
    
    /**
     * Loads progress data with encouraging context and supportive messaging
     */
    fun loadProgressWithEncouragement(celebrationContext: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                supportiveMessage = "Preparing your wellness journey insights..."
            )
            
            try {
                progressRepository.getWeeklyProgressWithSupportiveContext()
                    .catch { exception ->
                        handleProgressErrorWithSupport(exception)
                    }
                    .collect { progressResult ->
                        handleProgressResultWithCelebration(progressResult, celebrationContext)
                    }
                    
            } catch (exception: Exception) {
                handleProgressErrorWithSupport(exception)
            }
        }
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
                val supportiveInsights = supportiveInsightsManager.generateSupportiveInsights(weeklyData)
                val achievements = detectAchievementsWithCelebration(weeklyData)
                
                _uiState.value = ProgressUiState(
                    weeklyData = weeklyData,
                    isLoading = false,
                    supportiveMessage = supportiveInsights.motivationalMessage,
                    celebratoryFeedback = achievements.celebratoryMessage,
                    showEmptyState = !weeklyData.hasAnyData
                )
                
                // Emit supportive messages
                if (result.supportiveMessage.isNotEmpty()) {
                    _supportiveMessages.emit(result.supportiveMessage)
                }
                
                // Emit celebratory feedback for achievements
                if (achievements.hasAchievements) {
                    _celebratoryFeedback.emit(achievements.celebratoryMessage)
                }
                
                // Add celebration context if provided
                celebrationContext?.let { context ->
                    _supportiveMessages.emit(context)
                }
            }
            
            is ProgressDataResult.CachedSuccess -> {
                val weeklyData = result.data
                val supportiveInsights = supportiveInsightsManager.generateSupportiveInsights(weeklyData)
                
                _uiState.value = ProgressUiState(
                    weeklyData = weeklyData,
                    isLoading = false,
                    supportiveMessage = supportiveInsights.motivationalMessage,
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
        val errorResponse = progressErrorHandler.handleProgressError(
            com.vibehealth.android.ui.progress.ProgressError.fromException(exception)
        )
        
        _uiState.value = ProgressUiState(
            isLoading = false,
            hasError = true,
            supportiveMessage = when (errorResponse) {
                is ProgressErrorResponse.EncouragingRetry -> errorResponse.supportiveMessage
                is ProgressErrorResponse.SupportiveEmptyState -> errorResponse.supportiveMessage
                is ProgressErrorResponse.GentleFallback -> errorResponse.supportiveMessage
                is ProgressErrorResponse.ReassuringSolution -> errorResponse.supportiveMessage
            }
        )
        
        val recoveryGuidance = when (errorResponse) {
            is ProgressErrorResponse.EncouragingRetry -> errorResponse.recoveryGuidance
            is ProgressErrorResponse.SupportiveEmptyState -> errorResponse.recoveryGuidance
            is ProgressErrorResponse.GentleFallback -> errorResponse.recoveryGuidance
            is ProgressErrorResponse.ReassuringSolution -> errorResponse.recoveryGuidance
        }
        
        _supportiveMessages.emit(recoveryGuidance)
    }
    
    /**
     * Detects achievements and generates celebratory feedback
     */
    private fun detectAchievementsWithCelebration(weeklyData: WeeklyProgressData): AchievementCelebration {
        val achievements = mutableListOf<String>()
        
        // Check for goal achievements
        weeklyData.dailyData.forEach { dailyData ->
            if (dailyData.goalAchievements.stepsGoalAchieved) {
                achievements.add("Amazing! You reached your step goal on ${dailyData.date.dayOfWeek}!")
            }
            if (dailyData.goalAchievements.caloriesGoalAchieved) {
                achievements.add("Fantastic! You hit your calorie burn target on ${dailyData.date.dayOfWeek}!")
            }
            if (dailyData.goalAchievements.heartPointsGoalAchieved) {
                achievements.add("Outstanding! You earned your heart points goal on ${dailyData.date.dayOfWeek}!")
            }
        }
        
        // Check for weekly patterns
        val activeDays = weeklyData.dailyData.count { it.hasActivity }
        if (activeDays >= 5) {
            achievements.add("Incredible consistency! You were active on $activeDays days this week!")
        }
        
        // Check for improvement trends
        val weeklyTrends = supportiveInsightsManager.analyzeWeeklyTrends(weeklyData)
        if (weeklyTrends.isNotEmpty()) {
            achievements.add("Your progress is trending upward! Keep up the excellent work!")
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
        val celebratoryAchievements = supportiveInsightsManager.extractAchievements(currentData)
        
        // Convert CelebratoryAchievement to Achievement
        return celebratoryAchievements.map { celebratory ->
            Achievement(
                type = celebratory.achievementType,
                title = celebratory.metricName,
                description = celebratory.celebratoryText,
                celebratoryMessage = celebratory.celebratoryText,
                dateAchieved = java.time.LocalDate.now(),
                metricType = when (celebratory.metricName.lowercase()) {
                    "steps" -> MetricType.STEPS
                    "calories" -> MetricType.CALORIES
                    "heart points" -> MetricType.HEART_POINTS
                    else -> MetricType.STEPS
                },
                value = celebratory.metricName
            )
        }
    }
    
    /**
     * Provides supportive context for specific metrics
     */
    fun getMetricSupportiveContext(metricType: MetricType): String {
        val currentData = _uiState.value.weeklyData ?: return ""
        return supportiveInsightsManager.getMetricEncouragement(currentData, metricType)
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