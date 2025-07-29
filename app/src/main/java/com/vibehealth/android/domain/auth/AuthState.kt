package com.vibehealth.android.domain.auth

/**
 * Represents the different authentication states in the application
 */
sealed class AuthState {
    /**
     * Authentication state is being determined (loading)
     */
    object Loading : AuthState()
    
    /**
     * User is authenticated and logged in
     */
    data class Authenticated(val user: User) : AuthState()
    
    /**
     * User is not authenticated
     */
    object NotAuthenticated : AuthState()
    
    /**
     * Authentication error occurred
     */
    data class Error(val message: String, val exception: Throwable? = null) : AuthState()
}

/**
 * Represents the result of authentication operations
 */
sealed class AuthResult {
    /**
     * Authentication operation was successful
     */
    data class Success(val user: User) : AuthResult()
    
    /**
     * Authentication operation failed
     */
    data class Error(val exception: Exception) : AuthResult()
    
    /**
     * Authentication operation is in progress
     */
    object Loading : AuthResult()
}

/**
 * Represents the UI state for authentication screens
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEmailValid: Boolean = true,
    val isPasswordValid: Boolean = true,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isFormValid: Boolean = false
)

/**
 * Represents the result of form validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Represents different types of authentication errors with user-friendly messages
 */
sealed class AuthError {
    object NetworkError : AuthError()
    object InvalidCredentials : AuthError()
    object UserNotFound : AuthError()
    object EmailAlreadyExists : AuthError()
    object WeakPassword : AuthError()
    object InvalidEmail : AuthError()
    object UserDisabled : AuthError()
    object TooManyRequests : AuthError()
    data class Unknown(val message: String) : AuthError()
    
    /**
     * Converts AuthError to user-friendly message following the Companion Principle
     */
    fun toUserMessage(): String = when (this) {
        is NetworkError -> "Please check your internet connection and try again"
        is InvalidCredentials -> "The email or password you entered is incorrect"
        is UserNotFound -> "No account found with this email address"
        is EmailAlreadyExists -> "An account with this email already exists"
        is WeakPassword -> "Password must be at least 8 characters long"
        is InvalidEmail -> "Please enter a valid email address"
        is UserDisabled -> "This account has been disabled. Please contact support"
        is TooManyRequests -> "Too many attempts. Please try again later"
        is Unknown -> "Something went wrong. Please try again"
    }
}

/**
 * Simple User data class for authentication
 */
data class User(
    val uid: String,
    val email: String?,
    val displayName: String? = null,
    val isEmailVerified: Boolean = false
)