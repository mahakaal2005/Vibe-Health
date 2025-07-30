package com.vibehealth.android.data.user

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.data.user.local.UserProfileDao
import com.vibehealth.android.data.user.local.UserProfileEntity
import com.vibehealth.android.data.user.remote.UserProfileService
import com.vibehealth.android.domain.onboarding.OnboardingResult
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.SyncStatus
import com.vibehealth.android.domain.user.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserProfileRepository with mocked dependencies
 */
class UserProfileRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: UserProfileRepository
    private lateinit var mockUserProfileDao: UserProfileDao
    private lateinit var mockUserProfileService: UserProfileService
    private lateinit var mockEncryptionHelper: EncryptionHelper
    private lateinit var mockContext: Context
    private lateinit var mockConnectivityManager: ConnectivityManager
    private lateinit var mockNetwork: Network
    private lateinit var mockNetworkCapabilities: NetworkCapabilities

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

    private val testUserProfileEntity = UserProfileEntity.fromDomainModel(testUserProfile)

    @Before
    fun setup() {
        mockUserProfileDao = mockk()
        mockUserProfileService = mockk()
        mockEncryptionHelper = mockk()
        mockContext = mockk()
        mockConnectivityManager = mockk()
        mockNetwork = mockk()
        mockNetworkCapabilities = mockk()

        // Setup context and connectivity manager mocks
        every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true

        repository = UserProfileRepository(
            mockUserProfileDao,
            mockUserProfileService,
            mockEncryptionHelper,
            mockContext
        )
    }

    @Test
    fun saveUserProfile_withNetworkAvailable_shouldSaveToLocalAndCloud() = runTest {
        // Given
        coEvery { mockUserProfileDao.upsertUserProfile(any()) } returns Unit
        coEvery { mockUserProfileService.saveUserProfile(testUserProfile) } returns Result.success(testUserProfile)
        coEvery { mockUserProfileDao.markAsSynced(testUserProfile.userId, any()) } returns 1

        // When
        val result = repository.saveUserProfile(testUserProfile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUserProfile, result.getOrNull())
        
        coVerify { mockUserProfileDao.upsertUserProfile(any()) }
        coVerify { mockUserProfileService.saveUserProfile(testUserProfile) }
        coVerify { mockUserProfileDao.markAsSynced(testUserProfile.userId, any()) }
    }

    @Test
    fun saveUserProfile_withNetworkUnavailable_shouldSaveToLocalOnly() = runTest {
        // Given
        every { mockConnectivityManager.activeNetwork } returns null
        coEvery { mockUserProfileDao.upsertUserProfile(any()) } returns Unit

        // When
        val result = repository.saveUserProfile(testUserProfile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUserProfile, result.getOrNull())
        
        coVerify { mockUserProfileDao.upsertUserProfile(any()) }
        coVerify(exactly = 0) { mockUserProfileService.saveUserProfile(any()) }
    }

    @Test
    fun saveUserProfile_withCloudSaveFailed_shouldStillSucceedLocally() = runTest {
        // Given
        coEvery { mockUserProfileDao.upsertUserProfile(any()) } returns Unit
        coEvery { mockUserProfileService.saveUserProfile(testUserProfile) } returns Result.failure(Exception("Cloud error"))

        // When
        val result = repository.saveUserProfile(testUserProfile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUserProfile, result.getOrNull())
        
        coVerify { mockUserProfileDao.upsertUserProfile(any()) }
        coVerify { mockUserProfileService.saveUserProfile(testUserProfile) }
        coVerify(exactly = 0) { mockUserProfileDao.markAsSynced(any(), any()) }
    }

    @Test
    fun getUserProfile_withLocalDataOnly_shouldReturnLocalData() = runTest {
        // Given
        every { mockConnectivityManager.activeNetwork } returns null
        coEvery { mockUserProfileDao.getUserProfile(testUserProfile.userId) } returns testUserProfileEntity

        // When
        val result = repository.getUserProfile(testUserProfile.userId)

        // Then
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(testUserProfile.userId, profile.userId)
        assertEquals(testUserProfile.email, profile.email)
        
        coVerify { mockUserProfileDao.getUserProfile(testUserProfile.userId) }
        coVerify(exactly = 0) { mockUserProfileService.getUserProfile(any()) }
    }

    @Test
    fun getUserProfile_withCloudDataNewer_shouldReturnCloudData() = runTest {
        // Given
        val olderLocalProfile = testUserProfile.copy(
            updatedAt = Date(System.currentTimeMillis() - 60000) // 1 minute ago
        )
        val newerCloudProfile = testUserProfile.copy(
            updatedAt = Date(System.currentTimeMillis() - 30000) // 30 seconds ago
        )
        
        val olderLocalEntity = UserProfileEntity.fromDomainModel(olderLocalProfile)
        
        coEvery { mockUserProfileDao.getUserProfile(testUserProfile.userId) } returns olderLocalEntity
        coEvery { mockUserProfileService.getUserProfile(testUserProfile.userId) } returns Result.success(newerCloudProfile)
        coEvery { mockUserProfileDao.upsertUserProfile(any()) } returns Unit
        coEvery { mockUserProfileDao.markAsSynced(testUserProfile.userId, any()) } returns 1

        // When
        val result = repository.getUserProfile(testUserProfile.userId)

        // Then
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(newerCloudProfile.updatedAt, profile.updatedAt)
        
        coVerify { mockUserProfileDao.upsertUserProfile(any()) }
        coVerify { mockUserProfileDao.markAsSynced(testUserProfile.userId, any()) }
    }

    @Test
    fun getUserProfile_withLocalDataNewer_shouldReturnLocalData() = runTest {
        // Given
        val newerLocalProfile = testUserProfile.copy(
            updatedAt = Date(System.currentTimeMillis() - 30000) // 30 seconds ago
        )
        val olderCloudProfile = testUserProfile.copy(
            updatedAt = Date(System.currentTimeMillis() - 60000) // 1 minute ago
        )
        
        val newerLocalEntity = UserProfileEntity.fromDomainModel(newerLocalProfile)
        
        coEvery { mockUserProfileDao.getUserProfile(testUserProfile.userId) } returns newerLocalEntity
        coEvery { mockUserProfileService.getUserProfile(testUserProfile.userId) } returns Result.success(olderCloudProfile)

        // When
        val result = repository.getUserProfile(testUserProfile.userId)

        // Then
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(newerLocalProfile.updatedAt, profile.updatedAt)
    }

    @Test
    fun updateUserProfile_shouldUpdateLocalAndCloud() = runTest {
        // Given
        coEvery { mockUserProfileDao.upsertUserProfile(any()) } returns Unit
        coEvery { mockUserProfileService.updateUserProfile(any()) } returns Result.success(testUserProfile)
        coEvery { mockUserProfileDao.markAsSynced(testUserProfile.userId, any()) } returns 1

        // When
        val result = repository.updateUserProfile(testUserProfile)

        // Then
        assertTrue(result.isSuccess)
        val updatedProfile = result.getOrNull()
        assertNotNull(updatedProfile)
        assertTrue(updatedProfile.updatedAt.after(testUserProfile.updatedAt) || 
                  updatedProfile.updatedAt == testUserProfile.updatedAt)
        
        coVerify { mockUserProfileDao.upsertUserProfile(any()) }
        coVerify { mockUserProfileService.updateUserProfile(any()) }
        coVerify { mockUserProfileDao.markAsSynced(testUserProfile.userId, any()) }
    }

    @Test
    fun isOnboardingComplete_withLocalTrue_shouldReturnTrue() = runTest {
        // Given
        coEvery { mockUserProfileDao.isOnboardingComplete(testUserProfile.userId) } returns true

        // When
        val result = repository.isOnboardingComplete(testUserProfile.userId)

        // Then
        assertTrue(result)
        
        coVerify { mockUserProfileDao.isOnboardingComplete(testUserProfile.userId) }
        coVerify(exactly = 0) { mockUserProfileService.checkOnboardingStatus(any()) }
    }

    @Test
    fun isOnboardingComplete_withLocalFalseCloudTrue_shouldReturnTrueAndUpdateLocal() = runTest {
        // Given
        coEvery { mockUserProfileDao.isOnboardingComplete(testUserProfile.userId) } returns false
        coEvery { mockUserProfileService.checkOnboardingStatus(testUserProfile.userId) } returns Result.success(true)
        coEvery { mockUserProfileDao.updateOnboardingStatus(testUserProfile.userId, true, any()) } returns 1

        // When
        val result = repository.isOnboardingComplete(testUserProfile.userId)

        // Then
        assertTrue(result)
        
        coVerify { mockUserProfileDao.isOnboardingComplete(testUserProfile.userId) }
        coVerify { mockUserProfileService.checkOnboardingStatus(testUserProfile.userId) }
        coVerify { mockUserProfileDao.updateOnboardingStatus(testUserProfile.userId, true, any()) }
    }

    @Test
    fun getUserProfileFlow_shouldReturnFlowFromDao() = runTest {
        // Given
        val entityFlow = flowOf(testUserProfileEntity)
        every { mockUserProfileDao.getUserProfileFlow(testUserProfile.userId) } returns entityFlow

        // When
        val flow = repository.getUserProfileFlow(testUserProfile.userId)

        // Then
        flow.collect { profile ->
            assertNotNull(profile)
            assertEquals(testUserProfile.userId, profile.userId)
        }
        
        verify { mockUserProfileDao.getUserProfileFlow(testUserProfile.userId) }
    }

    @Test
    fun syncUserProfile_withNetworkUnavailable_shouldReturnError() = runTest {
        // Given
        every { mockConnectivityManager.activeNetwork } returns null

        // When
        val result = repository.syncUserProfile(testUserProfile.userId)

        // Then
        assertTrue(result.isError())
        assertTrue(result.canRetry())
        assertEquals("Please check your internet connection", result.getErrorMessage())
    }

    @Test
    fun syncUserProfile_withNoLocalData_shouldReturnError() = runTest {
        // Given
        coEvery { mockUserProfileDao.getUserProfile(testUserProfile.userId) } returns null

        // When
        val result = repository.syncUserProfile(testUserProfile.userId)

        // Then
        assertTrue(result.isError())
        assertFalse(result.canRetry())
        assertEquals("No user data found locally", result.getErrorMessage())
    }

    @Test
    fun syncUserProfile_successful_shouldReturnSuccess() = runTest {
        // Given
        coEvery { mockUserProfileDao.getUserProfile(testUserProfile.userId) } returns testUserProfileEntity
        coEvery { mockUserProfileService.getUserProfile(testUserProfile.userId) } returns Result.success(testUserProfile)
        coEvery { mockUserProfileService.updateUserProfile(any()) } returns Result.success(testUserProfile)
        coEvery { mockUserProfileDao.markAsSynced(testUserProfile.userId, any()) } returns 1

        // When
        val result = repository.syncUserProfile(testUserProfile.userId)

        // Then
        assertTrue(result.isSuccess())
        
        coVerify { mockUserProfileDao.getUserProfile(testUserProfile.userId) }
        coVerify { mockUserProfileService.getUserProfile(testUserProfile.userId) }
        coVerify { mockUserProfileService.updateUserProfile(any()) }
        coVerify { mockUserProfileDao.markAsSynced(testUserProfile.userId, any()) }
    }

    @Test
    fun syncAllDirtyProfiles_withNoDirtyProfiles_shouldReturnSuccess() = runTest {
        // Given
        coEvery { mockUserProfileDao.getDirtyUserProfiles() } returns emptyList()

        // When
        val result = repository.syncAllDirtyProfiles()

        // Then
        assertTrue(result.isSuccess())
        
        coVerify { mockUserProfileDao.getDirtyUserProfiles() }
        coVerify(exactly = 0) { mockUserProfileService.updateUserProfile(any()) }
    }

    @Test
    fun syncAllDirtyProfiles_withSomeFailures_shouldReturnPartialError() = runTest {
        // Given
        val dirtyProfile1 = testUserProfileEntity.copy(userId = "user1", isDirty = true)
        val dirtyProfile2 = testUserProfileEntity.copy(userId = "user2", isDirty = true)
        
        coEvery { mockUserProfileDao.getDirtyUserProfiles() } returns listOf(dirtyProfile1, dirtyProfile2)
        coEvery { mockUserProfileService.updateUserProfile(any()) } returnsMany listOf(
            Result.success(testUserProfile),
            Result.failure(Exception("Sync failed"))
        )
        coEvery { mockUserProfileDao.markAsSynced("user1", any()) } returns 1

        // When
        val result = repository.syncAllDirtyProfiles()

        // Then
        assertTrue(result.isError())
        assertTrue(result.canRetry())
        assertEquals("Some data couldn't be synced. Will retry automatically.", result.getErrorMessage())
        
        coVerify(exactly = 2) { mockUserProfileService.updateUserProfile(any()) }
        coVerify(exactly = 1) { mockUserProfileDao.markAsSynced(any(), any()) }
    }

    @Test
    fun clearLocalCache_shouldDeleteFromDao() = runTest {
        // Given
        coEvery { mockUserProfileDao.getUserProfile(testUserProfile.userId) } returns testUserProfileEntity
        coEvery { mockUserProfileDao.deleteUserProfile(testUserProfileEntity) } returns 1

        // When
        val result = repository.clearLocalCache(testUserProfile.userId)

        // Then
        assertTrue(result.isSuccess)
        
        coVerify { mockUserProfileDao.getUserProfile(testUserProfile.userId) }
        coVerify { mockUserProfileDao.deleteUserProfile(testUserProfileEntity) }
    }

    @Test
    fun getSyncStatus_withDirtyProfile_shouldReturnPendingSync() = runTest {
        // Given
        val dirtyEntity = testUserProfileEntity.copy(isDirty = true)
        coEvery { mockUserProfileDao.getUserProfile(testUserProfile.userId) } returns dirtyEntity

        // When
        val result = repository.getSyncStatus(testUserProfile.userId)

        // Then
        assertEquals(SyncStatus.PENDING_SYNC, result)
    }

    @Test
    fun getSyncStatus_withSyncedProfile_shouldReturnSynced() = runTest {
        // Given
        val syncedEntity = testUserProfileEntity.copy(isDirty = false, lastSyncAt = Date())
        coEvery { mockUserProfileDao.getUserProfile(testUserProfile.userId) } returns syncedEntity

        // When
        val result = repository.getSyncStatus(testUserProfile.userId)

        // Then
        assertEquals(SyncStatus.SYNCED, result)
    }

    @Test
    fun getSyncStatus_withNoNetwork_shouldReturnOffline() = runTest {
        // Given
        every { mockConnectivityManager.activeNetwork } returns null

        // When
        val result = repository.getSyncStatus(testUserProfile.userId)

        // Then
        assertEquals(SyncStatus.OFFLINE, result)
    }

    @Test
    fun getSyncStatus_withNoProfile_shouldReturnOffline() = runTest {
        // Given
        coEvery { mockUserProfileDao.getUserProfile(testUserProfile.userId) } returns null

        // When
        val result = repository.getSyncStatus(testUserProfile.userId)

        // Then
        assertEquals(SyncStatus.OFFLINE, result)
    }
}