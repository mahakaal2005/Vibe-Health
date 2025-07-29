package com.vibehealth.android.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vibehealth.android.domain.auth.AuthRepository
import com.vibehealth.android.domain.auth.AuthResult
import com.vibehealth.android.domain.auth.AuthState
import com.vibehealth.android.domain.auth.User
import com.vibehealth.android.domain.auth.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
class AuthViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var mockAuthRepository: AuthRepository
    
    private lateinit var authViewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock behaviors
        whenever(mockAuthRepository.getAuthStateFlow()).thenReturn(flowOf(AuthState.NotAuthenticated))
        
        authViewModel = AuthViewModel(mockAuthRepository)
    }
    
    @Test
    fun `validateEmail should return valid result for correct email`() {
        // Given
        val validEmail = "test@example.com"
        whenever(mockAuthRepository.validateEmail(validEmail))
            .thenReturn(ValidationResult(true))
        
        // When
        val result = authViewModel.validateEmail(validEmail)
        
        // Then
        assertTrue(result.isValid)
        verify(mockAuthRepository).validateEmail(validEmail)
    }
    
    @Test
    fun `validateEmail should return invalid result for incorrect email`() {
        // Given
        val invalidEmail = "invalid-email"
        val errorMessage = "Please enter a valid email address"
        whenever(mockAuthRepository.validateEmail(invalidEmail))
            .thenReturn(ValidationResult(false, errorMessage))
        
        // When
        val result = authViewModel.validateEmail(invalidEmail)
        
        // Then
        assertFalse(result.isValid)
        assertEquals(errorMessage, result.errorMessage)
        verify(mockAuthRepository).validateEmail(invalidEmail)
    }
    
    @Test
    fun `validatePassword should return valid result for strong password`() {
        // Given
        val strongPassword = "password123"
        whenever(mockAuthRepository.validatePassword(strongPassword))
            .thenReturn(ValidationResult(true))
        
        // When
        val result = authViewModel.validatePassword(strongPassword)
        
        // Then
        assertTrue(result.isValid)
        verify(mockAuthRepository).validatePassword(strongPassword)
    }
    
    @Test
    fun `validatePassword should return invalid result for weak password`() {
        // Given
        val weakPassword = "weak"
        val errorMessage = "Password must be at least 8 characters long"
        whenever(mockAuthRepository.validatePassword(weakPassword))
            .thenReturn(ValidationResult(false, errorMessage))
        
        // When
        val result = authViewModel.validatePassword(weakPassword)
        
        // Then
        assertFalse(result.isValid)
        assertEquals(errorMessage, result.errorMessage)
        verify(mockAuthRepository).validatePassword(weakPassword)
    }
    
    @Test
    fun `signIn should update UI state to loading then success`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val user = User("uid", email)
        
        whenever(mockAuthRepository.signIn(email, password))
            .thenReturn(AuthResult.Success(user))
        
        // When
        authViewModel.signIn(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(mockAuthRepository).signIn(email, password)
        assertEquals(AuthState.Authenticated(user), authViewModel.authState.value)
    }
    
    @Test
    fun `signIn should update UI state to loading then error on failure`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "wrongpassword"
        val exception = Exception("Invalid credentials")
        
        whenever(mockAuthRepository.signIn(email, password))
            .thenReturn(AuthResult.Error(exception))
        
        // When
        authViewModel.signIn(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(mockAuthRepository).signIn(email, password)
        assertTrue(authViewModel.authState.value is AuthState.Error)
    }
    
    @Test
    fun `signUp should call repository with correct parameters`() = runTest {
        // Given
        val email = "newuser@example.com"
        val password = "password123"
        val confirmPassword = "password123"
        val user = User("uid", email)
        
        whenever(mockAuthRepository.validatePasswordConfirmation(password, confirmPassword))
            .thenReturn(ValidationResult(true))
        whenever(mockAuthRepository.signUp(email, password))
            .thenReturn(AuthResult.Success(user))
        
        // When
        authViewModel.signUp(email, password, confirmPassword)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(mockAuthRepository).validatePasswordConfirmation(password, confirmPassword)
        verify(mockAuthRepository).signUp(email, password)
        assertEquals(AuthState.Authenticated(user), authViewModel.authState.value)
    }
    
    @Test
    fun `signUp should fail when passwords don't match`() = runTest {
        // Given
        val email = "newuser@example.com"
        val password = "password123"
        val confirmPassword = "different123"
        val errorMessage = "Passwords do not match"
        
        whenever(mockAuthRepository.validatePasswordConfirmation(password, confirmPassword))
            .thenReturn(ValidationResult(false, errorMessage))
        
        // When
        authViewModel.signUp(email, password, confirmPassword)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(mockAuthRepository).validatePasswordConfirmation(password, confirmPassword)
        // Should not call signUp if validation fails
        verify(mockAuthRepository, org.mockito.kotlin.never()).signUp(email, password)
    }
    
    @Test
    fun `signOut should call repository and update auth state`() = runTest {
        // Given
        whenever(mockAuthRepository.signOut())
            .thenReturn(AuthResult.Success(User("", null)))
        
        // When
        authViewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(mockAuthRepository).signOut()
        assertEquals(AuthState.NotAuthenticated, authViewModel.authState.value)
    }
    
    @Test
    fun `updateEmail should update email field and validate`() {
        // Given
        val email = "test@example.com"
        whenever(mockAuthRepository.validateEmail(email))
            .thenReturn(ValidationResult(true))
        
        // When
        authViewModel.updateEmail(email)
        
        // Then
        assertEquals(email, authViewModel.email.value)
        verify(mockAuthRepository).validateEmail(email)
    }
    
    @Test
    fun `clearError should clear error message in UI state`() {
        // When
        authViewModel.clearError()
        
        // Then
        assertEquals(null, authViewModel.uiState.value.errorMessage)
    }
}