package com.vibehealth.android.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.vibehealth.android.domain.auth.AuthError
import com.vibehealth.android.domain.auth.AuthResult
import com.vibehealth.android.domain.auth.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication service that handles all Firebase Auth operations
 */
@Singleton
class FirebaseAuthService @Inject constructor() {
    
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Sign in user with email and password
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                AuthResult.Success(firebaseUser.toUser())
            } else {
                AuthResult.Error(Exception("Authentication failed: User is null"))
            }
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(e.toAuthException())
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }
    
    /**
     * Create user account with email and password
     */
    suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                AuthResult.Success(firebaseUser.toUser())
            } else {
                AuthResult.Error(Exception("Account creation failed: User is null"))
            }
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(e.toAuthException())
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): AuthResult {
        return try {
            firebaseAuth.signOut()
            AuthResult.Success(User("", null))
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }
    
    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
    
    /**
     * Get authentication state as Flow for reactive updates
     */
    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        
        firebaseAuth.addAuthStateListener(authStateListener)
        
        // Send current state immediately
        trySend(firebaseAuth.currentUser)
        
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
    
    /**
     * Check if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult.Success(User("", email))
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(e.toAuthException())
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }
}

/**
 * Extension function to convert FirebaseUser to domain User
 */
private fun FirebaseUser.toUser(): User {
    return User(
        uid = this.uid,
        email = this.email,
        displayName = this.displayName,
        isEmailVerified = this.isEmailVerified
    )
}

/**
 * Extension function to convert FirebaseAuthException to appropriate Exception
 */
private fun FirebaseAuthException.toAuthException(): Exception {
    val authError = when (this.errorCode) {
        "ERROR_INVALID_EMAIL" -> AuthError.InvalidEmail
        "ERROR_WRONG_PASSWORD" -> AuthError.InvalidCredentials
        "ERROR_USER_NOT_FOUND" -> AuthError.UserNotFound
        "ERROR_USER_DISABLED" -> AuthError.UserDisabled
        "ERROR_TOO_MANY_REQUESTS" -> AuthError.TooManyRequests
        "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EmailAlreadyExists
        "ERROR_WEAK_PASSWORD" -> AuthError.WeakPassword
        "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.NetworkError
        else -> AuthError.Unknown(this.message ?: "Unknown authentication error")
    }
    
    return Exception(authError.toUserMessage(), this)
}