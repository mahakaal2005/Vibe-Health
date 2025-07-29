package com.vibehealth.android.core.validation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationUtilsTest {
    
    @Test
    fun `validateEmail should return valid for correct email formats`() {
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org",
            "123@example.com",
            "test.email.with+symbol@example.com"
        )
        
        validEmails.forEach { email ->
            val result = ValidationUtils.validateEmail(email)
            assertTrue(result.isValid, "Email $email should be valid")
        }
    }
    
    @Test
    fun `validateEmail should return invalid for incorrect email formats`() {
        val invalidEmails = listOf(
            "",
            "invalid-email",
            "@example.com",
            "test@",
            "test..test@example.com",
            "test@example",
            "test@.com",
            "test @example.com"
        )
        
        invalidEmails.forEach { email ->
            val result = ValidationUtils.validateEmail(email)
            assertFalse(result.isValid, "Email $email should be invalid")
        }
    }
    
    @Test
    fun `validateEmail should return error for blank email`() {
        val result = ValidationUtils.validateEmail("")
        assertFalse(result.isValid)
        assertEquals("Email is required", result.errorMessage)
    }
    
    @Test
    fun `validateEmail should return error for too long email`() {
        val longEmail = "a".repeat(250) + "@example.com"
        val result = ValidationUtils.validateEmail(longEmail)
        assertFalse(result.isValid)
        assertEquals("Email address is too long", result.errorMessage)
    }
    
    @Test
    fun `validatePassword should return valid for strong passwords`() {
        val validPasswords = listOf(
            "password123",
            "MyPassword1",
            "Test123456",
            "SecurePass1"
        )
        
        validPasswords.forEach { password ->
            val result = ValidationUtils.validatePassword(password)
            assertTrue(result.isValid, "Password $password should be valid")
        }
    }
    
    @Test
    fun `validatePassword should return invalid for weak passwords`() {
        val weakPasswords = mapOf(
            "" to "Password is required",
            "short" to "Password must be at least 8 characters long",
            "password" to "Password must contain at least one number",
            "12345678" to "Password must contain at least one letter",
            "pass word123" to "Password cannot contain spaces"
        )
        
        weakPasswords.forEach { (password, expectedError) ->
            val result = ValidationUtils.validatePassword(password)
            assertFalse(result.isValid, "Password $password should be invalid")
            assertEquals(expectedError, result.errorMessage)
        }
    }
    
    @Test
    fun `validatePassword should return error for too long password`() {
        val longPassword = "a".repeat(130) + "1"
        val result = ValidationUtils.validatePassword(longPassword)
        assertFalse(result.isValid)
        assertEquals("Password is too long (maximum 128 characters)", result.errorMessage)
    }
    
    @Test
    fun `validatePasswordConfirmation should return valid for matching passwords`() {
        val password = "password123"
        val confirmPassword = "password123"
        
        val result = ValidationUtils.validatePasswordConfirmation(password, confirmPassword)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `validatePasswordConfirmation should return invalid for non-matching passwords`() {
        val password = "password123"
        val confirmPassword = "different123"
        
        val result = ValidationUtils.validatePasswordConfirmation(password, confirmPassword)
        assertFalse(result.isValid)
        assertEquals("Passwords do not match", result.errorMessage)
    }
    
    @Test
    fun `validatePasswordConfirmation should return error for blank confirmation`() {
        val password = "password123"
        val confirmPassword = ""
        
        val result = ValidationUtils.validatePasswordConfirmation(password, confirmPassword)
        assertFalse(result.isValid)
        assertEquals("Please confirm your password", result.errorMessage)
    }
    
    @Test
    fun `getPasswordStrength should return correct strength levels`() {
        val testCases = mapOf(
            "" to PasswordStrength.Level.NONE,
            "weak" to PasswordStrength.Level.VERY_WEAK,
            "password" to PasswordStrength.Level.WEAK,
            "password1" to PasswordStrength.Level.WEAK,
            "Password1" to PasswordStrength.Level.MEDIUM,
            "Password123" to PasswordStrength.Level.STRONG,
            "Password123!" to PasswordStrength.Level.VERY_STRONG
        )
        
        testCases.forEach { (password, expectedLevel) ->
            val result = ValidationUtils.getPasswordStrength(password)
            assertEquals(expectedLevel, result.level, "Password $password should have strength $expectedLevel")
        }
    }
    
    @Test
    fun `validateDisplayName should return valid for correct names`() {
        val validNames = listOf(
            "John Doe",
            "Mary-Jane",
            "O'Connor",
            "Jean-Pierre",
            "Anna"
        )
        
        validNames.forEach { name ->
            val result = ValidationUtils.validateDisplayName(name)
            assertTrue(result.isValid, "Name $name should be valid")
        }
    }
    
    @Test
    fun `validateDisplayName should return invalid for incorrect names`() {
        val invalidNames = mapOf(
            "" to "Name is required",
            "A" to "Name must be at least 2 characters",
            "A".repeat(51) to "Name is too long (maximum 50 characters)",
            "John123" to "Name can only contain letters, spaces, hyphens, and apostrophes"
        )
        
        invalidNames.forEach { (name, expectedError) ->
            val result = ValidationUtils.validateDisplayName(name)
            assertFalse(result.isValid, "Name $name should be invalid")
            assertEquals(expectedError, result.errorMessage)
        }
    }
    
    @Test
    fun `validateTermsAcceptance should return valid when accepted`() {
        val result = ValidationUtils.validateTermsAcceptance(true)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `validateTermsAcceptance should return invalid when not accepted`() {
        val result = ValidationUtils.validateTermsAcceptance(false)
        assertFalse(result.isValid)
        assertEquals("Please accept the Terms & Conditions to continue", result.errorMessage)
    }
    
    @Test
    fun `validateEmailRealTime should not show error for empty field`() {
        val result = ValidationUtils.validateEmailRealTime("")
        assertTrue(result.isValid)
    }
    
    @Test
    fun `validateEmailRealTime should show error for invalid format`() {
        val result = ValidationUtils.validateEmailRealTime("invalid-email")
        assertFalse(result.isValid)
        assertEquals("Please enter a valid email address", result.errorMessage)
    }
    
    @Test
    fun `validatePasswordRealTime should not show error for empty field`() {
        val result = ValidationUtils.validatePasswordRealTime("")
        assertTrue(result.isValid)
    }
    
    @Test
    fun `validatePasswordRealTime should show error for short password`() {
        val result = ValidationUtils.validatePasswordRealTime("short")
        assertFalse(result.isValid)
        assertEquals("Password must be at least 8 characters", result.errorMessage)
    }
}