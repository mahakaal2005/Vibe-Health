package com.vibehealth.android.data.auth

import com.google.firebase.firestore.FirebaseFirestore
import com.vibehealth.android.domain.auth.AuthResult
import com.vibehealth.android.domain.auth.User
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class AuthRepositoryImplTest {
    
    @Mock
    private lateinit var mockFirebaseAuthService: FirebaseAuthService
    
    @Mock
    private lateinit var mockFirestore: FirebaseFirestore
    
    @Mock
    private lateinit var mockSessionManager: SessionManager
    
    private lateinit var authRepository: AuthRepositoryImpl
    
    @BeforeEach
    fun setup() {
        authRepository = AuthRepositoryImpl(mockFirebaseAuthService, mockFirestore, mockSessionManager)
    }
    
    @Test
    fun `validateEmail should return valid for correct email format`() {
        // Given
        val validEmail = "test@example.com"
        
        // When
        val result = authRepository.validateEmail(validEmail)
        
        // Then
        assertTrue(result.isValid)
        assertEquals(null, result.errorMessage)
    }
    
    @Test
    fun `validateEmail should return invalid for incorrect email format`() {
        // Given
        val invalidEmail = "invalid-email"
        
        // When
        val result = authRepository.validateEmail(invalidEmail)
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Please enter a valid email address", result.errorMessage)
    }
    
    @Test
    fun `validateEmail should return invalid for blank email`() {
        // Given
        val blankEmail = ""
        
        // When
        val result = authRepository.validateEmail(blankEmail)
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Email is required", result.errorMessage)
    }
    
    @Test
    fun `validatePassword should return valid for strong password`() {
        // Given
        val strongPassword = "password123"
        
        // When
        val result = authRepository.validatePassword(strongPassword)
        
        // Then
        assertTrue(result.isValid)
        assertEquals(null, result.errorMessage)
    }
    
    @Test
    fun `validatePassword should return invalid for short password`() {
        // Given
        val shortPassword = "pass"
        
        // When
        val result = authRepository.validatePassword(shortPassword)
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Password must be at least 8 characters long", result.errorMessage)
    }
    
    @Test
    fun `validatePassword should return invalid for password without numbers`() {
        // Given
        val passwordWithoutNumbers = "password"
        
        // When
        val result = authRepository.validatePassword(passwordWithoutNumbers)
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Password must contain at least one number", result.errorMessage)
    }
    
    @Test
    fun `validatePassword should return invalid for password without letters`() {
        // Given
        val passwordWithoutLetters = "12345678"
        
        // When
        val result = authRepository.validatePassword(passwordWithoutLetters)
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Password must contain at least one letter", result.errorMessage)
    }
    
    @Test
    fun `validatePasswordConfirmation should return valid for matching passwords`() {
        // Given
        val password = "password123"
        val confirmPassword = "password123"
        
        // When
        val result = authRepository.validatePasswordConfirmation(password, confirmPassword)
        
        // Then
        assertTrue(result.isValid)
        assertEquals(null, result.errorMessage)
    }
    
    @Test
    fun `validatePasswordConfirmation should return invalid for non-matching passwords`() {
        // Given
        val password = "password123"
        val confirmPassword = "different123"
        
        // When
        val result = authRepository.validatePasswordConfirmation(password, confirmPassword)
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Passwords do not match", result.errorMessage)
    }
    
    @Test
    fun `signIn should return error for invalid email`() = runTest {
        // Given
        val invalidEmail = "invalid-email"
        val password = "password123"
        
        // When
        val result = authRepository.signIn(invalidEmail, password)
        
        // Then
        assertTrue(result is AuthResult.Error)
        assertTrue((result as AuthResult.Error).exception.message?.contains("valid email") == true)
    }
    
    @Test
    fun `signIn should return error for invalid password`() = runTest {
        // Given
        val email = "test@example.com"
        val invalidPassword = "short"
        
        // When
        val result = authRepository.signIn(email, invalidPassword)
        
        // Then
        assertTrue(result is AuthResult.Error)
        assertTrue((result as AuthResult.Error).exception.message?.contains("8 characters") == true)
    }
    
    @Test
    fun `signIn should call FirebaseAuthService for valid credentials`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val expectedUser = User("uid", email)
        
        whenever(mockFirebaseAuthService.signInWithEmailAndPassword(email, password))
            .thenReturn(AuthResult.Success(expectedUser))
        
        // When
        val result = authRepository.signIn(email, password)
        
        // Then
        assertTrue(result is AuthResult.Success)
        assertEquals(expectedUser, (result as AuthResult.Success).user)
    }
}