package com.vibehealth.android.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations following Repository pattern
 */
interface AuthRepository {
    
    /**
     * Sign in user with email and password
     */
    suspend fun signIn(email: String, password: String): AuthResult
    
    /**
     * Sign up new user with email and password
     */
    suspend fun signUp(email: String, password: String): AuthResult
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): AuthResult
    
    /**
     * Get current authenticated user
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Check if user is currently authenticated
     */
    suspend fun isUserAuthenticated(): Boolean
    
    /**
     * Get authentication state as Flow for reactive updates
     */
    fun getAuthStateFlow(): Flow<AuthState>
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult
    
    /**
     * Validate email format
     */
    fun validateEmail(email: String): ValidationResult
    
    /**
     * Validate password strength
     */
    fun validatePassword(password: String): ValidationResult
    
    /**
     * Validate password confirmation
     */
    fun validatePasswordConfirmation(password: String, confirmPassword: String): ValidationResult
}