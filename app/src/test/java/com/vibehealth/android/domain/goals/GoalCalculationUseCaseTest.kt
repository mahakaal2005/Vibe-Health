package com.vibehealth.android.domain.goals

import com.vibehealth.android.data.goals.GoalRepository
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Calendar

/**
 * Comprehensive unit tests for GoalCalculationUseCase.
 * 
 * Tests cover orchestration logic, error handling, retry mechanisms,
 * and validation as specified in Task 3.1 requirements.
 */
class GoalCalculationUseCaseTest {

    @Mock
    private lateinit var goalCalculationService: GoalCalculationService
    
    @Mock
    private lateinit var goalRepository: GoalRepository
    
    @Mock
    private lateinit var userProfileRepository: UserProfileRepository
    
    private lateinit var goalCalculationUseCase: GoalCalculationUseCase
    
    private val testUserId = "test-user-123"
    
    private val testUserProfile = createTestUserProfile()
    
    private val testGoals = DailyGoals(
        userId = testUserId,
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = LocalDateTime.now(),
        calculationSource = CalculationSource.WHO_STANDARD
    )

    private fun createTestUserProfile(
        age: Int = 25,
        gender: Gender = Gender.MALE,
        heightInCm: Int = 175,
        weightInKg: Double = 70.0
    ): UserProfile {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -age)
        
