package com.vibehealth.android.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.domain.goals.*
import com.vibehealth.android.domain.user.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for profile management with integrated goal calculation.
 * 
 * Manages profile data, goal calculation state, and provides user-friendly
 * error handling for goal calculation operations.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val goalCalculationUseCase: GoalCalculationUseCase,
    private val profileUpdateUseCase: ProfileUpdateUseCase
) : ViewModel() {

    // Profile state
    private val _profileState = MutableLiveData<ProfileState>()
    val profileState: LiveData<ProfileState> = _profileState

    // Goal calculation state
    private val _goalCalculationState = MutableLiveData<GoalCalculationState>()
    val goalCalculationState: LiveData<GoalCalculationState> = _goalCalculationState

    // Current goals
    private val _currentGoals = MutableLiveData<DailyGoals?>()
    val currentGoals: LiveData<DailyGoals?> = _currentGoals

    // Error state
    private val _errorState = MutableLiveData<ProfileErrorState?>()
    val errorState: LiveData<ProfileErrorState?> = _errorState

    // Loading states
    private val _isProfileLoading = MutableLiveData<Boolean>()
    val isProfileLoading: LiveData<Boolean> = _isProfileLoading

    private val _isGoalCalculationLoading = MutableLiveData<Boolean>()
    val isGoalCalculationLoading: LiveData<Boolean> = _isGoalCalculationLoading

    // Current jobs for cancellation
    private var profileLoadJob: Job? = null
    private var goalCalculationJob: Job? = null
    private var profileUpdateJob: Job? = null

    /**
     * Load user profile and associated goals.
     * 
     * @param userId User ID to load profile for
     */
    fun loadProfile(userId: String) {
        profileLoadJob?.cancel()
        profileLoadJob = viewModelScope.launch {
            try {
                _isProfileLoading.value = true
                _errorState.value = null

                // Load profile
                val profileResult = userProfileRepository.getUserProfile(userId)
                if (profileResult.isFailure) {
                    _profileState.value = ProfileState.Error("Failed to load profile")
                    _errorState.value = ProfileErrorState.ProfileLoadFailed(
                        profileResult.exceptionOrNull()?.message ?: "Unknown error"
                    )
                    return@launch
                }

                val profile = profileResult.getOrNull()
                if (profile == null) {
                    _profileState.value = ProfileState.NotFound
                    return@launch
                }

                _profileState.value = ProfileState.Loaded(profile)

                // Load current goals
                loadCurrentGoals(userId)

            } catch (e: Exception) {
                _profileState.value = ProfileState.Error("Unexpected error: ${e.message}")
                _errorState.value = ProfileErrorState.UnexpectedError(e.message ?: "Unknown error")
            } finally {
                _isProfileLoading.value = false
            }
        }
    }

    /**
     * Update user profile and trigger goal recalculation if needed.
     * 
     * @param updatedProfile Updated profile data
     */
    fun updateProfile(updatedProfile: UserProfile) {
        profileUpdateJob?.cancel()
        profileUpdateJob = viewModelScope.launch {
            try {
                _isProfileLoading.value = true
                _errorState.value = null

                val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)

                when (result) {
                    is ProfileUpdateResult.Success -> {
                        _profileState.value = ProfileState.Loaded(result.updatedProfile)
                        
                        // Update goal calculation state based on result
                        result.goalRecalculationResult?.let { goalResult ->
                            when (goalResult) {
                                is GoalCalculationResult.Success -> {
                                    _currentGoals.value = goalResult.goals
                                    _goalCalculationState.value = GoalCalculationState.Success(
                                        goals = goalResult.goals,
                                        wasRecalculated = goalResult.wasRecalculated,
                                        reason = result.changesSummary.reason
                                    )
                                }
                                is GoalCalculationResult.Error -> {
                                    _goalCalculationState.value = GoalCalculationState.Error(
                                        goalResult.message,
                                        canRetry = true
                                    )
                                }
                            }
                        } ?: run {
                            // No goal recalculation was needed
                            _goalCalculationState.value = GoalCalculationState.NoRecalculationNeeded(
                                result.changesSummary.reason
                            )
                        }
                    }
                    
                    is ProfileUpdateResult.Error -> {
                        _errorState.value = when (result.error) {
                            is ProfileUpdateError.ConcurrentUpdate -> 
                                ProfileErrorState.ConcurrentUpdate("Another update is in progress")
                            is ProfileUpdateError.ProfileUpdateFailed -> 
                                ProfileErrorState.ProfileUpdateFailed(result.message)
                            is ProfileUpdateError.ProfileNotFound -> 
                                ProfileErrorState.ProfileNotFound("Profile not found")
                            is ProfileUpdateError.UnexpectedError -> 
                                ProfileErrorState.UnexpectedError(result.message)
                        }
                    }
                }

            } catch (e: Exception) {
                _errorState.value = ProfileErrorState.UnexpectedError(e.message ?: "Unknown error")
            } finally {
                _isProfileLoading.value = false
            }
        }
    }

    /**
     * Trigger manual goal recalculation.
     * 
     * @param userId User ID to recalculate goals for
     */
    fun recalculateGoals(userId: String) {
        goalCalculationJob?.cancel()
        goalCalculationJob = viewModelScope.launch {
            try {
                _isGoalCalculationLoading.value = true
                _goalCalculationState.value = GoalCalculationState.Loading("Recalculating your goals...")
                _errorState.value = null

                val result = goalCalculationUseCase.calculateAndStoreGoals(userId, forceRecalculation = true)

                when (result) {
                    is GoalCalculationResult.Success -> {
                        _currentGoals.value = result.goals
                        _goalCalculationState.value = GoalCalculationState.Success(
                            goals = result.goals,
                            wasRecalculated = result.wasRecalculated,
                            reason = "Manual recalculation requested"
                        )
                    }
                    
                    is GoalCalculationResult.Error -> {
                        _goalCalculationState.value = GoalCalculationState.Error(
                            getUserFriendlyErrorMessage(result.error),
                            canRetry = true
                        )
                        _errorState.value = ProfileErrorState.GoalCalculationFailed(result.message)
                    }
                }

            } catch (e: Exception) {
                _goalCalculationState.value = GoalCalculationState.Error(
                    "Unexpected error during goal calculation",
                    canRetry = true
                )
                _errorState.value = ProfileErrorState.UnexpectedError(e.message ?: "Unknown error")
            } finally {
                _isGoalCalculationLoading.value = false
            }
        }
    }

    /**
     * Load current goals for a user.
     * 
     * @param userId User ID to load goals for
     */
    private fun loadCurrentGoals(userId: String) {
        viewModelScope.launch {
            try {
                val hasValidGoals = goalCalculationUseCase.hasValidGoals(userId)
                
                if (hasValidGoals) {
                    // Goals exist and are valid, trigger a load (this would typically come from repository)
                    _goalCalculationState.value = GoalCalculationState.Loaded("Goals are up to date")
                } else {
                    // No valid goals, suggest recalculation
                    _goalCalculationState.value = GoalCalculationState.NeedsRecalculation(
                        "Your goals need to be calculated based on your current profile"
                    )
                }

            } catch (e: Exception) {
                _goalCalculationState.value = GoalCalculationState.Error(
                    "Failed to check goal status",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _errorState.value = null
    }

    /**
     * Retry failed operation based on current state.
     */
    fun retryFailedOperation() {
        val currentProfile = (_profileState.value as? ProfileState.Loaded)?.profile
        if (currentProfile != null) {
            when (_goalCalculationState.value) {
                is GoalCalculationState.Error -> {
                    recalculateGoals(currentProfile.userId)
                }
                is GoalCalculationState.NeedsRecalculation -> {
                    recalculateGoals(currentProfile.userId)
                }
                else -> {
                    // Reload profile
                    loadProfile(currentProfile.userId)
                }
            }
        }
    }

    /**
     * Get user-friendly error message for goal calculation errors.
     */
    private fun getUserFriendlyErrorMessage(error: GoalCalculationError): String {
        return when (error) {
            is GoalCalculationError.ProfileNotFound -> 
                "We couldn't find your profile information. Please check your profile is complete."
            is GoalCalculationError.CalculationFailed -> 
                "We're having trouble calculating your goals right now. Please try again in a moment."
            is GoalCalculationError.ValidationFailed -> 
                "Your profile information seems unusual. Please check your height, weight, and age are correct."
            is GoalCalculationError.StorageFailed -> 
                "We calculated your goals but couldn't save them. Please try again."
            is GoalCalculationError.UnexpectedError -> 
                "Something unexpected happened. Please try again or contact support if the problem persists."
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileLoadJob?.cancel()
        goalCalculationJob?.cancel()
        profileUpdateJob?.cancel()
    }
}

/**
 * State of the user profile.
 */
sealed class ProfileState {
    object Loading : ProfileState()
    object NotFound : ProfileState()
    data class Loaded(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

/**
 * State of goal calculation operations.
 */
sealed class GoalCalculationState {
    data class Loading(val message: String) : GoalCalculationState()
    data class Loaded(val message: String) : GoalCalculationState()
    data class Success(
        val goals: DailyGoals,
        val wasRecalculated: Boolean,
        val reason: String
    ) : GoalCalculationState()
    data class Error(val message: String, val canRetry: Boolean) : GoalCalculationState()
    data class NeedsRecalculation(val reason: String) : GoalCalculationState()
    data class NoRecalculationNeeded(val reason: String) : GoalCalculationState()
}

/**
 * Error states for profile operations.
 */
sealed class ProfileErrorState {
    data class ProfileLoadFailed(val message: String) : ProfileErrorState()
    data class ProfileUpdateFailed(val message: String) : ProfileErrorState()
    data class ProfileNotFound(val message: String) : ProfileErrorState()
    data class GoalCalculationFailed(val message: String) : ProfileErrorState()
    data class ConcurrentUpdate(val message: String) : ProfileErrorState()
    data class UnexpectedError(val message: String) : ProfileErrorState()
}