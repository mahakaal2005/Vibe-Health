package com.vibehealth.android.data.user.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserProfileDao
 */
@RunWith(AndroidJUnit4::class)
class UserProfileDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var userProfileDao: UserProfileDao

    private val testUserProfile = UserProfileEntity(
        userId = "test_user_123",
        email = "test@example.com",
        displayName = "Test User",
        firstName = "Test",
        lastName = "User",
        birthday = Date(System.currentTimeMillis() - 25 * 365 * 24 * 60 * 60 * 1000L), // 25 years ago
        gender = Gender.MALE,
        unitSystem = UnitSystem.METRIC,
        heightInCm = 175,
        weightInKg = 70.5,
        hasCompletedOnboarding = true,
        createdAt = Date(),
        updatedAt = Date(),
        lastSyncAt = null,
        isDirty = false
    )

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        userProfileDao = database.userProfileDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertUserProfile_shouldInsertSuccessfully() = runTest {
        // When
        val result = userProfileDao.insertUserProfile(testUserProfile)

        // Then
        assertTrue(result > 0)
        
        val retrievedProfile = userProfileDao.getUserProfile(testUserProfile.userId)
        assertNotNull(retrievedProfile)
        assertEquals(testUserProfile.userId, retrievedProfile.userId)
        assertEquals(testUserProfile.email, retrievedProfile.email)
        assertEquals(testUserProfile.displayName, retrievedProfile.displayName)
    }

    @Test
    fun getUserProfile_withNonExistentUser_shouldReturnNull() = runTest {
        // When
        val result = userProfileDao.getUserProfile("non_existent_user")

        // Then
        assertNull(result)
    }

    @Test
    fun updateUserProfile_shouldUpdateSuccessfully() = runTest {
        // Given
        userProfileDao.insertUserProfile(testUserProfile)
        val updatedProfile = testUserProfile.copy(
            displayName = "Updated Name",
            weightInKg = 75.0,
            updatedAt = Date()
        )

        // When
        val result = userProfileDao.updateUserProfile(updatedProfile)

        // Then
        assertEquals(1, result)
        
        val retrievedProfile = userProfileDao.getUserProfile(testUserProfile.userId)
        assertNotNull(retrievedProfile)
        assertEquals("Updated Name", retrievedProfile.displayName)
        assertEquals(75.0, retrievedProfile.weightInKg)
    }

    @Test
    fun deleteUserProfile_shouldDeleteSuccessfully() = runTest {
        // Given
        userProfileDao.insertUserProfile(testUserProfile)

        // When
        val result = userProfileDao.deleteUserProfile(testUserProfile)

        // Then
        assertEquals(1, result)
        
        val retrievedProfile = userProfileDao.getUserProfile(testUserProfile.userId)
        assertNull(retrievedProfile)
    }

    @Test
    fun getUserProfileFlow_shouldEmitUpdates() = runTest {
        // Given
        val flow = userProfileDao.getUserProfileFlow(testUserProfile.userId)

        // When - Initially no profile
        var currentProfile = flow.first()
        assertNull(currentProfile)

        // Insert profile
        userProfileDao.insertUserProfile(testUserProfile)
        currentProfile = flow.first()
        
        // Then
        assertNotNull(currentProfile)
        assertEquals(testUserProfile.userId, currentProfile.userId)
    }

    @Test
    fun isOnboardingComplete_shouldReturnCorrectStatus() = runTest {
        // Given
        val incompleteProfile = testUserProfile.copy(hasCompletedOnboarding = false)
        userProfileDao.insertUserProfile(incompleteProfile)

        // When
        val isComplete = userProfileDao.isOnboardingComplete(testUserProfile.userId)

        // Then
        assertEquals(false, isComplete)
    }

    @Test
    fun updateOnboardingStatus_shouldUpdateSuccessfully() = runTest {
        // Given
        userProfileDao.insertUserProfile(testUserProfile.copy(hasCompletedOnboarding = false))
        val updateTime = System.currentTimeMillis()

        // When
        val result = userProfileDao.updateOnboardingStatus(testUserProfile.userId, true, updateTime)

        // Then
        assertEquals(1, result)
        
        val isComplete = userProfileDao.isOnboardingComplete(testUserProfile.userId)
        assertEquals(true, isComplete)
    }

    @Test
    fun userProfileExists_shouldReturnCorrectStatus() = runTest {
        // When - Before insertion
        var exists = userProfileDao.userProfileExists(testUserProfile.userId)
        assertFalse(exists)

        // Insert profile
        userProfileDao.insertUserProfile(testUserProfile)
        
        // When - After insertion
        exists = userProfileDao.userProfileExists(testUserProfile.userId)
        assertTrue(exists)
    }

    @Test
    fun getDirtyUserProfiles_shouldReturnOnlyDirtyProfiles() = runTest {
        // Given
        val cleanProfile = testUserProfile.copy(userId = "clean_user", isDirty = false)
        val dirtyProfile = testUserProfile.copy(userId = "dirty_user", isDirty = true)
        
        userProfileDao.insertUserProfile(cleanProfile)
        userProfileDao.insertUserProfile(dirtyProfile)

        // When
        val dirtyProfiles = userProfileDao.getDirtyUserProfiles()

        // Then
        assertEquals(1, dirtyProfiles.size)
        assertEquals("dirty_user", dirtyProfiles[0].userId)
        assertTrue(dirtyProfiles[0].isDirty)
    }

    @Test
    fun markAsSynced_shouldUpdateSyncStatus() = runTest {
        // Given
        val dirtyProfile = testUserProfile.copy(isDirty = true)
        userProfileDao.insertUserProfile(dirtyProfile)
        val syncTime = System.currentTimeMillis()

        // When
        val result = userProfileDao.markAsSynced(testUserProfile.userId, syncTime)

        // Then
        assertEquals(1, result)
        
        val updatedProfile = userProfileDao.getUserProfile(testUserProfile.userId)
        assertNotNull(updatedProfile)
        assertFalse(updatedProfile.isDirty)
        assertNotNull(updatedProfile.lastSyncAt)
    }

    @Test
    fun markAsDirty_shouldUpdateDirtyFlag() = runTest {
        // Given
        val cleanProfile = testUserProfile.copy(isDirty = false)
        userProfileDao.insertUserProfile(cleanProfile)

        // When
        val result = userProfileDao.markAsDirty(testUserProfile.userId)

        // Then
        assertEquals(1, result)
        
        val updatedProfile = userProfileDao.getUserProfile(testUserProfile.userId)
        assertNotNull(updatedProfile)
        assertTrue(updatedProfile.isDirty)
    }

    @Test
    fun upsertUserProfile_shouldInsertWhenNotExists() = runTest {
        // When
        userProfileDao.upsertUserProfile(testUserProfile)

        // Then
        val retrievedProfile = userProfileDao.getUserProfile(testUserProfile.userId)
        assertNotNull(retrievedProfile)
        assertEquals(testUserProfile.userId, retrievedProfile.userId)
        assertTrue(retrievedProfile.isDirty) // Should be marked as dirty after upsert
    }

    @Test
    fun upsertUserProfile_shouldUpdateWhenExists() = runTest {
        // Given
        userProfileDao.insertUserProfile(testUserProfile)
        val updatedProfile = testUserProfile.copy(displayName = "Updated Name")

        // When
        userProfileDao.upsertUserProfile(updatedProfile)

        // Then
        val retrievedProfile = userProfileDao.getUserProfile(testUserProfile.userId)
        assertNotNull(retrievedProfile)
        assertEquals("Updated Name", retrievedProfile.displayName)
        assertTrue(retrievedProfile.isDirty) // Should be marked as dirty after upsert
    }

    @Test
    fun getUserProfileCount_shouldReturnCorrectCount() = runTest {
        // Given
        val profile1 = testUserProfile.copy(userId = "user1")
        val profile2 = testUserProfile.copy(userId = "user2")
        
        userProfileDao.insertUserProfile(profile1)
        userProfileDao.insertUserProfile(profile2)

        // When
        val count = userProfileDao.getUserProfileCount()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun deleteAllUserProfiles_shouldDeleteAllProfiles() = runTest {
        // Given
        val profile1 = testUserProfile.copy(userId = "user1")
        val profile2 = testUserProfile.copy(userId = "user2")
        
        userProfileDao.insertUserProfile(profile1)
        userProfileDao.insertUserProfile(profile2)

        // When
        val result = userProfileDao.deleteAllUserProfiles()

        // Then
        assertEquals(2, result)
        
        val count = userProfileDao.getUserProfileCount()
        assertEquals(0, count)
    }

    @Test
    fun getDatabaseStats_shouldReturnCorrectStatistics() = runTest {
        // Given
        val completedProfile = testUserProfile.copy(userId = "completed", hasCompletedOnboarding = true, isDirty = false)
        val incompleteProfile = testUserProfile.copy(userId = "incomplete", hasCompletedOnboarding = false, isDirty = true)
        
        userProfileDao.insertUserProfile(completedProfile)
        userProfileDao.insertUserProfile(incompleteProfile)

        // When
        val stats = userProfileDao.getDatabaseStats()

        // Then
        assertNotNull(stats)
        assertEquals(2, stats.totalProfiles)
        assertEquals(1, stats.completedOnboarding)
        assertEquals(1, stats.dirtyProfiles)
        assertNotNull(stats.oldestProfile)
        assertNotNull(stats.latestUpdate)
    }

    @Test
    fun typeConverters_shouldHandleEnumsCorrectly() = runTest {
        // Given
        val profileWithOtherGender = testUserProfile.copy(
            gender = Gender.OTHER,
            unitSystem = UnitSystem.IMPERIAL
        )

        // When
        userProfileDao.insertUserProfile(profileWithOtherGender)
        val retrievedProfile = userProfileDao.getUserProfile(testUserProfile.userId)

        // Then
        assertNotNull(retrievedProfile)
        assertEquals(Gender.OTHER, retrievedProfile.gender)
        assertEquals(UnitSystem.IMPERIAL, retrievedProfile.unitSystem)
    }

    @Test
    fun typeConverters_shouldHandleDatesCorrectly() = runTest {
        // Given
        val specificDate = Date(1640995200000L) // January 1, 2022
        val profileWithSpecificDate = testUserProfile.copy(
            birthday = specificDate,
            createdAt = specificDate,
            updatedAt = specificDate
        )

        // When
        userProfileDao.insertUserProfile(profileWithSpecificDate)
        val retrievedProfile = userProfileDao.getUserProfile(testUserProfile.userId)

        // Then
        assertNotNull(retrievedProfile)
        assertEquals(specificDate.time, retrievedProfile.birthday?.time)
        assertEquals(specificDate.time, retrievedProfile.createdAt.time)
        assertEquals(specificDate.time, retrievedProfile.updatedAt.time)
    }
}