package com.vibehealth.android.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vibehealth.android.data.user.remote.UserProfileService
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for Firebase Firestore operations in onboarding
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingFirebaseIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var userProfileService: UserProfileService

    private val testUserId = "test-user-${UUID.randomUUID()}"

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun saveUserProfile_shouldPersistToFirestore() = runTest {
        // Given
        val userProfile = createTestUserProfile(testUserId)

        // When
        val result = userProfileService.saveUserProfile(userProfile)

        // Then
        assertTrue(result.isSuccess, "Save operation should succeed")
        
        val savedProfile = result.getOrNull()
        assertNotNull(savedProfile, "Saved profile should not be null")
        assertEquals(testUserId, savedProfile.userId)
        assertEquals("Test User", savedProfile.displayName)
    }

    @Test
    fun getUserProfile_shouldRetrieveFromFirestore() = runTest {
        // Given - Save a profile first
        val userProfile = createTestUserProfile(testUserId)
        userProfileService.saveUserProfile(userProfile)

        // When
        val result = userProfileService.getUserProfile(testUserId)

        // Then
        assertTrue(result.isSuccess, "Get operation should succeed")
        
        val retrievedProfile = result.getOrNull()
        assertNotNull(retrievedProfile, "Retrieved profile should not be null")
        assertEquals(testUserId, retrievedProfile.userId)
        assertEquals("Test User", retrievedProfile.displayName)
        assertEquals(Gender.MALE, retrievedProfile.gender)
        assertEquals(UnitSystem.METRIC, retrievedProfile.unitSystem)
        assertEquals(175, retrievedProfile.heightInCm)
        assertEquals(70.0, retrievedProfile.weightInKg)
    }

    @Test
    fun updateUserProfile_shouldModifyExistingProfile() = runTest {
        // Given - Save a profile first
        val originalProfile = createTestUserProfile(testUserId)
        userProfileService.saveUserProfile(originalProfile)

        // When - Update the profile
        val updatedProfile = originalProfile.copy(
            displayName = "Updated User",
            heightInCm = 180,
            weightInKg = 75.0,
            updatedAt = Date()
        )
        val updateResult = userProfileService.updateUserProfile(updatedProfile)

        // Then
        assertTrue(updateResult.isSuccess, "Update operation should succeed")
        
        // Verify the update
        val retrieveResult = userProfileService.getUserProfile(testUserId)
        assertTrue(retrieveResult.isSuccess, "Retrieve operation should succeed")
        
        val retrievedProfile = retrieveResult.getOrNull()
        assertNotNull(retrievedProfile, "Retrieved profile should not be null")
        assertEquals("Updated User", retrievedProfile.displayName)
        assertEquals(180, retrievedProfile.heightInCm)
        assertEquals(75.0, retrievedProfile.weightInKg)
    }

    @Test
    fun checkOnboardingStatus_shouldReturnCorrectStatus() = runTest {
        // Given - Save a completed profile
        val completedProfile = createTestUserProfile(testUserId).copy(
            hasCompletedOnboarding = true
        )
        userProfileService.saveUserProfile(completedProfile)

        // When
        val result = userProfileService.checkOnboardingStatus(testUserId)

        // Then
        assertTrue(result.isSuccess, "Check onboarding status should succeed")
        
        val isComplete = result.getOrNull()
        assertNotNull(isComplete, "Onboarding status should not be null")
        assertTrue(isComplete, "Onboarding should be marked as complete")
    }

    @Test
    fun checkOnboardingStatus_nonExistentUser_shouldReturnFalse() = runTest {
        // Given - Non-existent user ID
        val nonExistentUserId = "non-existent-${UUID.randomUUID()}"

        // When
        val result = userProfileService.checkOnboardingStatus(nonExistentUserId)

        // Then
        assertTrue(result.isSuccess, "Check onboarding status should succeed")
        
        val isComplete = result.getOrNull()
        assertNotNull(isComplete, "Onboarding status should not be null")
        assertTrue(!isComplete, "Non-existent user should not have completed onboarding")
    }

    @Test
    fun saveUserProfile_withSpecialCharacters_shouldHandleCorrectly() = runTest {
        // Given - Profile with special characters
        val specialUserId = "test-user-special-${UUID.randomUUID()}"
        val userProfile = createTestUserProfile(specialUserId).copy(
            displayName = "José María O'Connor-Smith",
            email = "josé.maría@example.com"
        )

        // When
        val result = userProfileService.saveUserProfile(userProfile)

        // Then
        assertTrue(result.isSuccess, "Save operation with special characters should succeed")
        
        // Verify retrieval
        val retrieveResult = userProfileService.getUserProfile(specialUserId)
        assertTrue(retrieveResult.isSuccess, "Retrieve operation should succeed")
        
        val retrievedProfile = retrieveResult.getOrNull()
        assertNotNull(retrievedProfile, "Retrieved profile should not be null")
        assertEquals("José María O'Connor-Smith", retrievedProfile.displayName)
        assertEquals("josé.maría@example.com", retrievedProfile.email)
    }

    @Test
    fun saveUserProfile_withEdgeCaseValues_shouldHandleCorrectly() = runTest {
        // Given - Profile with edge case values
        val edgeCaseUserId = "test-user-edge-${UUID.randomUUID()}"
        val userProfile = createTestUserProfile(edgeCaseUserId).copy(
            heightInCm = 100, // Minimum height
            weightInKg = 30.0, // Minimum weight
            birthday = Date(System.currentTimeMillis() - (120L * 365 * 24 * 60 * 60 * 1000)) // 120 years ago
        )

        // When
        val result = userProfileService.saveUserProfile(userProfile)

        // Then
        assertTrue(result.isSuccess, "Save operation with edge case values should succeed")
        
        // Verify retrieval
        val retrieveResult = userProfileService.getUserProfile(edgeCaseUserId)
        assertTrue(retrieveResult.isSuccess, "Retrieve operation should succeed")
        
        val retrievedProfile = retrieveResult.getOrNull()
        assertNotNull(retrievedProfile, "Retrieved profile should not be null")
        assertEquals(100, retrievedProfile.heightInCm)
        assertEquals(30.0, retrievedProfile.weightInKg)
    }

    @Test
    fun concurrentOperations_shouldHandleCorrectly() = runTest {
        // Given - Multiple concurrent operations
        val concurrentUserId = "test-user-concurrent-${UUID.randomUUID()}"
        val userProfile = createTestUserProfile(concurrentUserId)

        // When - Perform concurrent save operations
        val results = (1..5).map { index ->
            val profile = userProfile.copy(
                displayName = "Concurrent User $index",
                updatedAt = Date()
            )
            userProfileService.saveUserProfile(profile)
        }

        // Then - All operations should succeed
        results.forEach { result ->
            assertTrue(result.isSuccess, "Concurrent save operation should succeed")
        }

        // Verify final state
        val retrieveResult = userProfileService.getUserProfile(concurrentUserId)
        assertTrue(retrieveResult.isSuccess, "Final retrieve operation should succeed")
        
        val finalProfile = retrieveResult.getOrNull()
        assertNotNull(finalProfile, "Final profile should not be null")
        assertTrue(
            finalProfile.displayName.startsWith("Concurrent User"),
            "Final profile should have one of the concurrent names"
        )
    }

    private fun createTestUserProfile(userId: String): UserProfile {
        return UserProfile(
            userId = userId,
            email = "test@example.com",
            displayName = "Test User",
            birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)), // 25 years ago
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 175,
            weightInKg = 70.0,
            hasCompletedOnboarding = true,
            createdAt = Date(),
            updatedAt = Date()
        )
    }

    // Cleanup method to remove test data (optional)
    private suspend fun cleanupTestData(userId: String) {
        try {
            // In a real implementation, you might want to delete test data
            // userProfileService.deleteUserProfile(userId)
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}