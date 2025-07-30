package com.vibehealth.android.ui.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vibehealth.android.core.validation.OnboardingValidationHelper
import com.vibehealth.android.core.validation.OnboardingValidationResult
import com.vibehealth.android.core.validation.MetricConversionResult
import com.vibehealth.android.domain.onboarding.OnboardingState
import com.vibehealth.android.domain.onboarding.OnboardingStep
import com.vibehealth.android.domain.onboarding.ValidationErrors
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.user.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for OnboardingViewModel
 */
@ExperimentalCoroutinesApi
class OnboardingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: OnboardingViewModel
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockValidationHelper: OnboardingValidationHelper

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockUserRepository = mockk()
        mockValidationHelper = mockk()

        // Setup default mocks
        every { mockValidationHelper.clearCache() } just Runs
        every { mockValidationHelper.getValidationSuggestions(any(), any(), any()) } returns listOf("Test suggestion")

        viewModel = OnboardingViewModel(mockUserRepository, mockValidationHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialization_shouldSetDefaultValues() {
        // Then
        assertEquals(OnboardingStep.WELCOME, viewModel.currentStep.value)
        assertEquals(OnboardingState.PersonalInfo, viewModel.onboardingState.value)
        assertEquals(UnitSystem.METRIC, viewModel.unitSystem.value)
        assertNotNull(viewModel.validationErrors.value)
        assertFalse(viewModel.validationErrors.value!!.hasErrors())
        assertEquals(0.0f, viewModel.progressPercentage.value)
    }

    @Test
    fun updatePersonalInfo_shouldUpdateUserProfile() = runTest {
        // Given
        val name = "John Doe"
        val birthday = Date()
        val validationResult = OnboardingValidationResult(
            errors = ValidationErrors(),
            parsedHeight = null,
            parsedWeight = null,
            isValid = true
        )
        
        every { 
            mockValidationHelper.validateCompleteOnboardingData(any(), any(), any(), any(), any(), any()) 
        } returns validationResult

        // When
        viewModel.updatePersonalInfo(name, birthday)
        advanceTimeBy(400) // Wait for debounce

        // Then
        val profile = viewModel.userProfile.value
        assertNotNull(profile)
        assertEquals(name, profile.displayName)
        assertEquals(birthday, profile.birthday)
    }

    @Test
    fun updatePhysicalInfo_withValidData_shouldUpdateProfile() {
        // Given
        val height = "175"
        val weight = "70"
        val gender = Gender.MALE
        val unitSystem = UnitSystem.METRIC
        
        val validationResult = OnboardingValidationResult(
            errors = ValidationErrors(),
            parsedHeight = 175.0,
            parsedWeight = 70.0,
            isValid = true
        )
        
        val conversionResult = MetricConversionResult.Success(175, 70.0)
        
        every { 
            mockValidationHelper.validateCompleteOnboardingData(any(), any(), height, weight, gender, unitSystem) 
        } returns validationResult
        
        every { 
            mockValidationHelper.convertToMetricForStorage(175.0, 70.0, unitSystem) 
        } returns conversionResult

        // When
        viewModel.updatePhysicalInfo(height, weight, gender, unitSystem)

        // Then
        val profile = viewModel.userProfile.value
        assertNotNull(profile)
        assertEquals(gender, profile.gender)
        assertEquals(unitSystem, profile.unitSystem)
        assertEquals(175, profile.heightInCm)
        assertEquals(70.0, profile.weightInKg)
    }

    @Test
    fun updatePhysicalInfo_withInvalidData_shouldSetValidationErrors() {
        // Given
        val height = "invalid"
        val weight = "invalid"
        val gender = Gender.MALE
        val unitSystem = UnitSystem.METRIC
        
        val validationResult = OnboardingValidationResult(
            errors = ValidationErrors(heightError = "Invalid height", weightError = "Invalid weight"),
            parsedHeight = null,
            parsedWeight = null,
            isValid = false
        )
        
        every { 
            mockValidationHelper.validateCompleteOnboardingData(any(), any(), height, weight, gender, unitSystem) 
        } returns validationResult

        // When
        viewModel.updatePhysicalInfo(height, weight, gender, unitSystem)

        // Then
        val errors = viewModel.validationErrors.value
        assertNotNull(errors)
        assertTrue(errors.hasErrors())
        assertEquals("Invalid height", errors.heightError)
        assertEquals("Invalid weight", errors.weightError)
    }

    @Test
    fun navigateToNextStep_withValidCurrentStep_shouldNavigateForward() {
        // Given - Set up valid personal info
        val profile = UserProfile(
            displayName = "John Doe",
            birthday = Date()
        )
        viewModel.updatePersonalInfo(profile.displayName, profile.birthday)

        // When
        viewModel.navigateToNextStep()

        // Then
        assertEquals(OnboardingStep.PHYSICAL_INFO, viewModel.currentStep.value)
        assertEquals(OnboardingState.PhysicalInfo, viewModel.onboardingState.value)
        assertTrue(viewModel.progressPercentage.value!! > 0.0f)
    }

    @Test
    fun navigateToNextStep_withInvalidCurrentStep_shouldNotNavigate() {
        // Given - Empty profile (invalid)
        val initialStep = viewModel.currentStep.value

        // When
        viewModel.navigateToNextStep()

        // Then
        assertEquals(initialStep, viewModel.currentStep.value)
    }

    @Test
    fun navigateToPreviousStep_shouldNavigateBackward() {
        // Given - Navigate to physical info first
        viewModel.updatePersonalInfo("John Doe", Date())
        viewModel.navigateToNextStep()
        assertEquals(OnboardingStep.PHYSICAL_INFO, viewModel.currentStep.value)

        // When
        viewModel.navigateToPreviousStep()

        // Then
        assertEquals(OnboardingStep.PERSONAL_INFO, viewModel.currentStep.value)
        assertEquals(OnboardingState.PersonalInfo, viewModel.onboardingState.value)
    }

    @Test
    fun switchUnitSystem_shouldUpdateUnitSystemAndEmitEvent() {
        // Given
        val newUnitSystem = UnitSystem.IMPERIAL
        assertEquals(UnitSystem.METRIC, viewModel.unitSystem.value)

        // When
        viewModel.switchUnitSystem(newUnitSystem)

        // Then
        assertEquals(newUnitSystem, viewModel.unitSystem.value)
    }

    @Test
    fun completeOnboarding_withValidProfile_shouldSaveAndComplete() = runTest {
        // Given
        val completeProfile = UserProfile(
            userId = "test_user",
            displayName = "John Doe",
            birthday = Date(),
            heightInCm = 175,
            weightInKg = 70.0
        )
        
        // Set up the profile in ViewModel
        viewModel.updatePersonalInfo(completeProfile.displayName, completeProfile.birthday)
        viewModel.updatePhysicalInfo("175", "70", Gender.MALE, UnitSystem.METRIC)
        
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.success(completeProfile)

        // When
        viewModel.completeOnboarding()

        // Then
        assertEquals(OnboardingState.Completed, viewModel.onboardingState.value)
        assertEquals(OnboardingStep.COMPLETION, viewModel.currentStep.value)
        assertEquals(1.0f, viewModel.progressPercentage.value)
        
        coVerify { mockUserRepository.saveUserProfile(any()) }
    }

    @Test
    fun completeOnboarding_withIncompleteProfile_shouldSetError() {
        // Given - Incomplete profile (no height/weight)
        viewModel.updatePersonalInfo("John Doe", Date())

        // When
        viewModel.completeOnboarding()

        // Then
        assertTrue(viewModel.onboardingState.value is OnboardingState.Error)
    }

    @Test
    fun completeOnboarding_withSaveFailure_shouldHandleError() = runTest {
        // Given
        val completeProfile = UserProfile(
            displayName = "John Doe",
            birthday = Date(),
            heightInCm = 175,
            weightInKg = 70.0
        )
        
        viewModel.updatePersonalInfo(completeProfile.displayName, completeProfile.birthday)
        viewModel.updatePhysicalInfo("175", "70", Gender.MALE, UnitSystem.METRIC)
        
        val exception = Exception("Save failed")
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.failure(exception)

        // When
        viewModel.completeOnboarding()

        // Then
        assertTrue(viewModel.onboardingState.value is OnboardingState.Error)
        assertNotNull(viewModel.errorRecoveryEvent.value)
    }

    @Test
    fun retryOnboardingCompletion_shouldRetryUpToMaxAttempts() = runTest {
        // Given
        val completeProfile = UserProfile(
            displayName = "John Doe",
            birthday = Date(),
            heightInCm = 175,
            weightInKg = 70.0
        )
        
        viewModel.updatePersonalInfo(completeProfile.displayName, completeProfile.birthday)
        viewModel.updatePhysicalInfo("175", "70", Gender.MALE, UnitSystem.METRIC)
        
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.failure(Exception("Save failed"))

        // When - First failure
        viewModel.completeOnboarding()
        assertTrue(viewModel.onboardingState.value is OnboardingState.Error)

        // When - Retry attempts
        repeat(3) {
            viewModel.retryOnboardingCompletion()
        }

        // Then - Should have attempted 4 times total (1 initial + 3 retries)
        coVerify(exactly = 4) { mockUserRepository.saveUserProfile(any()) }
        
        // When - Exceed max retries
        viewModel.retryOnboardingCompletion()
        
        // Then
        assertEquals(ErrorRecoveryEvent.MaxRetriesExceeded, viewModel.errorRecoveryEvent.value)
    }

    @Test
    fun saveAndRestoreState_shouldPreserveOnboardingState() {
        // Given
        val profile = UserProfile(
            displayName = "John Doe",
            birthday = Date(),
            heightInCm = 175,
            weightInKg = 70.0
        )
        val unitSystem = UnitSystem.IMPERIAL
        val errors = ValidationErrors(nameError = "Test error")
        
        viewModel.updatePersonalInfo(profile.displayName, profile.birthday)
        viewModel.switchUnitSystem(unitSystem)
        viewModel.navigateToNextStep()

        // When - Save state
        val savedState = viewModel.saveState()

        // Then - Verify saved state
        assertEquals(OnboardingStep.PHYSICAL_INFO, savedState.currentStep)
        assertEquals(unitSystem, savedState.unitSystem)
        assertNotNull(savedState.userProfile)

        // When - Create new ViewModel and restore state
        val newViewModel = OnboardingViewModel(mockUserRepository, mockValidationHelper)
        newViewModel.restoreState(savedState)

        // Then - Verify restored state
        assertEquals(savedState.currentStep, newViewModel.currentStep.value)
        assertEquals(savedState.unitSystem, newViewModel.unitSystem.value)
        assertEquals(savedState.userProfile.displayName, newViewModel.userProfile.value?.displayName)
    }

    @Test
    fun clearValidationErrors_shouldClearAllErrors() {
        // Given
        val errors = ValidationErrors(
            nameError = "Name error",
            heightError = "Height error"
        )
        // Simulate setting errors (this would normally come from validation)
        viewModel.updatePhysicalInfo("invalid", "invalid", Gender.MALE, UnitSystem.METRIC)

        // When
        viewModel.clearValidationErrors()

        // Then
        val clearedErrors = viewModel.validationErrors.value
        assertNotNull(clearedErrors)
        assertFalse(clearedErrors.hasErrors())
    }

    @Test
    fun getValidationSuggestions_shouldReturnHelpfulSuggestions() {
        // When
        val suggestions = viewModel.getValidationSuggestions(ValidationField.NAME)

        // Then
        assertTrue(suggestions.isNotEmpty())
        verify { mockValidationHelper.getValidationSuggestions(ValidationField.NAME, "", UnitSystem.METRIC) }
    }

    @Test
    fun onCleared_shouldCleanupResources() {
        // When
        viewModel.onCleared()

        // Then
        verify { mockValidationHelper.clearCache() }
    }

    @Test
    fun navigationEvents_shouldEmitCorrectEvents() {
        // Given
        val profile = UserProfile(displayName = "John Doe", birthday = Date())
        viewModel.updatePersonalInfo(profile.displayName, profile.birthday)

        // When
        viewModel.navigateToNextStep()

        // Then
        val navigationEvent = viewModel.navigationEvent.value
        assertTrue(navigationEvent is OnboardingNavigationEvent.NavigateForward)
        assertEquals(OnboardingStep.PHYSICAL_INFO, (navigationEvent as OnboardingNavigationEvent.NavigateForward).step)
    }

    @Test
    fun errorRecoveryEvents_shouldEmitCorrectEventTypes() = runTest {
        // Given
        val completeProfile = UserProfile(
            displayName = "John Doe",
            birthday = Date(),
            heightInCm = 175,
            weightInKg = 70.0
        )
        
        viewModel.updatePersonalInfo(completeProfile.displayName, completeProfile.birthday)
        viewModel.updatePhysicalInfo("175", "70", Gender.MALE, UnitSystem.METRIC)

        // Test network error
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.failure(Exception("Network error"))
        viewModel.completeOnboarding()
        assertTrue(viewModel.errorRecoveryEvent.value is ErrorRecoveryEvent.NetworkError)

        // Test storage error
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.failure(Exception("Storage error"))
        viewModel.completeOnboarding()
        assertTrue(viewModel.errorRecoveryEvent.value is ErrorRecoveryEvent.StorageError)

        // Test unknown error
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.failure(Exception("Unknown issue"))
        viewModel.completeOnboarding()
        assertTrue(viewModel.errorRecoveryEvent.value is ErrorRecoveryEvent.UnknownError)
    }

    @Test
    fun progressPercentage_shouldUpdateCorrectly() {
        // Given
        assertEquals(0.0f, viewModel.progressPercentage.value)

        // When - Navigate through steps
        viewModel.updatePersonalInfo("John Doe", Date())
        viewModel.navigateToNextStep()

        // Then
        assertTrue(viewModel.progressPercentage.value!! > 0.0f)
        assertTrue(viewModel.progressPercentage.value!! < 1.0f)

        // When - Complete onboarding
        viewModel.updatePhysicalInfo("175", "70", Gender.MALE, UnitSystem.METRIC)
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.success(UserProfile())
        viewModel.completeOnboarding()

        // Then
        assertEquals(1.0f, viewModel.progressPercentage.value)
    }
}