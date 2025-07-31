package com.vibehealth.android.ui.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehealth.android.core.utils.UnitConversionUtils
import com.vibehealth.android.core.utils.ConversionResult
import com.vibehealth.android.core.validation.OnboardingValidationHelper
import com.vibehealth.android.core.validation.MetricConversionResult
import com.vibehealth.android.domain.onboarding.ValidationField
import com.vibehealth.android.domain.onboarding.OnboardingResult
import com.vibehealth.android.domain.onboarding.OnboardingState
import com.vibehealth.android.domain.onboarding.OnboardingStep
import com.vibehealth.android.domain.onboarding.ValidationErrors
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for onboarding flow with reactive state management and error recovery
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val validationHelper: OnboardingValidationHelper,
    private val integrationManager: com.vibehealth.android.core.integration.OnboardingIntegrationManager,
    private val progressManager: com.vibehealth.android.core.persistence.OnboardingProgressManager,
    private val performanceOptimizer: com.vibehealth.android.core.performance.OnboardingPerformanceOptimizer,
    private val autoSaveManager: com.vibehealth.android.core.autosave.OnboardingAutoSaveManager,
    private val goalCalculationUseCase: com.vibehealth.android.domain.goals.GoalCalculationUseCase
) : ViewModel() {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    // Get optimized debounce delay from performance optimizer
    private val debounceDelayMs: Long
        get() = performanceOptimizer.getOptimizedDebounceDelay()

    // Reactive state management
    private val _onboardingState = MutableLiveData<OnboardingState>()
    val onboardingState: LiveData<OnboardingState> = _onboardingState

    private val _currentStep = MutableLiveData<OnboardingStep>()
    val currentStep: LiveData<OnboardingStep> = _currentStep

    private val _validationErrors = MutableLiveData<ValidationErrors>()
    val validationErrors: LiveData<ValidationErrors> = _validationErrors

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _progressPercentage = MutableLiveData<Float>()
    val progressPercentage: LiveData<Float> = _progressPercentage

    // User data
    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    private val _unitSystem = MutableLiveData<UnitSystem>()
    val unitSystem: LiveData<UnitSystem> = _unitSystem

    // Navigation events
    private val _navigationEvent = MutableLiveData<OnboardingNavigationEvent>()
    val navigationEvent: LiveData<OnboardingNavigationEvent> = _navigationEvent

    // Error recovery
    private val _errorRecoveryEvent = MutableLiveData<ErrorRecoveryEvent>()
    val errorRecoveryEvent: LiveData<ErrorRecoveryEvent> = _errorRecoveryEvent

    // Goal calculation state
    private val _goalCalculationState = MutableLiveData<OnboardingGoalCalculationState>()
    val goalCalculationState: LiveData<OnboardingGoalCalculationState> = _goalCalculationState

    // Debouncing for validation
    private var validationJob: Job? = null
    private var goalCalculationJob: Job? = null
    private var retryAttempts = 0

    // State persistence for process death recovery
    private var savedState: OnboardingSavedState? = null

    init {
        initializeOnboarding()
        restoreProgressIfAvailable()
    }

    /**
     * Initialize onboarding flow
     */
    private fun initializeOnboarding() {
        _currentStep.value = OnboardingStep.WELCOME
        _onboardingState.value = OnboardingState.PersonalInfo
        _unitSystem.value = UnitSystem.METRIC
        _validationErrors.value = ValidationErrors()
        _progressPercentage.value = OnboardingStep.WELCOME.getProgressPercentage()
        
        // Initialize empty user profile
        _userProfile.value = UserProfile()
    }

    /**
     * Update personal information with debounced validation
     */
    fun updatePersonalInfo(name: String, birthday: Date?) {
        val currentProfile = _userProfile.value ?: UserProfile()
        val updatedProfile = currentProfile.copy(
            displayName = name,
            birthday = birthday,
            updatedAt = Date()
        )
        _userProfile.value = updatedProfile

        // Auto-save the data
        autoSaveManager.autoSaveBirthday(birthday)
        
        // Debounced validation
        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            delay(debounceDelayMs)
            validatePersonalInfo(name, birthday)
        }
    }

    /**
     * Update physical information with unit conversion
     */
    fun updatePhysicalInfo(
        height: String,
        weight: String,
        gender: Gender?,
        unitSystem: UnitSystem
    ) {
        _unitSystem.value = unitSystem
        
        // Validate and parse input
        val validationResult = validationHelper.validateCompleteOnboardingData(
            name = _userProfile.value?.displayName ?: "",
            birthday = _userProfile.value?.birthday,
            height = height,
            weight = weight,
            gender = gender,
            unitSystem = unitSystem
        )

        _validationErrors.value = validationResult.errors

        if (validationResult.isValid && validationResult.parsedHeight != null && validationResult.parsedWeight != null) {
            // Convert to metric for storage
            val conversionResult = validationHelper.convertToMetricForStorage(
                validationResult.parsedHeight,
                validationResult.parsedWeight,
                unitSystem
            )

            when (conversionResult) {
                is MetricConversionResult.Success -> {
                    val currentProfile = _userProfile.value ?: UserProfile()
                    val updatedProfile = currentProfile.copy(
                        gender = gender ?: Gender.PREFER_NOT_TO_SAY,
                        unitSystem = unitSystem,
                        heightInCm = conversionResult.heightInCm,
                        weightInKg = conversionResult.weightInKg,
                        updatedAt = Date()
                    )
                    _userProfile.value = updatedProfile
                }
                is MetricConversionResult.Error -> {
                    val currentErrors = _validationErrors.value ?: ValidationErrors()
                    _validationErrors.value = currentErrors.copy(
                        heightError = "Conversion error: ${conversionResult.message}"
                    )
                }
            }
        }
    }

    /**
     * Navigate to next step with validation
     */
    fun navigateToNextStep() {
        val currentStep = _currentStep.value ?: OnboardingStep.WELCOME
        
        if (validateCurrentStep()) {
            val nextStep = currentStep.getNextStep()
            if (nextStep != null) {
                _currentStep.value = nextStep
                _progressPercentage.value = nextStep.getProgressPercentage()
                updateOnboardingState(nextStep)
                _navigationEvent.value = OnboardingNavigationEvent.NavigateForward(nextStep)
                
                // Prefetch resources for next step
                performanceOptimizer.prefetchNextStepResources(nextStep)
                
                // Save progress
                viewModelScope.launch {
                    progressManager.updateCurrentStep(nextStep)
                }
            } else {
                completeOnboarding()
            }
        }
    }

    /**
     * Navigate to previous step
     */
    fun navigateToPreviousStep() {
        val currentStep = _currentStep.value ?: OnboardingStep.WELCOME
        val previousStep = currentStep.getPreviousStep()
        
        if (previousStep != null) {
            _currentStep.value = previousStep
            _progressPercentage.value = previousStep.getProgressPercentage()
            updateOnboardingState(previousStep)
            _navigationEvent.value = OnboardingNavigationEvent.NavigateBackward(previousStep)
        }
    }

    /**
     * Handle unit system change with value preservation
     */
    fun switchUnitSystem(newUnitSystem: UnitSystem) {
        val currentSystem = _unitSystem.value ?: UnitSystem.METRIC
        if (currentSystem == newUnitSystem) return

        _unitSystem.value = newUnitSystem
        
        val currentProfile = _userProfile.value
        if (currentProfile != null && currentProfile.heightInCm > 0 && currentProfile.weightInKg > 0.0) {
            // Convert current values to new unit system for display
            val heightResult = UnitConversionUtils.convertHeight(
                currentProfile.heightInCm.toDouble(),
                UnitSystem.METRIC,
                newUnitSystem
            )
            
            val weightResult = UnitConversionUtils.convertWeight(
                currentProfile.weightInKg,
                UnitSystem.METRIC,
                newUnitSystem
            )

            // Emit unit conversion event for UI to update display values
            _navigationEvent.value = OnboardingNavigationEvent.UnitSystemChanged(
                newSystem = newUnitSystem,
                convertedHeight = if (heightResult is ConversionResult.Success) heightResult.data else null,
                convertedWeight = if (weightResult is ConversionResult.Success) weightResult.data else null
            )
        }
    }

    /**
     * Complete onboarding with goal calculation trigger
     */
    fun completeOnboarding() {
        android.util.Log.d("OnboardingViewModel", "completeOnboarding() called")
        val profile = _userProfile.value
        android.util.Log.d("OnboardingViewModel", "Current profile: $profile")
        
        if (profile == null || !isProfileComplete(profile)) {
            android.util.Log.e("OnboardingViewModel", "Profile is incomplete - profile: $profile, isComplete: ${profile?.let { isProfileComplete(it) }}")
            _onboardingState.value = OnboardingState.Error("Profile is incomplete")
            return
        }

        android.util.Log.d("OnboardingViewModel", "Profile is complete, starting completion process")
        _isLoading.value = true
        _onboardingState.value = OnboardingState.Completing

        viewModelScope.launch {
            try {
                val completedProfile = profile.copy(
                    hasCompletedOnboarding = true,
                    updatedAt = Date()
                )
                android.util.Log.d("OnboardingViewModel", "Created completed profile: $completedProfile")

                // Use integration manager for complete onboarding flow
                val integrationResult = integrationManager.completeOnboardingIntegration(completedProfile)
                android.util.Log.d("OnboardingViewModel", "Integration result: $integrationResult")
                
                when (integrationResult) {
                    is OnboardingResult.Success -> {
                        android.util.Log.d("OnboardingViewModel", "Onboarding completed successfully")
                        
                        // Trigger goal calculation after successful onboarding
                        triggerInitialGoalCalculation(completedProfile.userId)
                        
                        _onboardingState.value = OnboardingState.Completed
                        _currentStep.value = OnboardingStep.COMPLETION
                        _progressPercentage.value = 1.0f
                        _navigationEvent.value = OnboardingNavigationEvent.OnboardingComplete
                        
                        retryAttempts = 0 // Reset retry counter on success
                    }
                    is OnboardingResult.Error -> {
                        android.util.Log.e("OnboardingViewModel", "Onboarding integration failed", integrationResult.exception)
                        handleOnboardingError(integrationResult.exception)
                    }
                    is OnboardingResult.Loading -> {
                        android.util.Log.d("OnboardingViewModel", "Integration still loading")
                        // Keep loading state
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OnboardingViewModel", "Exception in completeOnboarding", e)
                handleOnboardingError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Retry onboarding completion
     */
    fun retryOnboardingCompletion() {
        if (retryAttempts < MAX_RETRY_ATTEMPTS) {
            retryAttempts++
            completeOnboarding()
        } else {
            _errorRecoveryEvent.value = ErrorRecoveryEvent.MaxRetriesExceeded
        }
    }

    /**
     * Save current state for process death recovery
     */
    fun saveState(): OnboardingSavedState {
        val state = OnboardingSavedState(
            currentStep = _currentStep.value ?: OnboardingStep.WELCOME,
            userProfile = _userProfile.value ?: UserProfile(),
            unitSystem = _unitSystem.value ?: UnitSystem.METRIC,
            validationErrors = _validationErrors.value ?: ValidationErrors()
        )
        savedState = state
        return state
    }

    /**
     * Restore state from saved state
     */
    fun restoreState(savedState: OnboardingSavedState) {
        this.savedState = savedState
        _currentStep.value = savedState.currentStep
        _userProfile.value = savedState.userProfile
        _unitSystem.value = savedState.unitSystem
        _validationErrors.value = savedState.validationErrors
        _progressPercentage.value = savedState.currentStep.getProgressPercentage()
        updateOnboardingState(savedState.currentStep)
    }

    /**
     * Clear validation errors
     */
    fun clearValidationErrors() {
        _validationErrors.value = ValidationErrors()
    }

    /**
     * Get validation suggestions for current field
     */
    fun getValidationSuggestions(field: ValidationField): List<String> {
        val unitSystem = _unitSystem.value ?: UnitSystem.METRIC
        return validationHelper.getValidationSuggestions(field, "", unitSystem)
    }

    /**
     * Validate current step
     */
    private fun validateCurrentStep(): Boolean {
        val currentStep = _currentStep.value ?: return false
        val profile = _userProfile.value ?: return false

        return when (currentStep) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.PERSONAL_INFO -> {
                profile.displayName.isNotBlank() && profile.birthday != null
            }
            OnboardingStep.PHYSICAL_INFO -> {
                profile.heightInCm > 0 && profile.weightInKg > 0.0
            }
            OnboardingStep.COMPLETION -> true
        }
    }

    /**
     * Validate personal information
     */
    private fun validatePersonalInfo(name: String, birthday: Date?) {
        val validationResult = validationHelper.validateCompleteOnboardingData(
            name = name,
            birthday = birthday,
            height = "0", // Not validated at this step
            weight = "0", // Not validated at this step
            gender = Gender.PREFER_NOT_TO_SAY, // Not validated at this step
            unitSystem = _unitSystem.value ?: UnitSystem.METRIC
        )

        // Only update name and birthday errors
        val currentErrors = _validationErrors.value ?: ValidationErrors()
        _validationErrors.value = currentErrors.copy(
            nameError = validationResult.errors.nameError,
            birthdayError = validationResult.errors.birthdayError
        )
    }

    /**
     * Update onboarding state based on current step
     */
    private fun updateOnboardingState(step: OnboardingStep) {
        _onboardingState.value = when (step) {
            OnboardingStep.WELCOME -> OnboardingState.Loading
            OnboardingStep.PERSONAL_INFO -> OnboardingState.PersonalInfo
            OnboardingStep.PHYSICAL_INFO -> OnboardingState.PhysicalInfo
            OnboardingStep.COMPLETION -> OnboardingState.Completing
        }
    }

    /**
     * Check if profile is complete
     */
    private fun isProfileComplete(profile: UserProfile): Boolean {
        val isComplete = profile.displayName.isNotBlank() &&
               profile.birthday != null &&
               profile.heightInCm > 0 &&
               profile.weightInKg > 0.0
        
        android.util.Log.d("OnboardingViewModel", "Profile completeness check: " +
            "displayName='${profile.displayName}' (${profile.displayName.isNotBlank()}), " +
            "birthday=${profile.birthday} (${profile.birthday != null}), " +
            "height=${profile.heightInCm} (${profile.heightInCm > 0}), " +
            "weight=${profile.weightInKg} (${profile.weightInKg > 0.0}), " +
            "overall: $isComplete")
        
        return isComplete
    }

    /**
     * Handle onboarding errors with recovery options
     */
    private fun handleOnboardingError(exception: Exception) {
        _onboardingState.value = OnboardingState.Error(
            message = "Failed to complete onboarding: ${exception.message}",
            exception = exception
        )
        
        _errorRecoveryEvent.value = when {
            exception.message?.contains("network", ignoreCase = true) == true -> {
                ErrorRecoveryEvent.NetworkError
            }
            exception.message?.contains("storage", ignoreCase = true) == true -> {
                ErrorRecoveryEvent.StorageError
            }
            else -> ErrorRecoveryEvent.UnknownError(exception.message ?: "Unknown error")
        }
    }

    /**
     * Restore progress if available
     */
    private fun restoreProgressIfAvailable() {
        viewModelScope.launch {
            try {
                val savedProgress = progressManager.getProgress()
                if (savedProgress != null) {
                    // Restore saved state
                    _currentStep.value = savedProgress.currentStep
                    _unitSystem.value = savedProgress.unitSystem
                    
                    val restoredProfile = UserProfile(
                        displayName = savedProgress.userName,
                        birthday = savedProgress.userBirthday,
                        gender = savedProgress.userGender,
                        unitSystem = savedProgress.unitSystem,
                        heightInCm = savedProgress.userHeight.toIntOrNull() ?: 0,
                        weightInKg = savedProgress.userWeight.toDoubleOrNull() ?: 0.0
                    )
                    _userProfile.value = restoredProfile
                    
                    _progressPercentage.value = savedProgress.currentStep.getProgressPercentage()
                    updateOnboardingState(savedProgress.currentStep)
                }
            } catch (e: Exception) {
                android.util.Log.w("OnboardingViewModel", "Failed to restore progress", e)
            }
        }
    }

    /**
     * Trigger initial goal calculation after onboarding completion.
     * 
     * @param userId User ID to calculate goals for
     */
    private fun triggerInitialGoalCalculation(userId: String) {
        goalCalculationJob?.cancel()
        goalCalculationJob = viewModelScope.launch {
            try {
                _goalCalculationState.value = OnboardingGoalCalculationState.Calculating(
                    "Setting up your personalized wellness goals..."
                )

                val result = goalCalculationUseCase.calculateAndStoreGoals(userId)

                when (result) {
                    is com.vibehealth.android.domain.goals.GoalCalculationResult.Success -> {
                        _goalCalculationState.value = OnboardingGoalCalculationState.Success(
                            goals = result.goals,
                            message = "Your personalized goals are ready!"
                        )
                        android.util.Log.d("OnboardingViewModel", "Goal calculation successful: ${result.goals}")
                    }
                    
                    is com.vibehealth.android.domain.goals.GoalCalculationResult.Error -> {
                        _goalCalculationState.value = OnboardingGoalCalculationState.Failed(
                            message = "We'll set up your goals shortly. You can continue to your dashboard.",
                            canRetry = true
                        )
                        android.util.Log.w("OnboardingViewModel", "Goal calculation failed: ${result.message}")
                    }
                }

            } catch (e: Exception) {
                _goalCalculationState.value = OnboardingGoalCalculationState.Failed(
                    message = "We'll set up your goals shortly. You can continue to your dashboard.",
                    canRetry = true
                )
                android.util.Log.e("OnboardingViewModel", "Goal calculation error", e)
            }
        }
    }

    /**
     * Retry goal calculation if it failed during onboarding.
     */
    fun retryGoalCalculation() {
        val profile = _userProfile.value
        if (profile != null && profile.hasCompletedOnboarding) {
            triggerInitialGoalCalculation(profile.userId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        validationJob?.cancel()
        goalCalculationJob?.cancel()
        validationHelper.clearCache()
        autoSaveManager.cleanup()
    }
}



/**
 * Saved state for process death recovery
 */
data class OnboardingSavedState(
    val currentStep: OnboardingStep,
    val userProfile: UserProfile,
    val unitSystem: UnitSystem,
    val validationErrors: ValidationErrors
)

/**
 * State of goal calculation during onboarding.
 */
sealed class OnboardingGoalCalculationState {
    object NotStarted : OnboardingGoalCalculationState()
    data class Calculating(val message: String) : OnboardingGoalCalculationState()
    data class Success(
        val goals: com.vibehealth.android.domain.goals.DailyGoals,
        val message: String
    ) : OnboardingGoalCalculationState()
    data class Failed(val message: String, val canRetry: Boolean) : OnboardingGoalCalculationState()
}