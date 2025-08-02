package com.vibehealth.android.ui.goals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehealth.android.data.goals.GoalRepository
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.goals.*
import com.vibehealth.android.domain.user.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * ViewModel for displaying and managing daily goals.
 * 
 * Handles goal presentation, unit conversion, WHO source information,
 * and goal refresh operations with proper state management.
 */
@HiltViewModel
class GoalDisplayViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val goalCalculationUseCase: GoalCalculationUseCase
) : ViewModel() {

    // Goal display state
    private val _goalDisplayState = MutableLiveData<GoalDisplayState>()
    val goalDisplayState: LiveData<GoalDisplayState> = _goalDisplayState

    // Current goals
    private val _currentGoals = MutableLiveData<DailyGoals?>()
    val currentGoals: LiveData<DailyGoals?> = _currentGoals

    // Formatted goals for display
    private val _formattedGoals = MutableLiveData<FormattedGoals?>()
    val formattedGoals: LiveData<FormattedGoals?> = _formattedGoals

    // Goal explanation state
    private val _explanationState = MutableLiveData<GoalExplanationState>()
    val explanationState: LiveData<GoalExplanationState> = _explanationState

    // Refresh state
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    // Error state
    private val _errorState = MutableLiveData<GoalDisplayError?>()
    val errorState: LiveData<GoalDisplayError?> = _errorState

    // Current unit system
    private val _unitSystem = MutableLiveData<UnitSystem>()
    val unitSystem: LiveData<UnitSystem> = _unitSystem

    // Jobs for cancellation
    private var goalsObservationJob: Job? = null
    private var refreshJob: Job? = null
    private var explanationJob: Job? = null

    /**
     * Start observing goals for a user.
     * 
     * @param userId User ID to observe goals for
     * @param unitSystem Unit system for display formatting
     */
    fun startObservingGoals(userId: String, unitSystem: UnitSystem = UnitSystem.METRIC) {
        _unitSystem.value = unitSystem
        
        goalsObservationJob?.cancel()
        goalsObservationJob = viewModelScope.launch {
            try {
                _goalDisplayState.value = GoalDisplayState.Loading
                _errorState.value = null

                goalRepository.getCurrentGoals(userId).collectLatest { goals ->
                    if (goals != null) {
                        _currentGoals.value = goals
                        _formattedGoals.value = formatGoalsForDisplay(goals, unitSystem)
                        _goalDisplayState.value = GoalDisplayState.Loaded(goals)
                    } else {
                        _goalDisplayState.value = GoalDisplayState.NoGoals
                    }
                }

            } catch (e: Exception) {
                _goalDisplayState.value = GoalDisplayState.Error("Failed to load goals")
                _errorState.value = GoalDisplayError.LoadFailed(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Refresh goals by triggering recalculation.
     * 
     * @param userId User ID to refresh goals for
     */
    fun refreshGoals(userId: String) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _errorState.value = null

                val result = goalCalculationUseCase.calculateAndStoreGoals(userId, forceRecalculation = true)

                when (result) {
                    is GoalCalculationResult.Success -> {
                        _currentGoals.value = result.goals
                        _formattedGoals.value = formatGoalsForDisplay(result.goals, _unitSystem.value ?: UnitSystem.METRIC)
                        _goalDisplayState.value = GoalDisplayState.Loaded(result.goals)
                    }
                    
                    is GoalCalculationResult.Error -> {
                        _errorState.value = GoalDisplayError.RefreshFailed(
                            getUserFriendlyErrorMessage(result.error)
                        )
                    }
                }

            } catch (e: Exception) {
                _errorState.value = GoalDisplayError.RefreshFailed(e.message ?: "Unknown error")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Load goal explanation and WHO source information.
     * 
     * @param userId User ID to get explanation for
     */
    fun loadGoalExplanation(userId: String) {
        explanationJob?.cancel()
        explanationJob = viewModelScope.launch {
            try {
                _explanationState.value = GoalExplanationState.Loading

                val breakdown = goalCalculationUseCase.getCalculationBreakdown(userId)
                
                if (breakdown != null) {
                    _explanationState.value = GoalExplanationState.Loaded(
                        breakdown = breakdown,
                        whoSources = getWHOSourceInformation(),
                        calculationMethodology = getCalculationMethodology(breakdown)
                    )
                } else {
                    _explanationState.value = GoalExplanationState.NotAvailable(
                        "Goal explanation is not available at this time"
                    )
                }

            } catch (e: Exception) {
                _explanationState.value = GoalExplanationState.Error(
                    "Failed to load goal explanation: ${e.message}"
                )
            }
        }
    }

    /**
     * Update unit system and reformat goals.
     * 
     * @param newUnitSystem New unit system for display
     */
    fun updateUnitSystem(newUnitSystem: UnitSystem) {
        _unitSystem.value = newUnitSystem
        
        val currentGoals = _currentGoals.value
        if (currentGoals != null) {
            _formattedGoals.value = formatGoalsForDisplay(currentGoals, newUnitSystem)
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _errorState.value = null
    }

    /**
     * Dismiss goal explanation.
     */
    fun dismissExplanation() {
        _explanationState.value = GoalExplanationState.Dismissed
    }

    // Private helper methods

    /**
     * Format goals for display with appropriate units and formatting.
     */
    private fun formatGoalsForDisplay(goals: DailyGoals, unitSystem: UnitSystem): FormattedGoals {
        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        
        return FormattedGoals(
            stepsGoal = formatStepsGoal(goals.stepsGoal),
            caloriesGoal = formatCaloriesGoal(goals.caloriesGoal),
            heartPointsGoal = formatHeartPointsGoal(goals.heartPointsGoal),
            calculatedAt = goals.calculatedAt.format(dateFormatter),
            calculationSource = formatCalculationSource(goals.calculationSource),
            sourceDescription = getSourceDescription(goals.calculationSource),
            lastUpdated = "Updated ${goals.calculatedAt.format(dateFormatter)}",
            isValid = goals.isValid,
            isFresh = goals.isFresh
        )
    }

    /**
     * Format steps goal for display.
     */
    private fun formatStepsGoal(steps: Int): String {
        return when {
            steps >= 10000 -> "${String.format("%,d", steps)} steps"
            steps >= 1000 -> "${String.format("%.1f", steps / 1000.0)}K steps"
            else -> "$steps steps"
        }
    }

    /**
     * Format calories goal for display.
     */
    private fun formatCaloriesGoal(calories: Int): String {
        return "${String.format("%,d", calories)} calories"
    }

    /**
     * Format heart points goal for display.
     */
    private fun formatHeartPointsGoal(heartPoints: Int): String {
        return "$heartPoints heart points"
    }

    /**
     * Format calculation source for display.
     */
    private fun formatCalculationSource(source: CalculationSource): String {
        return when (source) {
            CalculationSource.DEFAULT -> "Default Goals"
            CalculationSource.PERSONALIZED -> "Personalized Goals"
            CalculationSource.MANUAL -> "Manual Goals"
            CalculationSource.WHO_STANDARD -> "WHO Standards"
            CalculationSource.FALLBACK_DEFAULT -> "Health Guidelines"
            CalculationSource.USER_ADJUSTED -> "Personalized"
        }
    }

    /**
     * Get description for calculation source.
     */
    private fun getSourceDescription(source: CalculationSource): String {
        return when (source) {
            CalculationSource.DEFAULT -> 
                "Standard wellness goals based on general health recommendations"
            CalculationSource.PERSONALIZED -> 
                "Goals calculated specifically for your profile and activity level"
            CalculationSource.MANUAL -> 
                "Goals you've set manually based on your personal preferences"
            CalculationSource.WHO_STANDARD -> 
                "Calculated based on World Health Organization guidelines and your personal profile"
            CalculationSource.FALLBACK_DEFAULT -> 
                "Based on general health recommendations for wellness benefits"
            CalculationSource.USER_ADJUSTED -> 
                "Customized goals adjusted to your preferences"
        }
    }

    /**
     * Get WHO source information for transparency.
     */
    private fun getWHOSourceInformation(): WHOSourceInfo {
        return WHOSourceInfo(
            stepsSource = "WHO Physical Activity Guidelines 2020 - 10,000 steps baseline with age adjustments",
            caloriesSource = "Harris-Benedict Revised equation (1984) with WHO activity factors",
            heartPointsSource = "WHO 150 minutes/week moderate activity converted to daily heart points",
            whoWebsite = "https://www.who.int/news-room/fact-sheets/detail/physical-activity",
            lastUpdated = "WHO Guidelines 2020"
        )
    }

    /**
     * Get calculation methodology explanation.
     */
    private fun getCalculationMethodology(breakdown: GoalCalculationBreakdown): CalculationMethodology {
        return CalculationMethodology(
            stepsMethodology = "Base goal of 10,000 steps adjusted for age (${breakdown.userAge} years) and gender",
            caloriesMethodology = "BMR calculated using Harris-Benedict equation, multiplied by activity level (${breakdown.activityLevel.description})",
            heartPointsMethodology = "WHO 150 minutes/week moderate activity (≈21 points/day) adjusted for age and activity level",
            personalFactors = listOf(
                "Age: ${breakdown.userAge} years",
                "Gender: ${breakdown.userGender.getDisplayName()}",
                "Activity Level: ${breakdown.activityLevel.description}"
            )
        )
    }

    /**
     * Get user-friendly error message.
     */
    private fun getUserFriendlyErrorMessage(error: GoalCalculationError): String {
        return when (error) {
            is GoalCalculationError.ProfileNotFound -> 
                "We couldn't find your profile. Please check your profile is complete."
            is GoalCalculationError.CalculationFailed -> 
                "We're having trouble calculating your goals. Please try again."
            is GoalCalculationError.ValidationFailed -> 
                "Your profile information seems unusual. Please verify your details."
            is GoalCalculationError.StorageFailed -> 
                "We calculated your goals but couldn't save them. Please try again."
            is GoalCalculationError.UnexpectedError -> 
                "Something unexpected happened. Please try again."
        }
    }

    override fun onCleared() {
        super.onCleared()
        goalsObservationJob?.cancel()
        refreshJob?.cancel()
        explanationJob?.cancel()
    }
}

/**
 * State of goal display.
 */
sealed class GoalDisplayState {
    object Loading : GoalDisplayState()
    object NoGoals : GoalDisplayState()
    data class Loaded(val goals: DailyGoals) : GoalDisplayState()
    data class Error(val message: String) : GoalDisplayState()
}

/**
 * Formatted goals for display.
 */
data class FormattedGoals(
    val stepsGoal: String,
    val caloriesGoal: String,
    val heartPointsGoal: String,
    val calculatedAt: String,
    val calculationSource: String,
    val sourceDescription: String,
    val lastUpdated: String,
    val isValid: Boolean,
    val isFresh: Boolean
)

/**
 * State of goal explanation.
 */
sealed class GoalExplanationState {
    object NotRequested : GoalExplanationState()
    object Loading : GoalExplanationState()
    data class Loaded(
        val breakdown: GoalCalculationBreakdown,
        val whoSources: WHOSourceInfo,
        val calculationMethodology: CalculationMethodology
    ) : GoalExplanationState()
    data class NotAvailable(val reason: String) : GoalExplanationState()
    data class Error(val message: String) : GoalExplanationState()
    object Dismissed : GoalExplanationState()
}

/**
 * WHO source information for transparency.
 */
data class WHOSourceInfo(
    val stepsSource: String,
    val caloriesSource: String,
    val heartPointsSource: String,
    val whoWebsite: String,
    val lastUpdated: String
)

/**
 * Calculation methodology explanation.
 */
data class CalculationMethodology(
    val stepsMethodology: String,
    val caloriesMethodology: String,
    val heartPointsMethodology: String,
    val personalFactors: List<String>
)

/**
 * Goal display errors.
 */
sealed class GoalDisplayError {
    data class LoadFailed(val message: String) : GoalDisplayError()
    data class RefreshFailed(val message: String) : GoalDisplayError()
    data class ExplanationFailed(val message: String) : GoalDisplayError()
}