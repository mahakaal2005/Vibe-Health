package com.vibehealth.android.data.auth


import com.google.firebase.firestore.FirebaseFirestore
import com.vibehealth.android.data.auth.SessionManager
import com.vibehealth.android.domain.auth.AuthRepository
import com.vibehealth.android.domain.auth.AuthResult
import com.vibehealth.android.domain.auth.AuthState
import com.vibehealth.android.domain.auth.User
import com.vibehealth.android.domain.auth.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository that handles authentication business logic
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) : AuthRepository {
    
    override suspend fun signIn(email: String, password: String): AuthResult {
        // Validate input before attempting authentication
        val emailValidation = validateEmail(email)
        if (!emailValidation.isValid) {
            return AuthResult.Error(Exception(emailValidation.errorMessage))
        }
        
        val passwordValidation = validatePassword(password)
        if (!passwordValidation.isValid) {
            return AuthResult.Error(Exception(passwordValidation.errorMessage))
        }
        
        val result = firebaseAuthService.signInWithEmailAndPassword(email, password)
        
        // Save session if sign in successful
        if (result is AuthResult.Success) {
            sessionManager.saveUserSession(result.user, rememberMe = true)
        }
        
        return result
    }
    
    override suspend fun signUp(email: String, password: String): AuthResult {
        // Validate input before attempting registration
        val emailValidation = validateEmail(email)
        if (!emailValidation.isValid) {
            return AuthResult.Error(Exception(emailValidation.errorMessage))
        }
        
        val passwordValidation = validatePassword(password)
        if (!passwordValidation.isValid) {
            return AuthResult.Error(Exception(passwordValidation.errorMessage))
        }
        
        val result = firebaseAuthService.createUserWithEmailAndPassword(email, password)
        
        // If registration successful, create user profile and save session
        if (result is AuthResult.Success) {
            try {
                createUserProfile(result.user)
                sessionManager.saveUserSession(result.user, rememberMe = true)
            } catch (e: Exception) {
                // Log error but don't fail the registration
                // User can complete profile later
            }
        }
        
        return result
    }
    
    override suspend fun signOut(): AuthResult {
        val result = firebaseAuthService.signOut()
        
        // Clear session on successful sign out
        if (result is AuthResult.Success) {
            sessionManager.clearUserSession()
        }
        
        return result
    }
    
    override suspend fun getCurrentUser(): User? {
        // Check session first, then Firebase
        sessionManager.handleSessionTimeout()
        
        return if (sessionManager.isUserLoggedIn()) {
            sessionManager.getStoredUser() ?: run {
                // Fallback to Firebase if session exists but no stored user
                val firebaseUser = firebaseAuthService.getCurrentUser()
                firebaseUser?.let {
                    User(
                        uid = it.uid,
                        email = it.email,
                        displayName = it.displayName,
                        isEmailVerified = it.isEmailVerified
                    )
                }
            }
        } else {
            null
        }
    }
    
    override suspend fun isUserAuthenticated(): Boolean {
        sessionManager.handleSessionTimeout()
        return sessionManager.isUserLoggedIn()
    }
    
    override fun getAuthStateFlow(): Flow<AuthState> {
        return firebaseAuthService.getAuthStateFlow().map { firebaseUser ->
            when {
                firebaseUser == null -> AuthState.NotAuthenticated
                else -> AuthState.Authenticated(
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        isEmailVerified = firebaseUser.isEmailVerified
                    )
                )
            }
        }
    }
    
    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        val emailValidation = validateEmail(email)
        if (!emailValidation.isValid) {
            return AuthResult.Error(Exception(emailValidation.errorMessage))
        }
        
        return firebaseAuthService.sendPasswordResetEmail(email)
    }
    
    override fun validateEmail(email: String): ValidationResult {
        return com.vibehealth.android.core.validation.ValidationUtils.validateEmail(email)
    }
    
    override fun validatePassword(password: String): ValidationResult {
        return com.vibehealth.android.core.validation.ValidationUtils.validatePassword(password)
    }
    
    override fun validatePasswordConfirmation(password: String, confirmPassword: String): ValidationResult {
        return com.vibehealth.android.core.validation.ValidationUtils.validatePasswordConfirmation(password, confirmPassword)
    }
    
    /**
     * Create user profile in Firestore after successful registration
     */
    private suspend fun createUserProfile(user: User) {
        val userProfile = mapOf(
            "uid" to user.uid,
            "email" to user.email,
            "displayName" to user.displayName,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        firestore.collection("users")
            .document(user.uid)
            .set(userProfile)
            .await()
    }
}