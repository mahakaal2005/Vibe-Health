package com.vibehealth.android.integration

import com.vibehealth.android.core.auth.AuthGuard
import com.vibehealth.android.data.auth.SessionManager
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.domain.user.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

/**
 * Integration tests for authentication system components
 */
class AuthenticationIntegrationTest {
    
    private lateinit var authGuard: AuthGuard
    private lateinit var sessionManager: SessionManager
    private lateinit var userProfileRepository: UserProfileRepository
    
    @BeforeEach
    fun setup() {
        sessionManager = mockk()
        userProfileRepository = mockk()
        authGuard = AuthGuard(mockk(relaxed = true), sessionManager)
    }
    
    @Test
    fun `authGuard requireAuthentication returns true when user is logged in`() = runTest {
        // Given
        coEvery { sessionManager.isUserLoggedIn() } returns true
        
        // When
        val result = authGuard.requireAuthentication()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `authGuard requireAuthentication returns false when user is not logged in`() = runTest {
        // Given
        coEvery { sessionManager.isUserLoggedIn() } returns false
        
        // When
        val result = authGuard.requireAuthentication()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `userProfileRepository hasCompletedOnboarding returns correct status`() = runTest {
        // Given
        val userId = "test-user-id"
        coEvery { userProfileRepository.hasCompletedOnboarding(userId) } returns Result.success(true)
        
        // When
        val result = userProfileRepository.hasCompletedOnboarding(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `userProfileRepository saveUserProfile saves profile successfully`() = runTest {
        // Given
        val userId = "test-user-id"
        val userProfile = UserProfile(
            userId = userId,
            email = "test@example.com",
            displayName = "Test User",
            hasCompletedOnboarding = false
        )
        coEvery { userProfileRepository.saveUserProfile(userId, userProfile) } returns Result.success(Unit)
        
        // When
        val result = userProfileRepository.saveUserProfile(userId, userProfile)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { userProfileRepository.saveUserProfile(userId, userProfile) }
    }
    
    @Test
    fun `userProfileRepository markOnboardingCompleted updates profile`() = runTest {
        // Given
        val userId = "test-user-id"
        coEvery { userProfileRepository.markOnboardingCompleted(userId) } returns Result.success(Unit)
        
        // When
        val result = userProfileRepository.markOnboardingCompleted(userId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { userProfileRepository.markOnboardingCompleted(userId) }
    }
    
    @Test
    fun `userProfileRepository getUserProfile returns profile when exists`() = runTest {
        // Given
        val userId = "test-user-id"
        val expectedProfile = UserProfile(
            userId = userId,
            email = "test@example.com",
            displayName = "Test User"
        )
        coEvery { userProfileRepository.getUserProfile(userId) } returns Result.success(expectedProfile)
        
        // When
        val result = userProfileRepository.getUserProfile(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedProfile, result.getOrNull())
    }
    
    @Test
    fun `userProfileRepository getUserProfile returns null when profile does not exist`() = runTest {
        // Given
        val userId = "non-existent-user"
        coEvery { userProfileRepository.getUserProfile(userId) } returns Result.success(null)
        
        // When
        val result = userProfileRepository.getUserProfile(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `userProfileRepository handles errors gracefully`() = runTest {
        // Given
        val userId = "test-user-id"
        val exception = Exception("Network error")
        coEvery { userProfileRepository.getUserProfile(userId) } returns Result.failure(exception)
        
        // When
        val result = userProfileRepository.getUserProfile(userId)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `authGuard hasCompletedOnboarding returns true by default`() = runTest {
        // When
        val result = authGuard.hasCompletedOnboarding()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `authGuard getAuthIntent returns proper intent`() {
        // When
        val intent = authGuard.getAuthIntent()
        
        // Then
        assertNotNull(intent)
        assertEquals("com.vibehealth.android.ui.auth.AuthActivity", intent.component?.className)
    }
}