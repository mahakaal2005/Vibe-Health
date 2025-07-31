package com.vibehealth.android.domain.goals

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
import java.util.Date

/**
 * Comprehensive unit tests for ProfileUpdateUseCase.
 * 
 * Tests cover profile updates, goal recalculation triggers, change detection,
 * and concurrent update handling as specified in Task 3.2 requirements.
 */
class ProfileUpdateUseCaseTest {

    @Mock
    private lateinit var userProfileRepository: UserProfileRepository
    
    @Mock
    private lateinit var goalCalculationUseCase: GoalCalculationUseCase
    
    @Mock
    private lateinit var goalRecalculationTriggerService: GoalRecalculationTriggerService
    
    private lateinit var profileUpdateUseCase: ProfileUpdateUseCase
    
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
        profileUpdateUseCase = ProfileUpdateUseCase(
            userProfileRepository,
            goalCalculationUseCase,
            goalRecalculationTriggerService
        )
    }

    @Nested
    @DisplayName("Profile Update with Goal Recalculation")
    inner class ProfileUpdateWithGoalRecalculation {

        @Test
        @DisplayName("Should update profile and trigger goal recalculation for goal-affecting changes")
        fun shouldUpdateProfileAndTriggerGoalRecalculationForGoalAffectingChanges() = runTest {
            // Given
            val currentProfile = testUserProfile
            val updatedProfile = testUserProfile.copy(weightInKg = 75.0) // Weight change affects goals
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(currentProfile))
            whenever(userProfileRepository.updateUserProfile(updatedProfile)).thenReturn(Result.success(updatedProfile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as ProfileUpdateResult.Success
            assertEquals(updatedProfile, successResult.updatedProfile)
            assertNotNull(successResult.goalRecalculationResult)
            assertTrue(successResult.changesSummary.shouldRecalculate)
            assertTrue(successResult.changesSummary.hasGoalAffectingChanges())
            
            verify(userProfileRepository).updateUserProfile(updatedProfile)
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
            verify(goalRecalculationTriggerService).onProfileUpdated(currentProfile, updatedProfile)
        }

        @Test
        @DisplayName("Should update profile without goal recalculation for non-affecting changes")
        fun shouldUpdateProfileWithoutGoalRecalculationForNonAffectingChanges() = runTest {
            // Given
            val currentProfile = testUserProfile
            val updatedProfile = testUserProfile.copy(displayName = "Updated Name") // Name change doesn't affect goals
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(currentProfile))
            whenever(userProfileRepository.updateUserProfile(updatedProfile)).thenReturn(Result.success(updatedProfile))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as ProfileUpdateResult.Success
            assertEquals(updatedProfile, successResult.updatedProfile)
            assertNull(successResult.goalRecalculationResult)
            assertFalse(successResult.changesSummary.shouldRecalculate)
            assertFalse(successResult.changesSummary.hasGoalAffectingChanges())
            
            verify(userProfileRepository).updateUserProfile(updatedProfile)
            verify(goalCalculationUseCase, never()).recalculateGoalsForProfileUpdate(any(), any())
        }

        @Test
        @DisplayName("Should force goal recalculation when requested")
        fun shouldForceGoalRecalculationWhenRequested() = runTest {
            // Given
            val currentProfile = testUserProfile
            val updatedProfile = testUserProfile.copy(displayName = "Updated Name") // Non-affecting change
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(currentProfile))
            whenever(userProfileRepository.updateUserProfile(updatedProfile)).thenReturn(Result.success(updatedProfile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile, forceGoalRecalculation = true)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as ProfileUpdateResult.Success
            assertNotNull(successResult.goalRecalculationResult)
            
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, updatedProfile)
        }

        @Test
        @DisplayName("Should handle profile update failure")
        fun shouldHandleProfileUpdateFailure() = runTest {
            // Given
            val updatedProfile = testUserProfile.copy(weightInKg = 75.0)
            val exception = RuntimeException("Database error")
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(testUserProfile))
            whenever(userProfileRepository.updateUserProfile(updatedProfile)).thenReturn(Result.failure(exception))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as ProfileUpdateResult.Error
            assertTrue(errorResult.error is ProfileUpdateError.ProfileUpdateFailed)
            
            verify(goalCalculationUseCase, never()).recalculateGoalsForProfileUpdate(any(), any())
        }

        @Test
        @DisplayName("Should continue with success even if goal recalculation fails")
        fun shouldContinueWithSuccessEvenIfGoalRecalculationFails() = runTest {
            // Given
            val currentProfile = testUserProfile
            val updatedProfile = testUserProfile.copy(weightInKg = 75.0)
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(currentProfile))
            whenever(userProfileRepository.updateUserProfile(updatedProfile)).thenReturn(Result.success(updatedProfile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Error(GoalCalculationError.CalculationFailed(testUserId), "Calculation failed"))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)
            
            // Then
            assertTrue(result.isSuccess()) // Should still succeed
            val successResult = result as ProfileUpdateResult.Success
            assertEquals(updatedProfile, successResult.updatedProfile)
            assertNotNull(successResult.goalRecalculationResult)
            assertTrue(successResult.goalRecalculationResult is GoalCalculationResult.Error)
        }

        @Test
        @DisplayName("Should handle new profile creation")
        fun shouldHandleNewProfileCreation() = runTest {
            // Given
            val newProfile = testUserProfile
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(null)) // No existing profile
            whenever(userProfileRepository.updateUserProfile(newProfile)).thenReturn(Result.success(newProfile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, newProfile))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(newProfile)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as ProfileUpdateResult.Success
            assertEquals("New profile created", successResult.changesSummary.reason)
            assertTrue(successResult.changesSummary.shouldRecalculate)
            
            verify(goalCalculationUseCase).recalculateGoalsForProfileUpdate(testUserId, newProfile)
        }
    }

    @Nested
    @DisplayName("Partial Profile Updates")
    inner class PartialProfileUpdates {

        @Test
        @DisplayName("Should handle partial profile update correctly")
        fun shouldHandlePartialProfileUpdateCorrectly() = runTest {
            // Given
            val currentProfile = testUserProfile
            val partialUpdate = mapOf(
                "weightInKg" to 75.0,
                "displayName" to "Updated Name"
            )
            val expectedUpdatedProfile = currentProfile.copy(
                weightInKg = 75.0,
                displayName = "Updated Name",
                updatedAt = any()
            )
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(currentProfile))
            whenever(userProfileRepository.updateUserProfile(any())).thenReturn(Result.success(expectedUpdatedProfile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(eq(testUserId), any()))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // When
            val result = profileUpdateUseCase.updateProfilePartially(testUserId, partialUpdate)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as ProfileUpdateResult.Success
            assertEquals(75.0, successResult.updatedProfile.weightInKg)
            assertEquals("Updated Name", successResult.updatedProfile.displayName)
            assertTrue(successResult.changesSummary.shouldRecalculate) // Weight change affects goals
            
            verify(userProfileRepository).updateUserProfile(any())
        }

        @Test
        @DisplayName("Should handle partial update with unknown fields")
        fun shouldHandlePartialUpdateWithUnknownFields() = runTest {
            // Given
            val currentProfile = testUserProfile
            val partialUpdate = mapOf(
                "weightInKg" to 75.0,
                "unknownField" to "unknown value"
            )
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(currentProfile))
            whenever(userProfileRepository.updateUserProfile(any())).thenReturn(Result.success(currentProfile.copy(weightInKg = 75.0)))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(eq(testUserId), any()))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // When
            val result = profileUpdateUseCase.updateProfilePartially(testUserId, partialUpdate)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as ProfileUpdateResult.Success
            assertEquals(75.0, successResult.updatedProfile.weightInKg)
            // Unknown field should be ignored
        }

        @Test
        @DisplayName("Should handle partial update when current profile not found")
        fun shouldHandlePartialUpdateWhenCurrentProfileNotFound() = runTest {
            // Given
            val partialUpdate = mapOf("weightInKg" to 75.0)
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(null))
            
            // When
            val result = profileUpdateUseCase.updateProfilePartially(testUserId, partialUpdate)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as ProfileUpdateResult.Error
            assertTrue(errorResult.error is ProfileUpdateError.ProfileNotFound)
        }
    }

    @Nested
    @DisplayName("Change Detection")
    inner class ChangeDetection {

        @Test
        @DisplayName("Should detect goal-affecting field changes")
        fun shouldDetectGoalAffectingFieldChanges() = runTest {
            // Given
            val currentProfile = testUserProfile
            val updatedProfile = testUserProfile.copy(
                weightInKg = 75.0,
                heightInCm = 180,
                displayName = "Updated Name" // Non-affecting change
            )
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(currentProfile))
            whenever(userProfileRepository.updateUserProfile(updatedProfile)).thenReturn(Result.success(updatedProfile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as ProfileUpdateResult.Success
            val changesSummary = successResult.changesSummary
            
            assertTrue(changesSummary.shouldRecalculate)
            assertTrue(changesSummary.hasGoalAffectingChanges())
            assertTrue(changesSummary.changedFields.contains("weightInKg"))
            assertTrue(changesSummary.changedFields.contains("heightInCm"))
            assertFalse(changesSummary.changedFields.contains("displayName")) // Only goal-affecting fields tracked
        }

        @Test
        @DisplayName("Should detect profile validity changes")
        fun shouldDetectProfileValidityChanges() = runTest {
            // Given - Invalid profile becomes valid
            val invalidProfile = testUserProfile.copy(birthday = null) // Invalid
            val validProfile = testUserProfile // Valid
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(invalidProfile))
            whenever(userProfileRepository.updateUserProfile(validProfile)).thenReturn(Result.success(validProfile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, validProfile))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(validProfile)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as ProfileUpdateResult.Success
            val changesSummary = successResult.changesSummary
            
            assertTrue(changesSummary.shouldRecalculate)
            assertFalse(changesSummary.wasValidBefore)
            assertTrue(changesSummary.isValidAfter)
            assertEquals("Profile became valid for goal calculation", changesSummary.reason)
        }

        @Test
        @DisplayName("Should not recalculate when profile becomes invalid")
        fun shouldNotRecalculateWhenProfileBecomesInvalid() = runTest {
            // Given - Valid profile becomes invalid
            val validProfile = testUserProfile
            val invalidProfile = testUserProfile.copy(birthday = null) // Invalid
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(validProfile))
            whenever(userProfileRepository.updateUserProfile(invalidProfile)).thenReturn(Result.success(invalidProfile))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(invalidProfile)
            
            // Then
            assertTrue(result.isSuccess())
            val successResult = result as ProfileUpdateResult.Success
            val changesSummary = successResult.changesSummary
            
            assertFalse(changesSummary.shouldRecalculate)
            assertTrue(changesSummary.wasValidBefore)
            assertFalse(changesSummary.isValidAfter)
            assertEquals("Profile became invalid for goal calculation", changesSummary.reason)
            
            verify(goalCalculationUseCase, never()).recalculateGoalsForProfileUpdate(any(), any())
        }
    }

    @Nested
    @DisplayName("Concurrent Update Handling")
    inner class ConcurrentUpdateHandling {

        @Test
        @DisplayName("Should prevent concurrent updates for same user")
        fun shouldPreventConcurrentUpdatesForSameUser() = runTest {
            // Given
            val updatedProfile = testUserProfile.copy(weightInKg = 75.0)
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(testUserProfile))
            whenever(userProfileRepository.updateUserProfile(updatedProfile)).thenReturn(Result.success(updatedProfile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(testUserId, updatedProfile))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // Start first update (will be ongoing)
            val firstUpdateJob = kotlinx.coroutines.async {
                profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)
            }
            
            // Give first update time to start
            kotlinx.coroutines.delay(10)
            
            // When - Try second concurrent update
            val secondResult = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)
            
            // Then
            assertTrue(secondResult.isError())
            val errorResult = secondResult as ProfileUpdateResult.Error
            assertTrue(errorResult.error is ProfileUpdateError.ConcurrentUpdate)
            
            // Wait for first update to complete
            val firstResult = firstUpdateJob.await()
            assertTrue(firstResult.isSuccess())
        }

        @Test
        @DisplayName("Should allow concurrent updates for different users")
        fun shouldAllowConcurrentUpdatesForDifferentUsers() = runTest {
            // Given
            val user1Profile = testUserProfile.copy(userId = "user-1")
            val user2Profile = testUserProfile.copy(userId = "user-2")
            
            whenever(userProfileRepository.getUserProfile("user-1")).thenReturn(Result.success(user1Profile))
            whenever(userProfileRepository.getUserProfile("user-2")).thenReturn(Result.success(user2Profile))
            whenever(userProfileRepository.updateUserProfile(any())).thenReturn(Result.success(user1Profile), Result.success(user2Profile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(any(), any()))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // When - Concurrent updates for different users
            val update1Job = kotlinx.coroutines.async {
                profileUpdateUseCase.updateProfileWithGoalRecalculation(user1Profile.copy(weightInKg = 75.0))
            }
            val update2Job = kotlinx.coroutines.async {
                profileUpdateUseCase.updateProfileWithGoalRecalculation(user2Profile.copy(weightInKg = 80.0))
            }
            
            // Then
            val result1 = update1Job.await()
            val result2 = update2Job.await()
            
            assertTrue(result1.isSuccess())
            assertTrue(result2.isSuccess())
        }

        @Test
        @DisplayName("Should track ongoing updates correctly")
        fun shouldTrackOngoingUpdatesCorrectly() = runTest {
            // Given
            assertEquals(0, profileUpdateUseCase.getOngoingUpdatesCount())
            assertFalse(profileUpdateUseCase.isUpdateInProgress(testUserId))
            
            whenever(userProfileRepository.getUserProfile(testUserId)).thenReturn(Result.success(testUserProfile))
            whenever(userProfileRepository.updateUserProfile(any())).thenReturn(Result.success(testUserProfile))
            whenever(goalCalculationUseCase.recalculateGoalsForProfileUpdate(any(), any()))
                .thenReturn(GoalCalculationResult.Success(testGoals, true, CalculationSource.WHO_STANDARD))
            
            // When - Start update
            val updateJob = kotlinx.coroutines.async {
                profileUpdateUseCase.updateProfileWithGoalRecalculation(testUserProfile.copy(weightInKg = 75.0))
            }
            
            // Give update time to start
            kotlinx.coroutines.delay(10)
            
            // Then - Should track ongoing update
            assertTrue(profileUpdateUseCase.isUpdateInProgress(testUserId))
            assertEquals(1, profileUpdateUseCase.getOngoingUpdatesCount())
            
            // Wait for completion
            val result = updateJob.await()
            assertTrue(result.isSuccess())
            
            // Should clean up after completion
            assertFalse(profileUpdateUseCase.isUpdateInProgress(testUserId))
            assertEquals(0, profileUpdateUseCase.getOngoingUpdatesCount())
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("Should handle unexpected exceptions gracefully")
        fun shouldHandleUnexpectedExceptionsGracefully() = runTest {
            // Given
            val updatedProfile = testUserProfile.copy(weightInKg = 75.0)
            
            whenever(userProfileRepository.getUserProfile(testUserId))
                .thenThrow(RuntimeException("Unexpected error"))
            
            // When
            val result = profileUpdateUseCase.updateProfileWithGoalRecalculation(updatedProfile)
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as ProfileUpdateResult.Error
            assertTrue(errorResult.error is ProfileUpdateError.UnexpectedError)
            
            // Should clean up ongoing updates tracking
            assertFalse(profileUpdateUseCase.isUpdateInProgress(testUserId))
        }

        @Test
        @DisplayName("Should handle repository failure gracefully")
        fun shouldHandleRepositoryFailureGracefully() = runTest {
            // Given
            val updatedProfile = testUserProfile.copy(weightInKg = 75.0)
            
            whenever(userProfileRepository.getUserProfile(testUserId))
                .thenReturn(Result.failure(RuntimeException("Repository error")))
            
            // When
            val result = profileUpdateUseCase.updateProfilePartially(testUserId, mapOf("weightInKg" to 75.0))
            
            // Then
            assertTrue(result.isError())
            val errorResult = result as ProfileUpdateResult.Error
            assertTrue(errorResult.error is ProfileUpdateError.ProfileNotFound)
        }
    }
}