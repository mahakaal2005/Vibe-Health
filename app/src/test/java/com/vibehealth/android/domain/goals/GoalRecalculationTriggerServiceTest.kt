package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.common.UnitSystem
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.util.*

/**
 * Unit tests for GoalRecalculationTriggerService.
 * 
 * Tests cover change detection, debouncing, and trigger logic
 * as specified in Task 5.1 requirements.
 */
class GoalRecalculationTriggerServiceTest {

    @Mock
    private lateinit var goalCalculationUseCase: GoalCalculationUseCase
    
    private lateinit var triggerService: GoalRecalculationTriggerService
    
    private val testUserId = "test-user-123"
    
    private val baseProfile = UserProfile(
        userId = testUserId,
        email = "test@example.com",
        displayName = "Test User",
        firstName = "Test",
        lastName = "User",
        birthday = Calendar.getInstance().apply { add(Calendar.YEAR, -25) }.time,
        gender = Gender.MALE,
        unitSystem = UnitSystem.METRIC,
        heightInCm = 175,
        weightInKg = 70.0,
        hasCompletedOnboarding = true
    )

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        triggerService = GoalRecalculationTriggerService(goalCalculationUseCase)
    }

    @Nested
    @DisplayName("Change Detection")
    inner class ChangeDetection {

        @Test
        @DisplayName("Should detect weight change")
        fun shouldDetectWeightChange() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(weightInKg = 75.0)
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
        }

        @Test
        @DisplayName("Should detect height change")
        fun shouldDetectHeightChange() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(heightInCm = 180)
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
        }

        @Test
        @DisplayName("Should detect gender change")
        fun shouldDetectGenderChange() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(gender = Gender.FEMALE)
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
        }

        @Test
        @DisplayName("Should detect birthday change")
        fun shouldDetectBirthdayChange() = runTest {
            // Given
            val newBirthday = Calendar.getInstance().apply { add(Calendar.YEAR, -30) }.time
            val updatedProfile = baseProfile.copy(birthday = newBirthday)
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
        }

        @Test
        @DisplayName("Should ignore non-relevant changes")
        fun shouldIgnoreNonRelevantChanges() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(
                displayName = "Updated Name",
                firstName = "Updated",
                lastName = "Name",
                email = "updated@example.com"
            )

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase, never()).recalculateGoalsForProfileUpdate(any(), any())
        }

        @Test
        @DisplayName("Should ignore unit system changes")
        fun shouldIgnoreUnitSystemChanges() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(unitSystem = UnitSystem.IMPERIAL)

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase, never()).recalculateGoalsForProfileUpdate(any(), any())
        }

        @Test
        @DisplayName("Should ignore onboarding completion changes")
        fun shouldIgnoreOnboardingCompletionChanges() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(hasCompletedOnboarding = false)

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase, never()).recalculateGoalsForProfileUpdate(any(), any())
        }
    }

    @Nested
    @DisplayName("Multiple Changes")
    inner class MultipleChanges {

        @Test
        @DisplayName("Should handle multiple relevant changes")
        fun shouldHandleMultipleRelevantChanges() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(
                weightInKg = 75.0,
                heightInCm = 180,
                gender = Gender.FEMALE
            )
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
        }

        @Test
        @DisplayName("Should handle mixed relevant and non-relevant changes")
        fun shouldHandleMixedRelevantAndNonRelevantChanges() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(
                weightInKg = 75.0, // Relevant
                displayName = "Updated Name", // Not relevant
                email = "updated@example.com" // Not relevant
            )
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
        }
    }

    @Nested
    @DisplayName("Debouncing Logic")
    inner class DebouncingLogic {

        @Test
        @DisplayName("Should debounce rapid profile updates")
        fun shouldDebounceRapidProfileUpdates() = runTest {
            // Given
            val profile1 = baseProfile.copy(weightInKg = 71.0)
            val profile2 = baseProfile.copy(weightInKg = 72.0)
            val profile3 = baseProfile.copy(weightInKg = 73.0)
            
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(any(), any()))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When - Rapid successive updates
            triggerService.onProfileUpdated(baseProfile, profile1)
            triggerService.onProfileUpdated(profile1, profile2)
            val result = triggerService.onProfileUpdated(profile2, profile3)

            // Then - Should only trigger once for the final update
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase, times(1)).recalculateGoalsForProfileUpdate(testUserId, profile3)
        }

        @Test
        @DisplayName("Should handle debouncing timeout correctly")
        fun shouldHandleDebouncingTimeoutCorrectly() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(weightInKg = 75.0)
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)
            
            // Simulate waiting for debounce timeout
            Thread.sleep(1100) // Wait longer than debounce period

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("Should handle calculation failure gracefully")
        fun shouldHandleCalculationFailureGracefully() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(weightInKg = 75.0)
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Error(
                    error = GoalCalculationError.CalculationFailed(RuntimeException("Test error"))
                ))

            // When
            val result = triggerService.onProfileUpdated(baseProfile, updatedProfile)

            // Then
            assertFalse(result.isSuccess)
            assertTrue(result.exceptionOrNull() is RuntimeException)
        }

        @Test
        @DisplayName("Should handle null profiles gracefully")
        fun shouldHandleNullProfilesGracefully() = runTest {
            // When
            val result = triggerService.onProfileUpdated(null, baseProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase, never()).recalculateGoalsForProfileUpdate(any(), any())
        }

        @Test
        @DisplayName("Should handle same profile gracefully")
        fun shouldHandleSameProfileGracefully() = runTest {
            // When
            val result = triggerService.onProfileUpdated(baseProfile, baseProfile)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase, never()).recalculateGoalsForProfileUpdate(any(), any())
        }
    }

    @Nested
    @DisplayName("Manual Triggers")
    inner class ManualTriggers {

        @Test
        @DisplayName("Should handle manual recalculation trigger")
        fun shouldHandleManualRecalculationTrigger() = runTest {
            // Given
            whenever(goalCalculationUseCase.calculateAndStoreGoals(testUserId, forceRecalculation = true))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When
            val result = triggerService.triggerManualRecalculation(testUserId)

            // Then
            assertTrue(result.isSuccess)
            verify(goalCalculationUseCase).calculateAndStoreGoals(testUserId, forceRecalculation = true)
        }

        @Test
        @DisplayName("Should handle manual trigger failure")
        fun shouldHandleManualTriggerFailure() = runTest {
            // Given
            whenever(goalCalculationUseCase.calculateAndStoreGoals(testUserId, forceRecalculation = true))
                .thenReturn(GoalCalculationResult.Error(
                    error = GoalCalculationError.ProfileNotFound(testUserId)
                ))

            // When
            val result = triggerService.triggerManualRecalculation(testUserId)

            // Then
            assertFalse(result.isSuccess)
        }
    }

    @Nested
    @DisplayName("Calculation History")
    inner class CalculationHistory {

        @Test
        @DisplayName("Should track calculation triggers")
        fun shouldTrackCalculationTriggers() = runTest {
            // Given
            val updatedProfile = baseProfile.copy(weightInKg = 75.0)
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When
            triggerService.onProfileUpdated(baseProfile, updatedProfile)
            val history = triggerService.getCalculationHistory(testUserId)

            // Then
            assertTrue(history.isNotEmpty())
            assertTrue(history.any { it.trigger == "profile_update" })
        }

        @Test
        @DisplayName("Should limit calculation history size")
        fun shouldLimitCalculationHistorySize() = runTest {
            // Given
            val maxHistorySize = 10
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(any(), any()))
                .thenReturn(GoalCalculationResult.Success(
                    goals = createTestGoals(),
                    wasRecalculated = true,
                    calculationSource = CalculationSource.WHO_STANDARD
                ))

            // When - Trigger more calculations than max history size
            repeat(15) { index ->
                val updatedProfile = baseProfile.copy(weightInKg = 70.0 + index)
                triggerService.onProfileUpdated(baseProfile, updatedProfile)
            }

            val history = triggerService.getCalculationHistory(testUserId)

            // Then
            assertTrue(history.size <= maxHistorySize)
        }
    }

    private fun createTestGoals() = DailyGoals(
        userId = testUserId,
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = System.currentTimeMillis(),
        calculationSource = CalculationSource.WHO_STANDARD
    )
}