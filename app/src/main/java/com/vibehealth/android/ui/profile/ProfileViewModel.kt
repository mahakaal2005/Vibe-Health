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

    // Current goals with caching
    private val _currentGoals = MutableLiveData<DailyGoals?>()
    val currentGoals: LiveData<DailyGoals?> = _currentGoals
    
    // Goal cache to avoid unnecessary recalculations
    private var cachedGoals: DailyGoals? = null
    private var cachedGoalsUserId: String? = null
    private var cachedGoalsProfileHash: String? = null

    // Error state
    private val _errorState = MutableLiveData<ProfileErrorState?>()
    val errorState: LiveData<ProfileErrorState?> = _errorState

    // Loading states
    private val _isProfileLoading = MutableLiveData<Boolean>()
    val isProfileLoading: LiveData<Boolean> = _isProfileLoading

    private val _isGoalCalculationLoading = MutableLiveData<Boolean>()
    val isGoalCalculationLoading: LiveData<Boolean> = _isGoalCalculationLoading

    // Profile update state for edit functionality
    private val _profileUpdateState = MutableLiveData<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: LiveData<ProfileUpdateState> = _profileUpdateState

    // User profile flow for edit functionality
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

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
        android.util.Log.d("ProfileViewModel", "🔄 loadProfile called for: $userId")
        profileLoadJob?.cancel()
        profileLoadJob = viewModelScope.launch {
            try {
                _isProfileLoading.value = true
                _errorState.value = null

                // Load profile
                android.util.Log.d("ProfileViewModel", "📞 Calling repository.getUserProfile")
                val profileResult = userProfileRepository.getUserProfile(userId)
                android.util.Log.d("ProfileViewModel", "📋 Repository result success: ${profileResult.isSuccess}")
                if (profileResult.isFailure) {
                    val error = profileResult.exceptionOrNull()?.message ?: "Unknown error"
                    android.util.Log.e("ProfileViewModel", "❌ Profile load failed: $error")
                    _profileState.value = ProfileState.Error("Failed to load profile")
                    _errorState.value = ProfileErrorState.ProfileLoadFailed(error)
                    return@launch
                }

                val profile = profileResult.getOrNull()
                android.util.Log.d("ProfileViewModel", "👤 Profile data: $profile")
                if (profile == null) {
                    android.util.Log.w("ProfileViewModel", "⚠️ Profile is null - user needs onboarding")
                    _profileState.value = ProfileState.NotFound
                    return@launch
                }

                _profileState.value = ProfileState.Loaded(profile)
                _userProfile.value = profile

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
    fun updateUserProfile(updatedProfile: UserProfile) {
        profileUpdateJob?.cancel()
        profileUpdateJob = viewModelScope.launch {
            try {
                _profileUpdateState.value = ProfileUpdateState.Loading
                _errorState.value = null

                val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)

                when (result) {
                    is ProfileUpdateResult.Success -> {
                        _profileState.value = ProfileState.Loaded(result.updatedProfile)
                        _profileUpdateState.value = ProfileUpdateState.Success(result.updatedProfile)
                        
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
                        _profileUpdateState.value = ProfileUpdateState.Error(result.message)
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
                _profileUpdateState.value = ProfileUpdateState.Error("Unexpected error: ${e.message}")
                _errorState.value = ProfileErrorState.UnexpectedError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Load user profile for editing.
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                // Get current profile from state
                val currentState = _profileState.value
                if (currentState is ProfileState.Loaded) {
                    _userProfile.value = currentState.profile
                }
            } catch (e: Exception) {
                _errorState.value = ProfileErrorState.UnexpectedError(e.message ?: "Unknown error")
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
                        // Update cache with new goals
                        val currentProfile = (_profileState.value as? ProfileState.Loaded)?.profile
                        cachedGoals = result.goals
                        cachedGoalsUserId = userId
                        cachedGoalsProfileHash = currentProfile?.let { generateProfileHash(it) }
                        
                        _currentGoals.value = result.goals
                        _goalCalculationState.value = GoalCalculationState.Success(
                            goals = result.goals,
                            wasRecalculated = result.wasRecalculated,
                            reason = "Manual recalculation requested"
                        )
                        android.util.Log.d("ProfileViewModel", "✅ Goals recalculated and cached")
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
     * Load current goals for a user with smart caching.
     * 
     * @param userId User ID to load goals for
     */
    private fun loadCurrentGoals(userId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ProfileViewModel", "🎯 Loading goals for user: $userId")
                
                // Get current profile for cache validation
                val currentProfile = (_profileState.value as? ProfileState.Loaded)?.profile
                val profileHash = currentProfile?.let { generateProfileHash(it) }
                
                // Check if we have valid cached goals
                if (cachedGoals != null && 
                    cachedGoalsUserId == userId && 
                    cachedGoalsProfileHash == profileHash) {
                    android.util.Log.d("ProfileViewModel", "✅ Using cached goals")
                    _currentGoals.value = cachedGoals
                    _goalCalculationState.value = GoalCalculationState.Success(
                        goals = cachedGoals!!,
                        wasRecalculated = false,
                        reason = "Using cached goals"
                    )
                    return@launch
                }
                
                android.util.Log.d("ProfileViewModel", "🔄 Checking for valid goals in storage")
                val hasValidGoals = goalCalculationUseCase.hasValidGoals(userId)
                
                if (hasValidGoals) {
                    android.util.Log.d("ProfileViewModel", "📋 Valid goals found, loading from storage")
                    // Try to load existing goals
                    val result = goalCalculationUseCase.calculateAndStoreGoals(userId, forceRecalculation = false)
                    when (result) {
                        is GoalCalculationResult.Success -> {
                            // Cache the loaded goals
                            cachedGoals = result.goals
                            cachedGoalsUserId = userId
                            cachedGoalsProfileHash = profileHash
                            
                            _currentGoals.value = result.goals
                            _goalCalculationState.value = GoalCalculationState.Success(
                                goals = result.goals,
                                wasRecalculated = false,
                                reason = "Loaded existing goals"
                            )
                            android.util.Log.d("ProfileViewModel", "✅ Goals loaded and cached")
                        }
                        is GoalCalculationResult.Error -> {
                            android.util.Log.w("ProfileViewModel", "⚠️ Failed to load goals: ${result.message}")
                            _goalCalculationState.value = GoalCalculationState.NeedsRecalculation(
                                "Your goals need to be calculated based on your current profile"
                            )
                        }
                    }
                } else {
                    android.util.Log.d("ProfileViewModel", "🆕 No valid goals found, need calculation")
                    _goalCalculationState.value = GoalCalculationState.NeedsRecalculation(
                        "Your goals need to be calculated based on your current profile"
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "❌ Error loading goals", e)
                _goalCalculationState.value = GoalCalculationState.Error(
                    "Failed to check goal status",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Generate a hash of profile data that affects goal calculation.
     * This is used to determine if goals need to be recalculated.
     */
    private fun generateProfileHash(profile: UserProfile): String {
        return "${profile.birthday?.time}_${profile.gender}_${profile.heightInCm}_${profile.weightInKg}"
    }
    
    /**
     * Clear goal cache when user logs out or profile changes significantly.
     */
    fun clearGoalCache() {
        android.util.Log.d("ProfileViewModel", "🗑️ Clearing goal cache")
        cachedGoals = null
        cachedGoalsUserId = null
        cachedGoalsProfileHash = null
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
        clearGoalCache()
    }

    /**
     * State of profile update operations for editing functionality.
     */
    sealed class ProfileUpdateState {
        object Idle : ProfileUpdateState()
        object Loading : ProfileUpdateState()
        data class Success(val updatedProfile: UserProfile) : ProfileUpdateState()
        data class Error(val message: String) : ProfileUpdateState()
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

