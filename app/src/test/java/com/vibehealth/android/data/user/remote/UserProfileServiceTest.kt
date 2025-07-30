package com.vibehealth.android.data.user.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserProfileService with mocked Firebase dependencies
 */
@RunWith(AndroidJUnit4::class)
class UserProfileServiceTest {

    private lateinit var userProfileService: UserProfileService
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocument: DocumentReference
    private lateinit var mockDocumentSnapshot: DocumentSnapshot
    private lateinit var mockQuerySnapshot: QuerySnapshot
    private lateinit var mockTask: Task<DocumentSnapshot>
    private lateinit var mockVoidTask: Task<Void>
    private lateinit var mockQueryTask: Task<QuerySnapshot>

    private val testUserProfile = UserProfile(
        userId = "test_user_123",
        email = "test@example.com",
        displayName = "Test User",
        firstName = "Test",
        lastName = "User",
        birthday = Date(System.currentTimeMillis() - 25 * 365 * 24 * 60 * 60 * 1000L),
        gender = Gender.MALE,
        unitSystem = UnitSystem.METRIC,
        heightInCm = 175,
        weightInKg = 70.5,
        hasCompletedOnboarding = true,
        createdAt = Date(),
        updatedAt = Date()
    )

    @Before
    fun setup() {
        mockFirestore = mockk()
        mockCollection = mockk()
        mockDocument = mockk()
        mockDocumentSnapshot = mockk()
        mockQuerySnapshot = mockk()
        mockTask = mockk()
        mockVoidTask = mockk()
        mockQueryTask = mockk()

        // Setup Firebase mocks
        every { mockFirestore.collection("users") } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument

        userProfileService = UserProfileService()
        
        // Use reflection to set the mocked firestore instance
        val firestoreField = UserProfileService::class.java.getDeclaredField("firestore")
        firestoreField.isAccessible = true
        firestoreField.set(userProfileService, mockFirestore)
    }

    @Test
    fun saveUserProfile_successful_shouldReturnSuccess() = runTest {
        // Given
        every { mockDocument.set(any()) } returns mockVoidTask
        every { mockVoidTask.isSuccessful } returns true
        every { mockVoidTask.exception } returns null
        coEvery { mockVoidTask.await() } returns mockk()

        // When
        val result = userProfileService.saveUserProfile(testUserProfile)

        // Then
        assertTrue(result.isSuccess)
        val savedProfile = result.getOrNull()
        assertNotNull(savedProfile)
        assertEquals(testUserProfile.userId, savedProfile.userId)
        assertEquals(testUserProfile.email, savedProfile.email)
        
        verify { mockDocument.set(any()) }
    }

    @Test
    fun saveUserProfile_withFirebaseException_shouldReturnFailure() = runTest {
        // Given
        val exception = FirebaseFirestoreException("Network error", FirebaseFirestoreException.Code.UNAVAILABLE)
        every { mockDocument.set(any()) } returns mockVoidTask
        coEvery { mockVoidTask.await() } throws exception

        // When
        val result = userProfileService.saveUserProfile(testUserProfile)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun getUserProfile_existingUser_shouldReturnUserProfile() = runTest {
        // Given
        every { mockDocument.get() } returns mockTask
        every { mockTask.isSuccessful } returns true
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.toObject(UserProfile::class.java) } returns testUserProfile
        coEvery { mockTask.await() } returns mockDocumentSnapshot

        // When
        val result = userProfileService.getUserProfile(testUserProfile.userId)

        // Then
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(testUserProfile.userId, profile.userId)
        assertEquals(testUserProfile.email, profile.email)
        
        verify { mockDocument.get() }
    }

    @Test
    fun getUserProfile_nonExistentUser_shouldReturnNull() = runTest {
        // Given
        every { mockDocument.get() } returns mockTask
        every { mockTask.isSuccessful } returns true
        every { mockDocumentSnapshot.exists() } returns false
        coEvery { mockTask.await() } returns mockDocumentSnapshot

        // When
        val result = userProfileService.getUserProfile("non_existent_user")

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        
        verify { mockDocument.get() }
    }

