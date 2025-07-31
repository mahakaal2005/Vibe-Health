package com.vibehealth.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehealth.android.domain.dashboard.DashboardUseCase
import com.vibehealth.android.data.user.UserRepository
import com.vibehealth.android.ui.dashboard.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the dashboard following MVVM architectural pattern.
 * Manages UI state, data binding, and user interactions for the triple-ring display.
 * 
 * Integrates with Story 1.3 goal calculation service and provides real-time updates.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardUseCase: DashboardUseCase,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _dashboardState = MutableStateFlow(DashboardState.loading())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()
    
    private val _animationTrigger = MutableStateFlow<AnimationEvent?>(null)
    val animationTrigger: StateFlow<AnimationEvent?> = _animationTrigger.asStateFlow()
    
    private var dataUpdateJob: Job? = null
    
    /**
     * Starts real-time dashboard data updates using Flow and coroutines.
     * Integrates with Story 1.3 GoalRepository and ActivityRepository.
     */
    fun startDashboardUpdates() {
        dataUpdateJob?.cancel()
        dataUpdateJob = viewModelScope.launch {
            userRepository.getCurrentUserId()?.let { userId ->
                dashboardUseCase.getDashboardData(userId)
                    .flowOn(Dispatchers.IO)
                    .collect { newState ->
                        val previousState = _dashboardState.value
                        _dashboardState.value = newState
                        
                        // Trigger animations for progress changes
                        if (previousState.loadingState == LoadingState.LOADED && 
                            newState.loadingState == LoadingState.LOADED) {
                            checkForProgressChanges(previousState, newState)
                        }
                    }
            }
        }
    }
    
    /**
     * Stops dashboard updates when activity is destroyed.
     */
    fun stopDashboardUpdates() {
        dataUpdateJob?.cancel()
        dataUpdateJob = null
    }
    
    /**
     * Manually refreshes dashboard data.
     */
    fun refreshDashboard() {
        startDashboardUpdates()
    }
    
    /**
     * Detects progress changes and triggers appropriate animations.
     */
    private fun checkForProgressChanges(previous: DashboardState, current: DashboardState) {
        // Detect significant progress changes
        val progressChanges = detectProgressChanges(previous.progress, current.progress)
        if (progressChanges.isNotEmpty()) {
            _animationTrigger.value = AnimationEvent.ProgressUpdate(progressChanges)
        }
        
        // Check for goal achievements
        val newAchievements = detectNewAchievements(previous.progress, current.progress)
        if (newAchievements.isNotEmpty()) {
            _animationTrigger.value = AnimationEvent.GoalAchieved(newAchievements)
        }
    }
    
    private fun detectProgressChanges(previous: DailyProgress, current: DailyProgress): List<ProgressChange> {
        val changes = mutableListOf<ProgressChange>()
        
        if (previous.stepsProgress.percentage != current.stepsProgress.percentage) {
            changes.add(ProgressChange(RingType.STEPS, previous.stepsProgress.percentage, current.stepsProgress.percentage))
        }
        
        if (previous.caloriesProgress.percentage != current.caloriesProgress.percentage) {
            changes.add(ProgressChange(RingType.CALORIES, previous.caloriesProgress.percentage, current.caloriesProgress.percentage))
        }
        
        if (previous.heartPointsProgress.percentage != current.heartPointsProgress.percentage) {
            changes.add(ProgressChange(RingType.HEART_POINTS, previous.heartPointsProgress.percentage, current.heartPointsProgress.percentage))
        }
        
        return changes
    }
    
    private fun detectNewAchievements(previous: DailyProgress, current: DailyProgress): List<RingType> {
        val achievements = mutableListOf<RingType>()
        
        if (!previous.stepsProgress.isGoalAchieved && current.stepsProgress.isGoalAchieved) {
            achievements.add(RingType.STEPS)
        }
        
        if (!previous.caloriesProgress.isGoalAchieved && current.caloriesProgress.isGoalAchieved) {
            achievements.add(RingType.CALORIES)
        }
        
        if (!previous.heartPointsProgress.isGoalAchieved && current.heartPointsProgress.isGoalAchieved) {
            achievements.add(RingType.HEART_POINTS)
        }
        
        return achievements
    }
    
    override fun onCleared() {
        super.onCleared()
        stopDashboardUpdates()
    }
}