        return UserProfile(
            userId = testUserId,
            email = "test@example.com",
            displayName = "Test User",
            firstName = "Test",
            lastName = "User",
            birthday = calendar.time,
            gender = gender,
            unitSystem = UnitSystem.METRIC,
            heightInCm = heightInCm,
            weightInKg = weightInKg,
            hasCompletedOnboarding = true
        )
    }

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        goalCalculationUseCase = GoalCalculationUseCase(
            goalCalculationService,
            goalRepository,
            userProfileRepository
        )
    }

    @Nested
    @DisplayName("Calculate and Store Goals")
    inner class CalculateAndStoreGoals {

        @Test
        @DisplayName("Should successfully calculate and store goals for new user")
        fun shouldSuccessfullyCalculateAndStoreGoalsForNewUser() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(null)
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(testUserProfile))
            whenever(goalCalculationService.calculateGoals(testUserProfile)).thenReturn(testGoals)
            whenever(goalRepository.saveAndSyncGoals(testGoals)).thenReturn(Result.success(testGoals))
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as GoalCalculationResult.Success
            assertEquals(testGoals, successResult.goals)
            assertTrue(successResult.wasRecalculated)
            assertEquals(CalculationSource.WHO_STANDARD, successResult.calculationSource)
            
            verify(goalCalculationService).calculateGoals(testUserProfile)
            verify(goalRepository).saveAndSyncGoals(testGoals)
        }

        @Test
        @DisplayName("Should return existing goals when recalculation not needed")
        fun shouldReturnExistingGoalsWhenRecalculationNotNeeded() = runTest {
            // Given
            val existingGoals = testGoals.copy(calculatedAt = LocalDateTime.now().minusHours(1))
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(existingGoals)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(LocalDateTime.now().minusHours(1))
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId, forceRecalculation = false)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as GoalCalculationResult.Success
            assertEquals(existingGoals, successResult.goals)
            assertFalse(successResult.wasRecalculated)
            
            // Should not call calculation service
            verify(goalCalculationService, never()).calculateGoals(any())
        }

        @Test
        @DisplayName("Should handle profile not found error")
        fun shouldHandleProfileNotFoundError() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(null)
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(null))
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as GoalCalculationResult.Error
            assertTrue(errorResult.error is GoalCalculationError.ProfileNotFound)
            assertEquals(testUserId, (errorResult.error as GoalCalculationError.ProfileNotFound).userId)
        }

        @Test
        @DisplayName("Should handle calculation failure with retry")
        fun shouldHandleCalculationFailureWithRetry() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(null)
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(testUserProfile))
            whenever(goalCalculationService.calculateGoals(testUserProfile))
                .thenThrow(RuntimeException("Calculation error"))
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as GoalCalculationResult.Error
            assertTrue(errorResult.error is GoalCalculationError.CalculationFailed)
            
            // Should retry 3 times
            verify(goalCalculationService, times(3)).calculateGoals(testUserProfile)
        }

        @Test
        @DisplayName("Should handle storage failure")
        fun shouldHandleStorageFailure() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(null)
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(testUserProfile))
            whenever(goalCalculationService.calculateGoals(testUserProfile)).thenReturn(testGoals)
            whenever(goalRepository.saveAndSyncGoals(testGoals))
                .thenReturn(Result.failure(RuntimeException("Storage error")))
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as GoalCalculationResult.Error
            assertTrue(errorResult.error is GoalCalculationError.StorageFailed)
        }
    }

    @Nested
    @DisplayName("Goal Validation")
    inner class GoalValidation {

        @Test
        @DisplayName("Should validate normal goals successfully")
        fun shouldValidateNormalGoalsSuccessfully() = runTest {
            // Given
            val normalGoals = testGoals.copy(
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30
            )
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(null)
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(testUserProfile))
            whenever(goalCalculationService.calculateGoals(testUserProfile)).thenReturn(normalGoals)
            whenever(goalRepository.saveAndSyncGoals(normalGoals)).thenReturn(Result.success(normalGoals))
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isSuccess())
        }

        @Test
        @DisplayName("Should reject goals outside valid ranges")
        fun shouldRejectGoalsOutsideValidRanges() = runTest {
            // Given
            val invalidGoals = testGoals.copy(
                stepsGoal = 25000, // Too high
                caloriesGoal = 500,  // Too low
                heartPointsGoal = 100 // Too high
            )
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(null)
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(testUserProfile))
            whenever(goalCalculationService.calculateGoals(testUserProfile)).thenReturn(invalidGoals)
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as GoalCalculationResult.Error
            assertTrue(errorResult.error is GoalCalculationError.ValidationFailed)
        }
    }

    @Nested
    @DisplayName("Profile Update Recalculation")
    inner class ProfileUpdateRecalculation {

        @Test
        @DisplayName("Should recalculate goals when profile changes")
        fun shouldRecalculateGoalsWhenProfileChanges() = runTest {
            // Given
            val previousGoals = testGoals.copy(calculatedAt = LocalDateTime.now().minusHours(2))
            val updatedProfile = testUserProfile.copy(weightInKg = 75.0) // Weight changed
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(previousGoals)
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(updatedProfile))
            whenever(goalCalculationService.calculateGoals(updatedProfile)).thenReturn(testGoals)
            whenever(goalRepository.saveAndSyncGoals(testGoals)).thenReturn(Result.success(testGoals))
            
            // When
            val result = goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as GoalCalculationResult.Success
            assertTrue(successResult.wasRecalculated)
            
            verify(goalCalculationService).calculateGoals(updatedProfile)
        }

        @Test
        @DisplayName("Should handle profile not found during recalculation")
        fun shouldHandleProfileNotFoundDuringRecalculation() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(testGoals)
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(null))
            
            // When
            val result = goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as GoalCalculationResult.Error
            assertTrue(errorResult.error is GoalCalculationError.ProfileNotFound)
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    inner class UtilityMethods {

        @Test
        @DisplayName("Should check if user has valid goals")
        fun shouldCheckIfUserHasValidGoals() = runTest {
            // Given
            val validGoals = testGoals.copy(
                calculatedAt = LocalDateTime.now().minusHours(1),
                calculationSource = CalculationSource.WHO_STANDARD
            )
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(validGoals)
            
            // When
            val hasValidGoals = goalCalculationUseCase.hasValidGoals(testUserId)
            
            // Then
            assertTrue(hasValidGoals)
        }

        @Test
        @DisplayName("Should return false for fallback goals")
        fun shouldReturnFalseForFallbackGoals() = runTest {
            // Given
            val fallbackGoals = testGoals.copy(
                calculationSource = CalculationSource.FALLBACK_DEFAULT
            )
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(fallbackGoals)
            
            // When
            val hasValidGoals = goalCalculationUseCase.hasValidGoals(testUserId)
            
            // Then
            assertFalse(hasValidGoals)
        }

        @Test
        @DisplayName("Should return false when no goals exist")
        fun shouldReturnFalseWhenNoGoalsExist() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            
            // When
            val hasValidGoals = goalCalculationUseCase.hasValidGoals(testUserId)
            
            // Then
            assertFalse(hasValidGoals)
        }
    }

    @Nested
    @DisplayName("Retry Logic")
    inner class RetryLogic {

        @Test
        @DisplayName("Should retry profile retrieval on failure")
        fun shouldRetryProfileRetrievalOnFailure() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(null)
            whenever(userProfileRepository.getUserProfile(testUserId))
                .thenReturn(Result.failure(RuntimeException("Network error")))
                .thenReturn(Result.failure(RuntimeException("Network error")))
                .thenReturn(Result.success(testUserProfile))
            whenever(goalCalculationService.calculateGoals(testUserProfile)).thenReturn(testGoals)
            whenever(goalRepository.saveAndSyncGoals(testGoals)).thenReturn(Result.success(testGoals))
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isSuccess())
            verify(userProfileRepository, times(3)).getUserProfile(testUserId)
        }

        @Test
        @DisplayName("Should fail after maximum retry attempts")
        fun shouldFailAfterMaximumRetryAttempts() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(null)
            whenever(userProfileRepository.getUserProfile(testUserId))
                .thenReturn(Result.failure(RuntimeException("Persistent error")))
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isError())
            verify(userProfileRepository, times(3)).getUserProfile(testUserId) // Max 3 attempts
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("Should handle unexpected exceptions gracefully")
        fun shouldHandleUnexpectedExceptionsGracefully() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId))
                .thenThrow(RuntimeException("Unexpected database error"))
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as GoalCalculationResult.Error
            assertTrue(errorResult.error is GoalCalculationError.UnexpectedError)
        }

        @Test
        @DisplayName("Should handle null responses gracefully")
        fun shouldHandleNullResponsesGracefully() = runTest {
            // Given
            whenever(goalRepository.getCurrentGoalsSync(testUserId)).thenReturn(null)
            whenever(goalRepository.getLastCalculationTime(testUserId)).thenReturn(null)
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(testUserProfile))
            whenever(goalCalculationService.calculateGoals(testUserProfile)).thenReturn(null)
            
            // When
            val result = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as GoalCalculationResult.Error
            assertTrue(errorResult.error is GoalCalculationError.CalculationFailed)
        }
    }
}