    @Test
    fun getUserProfile_withFirebaseException_shouldReturnFailure() = runTest {
        // Given
        val exception = FirebaseFirestoreException("Network error", FirebaseFirestoreException.Code.UNAVAILABLE)
        every { mockDocument.get() } returns mockTask
        coEvery { mockTask.await() } throws exception

        // When
        val result = userProfileService.getUserProfile(testUserProfile.userId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun updateUserProfile_successful_shouldReturnUpdatedProfile() = runTest {
        // Given
        every { mockDocument.set(any()) } returns mockVoidTask
        every { mockVoidTask.isSuccessful } returns true
        coEvery { mockVoidTask.await() } returns mockk()

        // When
        val result = userProfileService.updateUserProfile(testUserProfile)

        // Then
        assertTrue(result.isSuccess)
        val updatedProfile = result.getOrNull()
        assertNotNull(updatedProfile)
        assertEquals(testUserProfile.userId, updatedProfile.userId)
        assertTrue(updatedProfile.updatedAt.after(testUserProfile.updatedAt) || 
                  updatedProfile.updatedAt == testUserProfile.updatedAt)
        
        verify { mockDocument.set(any()) }
    }

    @Test
    fun checkOnboardingStatus_completedUser_shouldReturnTrue() = runTest {
        // Given
        every { mockDocument.get() } returns mockTask
        every { mockTask.isSuccessful } returns true
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.getBoolean("hasCompletedOnboarding") } returns true
        coEvery { mockTask.await() } returns mockDocumentSnapshot

        // When
        val result = userProfileService.checkOnboardingStatus(testUserProfile.userId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false)
        
        verify { mockDocument.get() }
    }

    @Test
    fun checkOnboardingStatus_incompleteUser_shouldReturnFalse() = runTest {
        // Given
        every { mockDocument.get() } returns mockTask
        every { mockTask.isSuccessful } returns true
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.getBoolean("hasCompletedOnboarding") } returns false
        coEvery { mockTask.await() } returns mockDocumentSnapshot

        // When
        val result = userProfileService.checkOnboardingStatus(testUserProfile.userId)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun checkOnboardingStatus_nonExistentUser_shouldReturnFalse() = runTest {
        // Given
        every { mockDocument.get() } returns mockTask
        every { mockTask.isSuccessful } returns true
        every { mockDocumentSnapshot.exists() } returns false
        coEvery { mockTask.await() } returns mockDocumentSnapshot

        // When
        val result = userProfileService.checkOnboardingStatus("non_existent_user")

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun deleteUserProfile_successful_shouldReturnSuccess() = runTest {
        // Given
        every { mockDocument.delete() } returns mockVoidTask
        every { mockVoidTask.isSuccessful } returns true
        coEvery { mockVoidTask.await() } returns mockk()

        // When
        val result = userProfileService.deleteUserProfile(testUserProfile.userId)

        // Then
        assertTrue(result.isSuccess)
        
        verify { mockDocument.delete() }
    }

    @Test
    fun batchUpdateUserProfiles_successful_shouldReturnUpdatedProfiles() = runTest {
        // Given
        val profiles = listOf(
            testUserProfile.copy(userId = "user1"),
            testUserProfile.copy(userId = "user2")
        )
        
        val mockBatch = mockk<WriteBatch>()
        every { mockFirestore.batch() } returns mockBatch
        every { mockBatch.set(any(), any()) } returns mockBatch
        every { mockBatch.commit() } returns mockVoidTask
        every { mockVoidTask.isSuccessful } returns true
        coEvery { mockVoidTask.await() } returns mockk()

        // When
        val result = userProfileService.batchUpdateUserProfiles(profiles)

        // Then
        assertTrue(result.isSuccess)
        val updatedProfiles = result.getOrNull()
        assertNotNull(updatedProfiles)
        assertEquals(2, updatedProfiles.size)
        assertEquals("user1", updatedProfiles[0].userId)
        assertEquals("user2", updatedProfiles[1].userId)
        
        verify { mockFirestore.batch() }
        verify(exactly = 2) { mockBatch.set(any(), any()) }
        verify { mockBatch.commit() }
    }

    @Test
    fun getUserProfilesUpdatedAfter_shouldReturnFilteredProfiles() = runTest {
        // Given
        val timestamp = System.currentTimeMillis() - 60000L // 1 minute ago
        val mockQuery = mockk<Query>()
        val mockDocuments = listOf(mockDocumentSnapshot)
        
        every { mockCollection.whereGreaterThan("updatedAt", any<Date>()) } returns mockQuery
        every { mockQuery.get() } returns mockQueryTask
        every { mockQueryTask.isSuccessful } returns true
        every { mockQuerySnapshot.documents } returns mockDocuments
        every { mockDocumentSnapshot.toObject(UserProfile::class.java) } returns testUserProfile
        coEvery { mockQueryTask.await() } returns mockQuerySnapshot

        // When
        val result = userProfileService.getUserProfilesUpdatedAfter(timestamp)

        // Then
        assertTrue(result.isSuccess)
        val profiles = result.getOrNull()
        assertNotNull(profiles)
        assertEquals(1, profiles.size)
        assertEquals(testUserProfile.userId, profiles[0].userId)
        
        verify { mockCollection.whereGreaterThan("updatedAt", any<Date>()) }
        verify { mockQuery.get() }
    }

    @Test
    fun isFirestoreAvailable_withSuccessfulQuery_shouldReturnTrue() = runTest {
        // Given
        val mockQuery = mockk<Query>()
        every { mockCollection.limit(1) } returns mockQuery
        every { mockQuery.get() } returns mockQueryTask
        every { mockQueryTask.isSuccessful } returns true
        coEvery { mockQueryTask.await() } returns mockQuerySnapshot

        // When
        val result = userProfileService.isFirestoreAvailable()

        // Then
        assertTrue(result)
        
        verify { mockCollection.limit(1) }
        verify { mockQuery.get() }
    }

    @Test
    fun isFirestoreAvailable_withException_shouldReturnFalse() = runTest {
        // Given
        val mockQuery = mockk<Query>()
        every { mockCollection.limit(1) } returns mockQuery
        every { mockQuery.get() } returns mockQueryTask
        coEvery { mockQueryTask.await() } throws Exception("Network error")

        // When
        val result = userProfileService.isFirestoreAvailable()

        // Then
        assertFalse(result)
    }

    @Test
    fun saveUserProfile_withRetryLogic_shouldEventuallySucceed() = runTest {
        // Given
        var attemptCount = 0
        every { mockDocument.set(any()) } returns mockVoidTask
        coEvery { mockVoidTask.await() } answers {
            attemptCount++
            if (attemptCount < 2) {
                throw FirebaseFirestoreException("Temporary error", FirebaseFirestoreException.Code.UNAVAILABLE)
            } else {
                mockk()
            }
        }

        // When
        val result = userProfileService.saveUserProfile(testUserProfile)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(attemptCount >= 2) // Should have retried at least once
        
        verify(atLeast = 2) { mockDocument.set(any()) }
    }

    @Test
    fun saveUserProfile_withMaxRetriesExceeded_shouldReturnFailure() = runTest {
        // Given
        val exception = FirebaseFirestoreException("Persistent error", FirebaseFirestoreException.Code.UNAVAILABLE)
        every { mockDocument.set(any()) } returns mockVoidTask
        coEvery { mockVoidTask.await() } throws exception

        // When
        val result = userProfileService.saveUserProfile(testUserProfile)

        // Then
        assertTrue(result.isFailure)
        
        // Should have attempted 3 times (MAX_RETRY_ATTEMPTS)
        verify(exactly = 3) { mockDocument.set(any()) }
    }

    @Test
    fun saveUserProfile_shouldSanitizeData() = runTest {
        // Given
        val profileWithUnsafeData = testUserProfile.copy(
            displayName = "Test<script>alert('xss')</script>User",
            firstName = "Test<>",
            lastName = "User&amp;"
        )
        
        every { mockDocument.set(any()) } returns mockVoidTask
        every { mockVoidTask.isSuccessful } returns true
        coEvery { mockVoidTask.await() } returns mockk()

        // When
        val result = userProfileService.saveUserProfile(profileWithUnsafeData)

        // Then
        assertTrue(result.isSuccess)
        val savedProfile = result.getOrNull()
        assertNotNull(savedProfile)
        
        // Data should be sanitized
        assertFalse(savedProfile.displayName.contains("<script>"))
        assertFalse(savedProfile.firstName.contains("<>"))
        assertFalse(savedProfile.lastName.contains("&amp;"))
    }
}