package com.vibehealth.android.core.validation

import android.util.Patterns
import com.vibehealth.android.domain.auth.ValidationResult
import java.util.regex.Pattern

/**
 * Comprehensive validation utilities following the Companion Principle
 * Provides user-friendly, supportive error messages
 */
object ValidationUtils {
    
    // Email validation pattern (more strict than Android's default)
    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    )
    
    // Password validation patterns
    private val PASSWORD_DIGIT_PATTERN = Pattern.compile(".*\\d.*")
    private val PASSWORD_LETTER_PATTERN = Pattern.compile(".*[a-zA-Z].*")
    private val PASSWORD_SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")
    
    /**
     * Validate email address with comprehensive checks
     */
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult(false, "Email is required")
            email.length > 254 -> ValidationResult(false, "Email address is too long")
            !EMAIL_PATTERN.matcher(email).matches() -> 
                ValidationResult(false, "Please enter a valid email address")
            else -> ValidationResult(true)
        }
    }
    
    /**
     * Validate password with progressive feedback
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, "Password is required")
            password.length < 8 -> ValidationResult(false, "Password must be at least 8 characters long")
            password.length > 128 -> ValidationResult(false, "Password is too long (maximum 128 characters)")
            !PASSWORD_LETTER_PATTERN.matcher(password).matches() -> 
                ValidationResult(false, "Password must contain at least one letter")
            !PASSWORD_DIGIT_PATTERN.matcher(password).matches() -> 
                ValidationResult(false, "Password must contain at least one number")
            password.contains(" ") -> ValidationResult(false, "Password cannot contain spaces")
            else -> ValidationResult(true)
        }
    }
    
    /**
     * Get password strength score (0-4)
     */
    fun getPasswordStrength(password: String): PasswordStrength {
        return PasswordStrength.evaluate(password)
    }
    
    /**
     * Validate password confirmation
     */
    fun validatePasswordConfirmation(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult(false, "Please confirm your password")
            password != confirmPassword -> ValidationResult(false, "Passwords do not match")
            else -> ValidationResult(true)
        }
    }
    
    /**
     * Validate display name
     */
    fun validateDisplayName(displayName: String): ValidationResult {
        return when {
            displayName.isBlank() -> ValidationResult(false, "Name is required")
            displayName.length < 2 -> ValidationResult(false, "Name must be at least 2 characters")
            displayName.length > 50 -> ValidationResult(false, "Name is too long (maximum 50 characters)")
            !displayName.matches(Regex("^[a-zA-Z\\s'-]+$")) -> 
                ValidationResult(false, "Name can only contain letters, spaces, hyphens, and apostrophes")
            else -> ValidationResult(true)
        }
    }
    
    /**
     * Validate terms acceptance
     */
    fun validateTermsAcceptance(isAccepted: Boolean): ValidationResult {
        return if (isAccepted) {
            ValidationResult(true)
        } else {
            ValidationResult(false, "Please accept the Terms & Conditions to continue")
        }
    }
    
    /**
     * Real-time email validation for UI feedback
     */
    fun validateEmailRealTime(email: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult(true) // Don't show error for empty field initially
            email.length > 254 -> ValidationResult(false, "Email address is too long")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                ValidationResult(false, "Please enter a valid email address")
            else -> ValidationResult(true)
        }
    }
    
    /**
     * Real-time password validation for UI feedback
     */
    fun validatePasswordRealTime(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(true) // Don't show error for empty field initially
            password.length < 8 -> ValidationResult(false, "Password must be at least 8 characters")
            !PASSWORD_LETTER_PATTERN.matcher(password).matches() -> 
                ValidationResult(false, "Password must contain at least one letter")
            !PASSWORD_DIGIT_PATTERN.matcher(password).matches() -> 
                ValidationResult(false, "Password must contain at least one number")
            else -> ValidationResult(true)
        }
    }
}

